package utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Logger {
    private static Logger instance;
    private final JedisPool jedisPool;
    private final String logFile;
    private final DateTimeFormatter formatter;
    private final Object lock = new Object();
    private final String redisKey = "blockchain:logs";
    private final int maxLogsInMemory = 100;

    private Logger() {
        this.logFile = "logs/blockchain_" + System.currentTimeMillis() + ".log";
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        this.jedisPool = initializeRedis();
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

    private JedisPool initializeRedis() {
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(10);
            config.setMaxIdle(5);
            config.setMinIdle(2);
            config.setTestOnBorrow(true);
            config.setTestOnReturn(true);

            JedisPool pool = new JedisPool(config, "localhost", 6379);

            try (Jedis jedis = pool.getResource()) {
                jedis.ping();
                System.out.println("[Logger] Redis conectado exitosamente en localhost:6379");
            } catch (Exception e) {
                System.err.println("[Logger] Error conectando a Redis: " + e.getMessage());
                System.err.println("[Logger] Continuando sin Redis...");
                return null;
            }
            return pool;
        } catch (Exception e) {
            System.err.println("[Logger] Error inicializando Redis: " + e.getMessage());
            return null;
        }
    }

    public void log(String message) {
        synchronized (lock) {
            String timestamp = LocalDateTime.now().format(formatter);
            String logEntry = "[" + timestamp + "] " + message;

            writeToRedis(logEntry);
            System.out.println(logEntry);
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

    private void writeToRedis(String entry) {
        if (jedisPool == null) {
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.rpush(redisKey, entry);

            long logCount = jedis.llen(redisKey);
            if (logCount > maxLogsInMemory) {
                jedis.ltrim(redisKey, logCount - maxLogsInMemory, -1);
            }
        } catch (Exception e) {
            System.err.println("[Logger] Error escribiendo a Redis: " + e.getMessage());
        }
    }

    public synchronized List<String> getLogs() {
        if (jedisPool == null) {
            return new ArrayList<>();
        }

        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.lrange(redisKey, 0, -1);
        } catch (Exception e) {
            System.err.println("[Logger] Error leyendo de Redis: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public synchronized List<String> getLastLogs(int count) {
        if (jedisPool == null) {
            return new ArrayList<>();
        }

        try (Jedis jedis = jedisPool.getResource()) {
            long totalLogs = jedis.llen(redisKey);
            long start = Math.max(0, totalLogs - count);
            return jedis.lrange(redisKey, start, -1);
        } catch (Exception e) {
            System.err.println("[Logger] Error leyendo últimos logs de Redis: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public synchronized void clear() {
        if (jedisPool == null) {
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(redisKey);
        } catch (Exception e) {
            System.err.println("[Logger] Error limpiando Redis: " + e.getMessage());
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
        if (jedisPool != null) {
            jedisPool.close();
            System.out.println("[Logger] Conexión a Redis cerrada");
        }
    }
}
