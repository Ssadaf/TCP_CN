import java.io.*; 
  
class Packet implements java.io.Serializable 
{  
    private String ack_flag;
    private String syn_flag;
    private String source_port;
    private String destination_port;
    private String ack_number;
    private String data;

    public String get_ack_flag(){
        return ack_flag;
    }

    public String get_syn_flag(){
        return syn_flag;
    }

    public String get_ack_number(){
        return ack_number;
    }

    public String get_data(){
        return data;
    }
}