CC = gcc
CFLAGS = -Wall -Wextra -g
JAVAC = javac

all: oracle VN.class

oracle: oracle.c 
	$(CC) $(CFLAGS) -o oracle oracle.c

VN.class: VN.java
	$(JAVAC) VN.java

clean:
	rm -rf oracle *.class

.PHONY: all clean
