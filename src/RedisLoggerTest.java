import utils.Logger;
import java.util.List;

public class RedisLoggerTest {
    public static void main(String[] args) {
        Logger logger = Logger.getInstance();

        System.out.println("\n=== Prueba de Logger con Redis ===\n");

        logger.info("Iniciando test del logger");
        logger.debug("Este es un mensaje de debug");
        logger.warning("Este es un warning");
        logger.error("Este es un error");
        logger.success("¡Operación exitosa!");

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\n=== Últimos 5 logs desde Redis ===\n");
        List<String> logs = logger.getLastLogs(5);
        for (int i = 0; i < logs.size(); i++) {
            System.out.println((i + 1) + ". " + logs.get(i));
        }

        System.out.println("\n=== Total de logs en Redis ===\n");
        List<String> allLogs = logger.getLogs();
        System.out.println("Total: " + allLogs.size() + " logs almacenados en Redis");

        System.out.println("\n✅ Test completado exitosamente\n");
    }
}

