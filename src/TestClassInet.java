import java.net.InetAddress;
import java.util.Map;
import java.util.*;

public class TestClassInet{

    public static void main(String[] args) throws Exception{
        Map<Integer, String> map = new HashMap<Integer, String> ();
        map.put(1, "ciao");
        map.put(1,"a");
        map.put(1, "tutti");
        map.put(2,"hi");

        String s = map.get(1);
        System.out.println("valore"+s);
    }

}