package ru.mirea.dikanev.nik.os.demon;

import ru.mirea.dikanev.nik.os.IDataParse;
import ru.mirea.dikanev.nik.os.DataParse.*;

import java.sql.*;
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
                case IDataParse.TEXT:
                    insertText((NewsText) dataParse);
                    System.out.println("Write text id: " + dataParse.getId());
                    break;
                case IDataParse.LINK:
                    insertLink((NewsLink) dataParse);
                    System.out.println("Write link id: " + dataParse.getId());
                    break;
                case IDataParse.IMG:
                    insertPhoto((NewsPhoto) dataParse);
                    System.out.println("Write photo id: " + dataParse.getId());
                    break;
            }
        }

    }

    //Записывает в базу ссылки
    private String insertLink(NewsLink link) throws Exception{
        Statement stmt;
        String sql;

        if(checkPostBD(link.getId(), "link")){
            return link.getId();
        }

        stmt = connection.createStatement();
        sql = "INSERT INTO link(id, link, author) " +
                "VALUES((SELECT id FROM posts WHERE id_vk_post = '" + link.getId() + "'LIMIT 1), '" + link.getLink() + "', '" + link.getAuthor() + "')";
        stmt.executeUpdate(sql);

        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO links VALUES ((SELECT id FROM posts WHERE id_vk_post = ? LIMIT 1), ?)");
        link.getLinks().forEach((e) -> {
            try {
                preparedStatement.setString(1, link.getId());
                preparedStatement.setString(2, e);
                preparedStatement.executeUpdate();
            } catch (Exception ex) {
            }
        });

        return link.getId();
    }

    //Записывает в базу текст
    private String insertText(NewsText text) throws Exception{
        Statement stmt;
        String sql;

        if(checkPostBD(text.getId(), "text")){
            return text.getId();
        }

        sql = "INSERT INTO text(id, text) " +
                "VALUES((SELECT id FROM posts WHERE id_vk_post = '" + text.getId() + "' LIMIT 1), '" + text.getText() + "')";
        stmt = connection.createStatement();
        try {
            stmt.executeUpdate(sql);
        } catch (SQLException ignored) {
        } finally {
            stmt.close();
        }

        return text.getId();
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

        List<String> links = photo.getPathPhoto();
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
