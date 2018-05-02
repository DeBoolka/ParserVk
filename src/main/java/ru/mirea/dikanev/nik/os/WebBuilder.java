package ru.mirea.dikanev.nik.os;

import com.google.gson.Gson;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class WebBuilder {

    private volatile List<ParseFile> pendingData = new ArrayList<>();

    //Регистрирует на наблюдение переданный файл
    public <T extends IDataParse> void register(File path, ArrayList<T> list, T[] classOf) {
        pendingData.add(new ParseFile(path, list, classOf));
    }

    //Вызов построения отображения распарсенных данных
    public String build() {
        Gson gson = new Gson();

        //Загрузка всех json
        pendingData.forEach((k) -> {
            FileLock fileLock;
            FileChannel channel;
            try (RandomAccessFile file = new RandomAccessFile(k.pathFile, "rw")) {
                synchronized (k.pathFile) {
                    try {
                        channel = file.getChannel();
                        fileLock = channel.lock();
                    } catch (OverlappingFileLockException e) {
                    }

                    byte[] bufferReader = new byte[(int) file.length()];
                    file.read(bufferReader);

                    String jsonDoc = new String(bufferReader);
                    if (jsonDoc.equals("")) {
                        return;
                    }

                    k.contents = new ArrayList<>(Arrays.asList(gson.fromJson(jsonDoc, k.classOf.getClass())));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return CreateHtmlFile(CreateHtml());
    }

    //Формирует все отображение в виде html строки
    private String CreateHtml() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Parse List</title>\n" +
                "</head>\n" +
                "<body>\n");

        int indexPost = 0;
        while (true) {
            String currentId = "";
            List<IDataParse> indexRemoveAndPrint = new ArrayList<>();
            //Находим минимальные индексы и группируем в одном листе
            for (int i = 0; i < pendingData.size(); i++) {
                if (pendingData.get(i).contents.isEmpty()) {
                    continue;
                }

                IDataParse bufDataParse = null;
                if (currentId.equals("")) {
                    bufDataParse = ((IDataParse) (pendingData.get(i).contents.get(0)));
                    currentId = bufDataParse.getId();
                    indexRemoveAndPrint.clear();
                    indexRemoveAndPrint.add(bufDataParse);
                } else {
                    List lst = pendingData.get(i).contents;
                    for (Object item : lst) {
                        if (((IDataParse) item).getId().equals(currentId)) {
                            bufDataParse = (IDataParse) item;
                            indexRemoveAndPrint.add(bufDataParse);
                            break;
                        }
                    }
                }
                if (bufDataParse != null) {
                    pendingData.get(i).contents.remove(bufDataParse);
                }
            }

            //Запись
            stringBuilder.append((!currentId.equals("")) ? ("<p>id = " + (indexPost++) + "\n") : "");
            for (IDataParse item : indexRemoveAndPrint) {
                stringBuilder.append(item.getHtml());
            }

            if (indexRemoveAndPrint.isEmpty()) {
                break;
            }

            indexRemoveAndPrint.clear();
        }

        stringBuilder.append("</body>\n</html>");

        return stringBuilder.toString();
    }

    //Создает html файл и записывает в него всю страницу. Возвращает путь до файла.
    private String CreateHtmlFile(String html) {
        File file = new File("HtmlData.html");
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.print(html);
        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }

        return file.getPath();
    }

    //Распарсенный файл. Хранит класс хранимого элемента в файле, и весь контент.
    private class ParseFile<T extends IDataParse> {
        File pathFile;
        ArrayList<T> contents;
        public T[] classOf;

        public ParseFile(File pathFile, ArrayList<T> contents, T[] classOf) {
            this.pathFile = pathFile;
            this.contents = contents;
            this.classOf = classOf;
        }
    }
}
