package registry;

import java.rmi.*;
import java.net.*;
import java.util.List;

public interface RegistryInterface extends Remote {

    //Lista contiene IP e timestamp, in modo che dopo mezz'ora pulisce quelli con timestamp più vecchio di mezz'ora
    
    //Da fare metodo pulisci lista che viene chiamato da un thread in automatico ogni tot minuti che pulisce gli accessi vecchi

    //porta data da linea di comando quando viene chiamato, in caso fare well known è un attimo

    public boolean register(InetAddress address) throws RemoteException;

    public List getIPList() throws RemoteException;

}