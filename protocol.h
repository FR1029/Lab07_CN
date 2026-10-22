#ifndef PROTOCOL_H
#define PROTOCOL_H


struct message {
    uint32_t ip;
    uint16_t port;
} __attribute__((packed));

struct LSTuple {
    char id;
    uint32_t ip;
    uint16_t port;
    uint32_t cost;
} __attribute__((packed));

#endif