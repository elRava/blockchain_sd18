import java.net.InetAddress;

public class TestClassInet{

    public static void main(String[] args) throws Exception{
        InetAddress current = InetAddress.getLocalHost();
        System.out.println("Current address "+current.getHostAddress());
    }

}