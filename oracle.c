#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#define MAX_NODES 26
#define MYPORT 5000

struct Node {
    int fd;
    u_int32_t ip;
    u_int16_t port;
};

int read_config(const char *filename, int c[MAX_NODES][MAX_NODES], int *n){
    FILE *fd = fopen(filename, "r");
    if (fd == NULL){
        perror("Error in Opening the File");
        return -1;
    }
    char *l = NULL;
    size_t len = 0;
    while(getline(&l, &len, fd) != -1){
        char *p = l;
        while(isspace(*p)) p++;
        if(*p == '#' || *p == '\0') continue;
        if (*n >= MAX_NODES) {
            fprintf(stderr, "Too many nodes!\n");
            free(l);
            fclose(fd);
            return -1;
        }
        int costs[MAX_NODES];
        int k = 0;
        char *e;
        while(*p != '\0'){
            while(isspace(*p)) p++;
            if(*p == '\0') break;
            int cost = strtol(p, &e, 10);
            if(p == e || cost < -1){
                fprintf(stderr, "Invalid cost in row %d\n", *n+1);
                free(l);
                fclose(fd);
                return -1;
            }
            if(k >= MAX_NODES){
                fprintf(stderr, "Too many entries in row %d\n", *n+1);
                free(l);
                fclose(fd);
                return -1;
            }
            costs[k++] = cost;
            p = e;
        }
        for(int i = 0; i < k; i++){
            int j = *n+i+1;
            if(j >= MAX_NODES){
                fprintf(stderr, "Invalid row\n");
                free(l);
                fclose(fd);
                return -1;
            }
            c[*n][j] = costs[i];
            c[j][*n] = costs[i];
        }
        (*n)++;
    }
    free(l);
    fclose(fd);
    return 0;
}

int server_socket(int port){
    int sockfd;
    struct sockaddr_in my_addr;
    if((sockfd = socket(PF_INET, SOCK_STREAM, 0)) < 0){
        perror("Socket");
        return -1;
    };
    int opt = 1;
    setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
    my_addr.sin_family = AF_INET;
    my_addr.sin_port = htons(port);
    my_addr.sin_addr.s_addr = INADDR_ANY;
    memset(&(my_addr.sin_zero), '\0', 8);
    if(bind(sockfd, (struct sockaddr *)&my_addr, sizeof(struct sockaddr)) < 0){
        perror("Bind");
        return -1;
    }
    listen(sockfd, MAX_NODES);
    return sockfd;
}

int main(int argc, char *argv[]) {
    if (argc != 2) {
        fprintf(stderr, "Usage: %s <config-file>\n", argv[0]);
        return 1;
    }

    int n = 0;
    int c[MAX_NODES][MAX_NODES];
    for (int i = 0; i < MAX_NODES; i++) {
        for (int j = 0; j < MAX_NODES; j++) c[i][j] = (i == j) ? 0 : -1;
    }

    if(read_config(argv[1], c, &n) == -1) return 1;
    
    int sockfd = server_socket(MYPORT);
    if(sockfd < 0) return 1;

    struct Node nodes[MAX_NODES];
    
    printf("Number of nodes: %d\n", n);
    printf("Adjacency matrix:\n");
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) printf("%4d", c[i][j]);
        printf("\n");
    }

    return 0;
}
