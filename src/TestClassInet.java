import java.net.InetAddress;
import java.util.Map;
import java.util.*;
import java.net.*;
import java.net.DatagramSocket;

public class TestClassInet {

    public static void main(String[] args) throws Exception {
        Enumeration e = null;
        try {
            e = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException s) {
            s.printStackTrace();
        }
        while (e.hasMoreElements()) {
            NetworkInterface n = (NetworkInterface) e.nextElement();
            Enumeration ee = n.getInetAddresses();
            while (ee.hasMoreElements()) {
                InetAddress i = (InetAddress) ee.nextElement();
                if (!i.isLoopbackAddress() && i instanceof Inet4Address) {
                    i.get
                    //System.out.println()
                }
                // System.out.println(i.getHostAddress());
            }
        }
        /*
        return null;Random();
        int n = r.nextInt(0);
        System.out.println(" "+n);
        */
    }

}