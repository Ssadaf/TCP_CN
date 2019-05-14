import javax.tools.Tool;
import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

class Packet implements java.io.Serializable, Comparable< Packet >
{
    private String ackFlag;
    private String synFlag;
    private String finFlag;
    private int sourcePort;
    private int destinationPort;
    private int ackNumber;
    private int seqNumber;
    private byte[] data;
    private byte[] buffer;
    private int offset;
    private int length;
    private int emptyBufferSize;

    public Packet(String ackFlag, String synFlag, String finFlag,int sourcePort, int destinationPort
            , int ackNumber, int seqNumber, byte[] data, int offset, int emptyBufferSize) {
        this.ackFlag = ackFlag;
        this.synFlag = synFlag;
        this.finFlag = finFlag;
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.ackNumber = ackNumber;
        this.seqNumber = seqNumber;
        this.data = data;
        this.offset = offset;
        this.emptyBufferSize = emptyBufferSize;
    }

    private int convertByteToInt(int index, byte[] buff){
        ByteBuffer wrapped = ByteBuffer.wrap(buff, index,4);
        return wrapped.getInt();
    }

    public Packet(byte[] msg){
        this.ackFlag = String.valueOf(msg[0]);
        this.synFlag = String.valueOf(msg[1]);
        this.finFlag = String.valueOf(msg[2]);
        this.ackNumber = convertByteToInt(3, msg);
        this.sourcePort = convertByteToInt(7, msg);
        this.destinationPort = convertByteToInt(11, msg);
        this.emptyBufferSize = convertByteToInt(15, msg);
        this.seqNumber = convertByteToInt(19, msg);
        int dataLen = convertByteToInt(23, msg);
        this.data = Tools.deleteNullBytes(Arrays.copyOfRange(msg, 27, 27 + dataLen));

//        for(int i=0; i<tokens.length; i++){
//            if (tokens[i].startsWith("ack:")){
//                String[] parts = tokens[i].split(":");
//                this.ackFlag = parts[1];
//            }
//            else if(tokens[i].startsWith("syn:")){
//                String[] parts = tokens[i].split(":");
//                this.synFlag = parts[1];
//            }
//            else if(tokens[i].startsWith("fin:")){
//                String[] parts = tokens[i].split(":");
//                this.finFlag = parts[1];
//            }
//            else if(tokens[i].startsWith("ack_num:")){
//                String[] parts = tokens[i].split(":");
//                this.ackNumber = Integer.parseInt(parts[1]);
//            }
//            else if(tokens[i].startsWith("source_port:")){
//                String[] parts = tokens[i].split(":");
//                this.sourcePort = Integer.parseInt(parts[1]);
//            }
//            else if(tokens[i].startsWith("destination_port:")){
//                String[] parts = tokens[i].split(":");
//                this.destinationPort = Integer.parseInt(parts[1]);
//            }
//            else if(tokens[i].startsWith("seqNum:")){
//                String[] parts = tokens[i].split(":");
//                this.seqNumber = Integer.parseInt(parts[1]);
//            }
//            else if(tokens[i].startsWith("buffer:")){
//                String[] parts = tokens[i].split(":");
//                this.emptyBufferSize = Integer.parseInt(parts[1]);
//            }
//            else if(tokens[i].startsWith("data:")){
//                String[] parts = tokens[i].split(":", 2);
//                if(parts.length <= 1)
//                    this.data = "";
//                else
//                    this.data = parts[1];
//            }
//            else {
//                this.data = this.data + "\n" + tokens[i];
//            }
//        }
    }

    private byte[] convertFlagToByte(String flag) {
        byte[] flagByte = new byte[1];
        if(flag.equals("0")) {
            flagByte[0] = (byte)0;
        }
        else
            flagByte[0] = (byte)1;
        return flagByte;
    }

    private byte[] convertIntToByte(int num) {
        byte[] array = ByteBuffer.allocate(4).putInt(num).array();
        byte[] result = new byte[4];
        result[0] = array[0];
        result[1] = array[1];
        result[2] = array[2];
        result[3] = array[3];
        return result;
    }

    private byte[] createMessage(){
        byte[] msg = new byte[0];
        msg = Tools.concatenate(msg, convertFlagToByte(ackFlag));
        msg = Tools.concatenate(msg, convertFlagToByte(synFlag));
        msg = Tools.concatenate(msg, convertFlagToByte(finFlag));
        msg = Tools.concatenate(msg, convertIntToByte(ackNumber));
        msg = Tools.concatenate(msg, convertIntToByte(sourcePort));
        msg = Tools.concatenate(msg, convertIntToByte(destinationPort));
        msg = Tools.concatenate(msg, convertIntToByte(emptyBufferSize));
        msg = Tools.concatenate(msg, convertIntToByte(seqNumber));
        msg = Tools.concatenate(msg, convertIntToByte(data.length));
        msg = Tools.concatenate(msg, this.data);

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

    public int getEmptyBufferSize() { return emptyBufferSize; }

    public byte[] getData()
    {
        return data;
    }

    public DatagramPacket convertToDatagramPacket(int port, String IP) throws Exception
    {
        byte[] msg = createMessage();
        length = msg.length;
        DatagramPacket dPacket = new DatagramPacket(msg, offset, length);
        dPacket.setPort(port);
        dPacket.setAddress(InetAddress.getByName(IP));
        return dPacket;
    }

    @Override
    public int compareTo(Packet p) {
        return Integer.compare(this.getSeqNumber(), p.getSeqNumber());
    }
}