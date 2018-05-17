package ru.mirea.dikanev.nik.os;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.*;

public class Parser implements Runnable {

    private volatile PoolData poolData;
    private volatile WebDriver driver;
    private boolean checkParse;
    private volatile boolean isPause = false;

    private final String url = "https://www.instagram.com/";
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
        driver.findElement(By.className("_g9ean")).findElement(By.tagName("a")).click();

        Thread.sleep(2000);
        List<WebElement> elements = driver.findElements(By.className("_ph6vk"));
        elements.get(0).sendKeys(PrivateConfig.login);

        Thread.sleep(2000);
        elements.get(1).sendKeys(PrivateConfig.password);
        driver.findElement(By.className("_8fi2q")).click();

        Thread.sleep(1000);
        driver.findElement(By.className("_gexxb")).click();

        Thread.sleep(2000);
//        driver.get("https://www.instagram.com/ksenia_dikaneva/");
        driver.get("https://www.instagram.com/_agentgirl_/");

    }

    //Крутится и вызывает парсинг новых элементов. Работа с браузером.
    @Override
    public void run() {
        int parsNewsCount = 0;
        checkParse = true;
        while (checkParse) {
            if (isPause) {
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
        List<WebElement> elements = driver.findElements(By.className("_mck9w"));
        if (parsNewsCount < elements.size()) {
            elements = elements.subList(parsNewsCount, elements.size());
        } else {
            return parsNewsCount;
        }

        elements.forEach((element) -> {
            try {
                Thread.sleep(1000);
                element.click();

                DataParse news = new DataParse(Integer.toString(element.findElement(By.tagName("img")).getAttribute("src").hashCode()));
                news.photo.setDirPhoto(pathPhoto);

                List<Thread> workParseThData = new ArrayList<>();
                //Парсинг лайков
                Boolean isParseComment = false;
                workParseThData.add(new Thread(() -> {
                    synchronized (isParseComment) {
                        try {
                            isParseComment.wait();
                        } catch (InterruptedException ignore) {
                        }
                    }
                    WebElement elLikes = waitShowWebElement("_nzn1h");
                    if(elLikes == null){
                        return;
                    }
                    elLikes.click();
                    elLikes = waitShowWebElement("_1u2kv");
                    waitShowWebElement("_f5wpw");
                    if(elLikes == null){
                        return;
                    }
                    news.text = (DataParse.NewsLikes) news.text.parse(elLikes, poolData);
                }));
                //Парсинг картинок в посте
                workParseThData.add(new Thread(() -> news.photo = (DataParse.NewsPhoto) news.photo.parse(element, poolData)));
                //Парсинг comments
                workParseThData.add(new Thread(() -> {
                    WebElement elComments = waitShowWebElement("_784q7");
                    if (elComments == null) {
                        synchronized (isParseComment)  {
                            isParseComment.notifyAll();
                        }
                        return;
                    }
                    news.comments = (DataParse.NewsComments) news.comments.parse(elComments, poolData);
                    synchronized (isParseComment)  {
                        isParseComment.notifyAll();
                    }
                }));

                workParseThData.forEach(Thread::start);
                workParseThData.forEach((t) -> {
                    try {
                        t.join();
                    } catch (InterruptedException ignored) {
                    }
                });

                Thread.sleep(1000);
                driver.findElements(By.tagName("button"))
                        .stream()
                        .filter((e) -> e.getText().equals("Закрыть"))
                        .forEach(WebElement::click);
            } catch (InterruptedException ignore) {
            }
        });

        return parsNewsCount + elements.size();
    }

    public void exit() {
        driver.quit();
    }

    //Ставит парсинг на паузу
    public void setPause(boolean pauseMod) {
        this.isPause = pauseMod;
    }

    //Ожидание появление элемента
    private WebElement waitShowWebElement(String className){
        List<WebElement> elements = null;
        long startTime = System.currentTimeMillis();
        while(System.currentTimeMillis() - startTime < 5000){
            elements = driver.findElements(By.className(className));
            if (elements.size() > 0) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            }
        }

        return elements.size() > 0 ? elements.get(0) : null;
    }

    //Прошлый распарсенный элемент
    class LastElement<T extends IMasterDataParse> {
        public WebElement element;
        public T dataParse;
    }

    public static class PrivateConfig {
        public static String login = "wofimin@bitwhites.top";
        public static String password = "Фыва12345";
    }
}
