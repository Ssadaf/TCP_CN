import java.net.DatagramPacket;
import java.net.SocketTimeoutException;

public class TCPServerSocketImpl extends TCPServerSocket {
    private EnhancedDatagramSocket enSocket;

    public TCPServerSocketImpl(int port) throws Exception {
        super(port);
        this.enSocket = new EnhancedDatagramSocket(port);
    }


    public void sendSynAck( DatagramPacket synAckMsg ) throws Exception {
        byte[] msg = new byte[Config.maxMsgSize];
        while(true) {
            this.enSocket.send(synAckMsg);
            this.enSocket.setSoTimeout(1000);

            while (true){
                try {
                    DatagramPacket ackDatagramPacket = new DatagramPacket(msg, msg.length);
                    this.enSocket.receive(ackDatagramPacket);
                    Packet ackPacket = new Packet(new String(msg));
                    if(ackPacket.getAckFlag()!="1")
                        throw new Exception("This message is not ACK");
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
    public TCPSocket accept() throws Exception {
        byte[] msg = new byte[Config.maxMsgSize];
        DatagramPacket newDatagramPacket = new DatagramPacket(msg, msg.length);
        this.enSocket.receive(newDatagramPacket);
        System.out.println(new String(msg));
        Packet synPacket = new Packet(new String(msg));
        if(synPacket.getSynFlag()!="1")
            throw new Exception("This message is not SYN");

        TCPSocketImpl server = new TCPSocketImpl(Config.receiverIP,9797);
        Packet synAckPacket = new Packet("1", "1", "0", String.valueOf(9797), String.valueOf(Config.senderPortNum), "", "", 0);
        DatagramPacket synAckMsg = synAckPacket.convertToDatagramPacket(Config.senderPortNum, Config.senderIP);

        sendSynAck(synAckMsg);

        return server;
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }
}
