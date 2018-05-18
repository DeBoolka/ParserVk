package ru.mirea.dikanev.nik.os.demon;

import ru.mirea.dikanev.nik.os.IDataParse;
import ru.mirea.dikanev.nik.os.DataParse.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SQLController {

    private static final String URL = "jdbc:mysql://localhost:3306/vkposts?useSSL=false&serverTimezone=UTC&useUnicode=true&amp&characterEncoding=utf8&useServerPrepStmts=false";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";
    private static Connection connection;

    public SQLController() throws Exception {
        connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    private void main() throws Exception{
        Statement stmt;
        String sql;

        stmt = connection.createStatement();
        sql = "INSERT INTO users (id_user, s_name, name, p_name, age, gender, id_position) VALUES (NULL, 'НикитаTest', 'Диканев', 'Юрьевич', '2019-08-20', 'М', '1');";
        stmt.executeUpdate(sql);
        stmt.close();

        stmt = connection.createStatement();
        sql = "SELECT * FROM users";

        ResultSet res = stmt.executeQuery(sql);

        while (res.next()){
            int id_user = res.getInt("id_user");
            String s_name = res.getString("s_name");
            String name = res.getString("name");
            String p_name = res.getString("p_name");

            System.out.println(id_user + "\t" + s_name + "\t" + name + "\t" + p_name);
        }

        res.close();
        stmt.close();
    }

    //Записывает в базу данных элемент
    public void writeVkPosts(List<IDataParse> dataParses) throws Exception {

        Iterator<IDataParse> iterator = dataParses.iterator();
        while (iterator.hasNext()) {
            IDataParse dataParse = iterator.next();

            switch (dataParse.getGroup()) {
                case IDataParse.LIKE:
                    insertLike((NewsLikes) dataParse);
                    System.out.println("Write like id: " + dataParse.getId());
                    break;
                case IDataParse.COMMENT:
                    insertComment((NewsComments) dataParse);
                    System.out.println("Write comment id: " + dataParse.getId());
                    break;
                case IDataParse.IMG:
                    insertPhoto((NewsPhoto) dataParse);
                    System.out.println("Write photo id: " + dataParse.getId());
                    break;
            }
        }

    }

    /*private List<IDataParse> getUniqueDataParses(List<IDataParse> dataParses) throws Exception {
        if(dataParses.size() == 0){
            return dataParses;
        }
        int group = dataParses.get(0).getGroup();

        class Id{
            public int id;
            public String fId;

            public Id(int id, String fId) {
                this.id = id;
                this.fId = fId;
            }
        }

        //Подготовка запроса на получение идентификаторов в бд
        final StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT * FROM posts WHERE");
        dataParses.forEach((e) -> {
            if(e == dataParses.get(0)){
                sqlBuilder.append(" OR");
            }
            sqlBuilder.append(" id_vk_post = ").append(e.getId());
        });

        //Получение идентификаторов в бд
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(sqlBuilder.toString());
        List<Id> arrayId = new ArrayList<>();
        while (res.next()){
            Id id = new Id(res.getInt("id"), res.getString("id_vk_post"));
            arrayId.add(id);
            dataParses.remove(id.fId);
        }
        res.close();

        //Поиск уникальных
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append("SELECT * FROM ");
        switch (group){
            case IDataParse.LIKE:
                sqlBuild.append("link");
                break;
            case IDataParse.COMMENT:
                sqlBuild.append("text");
                break;
            case IDataParse.IMG:
                sqlBuild.append("photo");
                break;
        }
        sqlBuild.append(" WHERE");
        arrayId.forEach((e) -> {
            if(e.equals(arrayId.get(0))){
                sqlBuild.append(" OR");
            }
            sqlBuild.append(" id = ").append(Integer.toString(e.id));
        });

        //Получение уникальных запросов в бд
        stmt = connection.createStatement();
        List<IDataParse> uniqueDataParse = new ArrayList<>();
        res = stmt.executeQuery(sqlBuild.toString());
        while (res.next()){
            switch (dataParses.get(0).getGroup()){
                case IDataParse.LIKE:
                    uniqueDataParse.add(new NewsLikes())
                    break;
                case IDataParse.COMMENT:
                    sqlBuild.append("");
                    break;
                case IDataParse.IMG:
                    sqlBuild.append("");
                    break;
            }
            arrayId.add(res.getInt("id"));
        }
        res.close();

    }*/

    //Записывает в базу ссылки
    private String insertComment(NewsComments comment) throws Exception{
        Statement stmt;
        String sql;

        if(checkPostBD(comment.getId(), "text")){
            return comment.getId();
        }

        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO text VALUES ((SELECT id FROM posts WHERE id_vk_post = ? LIMIT 1), ?)");
        preparedStatement.setString(1, comment.getId());
        comment.getComments().forEach((e) -> {
            try {
                preparedStatement.setString(2, e);
                preparedStatement.executeUpdate();
            } catch (Exception ignore) {
            }
        });
        preparedStatement.close();

        return comment.getId();
    }

    //Записывает в базу текст
    private String insertLike(NewsLikes like) throws Exception{
        Statement stmt;
        String sql = "";

        if(checkPostBD(like.getId(), "link")){
            return like.getId();
        }

        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO link VALUES ((SELECT id FROM posts WHERE id_vk_post = ? LIMIT 1), ?, ?, ?)");
        preparedStatement.setString(1, like.getId());
        like.getLikes().forEach((e) -> {
            try {
                preparedStatement.setString(2, e.getLink());
                preparedStatement.setString(3, e.getLogin());
                preparedStatement.setString(4, e.getName());
                preparedStatement.executeUpdate();
            } catch (Exception ignore) {
            }
        });
        preparedStatement.close();

        return like.getId();
    }

    //Записывает в базу фото
    private String insertPhoto(NewsPhoto photo) throws Exception{
        String sql;

        if(checkPostBD(photo.getId(), "photo")){
            return photo.getId();
        }

        sql = "INSERT INTO photo(id, link) VALUES((SELECT id FROM posts WHERE id_vk_post = ? LIMIT 1), ?);";
        PreparedStatement preparedStatement =  connection.prepareStatement(sql);
        preparedStatement.setString(1, photo.getId());

        List<String> links = new ArrayList<>(List.of(photo.getPathPhoto()));
        links.forEach((e) -> {
            try {
                preparedStatement.setString(2, photo.getAbsolutePathPhoto(e));
                preparedStatement.executeUpdate();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        preparedStatement.close();
        return photo.getId();
    }

    //Проверяет, есть ли в бд такой пост
    private boolean checkPostBD(String idPost, String table) throws Exception {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT COUNT(*) as cnt FROM posts WHERE id_vk_post = ? LIMIT 1;");
        preparedStatement.setString(1, idPost);
        ResultSet res = preparedStatement.executeQuery();
        while (res.next()) {
            int cnt = res.getInt("cnt");
            if(cnt <= 0){
                createNewPost(idPost);
                return false;
            }
            preparedStatement = connection.prepareStatement("SELECT COUNT(*) as cnt FROM "  + table + " WHERE id = (SELECT id FROM posts WHERE id_vk_post = ? LIMIT 1) LIMIT 1;");
            preparedStatement.setString(1, idPost);
            res = preparedStatement.executeQuery();
            while (res.next()) {
                cnt = res.getInt("cnt");
                if(cnt > 0){
                    return true;
                }
            }
        }

        preparedStatement.close();
        return false;
    }

    //Создает новый пост в бд
    private void createNewPost(String idPost) throws Exception{
        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO posts VALUES (NULL, ?)");
        preparedStatement.setString(1, idPost);
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }
}
