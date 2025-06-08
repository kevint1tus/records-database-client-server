# Records Database Client-Server

A multi-threaded, TCP-based client-server application for querying a music records database.

## Overview

This project implements a three-tier architecture where a JavaFX-based client communicates with a server to retrieve information about music records from a database. The system is designed to handle multiple client requests concurrently and demonstrates the use of sockets, threads, and JDBC in Java.

## Components

- **Client:**  
  A graphical user interface built with JavaFX, allowing users to enter search criteria (artist surname and record shop city) and view results in a table. The client sends requests to the server over a TCP connection.

- **Server:**  
  The server listens for incoming client connections on a specified port. For each connection, it spawns a dedicated service thread to process the request, ensuring the server can handle multiple clients at once.

- **Service Thread:**  
  Each service thread is responsible for parsing the client's request, connecting to the database using JDBC, executing the relevant SQL query, and returning the results to the client in a serializable format.

## How It Works

1. The client collects user input and sends a formatted request to the server.
2. The server accepts the connection and delegates the request to a new service thread.
3. The service thread queries the database, packages the results, and sends them back to the client.
4. The client displays the results in the GUI.

## Technologies Used

- Java (multi-threading, sockets)
- JavaFX (GUI)
- JDBC (database connectivity)
- TCP/IP networking 