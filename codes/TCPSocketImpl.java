import java.io.ObjectOutputStream;
import java.util.Random;

public class TCPSocketImpl extends TCPSocket {
    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
    }

    @Override
    public void send(String pathToFile) throws Exception {
        // throw new RuntimeException("Not implemented!");
        Packet newPacket = Packet("0", "1", "", "Hello");
        try{
            //TODO: create socket
            ObjectOutputStream out = new ObjectOutputStream(socket);
              
            out.writeObject(newPacket); 
              
            out.close(); 
            socket.close();
        }
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
