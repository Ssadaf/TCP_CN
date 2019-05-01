import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Random;

public class TCPSocketImpl extends TCPSocket {
    private int serverPort;

    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
    }

    @Override
    public void send(String pathToFile){
        throw new RuntimeException("Not implemented!");
    }

    public void sendSyn(EnhancedDatagramSocket enSocket, DatagramPacket synDatagramPacket ) throws Exception {
        while(true){
            enSocket.send(synDatagramPacket);
            enSocket.setSoTimeout(1000);

            byte[] msg = new byte[Config.maxMsgSize];
            DatagramPacket synAckDatagramPacket = new DatagramPacket(msg, msg.length);
            Packet synAckPacket;
            while(true){
                try {
                    enSocket.receive(synAckDatagramPacket);
                    synAckPacket = new Packet(new String(msg));
                    if(synAckPacket.getSynFlag()!="1" || synAckPacket.getAckFlag() !="1")
                        throw new Exception("This message is not SYN ACK");
                    this.serverPort = Integer.parseInt(synAckPacket.getDestinationPort());
                    return;
                }
                catch (SocketTimeoutException e) {
                    // timeout exception.
                    System.out.println("Timeout reached!!! " + e);
                    break;
                }
            }
        }
    }

    @Override
    public void connect(String destinationIP, int destinationPort) throws Exception {
        EnhancedDatagramSocket enSocket = new EnhancedDatagramSocket(Config.senderPortNum);

        Packet synPacket = new Packet("0", "1", String.valueOf(Config.senderPortNum), String.valueOf(Config.receiverPortNum), "", "", 0);
        DatagramPacket synDatagramPacket = synPacket.convertToDatagramPacket(destinationPort, destinationIP);
        sendSyn(enSocket, synDatagramPacket);


        Packet ackPacket = new Packet("1", "0", String.valueOf(Config.senderPortNum), String.valueOf(Config.receiverPortNum), "", "", 0);
        DatagramPacket ackDatagramPacket = synPacket.convertToDatagramPacket(Config.receiverPortNum, destinationIP);

        for(int i=0; i<7; i++)
            enSocket.send(ackDatagramPacket);

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
