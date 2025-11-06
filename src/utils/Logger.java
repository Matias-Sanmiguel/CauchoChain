package utils;

import utils.dao.ILogDAO;
import utils.dao.RedisLogDAO;
import utils.dao.LocalLogDAO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Logger {
    private static Logger instance;
    private final ILogDAO logDAO;
    private final ILogDAO fallbackDAO;
    private final String logFile;
    private final DateTimeFormatter formatter;
    private final Object lock = new Object();

    private Logger() {
        this.logFile = "logs/blockchain_" + System.currentTimeMillis() + ".log";
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        RedisLogDAO redisDAO = new RedisLogDAO();

        if (redisDAO.isAvailable()) {
            this.logDAO = redisDAO;
            this.fallbackDAO = new LocalLogDAO();
            System.out.println("[Logger] Usando RedisLogDAO como almacenamiento principal");
        } else {
            redisDAO.close();
            this.logDAO = new LocalLogDAO();
            this.fallbackDAO = null;
            System.out.println("[Logger] Usando LocalLogDAO (Redis no disponible)");
        }
    }

    public Logger(ILogDAO logDAO) {
        this.logFile = "logs/blockchain_" + System.currentTimeMillis() + ".log";
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        this.logDAO = logDAO;
        this.fallbackDAO = new LocalLogDAO();
    }

    public static Logger getInstance() {
        if (instance == null) {
            synchronized (Logger.class) {
                if (instance == null) {
                    instance = new Logger();
                }
            }
        }
        return instance;
    }

    public void log(String message) {
        synchronized (lock) {
            String timestamp = LocalDateTime.now().format(formatter);
            String logEntry = "[" + timestamp + "] " + message;

            writeToStorage(logEntry);
            System.out.println(logEntry);
        }
    }

    private void writeToStorage(String entry) {
        try {
            logDAO.addLog(entry);
        } catch (Exception e) {
            if (fallbackDAO != null) {
                try {
                    fallbackDAO.addLog(entry);
                } catch (Exception ex) {
                    System.err.println("[Logger] Error guardando log: " + ex.getMessage());
                }
            }
        }
    }

    public void info(String message) {
        log("[INFO] " + message);
    }

    public void warning(String message) {
        log("[WARN] " + message);
    }

    public void error(String message) {
        log("[ERROR] " + message);
    }

    public void debug(String message) {
        log("[DEBUG] " + message);
    }

    public void success(String message) {
        log("[SUCCESS] " + message);
    }

    public synchronized List<String> getLogs() {
        try {
            return logDAO.getAllLogs();
        } catch (Exception e) {
            if (fallbackDAO != null) {
                try {
                    return fallbackDAO.getAllLogs();
                } catch (Exception ex) {
                    System.err.println("[Logger] Error obteniendo logs: " + ex.getMessage());
                }
            }
            return List.of();
        }
    }

    public synchronized List<String> getLastLogs(int count) {
        try {
            return logDAO.getLastLogs(count);
        } catch (Exception e) {
            if (fallbackDAO != null) {
                try {
                    return fallbackDAO.getLastLogs(count);
                } catch (Exception ex) {
                    System.err.println("[Logger] Error obteniendo ultimos logs: " + ex.getMessage());
                }
            }
            return List.of();
        }
    }

    public synchronized void clear() {
        try {
            logDAO.clearLogs();
        } catch (Exception e) {
            System.err.println("[Logger] Error limpiando logs: " + e.getMessage());
        }

        if (fallbackDAO != null) {
            try {
                fallbackDAO.clearLogs();
            } catch (Exception e) {
            }
        }
    }

    public synchronized void exportLogs(String filePath) {
        List<String> logs = getLogs();
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            if (path.getParent() != null) {
                java.nio.file.Files.createDirectories(path.getParent());
            }
            java.nio.file.Files.write(path, logs);
        } catch (Exception e) {
            System.err.println("[Logger] Error exportando logs: " + e.getMessage());
        }
    }

    public String getLogFile() {
        return logFile;
    }

    public void closeRedis() {
        if (logDAO != null) {
            logDAO.close();
        }
        if (fallbackDAO != null) {
            fallbackDAO.close();
        }
    }

    public ILogDAO getLogDAO() {
        return logDAO;
    }

    public boolean isStorageAvailable() {
        return logDAO.isAvailable();
    }
}
