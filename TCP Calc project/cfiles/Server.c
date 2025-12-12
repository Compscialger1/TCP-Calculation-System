#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <ctype.h>

int connectToServer(const char *host, int port);
int sendMessage(int sock, const char *msg);
int receiveMessage(int sock, char *buffer, size_t maxLen);
double getNumberFromUser(const char *prompt);
char getOperatorFromUser(void);
void displayResult(const char *response);

int main() {
    printf("\n=== Welcome to the Calculator Client ===\n\n");
    
    int serverSock = connectToServer("127.0.0.1", 5000);
    if (serverSock < 0) {
        printf("Sorry, I couldn't connect to the server.\n");
        printf("Make sure the server is running on port 5000.\n");
        return 1;
    }
    
    printf("Successfully connected to the server!\n\n");
    
    double num1 = getNumberFromUser("Enter the first number");
    
    char msg[256];
    snprintf(msg, sizeof(msg), "NUMBER:%.2f\n", num1);
    if (sendMessage(serverSock, msg) < 0) {
        printf("Error: Couldn't send the first number to the server.\n");
        close(serverSock);
        return 1;
    }
    
    double num2 = getNumberFromUser("Enter the second number");
    
    snprintf(msg, sizeof(msg), "NUMBER:%.2f\n", num2);
    if (sendMessage(serverSock, msg) < 0) {
        printf("Error: Couldn't send the second number to the server.\n");
        close(serverSock);
        return 1;
    }
    
    char op = getOperatorFromUser();
    
    snprintf(msg, sizeof(msg), "OPERATOR:%c\n", op);
    if (sendMessage(serverSock, msg) < 0) {
        printf("Error: Couldn't send the operation to the server.\n");
        close(serverSock);
        return 1;
    }
    
    char response[512];
    if (receiveMessage(serverSock, response, sizeof(response)) < 0) {
        printf("Error: Couldn't receive the answer from the server.\n");
        close(serverSock);
        return 1;
    }
    
    displayResult(response);
    
    close(serverSock);
    printf("\nThank you for using the calculator!\n\n");
    
    return 0;
}

int connectToServer(const char *host, int port) {
    int sock;
    struct sockaddr_in serverAddr;
    
    printf("Connecting to server at %s:%d...\n", host, port);
    
    sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0) {
        perror("Couldn't create socket");
        return -1;
    }
    
    memset(&serverAddr, 0, sizeof(serverAddr));
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(port);
    
    if (inet_pton(AF_INET, host, &serverAddr.sin_addr) <= 0) {
        perror("Invalid IP address");
        close(sock);
        return -1;
    }
    
    if (connect(sock, (struct sockaddr*)&serverAddr, sizeof(serverAddr)) < 0) {
        perror("Couldn't connect to server");
        close(sock);
        return -1;
    }
    
    return sock;
}

int sendMessage(int sock, const char *msg) {
    if (!msg) {
        return -1;
    }
    
    ssize_t sent = send(sock, msg, strlen(msg), 0);
    if (sent < 0) {
        perror("Error sending message");
        return -1;
    }
    
    return 0;
}

int receiveMessage(int sock, char *buffer, size_t maxLen) {
    if (!buffer || maxLen < 2) {
        return -1;
    }
    
    size_t total = 0;
    char c;
    
    while (total < maxLen - 1) {
        ssize_t n = recv(sock, &c, 1, 0);
        
        if (n < 0) {
            perror("Error receiving message");
            return -1;
        } else if (n == 0) {
            printf("Server closed the connection.\n");
            return -1;
        }
        
        if (c == '\n') {
            buffer[total] = '\0';
            return (int)total;
        }
        
        if (c == '\r') {
            continue;
        }
        
        buffer[total++] = c;
    }
    
    buffer[total] = '\0';
    return (int)total;
}

double getNumberFromUser(const char *prompt) {
    char input[256];
    double num;
    int valid = 0;
    
    while (!valid) {
        printf("%s: ", prompt);
        fflush(stdout);
        
        if (fgets(input, sizeof(input), stdin) == NULL) {
            printf("Error reading input.\n");
            continue;
        }
        
        size_t len = strlen(input);
        if (len > 0 && input[len - 1] == '\n') {
            input[len - 1] = '\0';
        }
        
        char *endptr;
        num = strtod(input, &endptr);
        
        if (endptr != input && (*endptr == '\0' || *endptr == ' ' || *endptr == '\n')) {
            valid = 1;
        } else {
            printf("That doesn't look like a number. Try again.\n");
        }
    }
    
    return num;
}

char getOperatorFromUser(void) {
    char input[256];
    char op;
    int valid = 0;
    
    printf("\nWhat do you want to do?\n");
    printf("  + for addition\n");
    printf("  - for subtraction\n");
    printf("  * for multiplication\n");
    printf("  / for division\n");
    
    while (!valid) {
        printf("Choose an operation (+, -, *, /): ");
        fflush(stdout);
        
        if (fgets(input, sizeof(input), stdin) == NULL) {
            printf("Error reading input.\n");
            continue;
        }
        
        int i = 0;
        while (input[i] && isspace(input[i])) {
            i++;
        }
        
        op = input[i];
        
        if (op == '+' || op == '-' || op == '*' || op == '/') {
            valid = 1;
        } else {
            printf("That's not a valid operation. Please use +, -, *, or /\n");
        }
    }
    
    return op;
}

void displayResult(const char *response) {
    if (!response) {
        return;
    }
    
    printf("\n");
    
    if (strncmp(response, "RESULT:", 7) == 0) {
        printf("The answer is: %s\n", response + 7);
    } else if (strncmp(response, "ERROR:", 6) == 0) {
        printf("Oops! The server said: %s\n", response + 6);
    } else {
        printf("Got an unexpected response: %s\n", response);
    }
}