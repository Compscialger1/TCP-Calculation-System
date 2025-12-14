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

volatile int serverRunning = 1;
int globalServerSock = -1;

int startServer(int port);
void *handleClient(void *clientSocket);
void logMessage(const char *text);
int OperationLogger_init(const char *path);
void OperationLogger_shutdown(void);
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

    if (OperationLogger_init("server_log.txt") != 0) {
        perror("Couldn't open log file");
        return 1;
    }
    
    logMessage("Starting up the calculation server on port 5000...");
    
    int serverSock = startServer(5000);
    if (serverSock < 0) {
        logMessage("Oops! The server failed to start");
        OperationLogger_shutdown();
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
    OperationLogger_shutdown();
    
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
