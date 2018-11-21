package registry;

import java.rmi.*;
import java.net.*;
import java.util.List;

public interface RegistryInterface extends Remote {

    public boolean register(InetAddress address) throws RemoteException;

    public List getIPList() throws RemoteException;

}