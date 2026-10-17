#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

int udp_socket(int port) {
    int sockfd;
    struct sockaddr_in vn_addr;
    if((sockfd = socket(PF_INET, SOCK_DGRAM, 0)) < 0){
        perror("Error: Failed to create UDP socket");
        return -1;
    };
    int opt = 1;
    setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
    vn_addr.sin_family = AF_INET;
    vn_addr.sin_port = htons(port);
    vn_addr.sin_addr.s_addr = INADDR_ANY;
    memset(&(vn_addr.sin_zero), '\0', 8);
    if(bind(sockfd, (struct sockaddr *)&vn_addr, sizeof(struct sockaddr)) < 0){
        perror("Error: Failed to bind UDP socket");
        close(sockfd);
        return -1;
    }
    return sockfd;
}

int main (int argc, char *argv[]) {
    if(argc != 4) {
        fprintf(stderr, "Usage: %s <ON_ip> <ON_port> <OWN_UDP_port>\n", argv[0]);
        exit(EXIT_FAILURE);
    }

    char *on_ip = argv[1];
    int on_port = atoi(argv[2]);
    int own_udp_port = atoi(argv[3]);

    int sockfd = udp_socket(own_udp_port);
    if (sockfd < 0) {
        perror("Error: Failed to create UDP socket");
        exit(EXIT_FAILURE); 
    }

    close(sockfd);
    return EXIT_SUCCESS; 
}