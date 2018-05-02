package ru.mirea.dikanev.nik.os;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class DataParse implements IMasterDataParse {

    private String id;

    public NewsText text;
    public NewsPhoto photo;
    public NewsLink link;

    public DataParse() {
        this.id = null;
        init();
    }

    public DataParse(String id) {
        this.id = id;
        init();
    }

    //region get-set functions
    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }
    //endregion

    //Инициализация переменных классов, которые парсим
    private void init() {
        text = new NewsText(id, IDataParse.TEXT);
        photo = new NewsPhoto(id, IDataParse.IMG);
        link = new NewsLink(id, IDataParse.LINK);
    }

    //Базовый класс элементов, которые парсим
    private abstract class BaseParseClass implements IDataParse {

        private String id;
        private int group;

        BaseParseClass(String id, int group) {
            this.id = id;
            this.group = group;
        }

        //region get-set functions
        @Override
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @Override
        public int getGroup() {
            return group;
        }
        //endregion

    }

    //region Классы, которые парсим
    //Класс парсинга текста
    public class NewsText extends BaseParseClass implements IDataParse {

        private String text;

        public NewsText(String id, int group) {
            super(id, group);
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public IDataParse parse(WebElement element, PoolData poolData) {
            try {
                String idPost = element.findElement(By.className("_post")).getAttribute("id");
                String text = element.findElement(By.className("wall_post_text")).getText();

                NewsText newsText = new NewsText(getId(), getGroup());
                newsText.setText(text);
                newsText.setId(idPost);
                poolData.push(newsText);

                return newsText;

            } catch (Exception e) {
                return this;
            }

        }

        @Override
        public String getHtml() {
            return "<p>Text: " + text + "\n";
        }

    }

    //Класс парсинга фото
    public class NewsPhoto extends BaseParseClass implements IDataParse {

        private List<String> pathPhoto;
        private transient String dirPhoto;

        public NewsPhoto(String id, int group) {
            super(id, group);
        }

        //region get-set functions
        public List<String> getPathPhoto() {
            return pathPhoto;
        }

        public void setDirPhoto(String dirPhoto) {
            this.dirPhoto = dirPhoto;
        }

        public void setPathPhoto(List<String> pathPhoto) {
            this.pathPhoto = pathPhoto;
        }

        public String getAbsolutePathPhoto(String _pathPhoto) {
            return new File(_pathPhoto).getAbsolutePath();
        }
        //endregion

        public IDataParse parse(WebElement element, PoolData poolData) {
            try {
                String idPost = element.findElement(By.className("_post")).getAttribute("id");
                final List<String> linksImg = new ArrayList<>();
                element.findElement(By.className("wall_text"))
                        .findElements(By.className("image_cover"))
                        .forEach((e) -> linksImg.add(e.getCssValue("background-image")));
                if (linksImg.size() == 0) {
                    return this;
                }

                final List<String> localLinksPhoto = new ArrayList<>();
                linksImg.forEach((e) -> {
                    e = e.substring(5, e.length() - 2);
                    String imgName = String.valueOf(System.currentTimeMillis()) + String.valueOf((int)(Math.random() * 1000)) + e.substring(e.length() - 4);
                    String absPathPhoto = dirPhoto + imgName;

                    //Сохранение изображения
                    new Thread(new LoadPhoto(e ,absPathPhoto)).start();
                    localLinksPhoto.add(absPathPhoto);
                });


                NewsPhoto newsPhoto = new NewsPhoto(getId(), getGroup());
                newsPhoto.setPathPhoto(localLinksPhoto);
                newsPhoto.setId(idPost);
                poolData.push(newsPhoto);

                return newsPhoto;

            } catch (Exception e) {
                return this;
            }
        }

        @Override
        public String getHtml() {
            final StringBuilder buildPhoto = new StringBuilder();
            pathPhoto.forEach((e) -> buildPhoto.append("<p><img src=\"").append(e).append("\" alt=\"\">\n"));
            return buildPhoto.toString();
        }

    }

    //Класс парсинга ссылок
    public class NewsLink extends BaseParseClass implements IDataParse {

        private String link;
        private String author;
        private List<String> links;

        public NewsLink(String id, int group) {
            super(id, group);
        }

        //region get-set functions
        public String getAuthor() {
            return author;
        }

        public List<String> getLinks() {
            return links;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public void setLinks(List<String> links) {
            this.links = links;
        }
        //endregion

        public IDataParse parse(WebElement element, PoolData poolData) {
            try {
                String idPost = element.findElement(By.className("_post")).getAttribute("id");
                //Парсинг ссылки на пост
                String link = element.findElement(By.className("post_link")).getAttribute("href");
                //Парсинг автора
                String author = "";
                try {
                    author = element.findElement(By.className("author")).getAttribute("href");
                } catch (Exception ignore) {
                }

                //Парсинг ссылок
                List<String> links = new ArrayList<>();
                try {
                    element.findElement(By.className("wall_post_text"))
                            .findElements(By.tagName("a"))
                            .forEach((k) -> links.add(k.getAttribute("href")));
                } catch (Exception ignore) {
                }

                NewsLink newsLink = new NewsLink(getId(), getGroup());
                newsLink.setId(idPost);
                newsLink.setLink(link);
                newsLink.setAuthor(author);
                newsLink.setLinks(links);
                poolData.push(newsLink);

                return newsLink;

            } catch (Exception ignore) {
                return this;
            }
        }

        @Override
        public String getHtml() {
            return "<p>Link: " + link + "\n<p>Author: " + author + "\n<p>Links: " + links + "\n";
        }
    }
    //endregion

    //Класс загрузки фото
    private class LoadPhoto implements Runnable{
        private String urlPhoto;
        private String fileName;

        LoadPhoto(final String urlPhoto, final String fileName) {
            this.urlPhoto = urlPhoto;
            this.fileName = fileName;
        }

        @Override
        public void run() {
            try {
                URL url = new URL(urlPhoto);
                URLConnection conn = url.openConnection();
                conn.setDoOutput(true);
                conn.setDoInput(true);

                try (BufferedInputStream bis = new BufferedInputStream(conn.getInputStream())) {
                    try (FileOutputStream fos = new FileOutputStream(new File(fileName))) {
                        int ch;
                        while ((ch = bis.read()) != -1) {
                            fos.write(ch);
                        }

                        fos.flush();
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
