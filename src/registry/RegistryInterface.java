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

<<<<<<< HEAD
    //Lista contiene IP e timestamp, in modo che dopo mezz'ora pulisce quelli con timestamp più vecchio di mezz'ora
    
    //Da fare metodo pulisci lista che viene chiamato da un thread in automatico ogni tot minuti che pulisce gli accessi vecchi

    //porta data da linea di comando quando viene chiamato, in caso fare well known è un attimo

    public boolean register(InetAddress address) throws RemoteException;
=======
    public void register(InetSocketAddress address) throws RemoteException;
>>>>>>> 88a5e7c7c48ac2fbc9c8aff01eeedc76c532aaf6

    public ArrayList<InetSocketAddress> getIPSet() throws RemoteException;

    public boolean exists(InetSocketAddress address) throws RemoteException;

}