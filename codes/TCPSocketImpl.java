import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Random;

enum State{
    TRANSFER , FIN_WAIT_1, FIN_WAIT_2, CLOSE_WAIT, TIMED_WAIT ;
}

public class TCPSocketImpl extends TCPSocket {
    private int destinationPort;
    private String destinationIP = Config.destinationIP;
    private EnhancedDatagramSocket enSocket;
    private State currState;
    private int sourcePort;
    private int currSeqNum;

    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
        this.sourcePort = port;
        this.enSocket = new EnhancedDatagramSocket(port);
        this.currState = State.TRANSFER;
        this.currSeqNum = 0;
    }

    public void setDestinationPort(int destinationPort){
        this.destinationPort = destinationPort;
    }

    @Override
    public void send(String pathToFile){
        throw new RuntimeException("Not implemented!");
    }

    public int sendSyn(DatagramPacket synDatagramPacket ) throws Exception {
        while(true){
            this.enSocket.send(synDatagramPacket);
            this.enSocket.setSoTimeout(1000);

            byte[] msg = new byte[Config.maxMsgSize];
            DatagramPacket synAckDatagramPacket = new DatagramPacket(msg, msg.length);
            Packet synAckPacket;
            while(true){
                try {
                    this.enSocket.receive(synAckDatagramPacket);
                    synAckPacket = new Packet(new String(msg));
                    if(synAckPacket.getSynFlag()!="1" || synAckPacket.getAckFlag() !="1")
                        throw new Exception("This message is not SYN ACK");
                    this.destinationPort = Integer.parseInt(synAckPacket.getDestinationPort());
                    int rcvSeqNum = synAckPacket.getSeqNumber();
                    return rcvSeqNum;
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
    public void connect(String serverIP, int serverPort) throws Exception {

        Packet synPacket = new Packet("0", "1", "0", String.valueOf(Config.senderPortNum), String.valueOf(Config.receiverPortNum), 0, 0, "", 0);
        DatagramPacket synDatagramPacket = synPacket.convertToDatagramPacket(serverPort, serverIP);
        int rcvSeqNum = sendSyn(synDatagramPacket);


        Packet ackPacket = new Packet("1", "0", "0", String.valueOf(Config.senderPortNum), String.valueOf(Config.receiverPortNum), rcvSeqNum + 1, 0, "", 0);
        DatagramPacket ackDatagramPacket = synPacket.convertToDatagramPacket(Config.receiverPortNum, serverIP);

        for(int i=0; i<7; i++)
            this.enSocket.send(ackDatagramPacket);

    }

    @Override
    public void receive(String pathToFile) throws Exception {
        byte[] msg = new byte[Config.maxMsgSize];
        DatagramPacket newDatagramPacket = new DatagramPacket(msg, msg.length);
        this.enSocket.receive(newDatagramPacket);
        Packet newPacket = new Packet(new String(msg));
        int rcvSeqNum = newPacket.getSeqNumber();
        if(newPacket.getFinFlag().equals("1")){
            if(this.currState == State.FIN_WAIT_2){
                this.currState = State.TIMED_WAIT;

                this.currSeqNum ++;
                Packet synPacket = new Packet("1", "0", "0", String.valueOf(sourcePort), String.valueOf(destinationPort), rcvSeqNum + 1, this.currSeqNum, "", 0);
                DatagramPacket synDatagramPacket = synPacket.convertToDatagramPacket(destinationPort, destinationIP);
                for(int i = 0; i < 7; i++)
                    sendSyn(synDatagramPacket);
            }
            else{
                this.currState = State.CLOSE_WAIT;

                this.currSeqNum ++;
                Packet synPacket = new Packet("1", "0", "0", String.valueOf(sourcePort), String.valueOf(destinationPort), rcvSeqNum + 1, this.currSeqNum, "", 0);
                DatagramPacket synDatagramPacket = synPacket.convertToDatagramPacket(destinationPort, destinationIP);
                sendSyn(synDatagramPacket);
            }
        }
        else if(newPacket.getAckFlag().equals("1")){
            if((this.currState == State.FIN_WAIT_1) & (newPacket.getAckNumber() == this.currSeqNum + 1 ))
                this.currState = State.FIN_WAIT_2;
        }
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public void close() throws Exception {
        this.currSeqNum ++;
        Packet closePacket = new Packet("0", "0", "1", String.valueOf(Config.senderPortNum), String.valueOf(Config.receiverPortNum), 0, this.currSeqNum, "", 0);
        DatagramPacket closeDatagramPacket = closePacket.convertToDatagramPacket(destinationPort, destinationIP);
        sendSyn(closeDatagramPacket);
        this.currState = State.FIN_WAIT_1;
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
