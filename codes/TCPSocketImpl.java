import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;

import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.io.*;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

enum State{
    SLOW_START, CONGESTION_AVOIDANCE, FAST_RECOVERY,
    FIN_WAIT_1, FIN_WAIT_2, CLOSE_WAIT, TIMED_WAIT, CLOSED, LAST_ACK ;
}

public class TCPSocketImpl extends TCPSocket {
    private int destinationPort;
    private String destinationIP = Config.destinationIP;
    private EnhancedDatagramSocket enSocket;
    private State currState;
    private int sourcePort;
    private int currSeqNum;
    private ArrayList<Packet> buffer = new ArrayList<>();
    private ArrayList<Packet> sentPackets = new ArrayList<>();
    private int nextToWriteOnFile;
    private BufferedWriter writer;
    private FileInputStream reader;
    private int cwnd;
    private int ackedSeqNum;
    private int numDupAck;
    private int SSthreshold;
    private int congestionAvoidanceTemp;
    private Timer timer = new Timer();
    private TimerTask task;
    private boolean endOfFile = false;
    private boolean shouldClose = false;
    private int timerCounter = 0;
    private int lastWindowPacketBeforeFR;
    private int windowLimit = 0;


    class MyTimerTask extends TimerTask{
        public void run(){
            try {
                System.out.println("TIMEDOUT");
                System.out.println("ACKED: " + ackedSeqNum + " SEQ NUM:" +currSeqNum);

                retransmitPacket(ackedSeqNum);
                cwnd = 1;
                SSthreshold = Math.max(1, cwnd / 2);
                onWindowChange();
                numDupAck = 0;

                currState = State.SLOW_START;

                System.out.println("SCHEDULE TIMEOUT AFTER TIMEOUT!");
            } catch (Exception e) {
                System.out.println("Retransmission timeout failed.");
                e.printStackTrace();
            }
        }
    }


    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
        this.sourcePort = port;
        this.enSocket = new EnhancedDatagramSocket(port);
        this.currState = State.SLOW_START;
        this.currSeqNum = 0;
        this.cwnd = 1;
        this.onWindowChange();
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
        for(Packet packet: sentPackets) {
            if(packet.getSeqNumber() == retransmitSeqNum) {
                DatagramPacket sendDatagramPacket = packet.convertToDatagramPacket(this.destinationPort, this.destinationIP);
                this.enSocket.send(sendDatagramPacket);
            }
        }
        System.out.println("RETRANSMIT " + currState + "  " + retransmitSeqNum + " ACKED  " + ackedSeqNum);
        timer.cancel();
        timer.purge();
        timerCounter --;
        timer = new Timer();
        task =  new MyTimerTask();
        timer.schedule(task, Config.receiveTimeout);
        timerCounter ++;
    }

    public void cleanSentBuffer(){
        while(sentPackets.size()>0) {
            if ((sentPackets.get(0).getSeqNumber() < ackedSeqNum)) {
                sentPackets.remove(0);
            }
            else
                return;
        }
    }

    private void handleDupAck(Packet ackPacket) throws Exception {
        System.out.println("GOT DUP ACK" );
        if(this.currState == State.SLOW_START | this.currState == State.CONGESTION_AVOIDANCE) {
            this.numDupAck++;
            System.out.println("Dup: " + numDupAck);

            if (this.numDupAck == 3) {
                System.out.println("FAST RECOVERY");

                this.SSthreshold = Math.max(1, this.cwnd / 2);
                this.cwnd = this.SSthreshold + 3;
                this.onWindowChange();
                retransmitPacket(this.ackedSeqNum);
                currState = State.FAST_RECOVERY;
                lastWindowPacketBeforeFR = windowLimit;
                System.out.println("!!!!!" + windowLimit);
                System.out.println("LAST_WINDOW_PACKET SET" + lastWindowPacketBeforeFR);
            }
        }
        else if(currState == State.FAST_RECOVERY){
            System.out.println(ackPacket.getAckNumber()+ " "+ ackedSeqNum);
            cwnd ++;
            this.onWindowChange();
        }
    }

    private void handleNewAck(Packet ackPacket) throws Exception {
        timer.cancel();
        timer.purge();
        timerCounter --;

        if(endOfFile & (currSeqNum + 1 == ackPacket.getAckNumber()) ){
            shouldClose = true;
            return;
        }

        int ackCount = ackPacket.getAckNumber() - ackedSeqNum;
        System.out.println("new ack: " + ackPacket.getAckNumber());
        ackedSeqNum = ackPacket.getAckNumber();

        System.out.println("SCHEDULE TIMEOUT AFTER NEW ACK");
        timer = new Timer();
        task =  new MyTimerTask();
        timer.schedule(task, Config.receiveTimeout);
        timerCounter ++;

        cleanSentBuffer();

        if(this.currState == State.SLOW_START) {

            this.cwnd = this.cwnd + ackCount;
            this.onWindowChange();
            this.numDupAck = 0;
            if(this.cwnd >= this.SSthreshold)
                this.currState = State.CONGESTION_AVOIDANCE;
        }
        else if(this.currState == State.CONGESTION_AVOIDANCE) {


            this.congestionAvoidanceTemp ++;
            if(congestionAvoidanceTemp == cwnd){
                congestionAvoidanceTemp = 0;
                cwnd ++;
                this.onWindowChange();
            }
            numDupAck = 0;
        }
        else if(currState == State.FAST_RECOVERY){
            if(ackPacket.getAckNumber() == (lastWindowPacketBeforeFR) ){
                System.out.println("OUT OF FAST RECOVERY");
                cwnd = SSthreshold;
                numDupAck = 0;
                currState = State.CONGESTION_AVOIDANCE;
            }
            else{//partial ack

                System.out.println("PARTIAL ACK IN FAST RECOVERY" + ackPacket.getAckNumber() + "  "+ lastWindowPacketBeforeFR);
                cwnd = cwnd - ackCount;
                retransmitPacket(ackPacket.getAckNumber());
            }
        }

    }

    private void handleAck(Packet ackPacket) throws Exception {
        if(ackPacket.getAckNumber() == (this.ackedSeqNum ) )
            handleDupAck(ackPacket);

        else{
            handleNewAck(ackPacket);
        }
    }

    @Override
    public void send(String pathToFile) throws Exception{
        this.enSocket.setSoTimeout(0);
        File file = new File(pathToFile);
        this.reader = new FileInputStream(file);
        this.currSeqNum = 0;
        this.ackedSeqNum = 0;
        this.numDupAck = 0;
        this.congestionAvoidanceTemp = 0;
        SSthreshold = 8;
        int receiverBuffer = 1;
        System.out.println("SCHEDULE TIMEOUT FOR THE FIRST TIME");

        timer = new Timer();
        task =  new MyTimerTask();
        timer.schedule(task, Config.receiveTimeout);
        timerCounter ++;

        while (true) {
            byte[] chunk = new byte[Config.chunkSize];
            windowLimit = this.ackedSeqNum + this.cwnd + this.numDupAck;
            System.out.println("START  ack: " + ackedSeqNum + "  win: " + windowLimit + " seq: " + currSeqNum + " acked: " + ackedSeqNum + "!!!" + lastWindowPacketBeforeFR);
            System.out.println("BEFORE WHILE");

            System.out.println("SEQ - ACK: " + (currSeqNum - ackedSeqNum) + " BUFFER: " + receiverBuffer);
            while((!endOfFile) & currSeqNum + 1 <= windowLimit & currSeqNum - ackedSeqNum < receiverBuffer) {
                System.out.println("IN WHILE" + currSeqNum);
                if(reader.read(chunk) == -1) {
                    System.out.println("END OF FILE");
                    endOfFile = true;
                    reader.close();
                    Packet sendPacket = new Packet("0", "0", "0", this.sourcePort, this.destinationPort, 0, this.currSeqNum, "\n*^*^*^END", 0, 0);
                    DatagramPacket sendDatagramPacket = sendPacket.convertToDatagramPacket(this.destinationPort, this.destinationIP);
                    sentPackets.add(sendPacket);
                    this.enSocket.send(sendDatagramPacket);
                    break;
                }
                chunk = new String(chunk).replaceAll("\0", "").getBytes();
                this.currSeqNum ++;
                Packet sendPacket = new Packet("0", "0", "0", this.sourcePort, this.destinationPort, 0, this.currSeqNum, new String(chunk, "UTF-8"), 0, 0);
                DatagramPacket sendDatagramPacket = sendPacket.convertToDatagramPacket(this.destinationPort, this.destinationIP);
                sentPackets.add(sendPacket);
                this.enSocket.send(sendDatagramPacket);
                System.out.println("SENDING " + currState + " " + currSeqNum);
            }
            System.out.println("AFTER WHILE");
            if(shouldClose){
                System.out.println("SHOULD CLOSE");
                System.out.println("TIMER COUNTER: " + timerCounter);

                for(int i = 0; i<timerCounter; ++i){
                    timer.cancel();
                    timer.purge();
                }
                return;
            }

            byte[] msg = new byte[Config.maxMsgSize];
            DatagramPacket ackDatagram = new DatagramPacket(msg, msg.length);
            this.enSocket.receive(ackDatagram);
            Packet ackPacket = new Packet(new String(msg).replaceAll("^\\x00*", ""));
            receiverBuffer = ackPacket.getEmptyBufferSize();

            System.out.println("RECEIVED " + ackPacket.getAckNumber());

            handleAck(ackPacket);

            System.out.println("END");
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
                    System.out.println("Timeout reached!!! " + e);
                    break;
                }
            }
        }
    }

    @Override
    public void connect(String serverIP, int serverPort) throws Exception {
        Packet synPacket = new Packet("0", "1", "0", Config.senderPortNum, Config.receiverPortNum, 0, 0, "", 0, 0);
        DatagramPacket synDatagramPacket = synPacket.convertToDatagramPacket(serverPort, serverIP);
        int rcvSeqNum = sendSyn(synDatagramPacket);

        Packet ackPacket = new Packet("1", "0", "0", Config.senderPortNum, Config.receiverPortNum, rcvSeqNum + 1, 0, "", 0, Config.receiverBufferSize);
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
        System.out.println("Writing to file: " + newPacket.getSeqNumber() );
    }

    private void addAllValidPacketsToFile() throws Exception{
        Collections.sort(buffer);

        while(buffer.size()>0) {
            System.out.println("VALID WRITES NEXT TO WRITE " + nextToWriteOnFile + " first in buffer: " + buffer.get(0).getSeqNumber());
            if((buffer.get(0).getSeqNumber() == nextToWriteOnFile)) {
                writeToFile(buffer.get(0));
                nextToWriteOnFile++;
                buffer.remove(0);
            }
            else
                return;
        }
    }

    private boolean existsInBuffer(Packet newPacket) {
        for(Packet packet: buffer) {
            if(packet.getSeqNumber() == newPacket.getSeqNumber())
                return true;
        }
        return false;
    }

    private void sendAck(int seqNum) throws Exception {
        System.out.println("SENDING ACK " + seqNum);
        Packet ackPacket = new Packet("1", "0", "0", sourcePort, this.destinationPort,
                seqNum, this.currSeqNum, "", 0, Config.receiverBufferSize);
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
                System.out.println("SENDER WANTS TO CLOSE");
                this.currState = State.CLOSE_WAIT;
                this.currSeqNum++;
                Packet finAckPacket = new Packet("1", "0", "0", sourcePort, this.destinationPort, rcvSeqNum + 1, this.currSeqNum, "", 0, 0);
                DatagramPacket finAckDatagramPacket = finAckPacket.convertToDatagramPacket(this.destinationPort, destinationIP);
                for(int i = 0; i < 7; i++)
                    this.enSocket.send(finAckDatagramPacket);
                TimeUnit.SECONDS.sleep(3);
                continue;
            }
            System.out.println(receivedPacket.getSeqNumber());
            if(receivedPacket.getSeqNumber() == nextToWriteOnFile) {
                writeToFile(receivedPacket);
                nextToWriteOnFile++;
                addAllValidPacketsToFile();
                System.out.println("WROTE ALL TILL" + nextToWriteOnFile);
                sendAck(nextToWriteOnFile);
            }
            else if( (receivedPacket.getSeqNumber() > nextToWriteOnFile && !existsInBuffer(receivedPacket))) {
                buffer.add(receivedPacket);
                sendAck(nextToWriteOnFile);
            }
            else
                sendAck(nextToWriteOnFile);
        }
        this.writer.close();
    }


    @Override
    public void close() throws Exception {
        System.out.println("CLOSING...");

        if(currState == State.CLOSE_WAIT) {
            this.enSocket.close();
            return;
        }

        byte[] msg = new byte[Config.maxMsgSize];
        currSeqNum = 1;
        Packet closePacket = new Packet("0", "0", "1", this.sourcePort, this.destinationPort, 0, this.currSeqNum, "", 0, 0);
        DatagramPacket closeDatagramPacket = closePacket.convertToDatagramPacket(this.destinationPort, this.destinationIP);
        for(int i=0; i<7; i++){
            this.enSocket.send(closeDatagramPacket);
        }

        while (true) {
            this.enSocket.send(closeDatagramPacket);
            this.currState = (currState == State.SLOW_START || currState == State.CONGESTION_AVOIDANCE  || currState == State.FAST_RECOVERY)? State.FIN_WAIT_1 : State.LAST_ACK;

            while (true) {
                System.out.println("2-HEREEE " + currState);

                DatagramPacket ackDatagramPacket = new DatagramPacket(msg, msg.length);
                this.enSocket.receive(ackDatagramPacket);
                System.out.println("RECEIVED PACKET IN CLOSE");
                Packet ackPacket = new Packet(new String(msg));
                if(ackPacket.getAckFlag().equals("1")){
                    if(currState == State.FIN_WAIT_2) {
                        return;
                    }
                    if(currState == State.FIN_WAIT_1){
                        if(ackPacket.getAckNumber() == (this.currSeqNum + 1))
                            currState = State.FIN_WAIT_2;
                        else
                            continue;
                    }
                    if(currState == State.LAST_ACK)
                        return;
                }
                if (ackPacket.getFinFlag().equals("1") && currState == State.FIN_WAIT_2) {
                    this.currState = State.TIMED_WAIT;
                    TimeUnit.SECONDS.sleep(3);
                    this.currState = State.CLOSED;
                    this.enSocket.close();
                    System.out.println("1-HEREEE");
                    return;
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
