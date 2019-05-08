import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;

class Packet implements java.io.Serializable
{
    private String ackFlag;
    private String synFlag;
    private String finFlag;
    private int sourcePort;
    private int destinationPort;
    private int ackNumber;
    private int seqNumber;
    private String data;
    private byte[] buffer;
    private int offset;
    private int length;

    public Packet(String ackFlag, String synFlag, String finFlag,int sourcePort, int destinationPort, int ackNumber, int seqNumber, String data, int offset) {
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
                String[] parts = tokens[i].split(":");
                this.ackFlag = parts[1];
            }
            else if(tokens[i].startsWith("syn:")){
                String[] parts = tokens[i].split(":");
                this.synFlag = parts[1];
            }
            else if(tokens[i].startsWith("fin:")){
                String[] parts = tokens[i].split(":");
                this.finFlag = parts[1];
            }
            else if(tokens[i].startsWith("ack_num:")){
                String[] parts = tokens[i].split(":");
                this.ackNumber = Integer.parseInt(parts[1]);
            }
            else if(tokens[i].startsWith("source_port:")){
                String[] parts = tokens[i].split(":");
                this.sourcePort = Integer.parseInt(parts[1]);
            }
            else if(tokens[i].startsWith("destination_port:")){
                String[] parts = tokens[i].split(":");
                this.destinationPort = Integer.parseInt(parts[1]);
            }
            else if(tokens[i].startsWith("seqNum:")){
                String[] parts = tokens[i].split(":");
                this.seqNumber = Integer.parseInt(parts[1]);
            }
            else if(tokens[i].startsWith("data:")){
                String[] parts = tokens[i].split(":");
                if(parts.length > 1)
                    this.data = parts[1];
                else
                    this.data = "";
            }
        }
    }

    public String createMessage(){
        String msg = "";
        msg += "ack:" + ackFlag + "\nsyn:" + synFlag +"\nfin:"+ finFlag+ "\nack_num:" + String.valueOf(ackNumber)
             + "\nsource_port:" + String.valueOf(sourcePort) + "\ndestination_port:" + String.valueOf(destinationPort) + "\nseqNum:" + String.valueOf(seqNumber) + "\ndata:" + data + "\n";
        return msg;
    }

    public int getDestinationPort()
    {
        return destinationPort;
    }

    public int getSourcePort(){ return sourcePort;}

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