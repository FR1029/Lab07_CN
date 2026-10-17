#ifndef PROTOCOL_H
#define PROTOCOL_H
#include <stdio.h>


struct message {
    u_int32_t ip;
    u_int16_t port;
} __attribute__((packed));

struct LSTuple {
    char id;
    u_int32_t ip;
    u_int16_t port;
    int32_t cost;
} __attribute__((packed));

struct LSP {
    char id;
    u_int32_t seq_num;
    u_int32_t num_links;
} __attribute__((packed));

#endif