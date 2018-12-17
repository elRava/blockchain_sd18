package blockchain;

import java.rmi.*;
import java.rmi.registry.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.io.*;
import java.util.Date;

import registry.RegistryInterface;

import java.lang.reflect.MalformedParametersException;

/**
 * Class that defines the Application of the miner
 * Run with parameters -p port -b backup file (both not mandatory)
 * Default port 7392. IP localhost.
 * Also provides auto cleaning and auto backup by two different threads.
 * @author Giuseppe Ravagnani
 * @version 1.0
 */
public class MinerApplication {
    public static void main(String[] args) {
        // -p port -b backup

        System.out.println("Registry application started");

        final int DEFAULT_PORT = 7392;
        int port = DEFAULT_PORT;
        boolean fromBackup = false;
        Miner min = null;
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
            // very bad mistake calling calss Registry while exists another calss Registry on java RMI
            java.rmi.registry.Registry r = LocateRegistry.createRegistry(port);
            min = new Miner();
            r.rebind("miner", min);
            //System.setProperty("java.rmi.server.hostname", "192.168.1.224");
            //System.out.println("......." + InetAddress.getLocalHost().getHostAddress());
            System.out.println("Miner bound at //localhost:" + port + "/miner");
            /*if(fromBackup) {
                System.out.println("Restoring registry from backup");
                reg.restore(new File(path));
                System.out.println("Registry restored from backup");
            }*/
        } catch(RemoteException re) {
            re.printStackTrace();
            System.exit(1);
        }

        int portReg = 7867;
        String ip = "192.168.1.224";
        RegistryInterface reg = null;
        try{
            reg = (RegistryInterface) Naming.lookup("//" + ip+":"+portReg + "/registry"); 
        }catch(RemoteException re){
            re.printStackTrace();
            
        }catch(NotBoundException nbe){
            nbe.printStackTrace();
            
        }catch(MalformedURLException mue){
            mue.printStackTrace();
            
        }
        min.addRegistry(reg);


        min.startThreads();

        



        /*
        RegistryApplication ra = new RegistryApplication();
        CleanThread ct = ra.new CleanThread(reg);
        Thread cleanThread = new Thread(ct);
        cleanThread.setDaemon(true);
        cleanThread.start();
        BackupThread bt = ra.new BackupThread(reg);
        Thread backupThread = new Thread(bt);
        backupThread.setDaemon(true);
        backupThread.start();
        */
    }



}