package ru.mirea.dikanev.nik.os;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Parser implements Runnable {

    private volatile PoolData poolData;
    private volatile WebDriver driver;
    private boolean checkParse;
    private volatile boolean isPause = false;

    private final String url = "https://vk.com";
    private final String pathPhoto = "./src/main/java/ru/mirea/dikanev/nik/os/img/";

    private Map<String, LastElement> lastDataParse = new HashMap<>();

    Parser(PoolData poolData, String login, String pwd) {
        PrivateConfig.login = login;
        PrivateConfig.password = pwd;

        this.poolData = poolData;
        driver = new ChromeDriver();
    }

    //Подготовка к парсингу
    public void init() throws Exception {
        register();
    }

    //Вход в аккаунт
    private void register() throws Exception {
        driver.get(url);

        Thread.sleep(2000);
        driver.findElement(By.id("index_email")).sendKeys(PrivateConfig.login);

        Thread.sleep(2000);
        driver.findElement(By.id("index_pass")).sendKeys(PrivateConfig.password);
        driver.findElement(By.className("login_mobile_header")).click();

        Thread.sleep(1000);
        driver.findElement(By.id("index_login_button")).click();

//        WebDriverWait wait = new WebDriverWait(driver, 10);
//        wait.until((By.id("menu")));
        while (true) {
            Thread.sleep(2000);
            System.out.println(driver.getCurrentUrl());
            if (driver.getCurrentUrl().equals("https://vk.com/")) {
                continue;
            } else if(!driver.getCurrentUrl().equals("https://vk.com/feed")){
                driver.quit();
                throw new Exception("Не верный логин или пароль");
            } else {
                break;
            }
        }

    }

    //Крутится и вызывает парсинг новых элементов. Работа с браузером.
    @Override
    public void run() {
        int parsNewsCount = 0;
        checkParse = true;
        while (checkParse) {
            if(isPause){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignore) {
                }
                continue;
            }
            try {
                parsNewsCount = parseNews(parsNewsCount);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("\n\n");
            }

            int x = 200;
            int y = 200;
            ((JavascriptExecutor) driver).executeScript("window.scrollBy(" + x + "," + y + ");");


        }
    }

    //Парсинг элементов
    private int parseNews(int parsNewsCount) {
        List<WebElement> elements = driver.findElements(By.className("feed_row"));
        if (parsNewsCount < elements.size()) {
            elements = elements.subList(parsNewsCount, elements.size());
        } else {
            return parsNewsCount;
        }

        //Итерация по только что распарсенным элементам
        //Вместо  for (WebElement element : elements)
        elements.forEach((element) -> {
            try {
                LastElement oldElement = lastDataParse.computeIfAbsent("feed_row", k -> new LastElement());
                if (oldElement.element != null) {
                    String oldId = oldElement.element.findElement(By.className("_post")).getAttribute("id");
                    String newId = element.findElement(By.className("_post")).getAttribute("id");

                    if (oldId.equals(newId)) {
                        return;
                    }
                }

                LastElement lastElement = new LastElement<DataParse>();
                lastElement.element = element;

                if (oldElement.dataParse == null) {
                    oldElement.dataParse = new DataParse("-1");
                }
                DataParse news = new DataParse(Integer.toString(Integer.valueOf(oldElement.dataParse.getId()) + 1));
                news.photo.setDirPhoto(pathPhoto);

                List<Thread> workParseThData = new ArrayList<>();
                //Парсинг текста поста
                workParseThData.add( new Thread(() -> news.text = (DataParse.NewsText) news.text.parse(element, poolData)));
                //Парсинг ссылки на пост
                workParseThData.add( new Thread(() -> news.link = (DataParse.NewsLink) news.link.parse(element, poolData)));
                //Парсинг картинок в посте
                workParseThData.add( new Thread(() -> news.photo = (DataParse.NewsPhoto) news.photo.parse(element, poolData)));

                workParseThData.forEach(Thread::start);
                workParseThData.forEach((t) -> {
                    try {
                        t.join();
                    } catch (InterruptedException e) {
                    }
                });

                lastElement.dataParse = news;
                lastDataParse.remove("feed_row");
                lastDataParse.put("feed_row", lastElement);

            } catch (Exception e) {
            }

        });

        return parsNewsCount + elements.size();
    }

    public void exit() {
        driver.quit();
    }

    //Ставит парсинг на паузу
    public void setPause(boolean pauseMod){
        this.isPause = pauseMod;
    }

    //Прошлый распарсенный элемент
    class LastElement<T extends IMasterDataParse> {
        public WebElement element;
        public T dataParse;
    }

    public static class PrivateConfig {
        public static String login = "";
        public static String password = "";
    }
}
