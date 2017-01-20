import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Properties;

public class Node {

    private final int listeningPort, emitPort, uiPort, id;
    private final Properties properties;
    private final boolean[][] adjacencyMatrix = {
            {false, true, true, true},
            {true, false, true, false},
            {true, true, false, true},
            {true, false, true, false}
    };

    public static void main(String... args) throws IOException {
        Node node = new Node(Integer.parseInt(args[0]));
        node.listen();
    }

    private Node(int id) throws IOException {
        this.id = id;
        this.properties = new Properties();
        properties.load(new FileInputStream("config.properties"));
        this.listeningPort = Integer.parseInt(properties.getProperty("nodeListeningPort")) + id;
        this.uiPort = Integer.parseInt(properties.getProperty("uiListeningPort")) + id;
        this.emitPort = Integer.parseInt(properties.getProperty("nodeEmitPort")) + id;
        System.out.println("Node port: " + listeningPort);
        System.out.println("UI Listening port: " + uiPort);
    }

    private void listen() throws IOException {
        byte[] buffer = new byte[256];

        DatagramSocket socket = new DatagramSocket(this.listeningPort);

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            socket.receive(packet);
            Message message = null;
            try {
                message = Message.fromByteArray(packet.getData());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            switch (message.getMessageType()) {
                case LOCAL:
                    System.out.println("Local: " + new String(message.getPayload()));
                    Message mess = new Message(MessageType.ECHO, "Hey".getBytes());
                    socket.send(new DatagramPacket(mess.toByteArray(), mess.toByteArray().length, InetAddress.getByName("localhost"), uiPort));
                    sendNewMessageToAdjacentNodes(message);
                    break;
                case PROBE:
                    System.out.println("Probe");
                    break;
                case ECHO:
                    System.out.println("Echo");
                    break;
            }
        }
    }

    private void sendNewMessageToAdjacentNodes(Message message) {
        boolean[] adjacentNodes = adjacencyMatrix[id];
        message.setMessageType(MessageType.PROBE);
        DatagramSocket tempSocket;
        try {
            tempSocket = new DatagramSocket(this.emitPort);
            for(int i = 0; i < adjacentNodes.length; i++) {
                if(adjacentNodes[i]) {
                    int port = Integer.parseInt(properties.getProperty("nodeListeningPort")) + i;
                    InetAddress address = InetAddress.getByName("localhost");
                    DatagramPacket packet = new DatagramPacket(message.toByteArray(), message.toByteArray().length, address, port);
                    tempSocket.send(packet);
                }
            }
            tempSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
