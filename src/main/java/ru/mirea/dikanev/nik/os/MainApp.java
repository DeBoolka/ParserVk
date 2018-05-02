package ru.mirea.dikanev.nik.os;

import org.apache.camel.main.Main;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

/**
 * A Camel Application
 */
public class MainApp {

    /**
     * A main() so we can easily run these routing rules in our IDE
     */
    public static void main(String... args) throws Exception {
        System.out.println("Starting...\n\n\n\n");

        System.setProperty("webdriver.chrome.driver", "B:\\JavaProjects\\ParseDriver\\src\\data\\chromedriver.exe");

        WebParser parser = new WebParser("", "");
        parser.start();
    }

}

