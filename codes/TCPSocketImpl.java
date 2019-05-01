import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;

public class TCPSocketImpl extends TCPSocket {

    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
    }

    @Override
    public void send(String pathToFile){
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public void connect(String destinationIP, int destinationPort) throws IOException {
        Packet newPacket = new Packet("0", "1", String.valueOf(Config.senderPortNum), String.valueOf(Config.receiverPortNum), "", "Hello", 0);
        DatagramPacket newDatagramPacket = newPacket.convertToDatagramPacket();
        newDatagramPacket.setPort(destinationPort);
        newDatagramPacket.setAddress(InetAddress.getByName(destinationIP));
        EnhancedDatagramSocket enSocket = new EnhancedDatagramSocket(Config.senderPortNum);
        enSocket.send(newDatagramPacket);
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
