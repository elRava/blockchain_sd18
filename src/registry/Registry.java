package registry;

//import blockchain.*;
import java.net.InetAddress;
import java.rmi.*;
import java.sql.Timestamp;
import java.util.*;

public class Registry implements RegistryInterface {

    Map<InetAddress, Timestamp> reg;

    public Registry() {
        reg = new HashMap<>();
    }

    public boolean register(InetAddress address) throws RemoteException {
        return true;
    }

    public List getIPList() throws RemoteException {
        return null;
    }

}