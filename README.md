## Lab07_CN

## Files Included
* `oracle.c`
* `VN.java`
* `protocol.h`
* `oracle-node-config.txt`

## Compilation
To compile the files run:  

    make or make all

## Execution
for the demo run:

PS: Please make sure that you are running all in different tabs simultaneously.


Start the Oracle Node

    './oracle oracle-node-config.txt'

Start the Virtual Nodes

    'java VN <ON-IP> <VN-IP> <port-no1>'
    'java VN <ON-IP> <VN-IP> <port-no2>'
    'java VN <ON-IP> <VN-IP> <port-no3>'
    'java VN <ON-IP> <VN-IP> <port-no4>'
    'java VN <ON-IP> <VN-IP> <port-no5>'

## Cleanup
To remove generated binaries and class files, run:
    'make clean'
