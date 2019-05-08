import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Random;
import java.io.*;

enum State{
    TRANSFER , FIN_WAIT_1, FIN_WAIT_2, CLOSE_WAIT, TIMED_WAIT, CLOSED ;
}

public class TCPSocketImpl extends TCPSocket {
    private int destinationPort;
    private String destinationIP = Config.destinationIP;
    private EnhancedDatagramSocket enSocket;
    private State currState;
    private int sourcePort;
    private int currSeqNum;
    private Packet[] buffer = new Packet[Config.maxBufferSize];
    private long nextToWriteOnFile = 0;
    private BufferedWriter writer;

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
                    if(!synAckPacket.getSynFlag().equals("1") || !synAckPacket.getAckFlag().equals("1"))
                        throw new Exception("This message is not SYN ACK");
                    this.destinationPort = synAckPacket.getSourcePort();
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

        Packet synPacket = new Packet("0", "1", "0", Config.senderPortNum, Config.receiverPortNum, 0, 0, "", 0);
        DatagramPacket synDatagramPacket = synPacket.convertToDatagramPacket(serverPort, serverIP);
        int rcvSeqNum = sendSyn(synDatagramPacket);


        Packet ackPacket = new Packet("1", "0", "0", Config.senderPortNum, Config.receiverPortNum, rcvSeqNum + 1, 0, "", 0);
        DatagramPacket ackDatagramPacket = ackPacket.convertToDatagramPacket(Config.receiverPortNum, serverIP);

        for(int i=0; i<7; i++)
            this.enSocket.send(ackDatagramPacket);

    }

    private boolean checkIfAckOrSyn(Packet receivedPacket) {
        return (receivedPacket.getAckFlag().equals("1") || receivedPacket.getSynFlag().equals("1"));
    }

    private void writeToFile(Packet newPacket) throws Exception{
        String data = newPacket.getData();
        this.writer.write(data);
    }

    @Override
    public void receive(String pathToFile) throws Exception {
        String data;
        this.writer = new BufferedWriter(new FileWriter(pathToFile, true));

        while(this.currState != State.CLOSE_WAIT) {
            byte[] msg = new byte[Config.maxMsgSize];
            DatagramPacket receivedDatagram = new DatagramPacket(msg, msg.length);
            Packet receivedPacket = new Packet(new String(msg));
            int rcvSeqNum = receivedPacket.getSeqNumber();
            if(checkIfAckOrSyn(receivedPacket))
                continue;
            if (receivedPacket.getFinFlag().equals("1")) {
                this.currState = State.CLOSE_WAIT;
                this.currSeqNum++;
                Packet finAckPacket = new Packet("1", "0", "0", sourcePort, this.destinationPort, rcvSeqNum + 1, this.currSeqNum, "", 0);
                DatagramPacket finAckDatagramPacket = finAckPacket.convertToDatagramPacket(this.destinationPort, destinationIP);
                this.enSocket.send(finAckDatagramPacket);
            }
            if(receivedPacket.getSeqNumber() == nextToWriteOnFile)
                writeToFile(receivedPacket);
        }
//        byte[] msg = new byte[Config.maxMsgSize];
//        DatagramPacket newDatagramPacket = new DatagramPacket(msg, msg.length);
//        while((this.currState != State.CLOSED) &(this.currState != State.CLOSE_WAIT)) {
//            this.enSocket.receive(newDatagramPacket);
//            Packet newPacket = new Packet(new String(msg));
//            int rcvSeqNum = newPacket.getSeqNumber();
//            if (newPacket.getFinFlag().equals("1")) {
//                if (this.currState == State.FIN_WAIT_2) {
//                    this.currState = State.TIMED_WAIT;
//
//                    this.currSeqNum++;
//                    Packet synPacket = new Packet("1", "0", "0", sourcePort, this.destinationPort, rcvSeqNum + 1, this.currSeqNum, "", 0);
//                    DatagramPacket synDatagramPacket = synPacket.convertToDatagramPacket(destinationPort, destinationIP);
//                    for (int i = 0; i < 7; i++)
//                        this.enSocket.send(synDatagramPacket);
//                    this.currState = State.CLOSED;
//
//                } else {
//                    this.currState = State.CLOSE_WAIT;
//
//                    this.currSeqNum++;
//                    Packet synPacket = new Packet("1", "0", "0", sourcePort, this.destinationPort, rcvSeqNum + 1, this.currSeqNum, "", 0);
//                    DatagramPacket synDatagramPacket = synPacket.convertToDatagramPacket(this.destinationPort, destinationIP);
//                    this.enSocket.send(synDatagramPacket);
//                }
//            }
//        }
        this.writer.close();
    }


    @Override
    public void close() throws Exception {
        byte[] msg = new byte[Config.maxMsgSize];
        this.currSeqNum ++;
        Packet closePacket = new Packet("0", "0", "1", Config.senderPortNum, Config.receiverPortNum, 0, this.currSeqNum, "", 0);
        DatagramPacket closeDatagramPacket = closePacket.convertToDatagramPacket(this.destinationPort, this.destinationIP);
        while(true){
            this.enSocket.send(closeDatagramPacket);
            this.currState = State.FIN_WAIT_1;
            this.enSocket.setSoTimeout(1000);

            while (true){
                try{
                    DatagramPacket ackDatagramPacket = new DatagramPacket(msg, msg.length);
                    this.enSocket.receive(ackDatagramPacket);
                    Packet ackPacket = new Packet(new String(msg));
                    if(!ackPacket.getAckFlag().equals("1"))
                        throw new Exception("This message is not ACK");
                    if(ackPacket.getAckNumber() != (this.currSeqNum + 1) )
                        //TODO AFTER RECEIVE
                        throw new Exception("This message is not my ACK -- WILL CHANGE AFTER IMPLEMENTATION OF RECEIVE");
                    this.currState = State.FIN_WAIT_2;
                    this.receive("");
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
    public long getSSThreshold() {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getWindowSize() {
        throw new RuntimeException("Not implemented!");
    }
}
