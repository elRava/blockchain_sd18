import java.net.InetAddress;
import java.util.Map;
import java.util.*;
import java.net.*;
import java.net.DatagramSocket;

public class TestClassInet {

    public static void main(String[] args) throws Exception {
        /*
         * Enumeration e = NetworkInterface.getNetworkInterfaces(); while
         * (e.hasMoreElements()) { NetworkInterface n = (NetworkInterface)
         * e.nextElement(); Enumeration ee = n.getInetAddresses(); while
         * (ee.hasMoreElements()) { InetAddress i = (InetAddress) ee.nextElement();
         * System.out.println(i.getHostAddress()); } }
         */
        String ip = null;
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ip = socket.getLocalAddress().getHostAddress();
        }
        System.out.println("IP "+ip);
    }

}