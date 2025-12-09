package client ;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TCPCalculationClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;
    private boolean connected;

    public TCPCalculationClient() {
        this.scanner = new Scanner(System.in);
        this.connected = false;
    }

    /**
     * Connects to the calculation server
     */
    public boolean connect() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            connected = true;
            
            System.out.println("✓ Connected to server at " + SERVER_HOST + ":" + SERVER_PORT);
            System.out.println("✓ Ready to send calculations\n");
            return true;
            
        } catch (UnknownHostException e) {
            System.err.println("✗ Error: Unknown host " + SERVER_HOST);
            return false;
        } catch (IOException e) {
            System.err.println("✗ Error: Could not connect to server");
            System.err.println("  Make sure the server is running on port " + SERVER_PORT);
            return false;
        }
    }

    /**
     * Main client loop - handles user interaction
     */
    public void start() {
        if (!connected) {
            System.err.println("✗ Not connected to server");
            return;
        }

        System.out.println("========================================");
        System.out.println("   TCP CALCULATION CLIENT");
        System.out.println("========================================");
        System.out.println("Commands:");
        System.out.println("  - Enter two numbers and an operator");
        System.out.println("  - Type 'exit' or 'quit' to disconnect");
        System.out.println("========================================\n");

        try {
            while (connected) {
                // Get first number
                System.out.print("Enter first number (or 'exit' to quit): ");
                String input1 = scanner.nextLine().trim();
                
                if (input1.equalsIgnoreCase("exit") || input1.equalsIgnoreCase("quit")) {
                    System.out.println("\n→ Disconnecting...");
                    break;
                }

                // Validate first number
                double num1;
                try {
                    num1 = Double.parseDouble(input1);
                } catch (NumberFormatException e) {
                    System.err.println("✗ Invalid number format. Please try again.\n");
                    continue;
                }

                // Get second number
                System.out.print("Enter second number: ");
                String input2 = scanner.nextLine().trim();
                
                if (input2.equalsIgnoreCase("exit") || input2.equalsIgnoreCase("quit")) {
                    System.out.println("\n→ Disconnecting...");
                    break;
                }

                // Validate second number
                double num2;
                try {
                    num2 = Double.parseDouble(input2);
                } catch (NumberFormatException e) {
                    System.err.println("✗ Invalid number format. Please try again.\n");
                    continue;
                }

                // Get operator
                System.out.print("Enter operator (+, -, *, /): ");
                String operator = scanner.nextLine().trim();
                
                if (operator.equalsIgnoreCase("exit") || operator.equalsIgnoreCase("quit")) {
                    System.out.println("\n→ Disconnecting...");
                    break;
                }

                // Validate operator
                if (!operator.equals("+") && !operator.equals("-") && 
                    !operator.equals("*") && !operator.equals("/")) {
                    System.err.println("✗ Invalid operator. Use +, -, *, or /\n");
                    continue;
                }

                // Send calculation request
                sendCalculation(num1, num2, operator);
                
                // Receive and display result
                receiveResult();
                
                System.out.println(); // Empty line for readability
            }
            
        } catch (Exception e) {
            System.err.println("✗ Error during communication: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    /**
     * Sends a calculation request to the server
     */
    private void sendCalculation(double num1, double num2, String operator) {
        try {
            System.out.println("\n→ Sending: " + num1 + " " + operator + " " + num2);
            
            // Send according to protocol
            out.println("NUMBER:" + num1);
            out.println("NUMBER:" + num2);
            out.println("OPERATOR:" + operator);
            
        } catch (Exception e) {
            System.err.println("✗ Error sending data: " + e.getMessage());
            connected = false;
        }
    }

    /**
     * Receives and displays the result from the server
     */
    private void receiveResult() {
        try {
            // Read response from server
            String response = in.readLine();
            
            if (response == null) {
                System.err.println("✗ Server disconnected");
                connected = false;
                return;
            }

            // Parse response
            if (response.startsWith("RESULT:")) {
                String resultValue = response.substring(7).trim();
                System.out.println("✓ Result: " + resultValue);
                
            } else if (response.startsWith("ERROR:")) {
                String errorMsg = response.substring(6).trim();
                System.err.println("✗ Server Error: " + errorMsg);
                
            } else {
                System.err.println("✗ Unknown response format: " + response);
            }
            
        } catch (IOException e) {
            System.err.println("✗ Error receiving result: " + e.getMessage());
            connected = false;
        }
    }

    /**
     * Disconnects from the server and closes resources
     */
    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (scanner != null) scanner.close();
            
            connected = false;
            System.out.println("✓ Disconnected from server");
            
        } catch (IOException e) {
            System.err.println("✗ Error closing connection: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        TCPCalculationClient client = new TCPCalculationClient();
        
        // Connect to server
        if (client.connect()) {
            // Start interactive session
            client.start();
        } else {
            System.err.println("\n✗ Failed to connect. Exiting...");
        }
    }
}
