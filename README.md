# UDP-Server-and-Client

Connection established using UDP.

Client sends a message and awaits for an answer that contain's his message. Server receives a messages and sends answer like:
```
Hello, %message%.
```

Here I used multithreading to send/receive a lot of messages at once.

Also added NonBlocking multithread.