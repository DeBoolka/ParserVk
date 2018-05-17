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

    public NewsLikes text;
    public NewsPhoto photo;
    public NewsComments comments;

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
        text = new NewsLikes(id, IDataParse.LIKE);
        photo = new NewsPhoto(id, IDataParse.IMG);
        comments = new NewsComments(id, IDataParse.COMMENT);
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
    public class NewsLikes extends BaseParseClass implements IDataParse {

        private List<Like> likes;

        public class Like {

            private String name;
            private String login;
            private String link;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getLogin() {
                return login;
            }

            public void setLogin(String login) {
                this.login = login;
            }

            public String getLink() {
                return link;
            }

            public void setLink(String link) {
                this.link = link;
            }
        }

        public NewsLikes(String id, int group) {
            super(id, group);
        }

        public List<Like> getLikes() {
            return likes;
        }

        public void setLikes(List<Like> likes) {
            this.likes = likes;
        }

        public IDataParse parse(WebElement element, PoolData poolData) {
            likes = new ArrayList<>();
            element.findElements(By.className("_6e4x5")).forEach((e) -> {
                Like like = new Like();
                WebElement buffElement = findElement(e, By.className("_9mmn5 "));
                like.setName((buffElement != null) ? buffElement.getText() : "");
                buffElement = findElement(e, By.className("_2g7d5 "));
                like.setLogin((buffElement != null) ? buffElement.getText() : "");
                like.setLink((buffElement != null) ? buffElement.getAttribute("href") : "");

                likes.add(like);
            });

            if(likes.size() > 0) {
                NewsLikes newsLikes = new NewsLikes(getId(), getGroup());
                newsLikes.setLikes(likes);
                poolData.push(newsLikes);
            }

            return this;
        }

        @Override
        public String getHtml() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("<p>Likes:<p><select>");
            likes.forEach((e) -> stringBuilder.append("<option>")
                    .append("[").append(e.login).append("]\n")
                    .append("[").append(e.name).append("]\n")
                    .append("[").append(e.link).append("]")
                    .append("</option>"));
            stringBuilder.append("</select>");

            return stringBuilder.toString();
        }

    }

    //Класс парсинга фото
    public class NewsPhoto extends BaseParseClass implements IDataParse {

        private String pathPhoto;
        private transient String dirPhoto;

        public NewsPhoto(String id, int group) {
            super(id, group);
        }

        //region get-set functions
        public String getPathPhoto() {
            return pathPhoto;
        }

        public void setDirPhoto(String dirPhoto) {
            this.dirPhoto = dirPhoto;
        }

        public void setPathPhoto(String pathPhoto) {
            this.pathPhoto = pathPhoto;
        }

        public String getAbsolutePathPhoto(String _pathPhoto) {
            return new File(_pathPhoto).getAbsolutePath();
        }
        //endregion

        public IDataParse parse(WebElement element, PoolData poolData) {

            String localLinkPhoto = dirPhoto + String.valueOf(System.currentTimeMillis()) + String.valueOf((int) (Math.random() * 1000)) + ".jpg";
            new Thread(new LoadPhoto(element.findElement(By.tagName("img")).getAttribute("src"), localLinkPhoto)).start();

            NewsPhoto newsPhoto = new NewsPhoto(getId(), getGroup());
            newsPhoto.setPathPhoto(localLinkPhoto);
            poolData.push(newsPhoto);

            return newsPhoto;
        }

        @Override
        public String getHtml() {
            final StringBuilder buildPhoto = new StringBuilder();
            buildPhoto.append("<p><img src=\"").append(pathPhoto).append("\" alt=\"\">\n");
            return buildPhoto.toString();
        }

    }

    //Класс парсинга ссылок
    public class NewsComments extends BaseParseClass implements IDataParse {

        private List<String> comments;

        public NewsComments(String id, int group) {
            super(id, group);
        }

        //region get-set functions

        public List<String> getComments() {
            return comments;
        }

        public void setComments(List<String> links) {
            this.comments = links;
        }
        //endregion

        public IDataParse parse(WebElement element, PoolData poolData) {
            try {
                //Парсинг комментариев
                final List<String> parseComments = new ArrayList<>();
                element.findElements(By.className("_ezgzd")).forEach((k) -> {
                    parseComments.add("[" + k.findElement(By.tagName("a")).getText() + "]\t" + k.findElement(By.tagName("span")).getText());
                });

                NewsComments newsComments = new NewsComments(getId(), getGroup());
                newsComments.setComments(parseComments);
                poolData.push(newsComments);

                return newsComments;

            } catch (Exception ignore) {
                return this;
            }
        }

        @Override
        public String getHtml() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("<p>Comments:<p><select>");
            comments.forEach((e) -> stringBuilder.append("<option>").append(e).append("</option>"));
            stringBuilder.append("</select>");

            return stringBuilder.toString();
        }
    }
    //endregion

    //Класс загрузки фото
    private class LoadPhoto implements Runnable {
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

    public static WebElement findElement(WebElement webElement, By by){
        List<WebElement> elements = webElement.findElements(by);
        if(elements.size() > 0){
            return elements.get(0);
        }
        return null;
    }
}
