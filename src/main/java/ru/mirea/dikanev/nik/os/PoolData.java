package ru.mirea.dikanev.nik.os;

import java.util.*;

public class PoolData {

    private volatile Map<Integer, List<IDataParse>> pool = new HashMap<>();

    public volatile boolean isUnload = false;

    //Добавляет файл в очередь на запись
    public boolean push(IDataParse data) {
        if (data == null) {
            return false;
        }

        List<IDataParse> queue;
        queue = getQueue(data.getGroup()) /*pool.computeIfAbsent(data.getGroup(), k -> new LinkedList<>())*/;

        synchronized (queue) {
            queue.add(data);
            queue.notifyAll();
        }

        return true;
    }

    //Берет файл из очереди на запись
    public IDataParse pull(int groupData) {
        List<IDataParse> listParse;
        synchronized (pool) {
            listParse = pool.get(groupData);

            if (listParse == null || listParse.size() <= 0) {
                return null;
            }

            return listParse.remove(0);
        }
    }

    //Ждет пока появятся данные на записи
    public void waitWork(int group) {
        List<IDataParse> listParse = getQueue(group);
        synchronized (listParse) {
            isUnload = false;
            while (listParse.isEmpty() && !isUnload) {
                try {
                    listParse.wait();
                } catch (InterruptedException ignore) {
                }
            }
        }
    }

    //Отдает очередь данных по группе
    private List<IDataParse> getQueue(int group) {
        synchronized (pool) {
            List<IDataParse> listParse = pool.get(group);
            if (listParse == null) {
                listParse = new LinkedList<>();
                pool.put(group, listParse);
            }

            return listParse;
        }
    }

    //Отдает итератор очередей
    private Iterator<Map.Entry<Integer, List<IDataParse>>> getIterator(){
        return pool.entrySet().iterator();
    }

    public void notifyList(int group) {
        List list = getQueue(group);
        synchronized (list) {
            isUnload = true;
            list.notifyAll();
        }
    }

    public void notifyAllList() {
        Iterator<Map.Entry<Integer, List<IDataParse>>> iterator = getIterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, List<IDataParse>> entry = iterator.next();
            List list = entry.getValue();
            synchronized (list) {
                isUnload = true;
                list.notifyAll();
            }
        }
    }
}
