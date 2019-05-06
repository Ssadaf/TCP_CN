import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;

class Packet implements java.io.Serializable
{
    private String ackFlag;
    private String synFlag;
    private String finFlag;
    private String sourcePort;
    private String destinationPort;
    private int ackNumber;
    private int seqNumber;
    private String data;
    private byte[] buffer;
    private int offset;
    private int length;

    public Packet(String ackFlag, String synFlag, String finFlag,String sourcePort, String destinationPort, int ackNumber, int seqNumber, String data, int offset) {
        this.ackFlag = ackFlag;
        this.synFlag = synFlag;
        this.finFlag = finFlag;
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.ackNumber = ackNumber;
        this.seqNumber = seqNumber;
        this.data = data;
        this.offset = offset;

    }

    public Packet(String data){
        String[] tokens = data.split("\n");
        for(int i=0; i<tokens.length; i++){
            if (tokens[i].startsWith("ack:")){
                String[] parts = data.split(":");
                this.ackFlag = parts[1];
            }
            else if(tokens[i].startsWith("syn:")){
                String[] parts = data.split(":");
                this.synFlag = parts[1];
            }
            else if(tokens[i].startsWith("fin:")){
                String[] parts = data.split(":");
                this.finFlag = parts[1];
            }
            else if(tokens[i].startsWith("ack_num:")){
                String[] parts = data.split(":");
                this.ackNumber = Integer.parseInt(parts[1]);
            }
            else if(tokens[i].startsWith("source_port:")){
                String[] parts = data.split(":");
                this.sourcePort = parts[1];
            }
            else if(tokens[i].startsWith("destination_port:")){
                String[] parts = data.split(":");
                this.destinationPort = parts[1];
            }
            else if(tokens[i].startsWith("seqNum:")){
                String[] parts = data.split(":");
                this.seqNumber = Integer.parseInt(parts[1]);
            }
            else if(tokens[i].startsWith("data:")){
                String[] parts = data.split(":");
                this.data = parts[1];
            }
        }
    }

    public String createMessage(){
        String msg = "";
        msg += "ack: " + ackFlag + "\nsyn: " + synFlag +"\nfin: "+ finFlag+ "\nack_num: " + String.valueOf(ackNumber)
             + "\nsource_port: " + sourcePort + "\ndestination_port: " + destinationPort + "\nseqNum" + String.valueOf(seqNumber) + "\ndata: " + data + "\n";
        return msg;
    }

    public String getDestinationPort()
    {
        return destinationPort;
    }

    public String getAckFlag()
    {
        return ackFlag;
    }

    public String getSynFlag()
    {
        return synFlag;
    }

    public String getFinFlag()
    {
        return finFlag;
    }

    public int getSeqNumber() { return seqNumber;}

    public int getAckNumber()
    {
        return ackNumber;
    }

    public String getData()
    {
        return data;
    }



    public DatagramPacket convertToDatagramPacket(int port, String IP) throws Exception
    {
        String msg = createMessage();
        buffer = msg.getBytes();
        length = msg.length();
        DatagramPacket dPacket = new DatagramPacket(buffer, offset, length);
        dPacket.setPort(port);
        dPacket.setAddress(InetAddress.getByName(IP));
        return dPacket;
    }
}