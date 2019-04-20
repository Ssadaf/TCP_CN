import java.io.*; 
  
class Packet implements java.io.Serializable 
{  
    private String ack_flag;
    private String syn_flag;
    private String source_port;
    private String destination_port;
    private String ack_number;
    private String seq_number;
}