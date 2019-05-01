import org.omg.DynamicAny.DynArray;

import java.net.DatagramPacket;

public class TCPServerSocketImpl extends TCPServerSocket {
    private EnhancedDatagramSocket enSocket;

    public TCPServerSocketImpl(int port) throws Exception {
        super(port);

        this.enSocket = new EnhancedDatagramSocket(Config.receiverPortNum);
    }

    @Override
    public TCPSocket accept() throws Exception {
        byte[] msg = new byte[Config.maxMsgSize];
        DatagramPacket newDatagramPacket = new DatagramPacket(msg, msg.length);
        this.enSocket.receive(newDatagramPacket);
        System.out.println(new String(msg));
        EnhancedDatagramSocket()
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }
}
