import java.io.IOException;
import java.net.*;

public class Sender {
    public static void main(String[] args) throws Exception {
        TCPSocketImpl tcpSocket = new TCPSocketImpl(Config.senderIP, Config.senderPortNum);
        tcpSocket.connect(Config.receiverIP, Config.receiverPortNum);
        tcpSocket.send("../1MB.txt");
        tcpSocket.close();
        tcpSocket.saveCongestionWindowPlot();
        System.out.println("DONE");
    }
}
