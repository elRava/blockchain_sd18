package registry;

import java.net.InetSocketAddress;
import java.rmi.*;
import java.rmi.server.*;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;
import java.io.*;

/**
 * Class that defines a distributed registry used by miners to create the p2p overlay network.
 * In this class each peer has to find the socket address of each other peer
 * but to get this, peers need to register themself at this registry
 * This registry provides also backup and restore non distrubuted functions
 * @author Giuseppe Ravagnani
 * @version 1.0
 */
public class Registry extends UnicastRemoteObject implements RegistryInterface {
    // the actual registry
    Map<InetSocketAddress, Timestamp> reg;

    /**
     * Constructor of the class
     * @throws RemoteException
     */
    public Registry() throws RemoteException {
        super();
        reg = new HashMap<>();
    }

    /**
     * Method that register a new InetSocketAddress in the registry.
     * It also saves the timestamp at which the registration has been made
     * so it is possible to remove old registrations.
     * If an address is still present, it will be overwritten and the timestamp will be updated
     * @param address the address that needs to be registred
     * @throws RemoteException
     */
    public void register(InetSocketAddress address) throws RemoteException {
        // synchronize on reg sonce it is a distributed registry and multiple hosts have access
        synchronized(reg) {
            Timestamp ts = new Timestamp(System.currentTimeMillis());
            reg.put(address, ts);
            System.out.println("... Registered " + address.getHostName() + ":" + address.getPort() + "   " + ts.toString());
        }
    }

    /**
     * Get the registry. Return in ArrayList form because it is a serializable object
     * @return the registry in ArrayList form
     * @throws RemoteException
     */
    public ArrayList<InetSocketAddress> getIPSet() throws RemoteException {
        // because Set is not serializable
        ArrayList<InetSocketAddress> list = new ArrayList<>();
        list.addAll(reg.keySet());
        return list;
    }

    /**
     * Check if a socket address is already in the registry
     * @param address the address that has to be checked
     * @throws RemoteException
     */
    public boolean exists(InetSocketAddress address) throws RemoteException {
        return reg.containsKey(address);
    }


    // FROM HERE ON, NO MORE REMOTE METHODS

    /**
     * Clean the registry by old registrations
     * @param millis the time difference allowed for the oldest registration
     */
    public void clean(long millis) {
        // synchronize in order to avoid new registrations while cleaning
        synchronized(reg) {
            // may take some time but it's ok
            for(Map.Entry<InetSocketAddress, Timestamp> e : reg.entrySet()) {
                if(System.currentTimeMillis() > e.getValue().getTime() + millis) {
                    reg.remove(e);
                }
            }
        }
    }

    /**
     * Create a backup file serializing the registry object
     * @param path the path at which save the file
     */
    public void backup(String path) {
        synchronized(reg) {
            ObjectOutputStream oos = null;
            try {
                File backup = new File(path);
                backup.createNewFile();
                oos = new ObjectOutputStream(new FileOutputStream(backup));
                oos.writeObject(reg);
                oos.flush();
                oos.close();
            } catch(FileNotFoundException fnfe) {
                fnfe.printStackTrace();
                System.exit(1);
            } catch(IOException ioe) {
                ioe.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Restore the registry from backup
     * @param backup the file containing the backup
     */
    // WARNING PAY ATTENTION
    @SuppressWarnings("unchecked")
    public void restore(File backup) {
        synchronized(reg) {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(new FileInputStream(backup));
                reg = (Map<InetSocketAddress, Timestamp>) ois.readObject();
                ois.close();
            } catch(FileNotFoundException fnfe) {
                fnfe.printStackTrace();
                System.exit(1);
            } catch(IOException ioe) {
                ioe.printStackTrace();
                System.exit(1);
            } catch(ClassNotFoundException cnfe) {
                cnfe.printStackTrace();
                System.exit(1);
            }
            
        }
    }

}