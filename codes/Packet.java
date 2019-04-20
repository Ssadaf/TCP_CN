import java.io.*;

class Packet implements java.io.Serializable 
{  
    private String ack_flag;
    private String syn_flag;
    private static final String source_port = "1111";
    private static final String destination_port = "7777";
    private String ack_number;
    private String seq_number;
    private String data;

    public Packet(String ack_flag, String syn_flag, String ack_number, String data)
    {        
        this.ack_flag = ack_flag;
        this.syn_flag = syn_flag;
        this.ack_number = ack_number;
        this.data = data;
    }

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