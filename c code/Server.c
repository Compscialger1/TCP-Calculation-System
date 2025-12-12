#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <pthread.h>
#include <time.h>
#include <signal.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <errno.h>
#include <ctype.h>

pthread_mutex_t logMutex = PTHREAD_MUTEX_INITIALIZER;
FILE *logFile = NULL;

volatile int serverRunning = 1;
int globalServerSock = -1;

int startServer(int port);
void *handleClient(void *clientSocket);
int parseNumber(const char *msg, double *out);
int parseOperator(const char *msg, char *op);
void sendResult(int clientSock, double result);
void sendError(int clientSock, const char *errorMsg);
void logMessage(const char *text);
void getCurrentTime(char *buffer, size_t bufferSize);
int readLineFromSocket(int sock, char *buffer, size_t maxLen);
void handleShutdown(int signum);

void handleShutdown(int signum) {
    if (signum == SIGINT || signum == SIGTERM) {
        logMessage("Shutting down the server...");
        serverRunning = 0;
        if (globalServerSock >= 0) {
            close(globalServerSock);
            globalServerSock = -1;
        }
    }
}

int main() {
    signal(SIGINT, handleShutdown);
    signal(SIGTERM, handleShutdown);
    
    logFile = fopen("server_log.txt", "a");
    if (!logFile) {
        perror("Couldn't open log file");
        return 1;
    }
    
    logMessage("Starting up the calculation server on port 5000...");
    
    int serverSock = startServer(5000);
    if (serverSock < 0) {
        logMessage("Oops! The server failed to start");
        fclose(logFile);
        return 1;
    }
    
    globalServerSock = serverSock;
    logMessage("Server is ready! Waiting for clients to connect...");
    
    while (serverRunning) {
        struct sockaddr_in clientAddr;
        socklen_t clientLen = sizeof(clientAddr);
        
        int *clientSock = malloc(sizeof(int));
        if (!clientSock) {
            perror("Memory problem");
            logMessage("Error: Ran out of memory for a new client");
            continue;
        }
        
        *clientSock = accept(serverSock, (struct sockaddr*)&clientAddr, &clientLen);
        
        if (*clientSock < 0) {
            if (serverRunning) {
                perror("Accept failed");
                logMessage("Error: Failed to accept a client connection");
            }
            free(clientSock);
            continue;
        }
        
        char logMsg[256];
        snprintf(logMsg, sizeof(logMsg), "New client connected from %s (port %d)",
                 inet_ntoa(clientAddr.sin_addr), ntohs(clientAddr.sin_port));
        logMessage(logMsg);
        
        pthread_t thread;
        if (pthread_create(&thread, NULL, handleClient, clientSock) != 0) {
            perror("Couldn't create thread");
            logMessage("Error: Failed to create a thread for the new client");
            close(*clientSock);
            free(clientSock);
        } else {
            pthread_detach(thread);
        }
    }
    
    logMessage("Server has stopped");
    if (serverSock >= 0) {
        close(serverSock);
    }
    if (logFile) {
        fclose(logFile);
    }
    pthread_mutex_destroy(&logMutex);
    
    return 0;
}

int startServer(int port) {
    int serverSock;
    struct sockaddr_in serverAddr;
    
    serverSock = socket(AF_INET, SOCK_STREAM, 0);
    if (serverSock < 0) {
        perror("Couldn't create socket");
        return -1;
    }
    
    int opt = 1;
    if (setsockopt(serverSock, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) < 0) {
        perror("Socket option failed");
        close(serverSock);
        return -1;
    }
    
    memset(&serverAddr, 0, sizeof(serverAddr));
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_addr.s_addr = INADDR_ANY;
    serverAddr.sin_port = htons(port);
    
    if (bind(serverSock, (struct sockaddr*)&serverAddr, sizeof(serverAddr)) < 0) {
        perror("Bind failed");
        close(serverSock);
        return -1;
    }
    
    if (listen(serverSock, 10) < 0) {
        perror("Listen failed");
        close(serverSock);
        return -1;
    }
    
    return serverSock;
}

int readLineFromSocket(int sock, char *buffer, size_t maxLen) {
    size_t total = 0;
    char c;
    
    if (!buffer || maxLen < 2) {
        return -1;
    }
    
    while (total < maxLen - 1) {
        int n = recv(sock, &c, 1, 0);
        
        if (n < 0) {
            return -1;
        } else if (n == 0) {
            if (total > 0) {
                return -1;
            }
            return 0;
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
    return -2;
}

void *handleClient(void *arg) {
    int clientSock = *(int*)arg;
    free(arg);
    
    if (clientSock < 0) {
        logMessage("Error: Got an invalid socket for a client");
        return NULL;
    }
    
    char line1[256], line2[256], line3[256];
    double num1, num2, result;
    char op;
    int len;
    
    len = readLineFromSocket(clientSock, line1, sizeof(line1));
    if (len == 0) {
        logMessage("Client closed the connection without sending anything");
        close(clientSock);
        return NULL;
    } else if (len == -2) {
        logMessage("Error: First message was too long");
        sendError(clientSock, "Your message was too long");
        close(clientSock);
        return NULL;
    } else if (len < 0) {
        logMessage("Error: Trouble reading from the client");
        close(clientSock);
        return NULL;
    }
    
    if (parseNumber(line1, &num1) != 0) {
        char errMsg[256];
        snprintf(errMsg, sizeof(errMsg), "Couldn't understand the first number: %s", line1);
        logMessage(errMsg);
        sendError(clientSock, "I didn't understand your first number");
        close(clientSock);
        return NULL;
    }
    
    len = readLineFromSocket(clientSock, line2, sizeof(line2));
    if (len == 0) {
        logMessage("Error: Client disconnected while sending the second number");
        close(clientSock);
        return NULL;
    } else if (len == -2) {
        logMessage("Error: Second message was too long");
        sendError(clientSock, "Your message was too long");
        close(clientSock);
        return NULL;
    } else if (len < 0) {
        logMessage("Error: Trouble reading from the client");
        close(clientSock);
        return NULL;
    }
    
    if (parseNumber(line2, &num2) != 0) {
        char errMsg[256];
        snprintf(errMsg, sizeof(errMsg), "Couldn't understand the second number: %s", line2);
        logMessage(errMsg);
        sendError(clientSock, "I didn't understand your second number");
        close(clientSock);
        return NULL;
    }
    
    len = readLineFromSocket(clientSock, line3, sizeof(line3));
    if (len == 0) {
        logMessage("Error: Client disconnected while sending the operation");
        close(clientSock);
        return NULL;
    } else if (len == -2) {
        logMessage("Error: Operation message was too long");
        sendError(clientSock, "Your message was too long");
        close(clientSock);
        return NULL;
    } else if (len < 0) {
        logMessage("Error: Trouble reading from the client");
        close(clientSock);
        return NULL;
    }
    
    if (parseOperator(line3, &op) != 0) {
        char errMsg[256];
        snprintf(errMsg, sizeof(errMsg), "Couldn't understand the operation: %s", line3);
        logMessage(errMsg);
        sendError(clientSock, "I didn't understand your operation");
        close(clientSock);
        return NULL;
    }
    
    char requestLog[256];
    snprintf(requestLog, sizeof(requestLog), "Calculating: %.2f %c %.2f", num1, op, num2);
    logMessage(requestLog);
    
    int error = 0;
    switch (op) {
        case '+':
            result = num1 + num2;
            break;
        case '-':
            result = num1 - num2;
            break;
        case '*':
            result = num1 * num2;
            break;
        case '/':
            if (num2 == 0.0 || num2 == -0.0) {
                logMessage("Error: Client tried to divide by zero");
                sendError(clientSock, "Can't divide by zero!");
                error = 1;
            } else {
                result = num1 / num2;
            }
            break;
        default:
            logMessage("Error: Unknown operation");
            sendError(clientSock, "Unknown operation");
            error = 1;
            break;
    }
    
    if (!error) {
        char resultLog[256];
        snprintf(resultLog, sizeof(resultLog), "Answer: %.2f", result);
        logMessage(resultLog);
        
        sendResult(clientSock, result);
    }
    
    close(clientSock);
    logMessage("Client disconnected");
    
    return NULL;
}

int parseNumber(const char *msg, double *out) {
    if (!msg || !out) {
        return -1;
    }
    
    const char *colon = strchr(msg, ':');
    if (!colon) {
        return -1;
    }
    
    if (strncmp(msg, "NUMBER", 6) != 0) {
        return -1;
    }
    
    if (msg + 6 != colon) {
        return -1;
    }
    
    const char *valueStr = colon + 1;
    
    while (*valueStr && isspace(*valueStr)) {
        valueStr++;
    }
    
    if (*valueStr == '\0') {
        return -1;
    }
    
    const char *ptr = valueStr;
    int dotCount = 0;
    int digitCount = 0;
    
    if (*ptr == '+' || *ptr == '-') {
        ptr++;
    }
    
    if (*ptr == '\0') {
        return -1;
    }
    
    while (*ptr && !isspace(*ptr)) {
        if (isdigit(*ptr)) {
            digitCount++;
        } else if (*ptr == '.') {
            dotCount++;
            if (dotCount > 1) {
                return -1;
            }
        } else {
            return -1;
        }
        ptr++;
    }
    
    if (digitCount == 0) {
        return -1;
    }
    
    while (*ptr) {
        if (!isspace(*ptr)) {
            return -1;
        }
        ptr++;
    }
    
    char *endptr;
    *out = strtod(valueStr, &endptr);
    
    if (endptr == valueStr) {
        return -1;
    }
    
    return 0;
}

int parseOperator(const char *msg, char *op) {
    if (!msg || !op) {
        return -1;
    }
    
    const char *colon = strchr(msg, ':');
    if (!colon) {
        return -1;
    }
    
    if (strncmp(msg, "OPERATOR", 8) != 0) {
        return -1;
    }
    
    if (msg + 8 != colon) {
        return -1;
    }
    
    const char *opStr = colon + 1;
    
    while (*opStr && isspace(*opStr)) {
        opStr++;
    }
    
    if (*opStr == '\0') {
        return -1;
    }
    
    char operator = *opStr;
    
    if (operator != '+' && operator != '-' && operator != '*' && operator != '/') {
        return -1;
    }
    
    opStr++;
    
    while (*opStr) {
        if (!isspace(*opStr)) {
            return -1;
        }
        opStr++;
    }
    
    *op = operator;
    return 0;
}

void sendResult(int clientSock, double result) {
    char response[256];
    int len = snprintf(response, sizeof(response), "RESULT:%.2f\n", result);
    
    if (len > 0 && len < (int)sizeof(response)) {
        ssize_t sent = send(clientSock, response, len, 0);
        if (sent < 0) {
            logMessage("Error: Couldn't send the answer to the client");
        } else if (sent < len) {
            logMessage("Warning: Only sent part of the answer to the client");
        }
    }
}

void sendError(int clientSock, const char *errorMsg) {
    if (!errorMsg) {
        return;
    }
    
    char response[512];
    int len = snprintf(response, sizeof(response), "ERROR:%s\n", errorMsg);
    
    if (len > 0 && len < (int)sizeof(response)) {
        ssize_t sent = send(clientSock, response, len, 0);
        if (sent < 0) {
            logMessage("Error: Couldn't send the error message to the client");
        } else if (sent < len) {
            logMessage("Warning: Only sent part of the error message to the client");
        }
    }
}

void getCurrentTime(char *buffer, size_t bufferSize) {
    if (!buffer || bufferSize < 20) {
        return;
    }
    
    time_t now = time(NULL);
    struct tm *t = localtime(&now);
    
    if (t) {
        strftime(buffer, bufferSize, "%Y-%m-%d %H:%M:%S", t);
    } else {
        snprintf(buffer, bufferSize, "???");
    }
}

void logMessage(const char *text) {
    if (!text) {
        return;
    }
    
    pthread_mutex_lock(&logMutex);
    
    char timestamp[64];
    getCurrentTime(timestamp, sizeof(timestamp));
    
    if (logFile) {
        fprintf(logFile, "[%s] %s\n", timestamp, text);
        fflush(logFile);
    }
    
    printf("[%s] %s\n", timestamp, text);
    fflush(stdout);
    
    pthread_mutex_unlock(&logMutex);
}
