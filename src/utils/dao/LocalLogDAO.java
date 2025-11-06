package utils.dao;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class LocalLogDAO implements ILogDAO {

    private final Queue<String> logs;
    private final int maxLogs;

    public LocalLogDAO() {
        this(100);
    }

    public LocalLogDAO(int maxLogs) {
        this.maxLogs = maxLogs;
        this.logs = new LinkedList<>();
    }

    @Override
    public synchronized void addLog(String entry) {
        logs.add(entry);
        if (logs.size() > maxLogs) {
            logs.poll();
        }
    }

    @Override
    public synchronized List<String> getAllLogs() {
        return new ArrayList<>(logs);
    }

    @Override
    public synchronized List<String> getLastLogs(int count) {
        List<String> allLogs = new ArrayList<>(logs);
        if (allLogs.size() <= count) {
            return allLogs;
        }
        return new ArrayList<>(allLogs.subList(allLogs.size() - count, allLogs.size()));
    }

    @Override
    public synchronized void clearLogs() {
        logs.clear();
    }

    @Override
    public synchronized void deleteLogs() {
        logs.clear();
    }

    @Override
    public synchronized long getLogCount() {
        return logs.size();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void close() {
    }
}
