/**
 * Created by Henrik on 20-Jan-17.
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Properties;
import java.util.Scanner;

public class UserInterface {

    private Properties properties;
    private int nodePort;
    private int id;

    private UserInterface(int id) throws IOException {
        this.id = id;
        this.properties = new Properties();
        properties.load(new FileInputStream("config.properties"));
        int listeningPort = Integer.parseInt(properties.getProperty("uiListeningPort")) + id;
        this.nodePort = Integer.parseInt(properties.getProperty("nodeListeningPort")) + id;
        launchListener(listeningPort);
        System.out.println("Node port: " + nodePort);
        System.out.println("UI Listening port: " + listeningPort);
    }

    public static void main(String... args) {

        Scanner scanner = new Scanner(System.in);
        String message = "";
        System.out.println("Enter message to send");

        try {
            UserInterface ui = new UserInterface(Integer.parseInt(args[0]));
            while (!message.equals("exit")) {
                message = scanner.nextLine();
                if (message.length() <= 230)
                    ui.emit(message);
                else
                    System.out.println("Message length exceeded, try again");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void emit(String messageContent) throws IOException {
        Message message = new Message(MessageType.LOCAL, messageContent.getBytes());
        InetAddress address = InetAddress.getByName(properties.getProperty("host"));
        DatagramPacket packet = new DatagramPacket(message.toByteArray(), message.toByteArray().length, address, nodePort);
        DatagramSocket socket = new DatagramSocket(Integer.parseInt(properties.getProperty("uiEmitPort")) + id);
        socket.send(packet);
        socket.close();
    }

    private void launchListener(int listeningPort) {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[256];
                DatagramSocket socket = new DatagramSocket(listeningPort);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                while (true) {
                    socket.receive(packet);
                    Message message = Message.fromByteArray(packet.getData());
                    System.out.println("Received: [" + message.getMessageType() + "] " + new String(message.getPayload()));
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
