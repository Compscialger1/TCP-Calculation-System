package server ;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;



/* server folder to do list 
1-stream socket implmentation with all functions, 
2-using the protocol for what to send and revieve
protocol of request : 
NUMBER : < value >
NUMBER : < value >
OPERATOR :+ | - | * | /
protocol of response :
RESULT : < value >
ERROR : < error message >
3-multi threading for multiple clients
4-calculation functions with error handling
5-logging system
  */
public class TCPServer {
    
     private static final int PORT = 8080;
    private static final int MAX_THREADS = 50;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private OperationLogger logger;
    private volatile boolean running;

    public TCPServer() {
        this.threadPool = Executors.newFixedThreadPool(MAX_THREADS);
        this.logger = new OperationLogger("../documentation/Syslogs.txt");
        this.running = false;
    }

    
     //Start the TCP server and accept client connections
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            logger.log("SERVER_START", "Server started on port " + PORT);
            System.out.println("TCP Calculation Server started on port " + PORT);
            System.out.println(" Waiting for client connections...");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientInfo = clientSocket.getInetAddress().getHostAddress() + 
                                      ":" + clientSocket.getPort();
                    System.out.println(" New client connected: " + clientInfo);
                    logger.log("CLIENT_CONNECT", clientInfo);
                    
                    // Handle each client in a separate thread
                    threadPool.execute(new ClientHandler(clientSocket, logger));
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client: " + e.getMessage());
                        logger.log("ERROR", "Accept failed: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println(" Failed to start server: " + e.getMessage());
            logger.log("ERROR", "Server start failed: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    
     // shut down the server
     
    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
            logger.log("SERVER_STOP", "Server shutdown complete");
            logger.close();
            System.out.println(" Server stopped successfully");
        } catch (Exception e) {
            System.err.println(" Error during shutdown: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        TCPServer server = new TCPServer();
        
        // Add shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\\n→ Shutting down server...");
            server.shutdown();
        }));
        
        server.start();
    }

    
}
