#include <stdio.h>
#include <stddef.h>
#include <pthread.h>
#include <time.h>

static pthread_mutex_t logMutex = PTHREAD_MUTEX_INITIALIZER;
static FILE *logFile = NULL;

static void getCurrentTime(char *buffer, size_t bufferSize);

int OperationLogger_init(const char *path) {
    const char *logPath = path ? path : "server_log.txt";
    logFile = fopen(logPath, "a");
    if (!logFile) {
        return -1;
    }
    return 0;
}

void OperationLogger_shutdown(void) {
    if (logFile) {
        fclose(logFile);
        logFile = NULL;
    }
    pthread_mutex_destroy(&logMutex);
}

static void getCurrentTime(char *buffer, size_t bufferSize) {
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
