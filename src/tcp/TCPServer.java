package tcp;

import java.io.*;
import java.net.*;

public class TCPServer {

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(5555);
            System.out.println("Socket opened");
        } catch (IOException e) {
            System.out.println("Could not open socket, error: " + e);
        }

        while(true) {
            Socket clientSocket = null;
            try {
                clientSocket = serverSocket.accept();
                System.out.println("Established connection with address: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                System.out.println("Could not accept connection, error: " + e);
            }

            try {
                new ClientHandler(clientSocket);
            } catch (Exception e) {
                System.out.println("Could not initialize handler");
            }
        }
    }
}
