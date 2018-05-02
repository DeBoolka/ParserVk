package ru.mirea.dikanev.nik.os.demon;

import com.google.gson.Gson;
import org.apache.commons.codec.digest.DigestUtils;
import ru.mirea.dikanev.nik.os.IDataParse;
import ru.mirea.dikanev.nik.os.DataParse;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.*;

public class Demon {

    private volatile Map<File, NodeSpyFile> filesHash = new HashMap<>();
    private SQLController sqlCtr = new SQLController();

    public Demon() throws Exception {
//        connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    //Запускает демона
    public static void main(String... args) throws Exception {

        new Thread(()->{
            try {
                Demon demon = new Demon();

                demon.spy("B:\\JavaProjects\\ParseDriver\\src\\main\\java\\ru\\mirea\\dikanev\\nik\\os\\data\\ParseText.jsondemon", new DataParse.NewsText[1]);
                demon.spy("B:\\JavaProjects\\ParseDriver\\src\\main\\java\\ru\\mirea\\dikanev\\nik\\os\\data\\ParseLink.jsondemon", new DataParse.NewsLink[1]);
                demon.spy("B:\\JavaProjects\\ParseDriver\\src\\main\\java\\ru\\mirea\\dikanev\\nik\\os\\data\\ParseImg.jsondemon", new DataParse.NewsPhoto[1]);

                System.out.println("Start Demon...");
                demon.watcher();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    //Начинает следить за файлом указанным в path
    public <T extends IDataParse> void spy(String path, T[] classOf) {
        File file = new File(path);
        if(!file.isFile()){
            try {
                file.createNewFile();
            } catch (IOException e) {
            }
        }

        if(!filesHash.containsKey(file)) {
            filesHash.put(file, new NodeSpyFile("", classOf));
        }
    }

    //Проходит по файлам за которыми следим. Если видит, что он изменился записывает измения в бд и очищает, что прочел
    private void watcher(){
        while (true) {
            Iterator<Map.Entry<File, NodeSpyFile>> iterator = filesHash.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<File, NodeSpyFile> entry = iterator.next();

                String hashFile = getFileHash(entry.getKey());
                if (entry.getValue().hash.equals(hashFile) || entry.getKey().length() == 0) {
                    continue;
                }

                try (RandomAccessFile file = new RandomAccessFile(entry.getKey(), "rw")) {
                    FileChannel channel = file.getChannel();
                    FileLock lock = channel.lock();

                    byte[] byteByffer = new byte[(int)entry.getKey().length()];
                    file.read(byteByffer);
                    try {
                        List data = new ArrayList<>(Arrays.asList(new Gson().fromJson(new String(byteByffer), entry.getValue().classOf.getClass())));
                        sqlCtr.writeVkPosts(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try(PrintWriter writer = new PrintWriter(entry.getKey())) {
                        writer.print("");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    lock.release();
                } catch (IOException e) {
                }

                entry.getValue().hash = getFileHash(entry.getKey());
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
        }
    }

    //Получает хэш файла
    private String getFileHash(File file) {
        String hash = null;

        try(FileInputStream fis = new FileInputStream(file)){

            hash = DigestUtils.md5Hex(fis);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return hash;
    }

    //Описывает файл за которым следим
    class NodeSpyFile<T extends IDataParse>{
        public String hash;

        public T[] classOf;

        public NodeSpyFile(String hash, T[] classOf) {
            this.hash = hash;
            this.classOf = classOf;
        }

    }
}
