package registry;

import java.rmi.*;
import java.rmi.registry.*;
import java.text.SimpleDateFormat;
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.MalformedParametersException;

/**
 * Class that defines the Application of the registry Run with parameters -p
 * port -b backup file (both not mandatory) Default port 7867. IP localhost.
 * Also provides auto cleaning and auto backup by two different threads.
 * 
 * @author Giuseppe Ravagnani
 * @version 1.0
 */
public class RegistryApplication {
    public static void main(String[] args) {
        // -p port -b backup

        System.out.println("Registry application started");

        final int DEFAULT_PORT = 7867;
        int port = DEFAULT_PORT;
        boolean fromBackup = false;
        Registry reg = null;
        String path = null;

        // read parameters from command line
        if (args.length % 2 != 0) {
            throw new MalformedParametersException();
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-p")) {
                port = Integer.valueOf(args[++i]);
            } else if (args[i].equals("-b")) {
                fromBackup = true;
                path = args[++i];
            }
        }

        try {
            // very bad mistake calling calss Registry while exists another calss Registry
            // on java RMI
            java.rmi.registry.Registry r = LocateRegistry.createRegistry(port);
            reg = new Registry();
            r.rebind("registry", reg);

            InetAddress myAddress = getMyAddress();

            // set hostname
            System.setProperty("java.rmi.server.hostname", myAddress.getHostAddress());
            System.out.println("Registry bound at //" + myAddress.getHostAddress() + ":" + port + "/registry");

            // restore from backup
            if (fromBackup) {
                System.out.println("Restoring registry from backup");
                reg.restore(new File(path));
                System.out.println("Registry restored from backup");
            }
        } catch (RemoteException re) {
            re.printStackTrace();
            System.exit(1);
        }

        // start threads
        RegistryApplication ra = new RegistryApplication();
        CleanThread ct = ra.new CleanThread(reg);
        Thread cleanThread = new Thread(ct);
        cleanThread.setDaemon(true);
        cleanThread.start();
        BackupThread bt = ra.new BackupThread(reg);
        Thread backupThread = new Thread(bt);
        backupThread.setDaemon(true);
        backupThread.start();

    }

    /**
     * Get my address In each pc there are different IP addresses (localhost, net
     * address). Return the IP visible on LAN
     * 
     * @return the IP address
     */
    public static InetAddress getMyAddress() {
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
                    return i;
                }
            }
        }
        return null;

    }

    /**
     * Inner class that defines a thread that cleans the registry
     * 
     * @author Giuseppe Ravagnani
     * @version 1.0
     */
    private class CleanThread implements Runnable {

        Registry reg;
        // 1 minute
        public static final long TIME_WAIT = 1000 * 60;

        public CleanThread(Registry reg) {
            this.reg = reg;
        }

        public void run() {
            while (true) {
                try {
                    Thread.sleep(TIME_WAIT);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                    System.exit(1);
                }
                // half an hour
                System.out.println("Cleaning the registry");
                int before = reg.reg.size();
                reg.clean(1000 * 30);
                int after = reg.reg.size();
                System.out.println("Registry cleaned. Removed " + (before - after) + " elements");

            }
        }

    }

    /**
     * Inner class that defines a thread that created the backup files
     * 
     * @author Giuseppe Ravagnani
     * @version 1.0
     */
    private class BackupThread implements Runnable {

        Registry reg;
        // 2 minutes
        public static final long TIME_WAIT = 1000 * 60 * 2;

        public BackupThread(Registry reg) {
            this.reg = reg;
        }

        public void run() {
            while (true) {
                try {
                    Thread.sleep(TIME_WAIT);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                    System.exit(1);
                }
                // half an hour
                String path = "registry/data/"
                        + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(System.currentTimeMillis()))
                        + ".backup";
                System.out.println("Writing backup - " + path);
                reg.backup(path);
                System.out.println("Backup written");

            }
        }

    }

}