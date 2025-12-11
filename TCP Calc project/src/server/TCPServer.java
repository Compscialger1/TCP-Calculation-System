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
 /*TCPserver tasks:
 create server socket
    listen for client connections
    handle multiple clients concurrently
    integrate logging mechanism
    error handling and resource management
    shutdown procedure with hook and forced shutdownnow 
  */
public class TCPServer {
    
     private static final int PORT = 8080; //port number for server 
    private static final int MAX_THREADS = 8; //max client threads
    private ServerSocket serverSocket; //initalise server socket class (java.net)
    private ExecutorService threadPool; //thread pool for client threads
    private OperationLogger logger; //logger instance
    private volatile boolean running; //server running flag

    
     //Constructor for TCPServer

    public TCPServer() {
        this.serverSocket = null; //initialise server socket
        this.threadPool = Executors.newFixedThreadPool(MAX_THREADS); //fixed thread pool
        this.logger = new OperationLogger("documentation/Syslogs.txt"); //logger with log file path
        this.running = false;
    }

    
     //Start the TCP server and accept client connections
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT); //bind server socket to port
            running = true; //set running flag
            logger.log("SERVER_START", "Server started on port " + PORT); //log server start
            System.out.println("TCP Calculation Server started on port " + PORT); //console message
            System.out.println(" Waiting for client connections..."); //console message

             //Main server loop to accept clients

            while (running) { //accept clients with infinite loop 
                try {
                    Socket clientSocket = serverSocket.accept(); //accept client connection
                    String clientInfo = clientSocket.getInetAddress().getHostAddress() + 
                                      ":" + clientSocket.getPort(); //get client info
                    System.out.println(" New client connected: " + clientInfo);  //consol message
                    logger.log("CLIENT_CONNECT", clientInfo); //log client connection
                    
                    // Handle each client in a separate thread
                    threadPool.execute(new ClientHandler(clientSocket, logger)); //new client handler thread if clients less than max threads
                } catch (IOException e) { //error handling for client accept 
                    if (running) {
                        System.err.println("Error accepting client: " + e.getMessage());
                        logger.log("ERROR", "Accept failed: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) { //error handling for server socket
            System.err.println(" Failed to start server: " + e.getMessage()); //print errors 
            logger.log("ERROR", "Server start failed: " + e.getMessage()); //log errors 
        } finally { //call server shutdown method
            shutdown();
        }
    }

    
     // shut down the server method implement
     
    public void shutdown() {
        running = false; //set running flag to false
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); //close server socket
            }
            threadPool.shutdown();
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow(); //force shutdown if not terminated
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
         //graceful shutdown hook for cleanup  
         /*shut down hook is a a specialized construct in the Java Virtual Machine (JVM)
          that allows developers to register an arbitrary code block to run when the program
         shuts down for releasing recources , cleanup ,logging*/
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { 
            System.out.println("Shutting down server...");//console message
            server.shutdown();
        }));
        
        server.start(); //start server
    }

    
}
