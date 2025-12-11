package server ;
import java.io.*;
import  java.net.*;
import java.util.concurrent.*;

/*handles individual client connections
 Implements the calculation protocol
 Validate inputs and performs operations*/
class ClientHandler implements Runnable {
    private Socket clientSocket;
    private OperationLogger logger;
    private BufferedReader in;
    private PrintWriter out;
    private String clientInfo;
//Constructor for ClientHandler 
    public ClientHandler(Socket socket, OperationLogger logger) {
        this.clientSocket = socket;
        this.logger = logger;
        this.clientInfo = socket.getInetAddress().getHostAddress() + 
                         ":" + socket.getPort();
    }
//Main run method for handling client communication
    @Override
    public void run() {
        try {
            in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream())
            );
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String line;
            Double number1 = null;
            Double number2 = null;
            String operator = null;

            while ((line = in.readLine()) != null) {
                line = line.trim();
                
                if (line.isEmpty()) continue;

                // Parse protocol messages
                if (line.startsWith("NUMBER:")) {
                    String valueStr = line.substring(7).trim();
                    try {
                        double value = Double.parseDouble(valueStr);
                        if (number1 == null) {
                            number1 = value;
                            logger.log("INPUT", clientInfo + " | NUMBER1: " + value);
                        } else if (number2 == null) {
                            number2 = value;
                            logger.log("INPUT", clientInfo + " | NUMBER2: " + value);
                        }
                    } catch (NumberFormatException e) {
                        sendError("Invalid number format: " + valueStr);
                        logger.log("ERROR", clientInfo + " | Invalid format: " + valueStr);
                        // Reset state
                        number1 = null;
                        number2 = null;
                        operator = null;
                        continue;
                    }
                } 
                else if (line.startsWith("OPERATOR:")) {
                    operator = line.substring(9).trim();
                    logger.log("INPUT", clientInfo + " | OPERATOR: " + operator);
                    
                    // Validate operator
                    if (!isValidOperator(operator)) {
                        sendError("Invalid operator: " + operator);
                        logger.log("ERROR", clientInfo + " | Invalid operator: " + operator);
                        // Reset state
                        number1 = null;
                        number2 = null;
                        operator = null;
                        continue;
                    }

                    // Perform calculation if we have all inputs
                    if (number1 != null && number2 != null && operator != null) {
                        performCalculation(number1, number2, operator);
                        // Reset for next operation
                        number1 = null;
                        number2 = null;
                        operator = null;
                    }
                } 
                else {
                    sendError("Protocol error: Invalid message format");
                    logger.log("ERROR", clientInfo + " | Protocol error: " + line);
                }
            }
        } catch (IOException e) {
            logger.log("ERROR", clientInfo + " | Connection error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    
     //Validates operator
    private boolean isValidOperator(String op) {
        return op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/");
    }

    
     //Perform calculation and send the result to client
    private void performCalculation(double num1, double num2, String op) {
        try {
            double result = 0;
            
            switch (op) {
                case "+":
                    result = num1 + num2;
                    break;
                case "-":
                    result = num1 - num2;
                    break;
                case "*":
                    result = num1 * num2;
                    break;
                case "/":
                    if (num2 == 0) {
                        sendError("Division by zero");
                        logger.log("ERROR", clientInfo + " | Division by zero: " + 
                                  num1 + " / " + num2);
                        return;
                    }
                    result = num1 / num2;
                    break;
            }

            String resultMsg = "RESULT:" + result;
            out.println(resultMsg);
            
            String operation = num1 + " " + op + " " + num2 + " = " + result;
            logger.log("CALCULATION", clientInfo + " | " + operation);
            System.out.println("→ Calculation: " + operation + " [" + clientInfo + "]");
            
        } catch (Exception e) {
            sendError("Calculation error: " + e.getMessage());
            logger.log("ERROR", clientInfo + " | Calculation error: " + e.getMessage());
        }
    }

    
     //Send error message to client
    private void sendError(String message) {
        out.println("ERROR:" + message);
    }

    
      //Close connections and logs disconnection
    private void cleanup() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            logger.log("CLIENT_DISCONNECT", clientInfo);
            System.out.println("Client disconnected: " + clientInfo);
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}