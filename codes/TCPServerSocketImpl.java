import java.net.DatagramPacket;

public class TCPServerSocketImpl extends TCPServerSocket {
    private EnhancedDatagramSocket enSocket;

    public TCPServerSocketImpl(int port) throws Exception {
        super(port);

        this.enSocket = new EnhancedDatagramSocket(Config.receiverPortNum);
    }

    @Override
    public TCPSocket accept() throws Exception {
        byte[] msg = new byte[65535];
        DatagramPacket synDatagramPacket = new DatagramPacket(msg, msg.length);
        this.enSocket.receive(synDatagramPacket);
        System.out.println(new String(msg));
        Packet synPacket = new Packet(new String(msg));
        if(synPacket.getSynFlag()!="1")
            throw new Exception("This message is not SYN");

        TCPSocketImpl server = new TCPSocketImpl(Config.receiverIP,9797);
        Packet synAckPacket = new Packet("1", "1", String.valueOf(9797), String.valueOf(Config.senderPortNum), "", "", 0);
        DatagramPacket synAckMsg = synAckPacket.convertToDatagramPacket(Config.senderPortNum, Config.senderIP);
        this.enSocket.send(synAckMsg);


        DatagramPacket ackDatagramPacket = new DatagramPacket(msg, msg.length);
        this.enSocket.receive(ackDatagramPacket);
        Packet ackPacket = new Packet(new String(msg));
        if(ackPacket.getAckFlag()!="1")
            throw new Exception("This message is not ACK");
        return server;
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }
}
