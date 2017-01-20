import java.io.IOException;
import java.net.*;
import java.util.Arrays;

/**
 * Created by Henrik on 18-Jan-17.
 */
public class Node {

    private static int numberOfNodes;
    private int nodeId;
    int listeningPort;
    int uiPort;

    public static void main(String... args) throws IOException {
        Node node = new Node();
        node.listen();
    }
    private Node() {
        this.nodeId = numberOfNodes++;
        this.listeningPort = 20001 + nodeId;
        this.uiPort = 30001 + nodeId;
    }

    private void listen() throws IOException {
        byte[] buffer = new byte[256];

        DatagramSocket socket = new DatagramSocket(this.listeningPort);

        DatagramPacket packet = new DatagramPacket(buffer,buffer.length);

        while(true) {
            socket.receive(packet);
            Message message = null;
            try {
                message = Message.fromByteArray(packet.getData());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            switch(message.getMessageType()) {
                case LOCAL:
                    System.out.println("Local: " + new String(message.getPayload()));
                    Message mess = new Message(MessageType.ECHO, "Hey".getBytes());
                    socket.send(new DatagramPacket(mess.toByteArray(), mess.toByteArray().length, InetAddress.getByName("localhost"), uiPort));//mess.toByteArray(), mess.toByteArray().length, packet.getPort())
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
}
