package utils.dao;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import java.util.ArrayList;
import java.util.List;

public class RedisLogDAO implements ILogDAO {

    private final JedisPool jedisPool;
    private final String redisKey;
    private final int maxLogsInMemory;
    private boolean available;

    public RedisLogDAO() {
        this("blockchain:logs", 100);
    }

    public RedisLogDAO(String redisKey, int maxLogsInMemory) {
        this.redisKey = redisKey;
        this.maxLogsInMemory = maxLogsInMemory;
        this.jedisPool = initializeRedis();
        this.available = (jedisPool != null);
    }

    public RedisLogDAO(JedisPool jedisPool, String redisKey, int maxLogsInMemory) {
        this.jedisPool = jedisPool;
        this.redisKey = redisKey;
        this.maxLogsInMemory = maxLogsInMemory;
        this.available = testConnection();
    }

    private JedisPool initializeRedis() {
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(10);
            config.setMaxIdle(5);
            config.setMinIdle(2);
            config.setTestOnBorrow(true);
            config.setTestOnReturn(true);
            config.setTestWhileIdle(true);
            config.setMinEvictableIdleTimeMillis(60000);
            config.setTimeBetweenEvictionRunsMillis(30000);

            JedisPool pool = new JedisPool(config, "localhost", 6379);

            if (testConnection(pool)) {
                System.out.println("[RedisLogDAO] Redis conectado exitosamente en localhost:6379");
                return pool;
            } else {
                pool.close();
                System.err.println("[RedisLogDAO] No se pudo conectar a Redis");
                return null;
            }
        } catch (Exception e) {
            System.err.println("[RedisLogDAO] Error inicializando Redis: " + e.getMessage());
            return null;
        }
    }

    private boolean testConnection() {
        return testConnection(this.jedisPool);
    }

    private boolean testConnection(JedisPool pool) {
        if (pool == null) return false;
        try (Jedis jedis = pool.getResource()) {
            String response = jedis.ping();
            return "PONG".equals(response);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void addLog(String entry) {
        if (!available || jedisPool == null) {
            throw new RuntimeException("Redis no esta disponible");
        }

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.rpush(redisKey, entry);

            long logCount = jedis.llen(redisKey);
            if (logCount > maxLogsInMemory) {
                jedis.ltrim(redisKey, logCount - maxLogsInMemory, -1);
            }
        } catch (Exception e) {
            available = false;
            throw new RuntimeException("Error agregando log a Redis: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getAllLogs() {
        if (!available || jedisPool == null) {
            return new ArrayList<>();
        }

        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.lrange(redisKey, 0, -1);
        } catch (Exception e) {
            available = false;
            System.err.println("[RedisLogDAO] Error obteniendo logs: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getLastLogs(int count) {
        if (!available || jedisPool == null) {
            return new ArrayList<>();
        }

        try (Jedis jedis = jedisPool.getResource()) {
            long totalLogs = jedis.llen(redisKey);
            long start = Math.max(0, totalLogs - count);
            return jedis.lrange(redisKey, start, -1);
        } catch (Exception e) {
            available = false;
            System.err.println("[RedisLogDAO] Error obteniendo ultimos logs: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void clearLogs() {
        if (!available || jedisPool == null) {
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(redisKey);
        } catch (Exception e) {
            available = false;
            System.err.println("[RedisLogDAO] Error limpiando logs: " + e.getMessage());
        }
    }

    @Override
    public void deleteLogs() {
        clearLogs();
    }

    @Override
    public long getLogCount() {
        if (!available || jedisPool == null) {
            return 0;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.llen(redisKey);
        } catch (Exception e) {
            available = false;
            System.err.println("[RedisLogDAO] Error obteniendo cuenta de logs: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public boolean isAvailable() {
        if (!available) {
            available = testConnection();
        }
        return available;
    }

    @Override
    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
            System.out.println("[RedisLogDAO] Conexion a Redis cerrada");
        }
    }
}
