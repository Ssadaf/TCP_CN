import java.io.IOException;
import java.net.DatagramPacket;

public class Receiver {
    public static void main(String[] args) throws Exception {
        TCPServerSocketImpl tcpServerSocket = new TCPServerSocketImpl(Config.receiverPortNum);
        TCPSocket tcpSocket = tcpServerSocket.accept();

        tcpSocket.receive("../test.txt");

//        tcpSocket.receive("receiving.mp3");
        tcpSocket.close();
//        tcpServerSocket.close();
    }
}
