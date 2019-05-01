import java.io.IOException;
import java.net.*;

public class Sender {
    public static void main(String[] args) throws Exception {
        TCPSocket tcpSocket = new TCPSocketImpl(Config.sourceIP, Config.sourcePortNum);
        tcpSocket.connect(Config.destinationIP, Config.destinationPortNum);
//        tcpSocket.send("sending.mp3");
//        tcpSocket.close();
//       tcpSocket.saveCongestionWindowPlot();
    }
}
