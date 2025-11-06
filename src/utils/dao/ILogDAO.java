package utils.dao;

import java.util.List;

public interface ILogDAO {

    void addLog(String entry);

    List<String> getAllLogs();

    List<String> getLastLogs(int count);

    void clearLogs();

    void deleteLogs();

    long getLogCount();

    boolean isAvailable();

    void close();
}
