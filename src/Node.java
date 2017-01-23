/**
 * Node class. Knows the network topology
 *
 * The node listens for messages, which are:
 *  - LOCAL: Message from the UserInterface, only a string (no sender or id provided).
 *           Signals the node to diffuse a new message to the rest of the network
 *  - PROBE: New message received from another node.
 *  - ECHO:  Response that a node sends to the emitter. A node receives one Echo for each PROBE sent.
 *
 * HOW TO LAUNCH: needs one command line argument, an integer representing the node id between 0 and 3
 *                  example: "java Node 0"
 *
 * @author Henrik Akesson
 * @author Fabien Salathe
 */

import javafx.util.Pair;
import java.io.*;
import java.net.*;
import java.util.*;

public class Node {
    private LinkedList<Pair<Integer, Integer>> L = new LinkedList<>();
    private final int listeningPort, uiPort, id;
    private final Properties properties;
    private int nbNeighbors;
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
        for (boolean b : adjacencyMatrix[id]) {
            if (b) {
                nbNeighbors++;
            }
        }
        this.properties = new Properties();
        properties.load(new FileInputStream("config.properties"));
        this.listeningPort = Integer.parseInt(properties.getProperty("nodeListeningPort")) + id;
        this.uiPort = Integer.parseInt(properties.getProperty("uiListeningPort")) + id;
    }

    /**
     * Listens for new messages and calls the appropriate method
     * @throws IOException
     */
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
                    handleLocal(message);
                    break;
                case PROBE:
                    handleProbe(message);
                    break;
                case ECHO:
                    handleEcho(message);
                    break;
            }
        }
    }

    /**
     * Handles ECHO type messages according to given algorithm
     *
     * @param message
     */
    private synchronized void handleEcho(Message message) {
        for (Pair<Integer, Integer> pair : L) {
            if (pair.getKey() == message.getId()) {
                L.remove(pair);
                if(pair.getValue() > 1) {
                    L.add(new Pair<>(pair.getKey(), pair.getValue() - 1));
                }
                return;
            }
        }
    }

    /**
     * Handles PROBE type messages according to given algorithm
     * @param message
     */
    private synchronized void handleProbe(Message message) {
        int ID = message.getId();
        boolean contains = false;
        for (Pair<Integer, Integer> pair : L) {
            if (pair.getKey() == ID) {
                contains = true;
                L.remove(pair);

                if (pair.getValue() > 1) {
                    L.add(new Pair<>(pair.getKey(), pair.getValue() - 1));
                }
            }
        }
        if (!contains) {
            try {
                // Send probe to adjacent nodes except emitter
                boolean[] adjacentNodes = adjacencyMatrix[id];
                Message messageToSend = new Message(MessageType.PROBE, message.getContent(), message.getId(), id);
                DatagramSocket socket = new DatagramSocket();
                for (int i = 0; i < adjacentNodes.length; i++) {
                    if (adjacentNodes[i] && i != message.getSender()) {
                        int port = Integer.parseInt(properties.getProperty("nodeListeningPort")) + i;
                        InetAddress address = InetAddress.getByName(properties.getProperty("host"));
                        DatagramPacket packet = new DatagramPacket(messageToSend.toByteArray(), messageToSend.toByteArray().length, address, port);
                        socket.send(packet);
                    }
                }

                // Send Echo to emitter
                messageToSend = new Message(MessageType.ECHO, message.getContent(), message.getId(), id);
                int port = Integer.parseInt(properties.getProperty("nodeListeningPort")) + message.getSender();
                InetAddress address = InetAddress.getByName(properties.getProperty("host"));
                DatagramPacket packet = new DatagramPacket(messageToSend.toByteArray(), messageToSend.toByteArray().length, address, port);
                socket.send(packet);

                // Send message content to UserInterface
                messageToSend = new Message(MessageType.ECHO, message.getContent(), message.getId(), id);
                port = Integer.parseInt(properties.getProperty("uiListeningPort")) + message.getSender();
                address = InetAddress.getByName(properties.getProperty("host"));
                packet = new DatagramPacket(messageToSend.toByteArray(), messageToSend.toByteArray().length, address, port);
                socket.send(packet);
                socket.close();
                L.add(new Pair<>(message.getId(), nbNeighbors - 1));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Handles LOCAL type messages according to specification
     * @param message
     */
    private synchronized void handleLocal(Message message) {
        // Generate random ID
        int ID = Math.abs(new Random(System.currentTimeMillis()).nextInt());
        L.add(new Pair<>(ID, nbNeighbors));
        message.setMessageType(MessageType.PROBE);
        message.setId(ID);
        message.setSender(id);
        try {
            DatagramSocket socket = new DatagramSocket();
            // Determines which nodes it should send to. if adjacentNodes[i] == true, then Node i is connected to this one
            // and a message should be sent to it
            boolean[] adjacentNodes = adjacencyMatrix[id];
            for (int i = 0; i < adjacentNodes.length; i++) {
                if (adjacentNodes[i]) {
                    int port = Integer.parseInt(properties.getProperty("nodeListeningPort")) + i;
                    InetAddress address = InetAddress.getByName(properties.getProperty("host"));
                    DatagramPacket packet = new DatagramPacket(message.toByteArray(), message.toByteArray().length, address, port);
                    socket.send(packet);
                }
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
