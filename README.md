# Distributed Group Chat with Causal Ordering

## Overview

This project is a fully distributed group chat application that ensures high availability and causal message ordering. Users can create chat rooms, invite participants at creation time, and exchange messages while maintaining causal order. The application does not rely on a centralized server and allows users to continue reading and writing messages even during temporary network disconnections.

## Features

- **Distributed Architecture**: No central server; users communicate directly.
- **Causal Message Ordering**: Ensures that messages are delivered in a causally consistent order.
- **High Availability**: Users can send and receive messages even if they temporarily disconnect from the network.
- **Room Management**: Users can create and delete chat rooms.
- **Fixed Participants**: The set of participants is determined at room creation and remains unchanged.
- **Resilience to Network Failures**: The system can tolerate network partitions and client departures.

## Implementation

The project is implemented in **Java** as a real distributed application using peer-to-peer communication.

## Assumptions

- Clients are reliable but can join and leave the network at any time.
- Network failures and partitions may occur.

## Setup and Usage

### Java Implementation

1. Clone the repository:
   ```bash
   git clone https://github.com/federico-zanca/Distributed-Systems-Project.git
   cd Distributed-Systems-Project
   ```
2. Run the application using the provided JAR artifact:
   ```bash
   java -jar out/artifacts/DisSys_jar/DisSys.jar
   ```
3. Follow on-screen instructions to create rooms, join chats, and send messages.

## Authors

Federico Zanca - [GitHub Profile](https://github.com/federico-zanca)
Valerio Xie - [GitHub Profile](https://github.com/ValerioXIe)
## Acknowledgments

- Distributed Systems Course Project

