import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
  
class Packet implements java.io.Serializable 
{  
    private String ackFlag;
    private String synFlag;
    private String sourcePort;
    private String destinationPort;
    private String ackNumber;
    private String seqNumber;
    private String data;
    private byte[] buffer;
    private int offset;
    private int length;

    public Packet(String ackFlag, String synFlag, String sourcePort, String destinationPort, String ackNumber, String data, int offset)
    {        
        this.ackFlag = ackFlag;
        this.synFlag = synFlag;
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.ackNumber = ackNumber;
        this.data = data;
        this.offset = offset;

    }

    public String createMessage(){
        String msg = "";
        msg += "ack: " + ackFlag + "\nsyn: " + synFlag + "\nack_num: " + ackNumber
             + "\nsource_port: " + sourcePort + "\ndestination_port: " + destinationPort + "\ndata: " + data + "\n";
        return msg;
    }

    public String get_ack_flag()
    {
        return ackFlag;
    }

    public String get_syn_flag()
    {
        return synFlag;
    }

    public String get_ack_number()
    {
        return ackNumber;
    }

    public String get_data()
    {
        return data;
    }


    public DatagramPacket convertToDatagramPacket()
    {
        String msg = createMessage();
        buffer = msg.getBytes();
        length = msg.length();
        DatagramPacket dPacket = new DatagramPacket(buffer, offset, length);
        return dPacket;
    }
}