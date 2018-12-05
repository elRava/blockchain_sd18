package registry;

import java.rmi.*;
import java.net.*;
import java.util.*;

/**
 * Interface of a remote object that defines a registry
 * @author Giuseppe Ravagnani
 * @version 1.0
 */
public interface RegistryInterface extends Remote {

    public void register(InetSocketAddress address) throws RemoteException;

    public ArrayList<InetSocketAddress> getIPSet() throws RemoteException;

    public boolean exists(InetSocketAddress address) throws RemoteException;

}