class Config
{
    public static final String destinationIP = "127.0.0.1";
    public static final String senderIP = "127.0.0.1";
    public static final int senderPortNum = 8888;
    public static final String receiverIP = "127.0.0.1";
    public static final int receiverPortNum = 7777;
    public static final int maxMsgSize = 65535;
    public static final int maxBufferSize = 2000000;
    public static final int chunkSize = 512;
    public static final long receiveTimeout = 1000;
    public static final int receiverBufferSize = 20;
    public static final int msgSizeLimit = 1000;
}