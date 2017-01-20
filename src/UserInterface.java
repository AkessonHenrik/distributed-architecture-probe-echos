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

    private UserInterface(int nodePort) throws IOException {
        this.nodePort = nodePort;
        this.properties = new Properties();
        properties.load(new FileInputStream("config.properties"));
        launchListener(nodePort);
    }

    public static void main(String... args) {

        int nodePort = Integer.parseInt(args[0]);
        Scanner scanner = new Scanner(System.in);
        String message = "";
        System.out.println("Enter message to send");

        try {
            UserInterface ui = new UserInterface(nodePort);
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
        DatagramSocket socket = new DatagramSocket(9000);
        socket.send(packet);
        socket.close();
    }

    private void launchListener(int listeningPort) {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[256];
                DatagramSocket socket = new DatagramSocket(30001);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                while (true) {
                    socket.receive(packet);
                    Message message = Message.fromByteArray(packet.getData());
                    System.out.println("Received: " + new String(message.getPayload()));
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
