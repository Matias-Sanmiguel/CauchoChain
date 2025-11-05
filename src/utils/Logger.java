package utils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Logger {
    private final List<String> logs; // Marcado como final para evitar reasignación

    public Logger() {
        this(null);
    }

    public Logger(List<String> logs) {
        this.logs = logs != null ? logs : new ArrayList<>();
    }


    public synchronized void log(String message) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME); // es el estandar para los Logs
        logs.add("[" + ts + "] " + message);
    }


    public synchronized List<String> getLogs() {
        return new ArrayList<>(logs);
    }

    // esto lo hacemos porque cada tanto tenemos que limpiar los logs para que no se haga demasiado pesado, el garbage colector no sirve porque siempre está en uso
    public synchronized void clear() {
        logs.clear();
    }

    //escribe esto en un archivo de texto
    public synchronized void exportLogs(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, logs);
    }
}
