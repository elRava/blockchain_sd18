package registry;

import java.rmi.*;
import java.rmi.registry.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.io.*;
import java.util.Date;
import java.lang.reflect.MalformedParametersException;

/**
 * Class that defines the Application of the registry
 * Run with parameters -p port -b backup file (both not mandatory)
 * Default port 7867. IP localhost.
 * Also provides auto cleaning and auto backup by two different threads.
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
        if(args.length % 2 != 0) {
            throw new MalformedParametersException();
        }
        for(int i = 0; i < args.length; i++) {
            if(args[i].equals("-p")) {
                port = Integer.valueOf(args[++i]);
            } else if(args[i].equals("-b")) {
                fromBackup = true;
                path = args[++i];
            }
        }

        try {
            LocateRegistry.createRegistry(port);
            reg = new Registry();
            Naming.bind("//localhost:" + port + "/registry", reg);
            //System.setProperty("java.rmi.server.hostname", "192.168.1.224");
            //System.out.println("......." + InetAddress.getLocalHost().getHostAddress());
            System.out.println("Registry bound at //localhost:" + port + "/registry");
            if(fromBackup) {
                System.out.println("Restoring registry from backup");
                reg.restore(new File(path));
                System.out.println("Registry restored from backup");
            }
        } catch(RemoteException re) {
            re.printStackTrace();
            System.exit(1);
        } catch(AlreadyBoundException abe) {
            abe.printStackTrace();
            System.exit(1);
        } catch(MalformedURLException mue) {
            mue.printStackTrace();
            System.exit(1);
        } catch(UnknownHostException uhe) {
            uhe.printStackTrace();
            System.exit(1);
        }

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
     * Inner class that defines a thread that cleans the registry
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
            while(true) {
                try {
                    Thread.sleep(TIME_WAIT);
                } catch(InterruptedException ie) {
                    ie.printStackTrace();
                    System.exit(1);
                }
                // half an hour
                System.out.println("Cleaning the registry");
                int before = reg.reg.size();
                reg.clean(1000*30);
                int after = reg.reg.size();
                System.out.println("Registry cleaned. Removed " + (before - after) + " elements");
                
            }
        }

    }


    /**
     * Inner class that defines a thread that created the backup files
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
            while(true) {
                try {
                    Thread.sleep(TIME_WAIT);
                } catch(InterruptedException ie) {
                    ie.printStackTrace();
                    System.exit(1);
                }
                // half an hour
                String path = "registry/data/" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(System.currentTimeMillis())) + ".backup";
                System.out.println("Writing backup - " + path);
                reg.backup(path);
                System.out.println("Backup written");
                
            }
        }

    }




}