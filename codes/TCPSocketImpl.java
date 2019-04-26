import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Random;

public class TCPSocketImpl extends TCPSocket {
    private int port;
    private String ip;

    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
        this.port = port;
        this.ip = ip;
    }

    @Override
    public void send(String pathToFile) throws Exception {
        // throw new RuntimeException("Not implemented!");
        Packet newPacket = new Packet("0", "1", "", "", "", "Hello", 0);
        DatagramPacket newDatagramPacket = newPacket.convertToDatagramPacket();
        EnhancedDatagramSocket socket = new EnhancedDatagramSocket(this.port);
        socket.send(newDatagramPacket);
    }

    @Override
    public void receive(String pathToFile) throws Exception {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getSSThreshold() {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getWindowSize() {
        throw new RuntimeException("Not implemented!");
    }
}
