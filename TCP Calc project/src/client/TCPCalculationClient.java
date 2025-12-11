import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TCPCalculationClient {
    
    // server config
    
    private static final String SERVER_IP = "localhost";  // ip ml server
    private static final int SERVER_PORT = 12345;         // port ml server
    
    
    // Main
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        displayWelcomeMessage();
        
        boolean running = true;
        
        while (running) {
            try {
                // Get calc input from user
                CalculationInput input = getUserInput(scanner);
                
                // protocol format 
                String request = formatRequest(input);
                System.out.println("\n[DEBUG] Formatted request to send:");
                System.out.println("---");
                System.out.println(request);
                System.out.println("---");
                
            
                // server connection
                // Uncomment when server is ready
                /*
                System.out.println("\n[CONNECTING] Attempting to connect to server...");
                String serverResponse = connectAndSend(request);
                displayResult(serverResponse);
                */
                
                //testing, remove when server is ready
                System.out.println("\n[MOCK MODE] Server connection not implemented yet.");
                System.out.println("Waiting for Person 1 to complete TCP server.");
                String mockResponse = generateMockResponse(input);
                displayResult(mockResponse);
                
            } catch (Exception e) {
                System.out.println("\n[ERROR] Client error: " + e.getMessage());
            }
            
        
            running = askToContinue(scanner);
        }
        
        System.out.println("\n[INFO] TCP Calculation Client shutting down. Goodbye!");
        scanner.close();
    }
    
    // user input 
    
    private static void displayWelcomeMessage() {
        System.out.println("=".repeat(50));
        System.out.println("TCP CALCULATION CLIENT");
        System.out.println("=".repeat(50));
        System.out.println("Server: " + SERVER_IP + ":" + SERVER_PORT);
        System.out.println("Protocol: NUMBER:<value>, OPERATOR:<+|-|*|/>");
        System.out.println("=".repeat(50) + "\n");
    }
    
    private static CalculationInput getUserInput(Scanner scanner) {
        CalculationInput input = new CalculationInput();
        
        System.out.println("\n" + "=".repeat(30));
        System.out.println("ENTER CALCULATION");
        System.out.println("=".repeat(30));
        
        input.num1 = getValidNumber(scanner, "Enter first number: ");
        input.num2 = getValidNumber(scanner, "Enter second number: ");
        input.operator = getValidOperator(scanner);
        
        return input;
    }
    
    private static double getValidNumber(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Invalid number! Please enter a valid number (e.g., 5, 3.14, -2.5)");
            }
        }
    }
    
    private static char getValidOperator(Scanner scanner) {
        final String validOperators = "+-*/";
        
        while (true) {
            System.out.print("Enter operator (+, -, *, /): ");
            String input = scanner.nextLine().trim();
            
            if (input.length() == 1 && validOperators.contains(input)) {
                return input.charAt(0);
            }
            System.out.println("[ERROR] Invalid operator! Please use one of: +, -, *, /");
        }
    }
    
    private static boolean askToContinue(Scanner scanner) {
        while (true) {
            System.out.print("\nPerform another calculation? (yes/no): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            
            if (choice.equals("yes") || choice.equals("y")) {
                return true;
            } else if (choice.equals("no") || choice.equals("n")) {
                return false;
            } else {
                System.out.println("[ERROR] Please enter 'yes' or 'no'");
            }
        }
    }
    
    private static String formatRequest(CalculationInput input) {
        // protocol format : NUMBER:<value> (each on separate line)
        return "NUMBER:" + input.num1 + "\n" +
               "NUMBER:" + input.num2 + "\n" +
               "OPERATOR:" + input.operator;
    }
    
    
    // server connection
    
    private static String connectAndSend(String request) {
        //replace with actual implementation when server is ready
        // need server for connection timeout requirements
        // need server for error response formats
        
        /*
        implementation
        
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
            socket.setSoTimeout(5000); // 5 second timeout
            
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Send request
            out.print(request);
            out.flush();
            
            // Read response
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
                if (in.ready()) response.append("\n");
            }
            
            return response.toString();
            
        } catch (SocketTimeoutException e) {
            return "ERROR: Server timeout - no response after 5 seconds";
        } catch (ConnectException e) {
            return "ERROR: Cannot connect to server at " + SERVER_IP + ":" + SERVER_PORT;
        } catch (IOException e) {
            return "ERROR: Network error - " + e.getMessage();
        }
        */
        
        return "ERROR: Server connection not implemented yet";
    }
    
    // response handling
    
    private static void displayResult(String response) {
        System.out.println("\n" + "=".repeat(30));
        System.out.println("RESULT");
        System.out.println("=".repeat(30));
        
        if (response.startsWith("RESULT:")) {
            String result = response.substring(7);
            System.out.println("✓ Success: " + result);
        } else if (response.startsWith("ERROR:")) {
            String error = response.substring(6);
            System.out.println("✗ Error: " + error);
        } else {
            System.out.println("? Unknown response format: " + response);
        }
    }
    
    // test response
    
    private static String generateMockResponse(CalculationInput input) {
        try {
            double result;
            
            switch (input.operator) {
                case '+':
                    result = input.num1 + input.num2;
                    break;
                case '-':
                    result = input.num1 - input.num2;
                    break;
                case '*':
                    result = input.num1 * input.num2;
                    break;
                case '/':
                    if (input.num2 == 0) {
                        return "ERROR:Division by zero";
                    }
                    result = input.num1 / input.num2;
                    break;
                default:
                    return "ERROR:Invalid operator";
            }
            
            // Format to 2 decimal places if needed
            if (result == (long) result) {
                return "RESULT:" + (long) result;
            } else {
                return "RESULT:" + String.format("%.2f", result);
            }
            
        } catch (Exception e) {
            return "ERROR:Mock calculation failed";
        }
    }
    
    // helper class for organization
    
    static class CalculationInput {
        double num1;
        double num2;
        char operator;
    }
}