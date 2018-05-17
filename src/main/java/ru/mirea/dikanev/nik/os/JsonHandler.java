package ru.mirea.dikanev.nik.os;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JsonHandler {
    //Проверяет есть ли в json файле поле с заданным значением, не выгружая весь файл в память
    public static boolean findJsonAttribute(String attribute, String val, File file){
        attribute = "\"" + attribute;
        Set<Character> ignoreList = new HashSet<>(List.of(' ', '\t', '\n', '\r', ':'));

        try(FileReader reader = new FileReader(file)){
            int ch;
            int posTrueAttr = 0;
            int posTrueVal = 0;

            while ((ch = reader.read()) != -1) {
                if(ignoreList.contains((char)ch)){
                    continue;
                } else if (posTrueAttr >= attribute.length() && posTrueVal < val.length() && (char)ch == val.charAt(posTrueVal)) {
                    posTrueVal++;
                } else if (posTrueAttr < attribute.length() && (char) ch == attribute.charAt(posTrueAttr)) {
                    posTrueAttr++;
                    ignoreList.add('"');
                } else if (posTrueVal >= val.length()) {
                    return true;
                } else {
                    posTrueAttr = posTrueVal = 0;
                    ignoreList.remove('"');
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    //Добавляет массив json элементов в файл за O(1)
    public static void writeJson(File pathFile, String jsonData) {
        try (RandomAccessFile file = new RandomAccessFile(pathFile, "rw")) {
            FileChannel channel;
            FileLock fileLock;
            synchronized (pathFile) {
                channel = file.getChannel();
                fileLock = channel.lock();

                if (file.length() < 2) {
                    channel.write(ByteBuffer.wrap(jsonData.getBytes()));
                } else if(!jsonData.equals("[]")){
                    file.seek(file.length() - 1);
                    channel.write(ByteBuffer.wrap(("," + jsonData.substring(1, jsonData.length())).getBytes()));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
