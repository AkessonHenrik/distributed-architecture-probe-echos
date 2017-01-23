/**
 * User Interfacing class.
 * This class handles User messages and notifications
 * Once launched, waits for the user to enter a message. If the message exceeds 230 characters,
 * the user will be prompted to enter another one.
 * Once the message content is validated, it is send as a LOCAL message to the associated node.
 *
 * [COMMAND LINE ARGUMENTS]: one integer representing the Node id (in our context between 0 and 3)
 *
 * The listening part of this class is a separate thread that outputs received messages to the console
 * Additional information is obtained through the config.properties file
 *
 * @author Henrik Akesson
 * @author Fabien Salathe
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Properties;
import java.util.Scanner;

public class UserInterface {

    private Properties properties;
    private int nodePort;

    private UserInterface(int id) throws IOException {
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
        String message;
        System.out.println("Enter message to send");

        try {
            UserInterface ui = new UserInterface(Integer.parseInt(args[0]));
            while (true) {
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

    /**
     * Emits a message to the associated node (the one with the same index)
     *
     * @param messageContent : Message to send
     * @throws IOException
     */
    private void emit(String messageContent) throws IOException {
        Message message = new Message(MessageType.LOCAL, messageContent);
        InetAddress address = InetAddress.getByName(properties.getProperty("host"));
        DatagramPacket packet = new DatagramPacket(message.toByteArray(), message.toByteArray().length, address, nodePort);
        DatagramSocket socket = new DatagramSocket();
        socket.send(packet);
        socket.close();
    }

    /**
     * Launches a separate thread which listens for incoming messages
     *
     * @param listeningPort: Port that the listener should listen to
     */
    private void launchListener(int listeningPort) {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[256];
                DatagramSocket socket = new DatagramSocket(listeningPort);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                while (true) {
                    socket.receive(packet);
                    Message message = Message.fromByteArray(packet.getData());
                    System.out.println(message);
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}