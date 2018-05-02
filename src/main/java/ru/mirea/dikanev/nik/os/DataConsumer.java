package ru.mirea.dikanev.nik.os;

import com.google.gson.Gson;

import java.io.*;
import java.util.*;

public class DataConsumer {

    private volatile PoolData poolData;
    private Gson gson = new Gson();
    private DataParse news = new DataParse();

    private final int LIMIT_COUNT_DATA = 10;

    public volatile static Set loadData = new HashSet();

    public DataConsumer(PoolData poolData) {
        this.poolData = poolData;
    }

    //Запускает потребителя и возвращает вновь соданный и запущенный поток
    public Thread run(int group, File jsonFile) {
        try {
            jsonFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*try(PrintWriter writer = new PrintWriter(jsonFile)) {
            writer.print("");
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        Thread th = new Thread(() -> worker(group, jsonFile));
        th.setDaemon(true);
        th.start();

        return th;
    }

    //Собирает потребляет данные и при накопленние больше чем LIMIT_COUNT_DATA записывает их в файл
    private <T extends IDataParse> void worker(int group, File jsonFile) {
        List<T> listData = new ArrayList();

        while (true) {
            poolData.waitWork(group);
            T consumeData = (T) poolData.pull(group);
            if (consumeData != null && !isHasRecordPost(consumeData.getId(), jsonFile)) {
                listData.add(consumeData);
            }

            if (listData.size() >= LIMIT_COUNT_DATA || loadData.contains(group)) {
                String jsonData = gson.toJson(listData.toArray());


                //Запись в файл
                JsonHandler.writeJson(jsonFile, jsonData);
                JsonHandler.writeJson(new File(jsonFile.getPath()+"demon"), jsonData);

                System.out.println("JsonWrite: " + jsonData);

                listData.clear();
                loadData.remove(group);
            }
        }
    }

    //Потребляет все
    public void consumeAll(){
        Iterator<Map.Entry<Integer, List<IDataParse>>> iterator = poolData.getIterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, List<IDataParse>> entry = iterator.next();

            List list = entry.getValue();
            synchronized (list) {
                list.notifyAll();
            }

            loadData.add(entry.getKey());
        }

        while (DataConsumer.loadData.size() > 0){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
    }

    //Проверка на то, что это еще не записывали
    private boolean isHasRecordPost(String idPost, File jsonFile) {
        return JsonHandler.findJsonAttribute("id", idPost, jsonFile);
    }

}
