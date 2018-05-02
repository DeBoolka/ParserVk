package ru.mirea.dikanev.nik.os;

import java.io.File;
import java.util.ArrayList;

public class WebParser {
    private volatile PoolData poolData;
    private volatile Parser parser;
    private volatile WebBuilder webBuilder;
    private volatile DataConsumer dataConsumer;

    private String login = "";
    private String pwd = "";

    public WebParser(String login, String pwd){
        this.login = login;
        this.pwd = pwd;

        init();
    }

    private void init(){
        poolData = new PoolData();
    }

    //Стартует парсинг и описывает, что куда парсим
    public void start() throws Exception{
        Parser parser = new Parser(poolData, login, pwd);
        parser.init();

        Thread thParser = new Thread(parser);
        thParser.setDaemon(true);
        thParser.start();

        DataConsumer dataConsumer = new DataConsumer(poolData);

        File fileText = new File(".\\src\\main\\java\\ru\\mirea\\dikanev\\nik\\os\\data\\ParseText.json");
        File fileLink = new File(".\\src\\main\\java\\ru\\mirea\\dikanev\\nik\\os\\data\\ParseLink.json");
        File fileImg = new File(".\\src\\main\\java\\ru\\mirea\\dikanev\\nik\\os\\data\\ParseImg.json");

        dataConsumer.run(IDataParse.TEXT, fileText);
        dataConsumer.run(IDataParse.LINK, fileLink);
        dataConsumer.run(IDataParse.IMG, fileImg);

        WebBuilder webBuilder = new WebBuilder();
        webBuilder.register(fileLink, new ArrayList<DataParse.NewsLink>(), new DataParse.NewsLink[1]);
        webBuilder.register(fileText, new ArrayList<DataParse.NewsText>(), new DataParse.NewsText[1]);
        webBuilder.register(fileImg, new ArrayList<DataParse.NewsPhoto>(), new DataParse.NewsPhoto[1]);

        this.parser = parser;
        this.webBuilder = webBuilder;
        this.dataConsumer = dataConsumer;

        //        try {
//            Thread.sleep(40000);
//        } catch (InterruptedException e) {
//        }
//
//        dataConsumer.consumeAll();
//        System.out.println(webBuilder.build());
    }

    public String buildHtml(){
        parser.setPause(true);
        dataConsumer.consumeAll();
        String htmlUrl = webBuilder.build();
        parser.setPause(false);
        return htmlUrl;
    }

    public void exit(){
        parser.setPause(true);
        dataConsumer.consumeAll();
        parser.exit();
    }
}
