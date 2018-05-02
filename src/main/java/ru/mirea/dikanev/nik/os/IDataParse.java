package ru.mirea.dikanev.nik.os;

import org.openqa.selenium.WebElement;

public interface IDataParse {

    //Группы объекта
    int TEXT = 1;
    int LINK = 2;
    int IMG = 3;

    //Возвращает тип объекта
    int getGroup();

    //Возвращает id
    String getId();

    //Метод который парсит WebElement
    IDataParse parse(WebElement element, PoolData poolData);

    String getHtml();

}
