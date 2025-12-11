package server;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*logger tasks :
log operations with types : error,result,input,system,client connection, calculation,server start with format */
public class OperationLogger {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private String logFilePath;
    private PrintWriter writer;

    public OperationLogger(String logFilePath) {
        this.logFilePath = logFilePath;
       
        try {
            writer = new PrintWriter(new FileWriter(logFilePath, true), true);
            log("SYSTEM", "Logger initialized");
        } catch (IOException e) {
            System.err.println(" Failed to initialize logger: " + e.getMessage());
        }
    }

      //Logs a message with timestamp and type
    public synchronized void log(String type, String message) {
        if (writer != null) {
            String timestamp = LocalDateTime.now().format(formatter);
            String logEntry = String.format("[%s] [%s] %s", timestamp, type, message); // Format log entry as [timestamp] [type] message
            writer.println(logEntry);
            writer.flush();
        }
    }

    
      //Closes the logger
    public synchronized void close() {
        if (writer != null) {
            log("SYSTEM", "Logger closing");
            writer.close();
        }
    }
}
