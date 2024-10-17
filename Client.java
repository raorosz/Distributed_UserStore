// Client.java
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private String host;
    private int port;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Choose operation: 1) Read 2) Write 3) Exit");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline
            try {
                if (choice == 1) {
                    System.out.print("Enter username to read: ");
                    String username = scanner.nextLine();
                    ReadRequest req = new ReadRequest(username);
                    ReadResponse resp = (ReadResponse) sendMessage(req);
                    if (resp.isFound()) {
                        System.out.println("User: " + resp.getUsername() + ", SSN: " + resp.getSsn());
                    } else {
                        System.out.println("User not found.");
                    }
                } else if (choice == 2) {
                    System.out.print("Enter username to write: ");
                    String username = scanner.nextLine();
                    System.out.print("Enter SSN: ");
                    String ssn = scanner.nextLine();
                    WriteRequest req = new WriteRequest(username, ssn);
                    sendMessage(req);
                    System.out.println("Write operation acknowledged.");
                } else if (choice == 3) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        scanner.close();
    }

    private Message sendMessage(Message msg) {
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            System.out.println("Client connected to server at " + host + ":" + port);
            out.writeObject(msg);
            out.flush(); // Ensure data is sent immediately
            System.out.println("Message sent: " + msg.getClass().getSimpleName());

              Message response = (Message) in.readObject();
            System.out.println("Received response: " + response.getClass().getSimpleName());
            return response;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error occurred while sending message.");
            e.printStackTrace();
            return null;
        }
    }
    

    // Main method
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Client <host> <port>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        Client client = new Client(host, port);
        client.start();
    }
}

