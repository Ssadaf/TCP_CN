import java.io.IOException;
import java.net.*;

public class Sender {
    public static void main(String[] args) throws Exception {
        TCPSocketImpl tcpSocket = new TCPSocketImpl(Config.senderIP, Config.senderPortNum);
        tcpSocket.connect(Config.receiverIP, Config.receiverPortNum);
        tcpSocket.send("../sending.txt");
//        tcpSocket.close();
        tcpSocket.saveCongestionWindowPlot();
    }
}
