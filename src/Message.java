import java.io.*;

/**
 * Message used to communicate between instances on the network.
 * Messages are serializable in order to be able to get a binary buffer to send to the network
 */
public class Message implements Serializable, ByteArrayable {

    private static final long serialVersionUID = 8073741970285089526L;
    private int sender;
    private int id;
    private String content;

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    /**
     * Type of message
     */
    private MessageType messageType;

    public Message(MessageType messageType, String content) {
        this.messageType = messageType;
        this.content = content;
        this.id = -1;
        this.sender = -1;
    }

    public Message(MessageType messageType, String content, int id, int sender) {
        this.messageType = messageType;
        this.content = content;
        this.id = id;
        this.sender = sender;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setSender(int sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public static Message fromByteArray(final byte[] bytes) throws IOException, ClassNotFoundException {
        return (Message) ByteArrayable.fromByteArray(bytes);
    }

    public int getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return "[" + messageType + "]: " + "Content:" + content + ", Sender:" + sender + ", ID:" + id;
    }

    public int getSender() {
        return this.sender;
    }
}