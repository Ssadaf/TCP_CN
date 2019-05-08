import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Random;
import java.io.*;

enum State{
    TRANSFER , FIN_WAIT_1, FIN_WAIT_2, CLOSE_WAIT, TIMED_WAIT, CLOSED, LAST_ACK ;
}

public class TCPSocketImpl extends TCPSocket {
    private int destinationPort;
    private String destinationIP = Config.destinationIP;
    private EnhancedDatagramSocket enSocket;
    private State currState;
    private int sourcePort;
    private int currSeqNum;
    private ArrayList<Packet> buffer = new ArrayList<>();
    private int nextToWriteOnFile;
    private BufferedWriter writer;
    private FileInputStream reader;
    private int cwnd;
    private int ackedSeqNum;
    private int numDupAck;
    private int SSthreshold;

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


    public int getCurrentDupAckLimit() {
        if (this.ackedSeqNum == (this.currSeqNum - this.cwnd + 1))
            return 3;
        else
            return 2;
    }
    public void retransmitPacket(int retransmitSeqNum) throws Exception{
        for(Packet packet: buffer) {
            if(packet.getSeqNumber() == retransmitSeqNum) {
                DatagramPacket sendDatagramPacket = packet.convertToDatagramPacket(this.destinationPort, this.destinationIP);
                this.enSocket.send(sendDatagramPacket);
            }
        }
    }

    @Override
    public void send(String pathToFile) throws Exception{
        File file = new File(pathToFile);
        this.reader = new FileInputStream(file);
        byte[] chunk = new byte[Config.chunkSize];
        this.currSeqNum = 0;
        this.ackedSeqNum = 0;
        this.numDupAck = 0;
        while (reader.read(chunk) != -1) {
            while(currSeqNum <= this.ackedSeqNum +this.cwnd + this.numDupAck) {
                this.currSeqNum ++;
                Packet sendPacket = new Packet("0", "0", "0", this.sourcePort, this.destinationPort, 0, this.currSeqNum, new String(chunk), 0);
                DatagramPacket sendDatagramPacket = sendPacket.convertToDatagramPacket(this.destinationPort, this.destinationIP);
                buffer.add(sendPacket);
                this.enSocket.send(sendDatagramPacket);
            }
            byte[] msg = new byte[Config.maxMsgSize];
            DatagramPacket ackDatagram = new DatagramPacket(msg, msg.length);
            Packet ackPacket = new Packet(new String(msg));
            if(ackPacket.getAckNumber() == (this.ackedSeqNum + 1) ){
                this.numDupAck ++;
                if(this.numDupAck > getCurrentDupAckLimit()) {
                    this.SSthreshold = this.cwnd / 2;
                    retransmitPacket(this.ackedSeqNum + 1);
                    this.numDupAck--;
                    this.cwnd = this.SSthreshold;
                }
            }
            else{
                if(this.cwnd < this.SSthreshold)
                    this.cwnd = this.cwnd * 2;
                else
                    this.cwnd = this.cwnd + 1;
                this.onWindowChange();
            }

        }

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
        System.out.println("Writing to file: " + data);
    }

    private void addAllValidPacketsToFile() throws Exception{
        while(buffer.size()>0) {
            if((buffer.get(0).getSeqNumber() == nextToWriteOnFile)) {
                writeToFile(buffer.get(0));
                nextToWriteOnFile++;
                buffer.remove(0);
            }
            else
                return;
        }
    }

    private void sendAck(int rcvSeqNum) throws Exception {
        Packet ackPacket = new Packet("1", "0", "0", sourcePort, this.destinationPort, rcvSeqNum + 1, this.currSeqNum, "", 0);
        DatagramPacket ackDatagramPacket = ackPacket.convertToDatagramPacket(destinationPort, destinationIP);
        this.enSocket.send(ackDatagramPacket);
    }

    @Override
    public void receive(String pathToFile) throws Exception {
        FileWriter newFile = new FileWriter(pathToFile);
        newFile.close();
        this.writer = new BufferedWriter(new FileWriter(pathToFile, true));
        this.nextToWriteOnFile = 1;

        while(this.currState != State.CLOSE_WAIT) {
            byte[] msg = new byte[Config.maxMsgSize];
            DatagramPacket receivedDatagram = new DatagramPacket(msg, msg.length);
            this.enSocket.receive(receivedDatagram);
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
                continue;
            }
            if(receivedPacket.getSeqNumber() == nextToWriteOnFile) {
                writeToFile(receivedPacket);
                nextToWriteOnFile++;
                addAllValidPacketsToFile();
                sendAck(nextToWriteOnFile);

            }
            else if(buffer.size() == 0 || receivedPacket.getSeqNumber() > buffer.get(buffer.size() - 1).getSeqNumber())
                buffer.add(receivedPacket);
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
        if(this.currState == State.TRANSFER || this.currState == State.CLOSE_WAIT) {
            byte[] msg = new byte[Config.maxMsgSize];
            this.currSeqNum++;
            Packet closePacket = new Packet("0", "0", "1", this.sourcePort, this.destinationPort, 0, this.currSeqNum, "", 0);
            DatagramPacket closeDatagramPacket = closePacket.convertToDatagramPacket(this.destinationPort, this.destinationIP);
            while (true) {
                this.enSocket.send(closeDatagramPacket);
                this.currState = this.currState == State.TRANSFER ? State.FIN_WAIT_1 : State.LAST_ACK;
                this.enSocket.setSoTimeout(1000);

                while (true) {
                    try {
                        DatagramPacket ackDatagramPacket = new DatagramPacket(msg, msg.length);
                        this.enSocket.receive(ackDatagramPacket);
                        Packet ackPacket = new Packet(new String(msg));
                        if (!ackPacket.getAckFlag().equals("1"))
                            throw new Exception("This message is not ACK");
                        if (ackPacket.getAckNumber() != (this.currSeqNum + 1))
                            //TODO: AFTER RECEIVE
                            throw new Exception("This message is not my ACK -- WILL CHANGE AFTER IMPLEMENTATION OF RECEIVE");
                        this.currState = this.currState == State.FIN_WAIT_1 ? State.FIN_WAIT_2 : State.CLOSED;
//                        if(this.currState == State.FIN_WAIT_2)
                            //TODO RECEIVE
                    } catch (SocketTimeoutException e) {
                        // timeout exception.
                        System.out.println("Timeout reached!!! " + e);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public long getSSThreshold() {
        return this.SSthreshold;
    }

    @Override
    public long getWindowSize() {
        return this.cwnd;
    }
}
