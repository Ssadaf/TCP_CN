import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.util.Random;
import Packet.java;
import TCPSocket.java;

public class TCPSocketImpl extends TCPSocket {
    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
    }

    @Override
    public void send(String pathToFile) throws Exception {
        // throw new RuntimeException("Not implemented!");
        Packet newPacket = Packet("0", "1", "", "", "", "Hello");
        DatagramPacket newDatagramPacket = newPacket.convertToDatagramPacket();
        EnhancedDatagramSocket socket = new EnhancedDatagramSocket(port);
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
