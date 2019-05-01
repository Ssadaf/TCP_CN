import java.io.IOException;
import java.net.*;

public class Sender {
    public static void main(String[] args) throws Exception {
        TCPSocket tcpSocket = new TCPSocketImpl(Config.senderIP, Config.senderPortNum);
        tcpSocket.connect(Config.receiverIP, Config.receiverPortNum);
//        tcpSocket.send("sending.mp3");
        tcpSocket.close();
//       tcpSocket.saveCongestionWindowPlot();
    }
}
