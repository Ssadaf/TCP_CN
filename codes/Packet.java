import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
  
class Packet implements java.io.Serializable 
{  
    private String ack_flag;
    private String syn_flag;
    private String source_port;
    private String destination_port; 
    private String ack_number;
    private String seq_number;
    private String data;
    private byte[] buffer;
    private int offset;
    private int length;

    public Packet(String ack_flag, String syn_flag, String source_port, String destination_port, String ack_number, String data, int offset)
    {        
        this.ack_flag = ack_flag;
        this.syn_flag = syn_flag;
        this.source_port = source_port;
        this.destination_port = destination_port;
        this.ack_number = ack_number;
        this.data = data;
        this.offset = offset;

    }

    public String createMessage(){
        String msg = "";
        msg += "ack: " + ack_flag + "\nsyn: " + syn_flag + "\nack_num: " + ack_number
             + "\nsource_port: " + source_port + "\ndestination_port: " + destination_port + "\n";
        return msg;
    }

    public String get_ack_flag()
    {
        return ack_flag;
    }

    public String get_syn_flag()
    {
        return syn_flag;
    }

    public String get_ack_number()
    {
        return ack_number;
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