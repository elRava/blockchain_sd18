package blockchain;

import java.rmi.*;
import java.rmi.registry.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.io.*;
import java.util.*;

import registry.RegistryInterface;

import java.lang.reflect.MalformedParametersException;

/**
 * Class that defines the Application of the miner Run with parameters -p port
 * -b backup file (both not mandatory) Default port 7392. IP localhost. Also
 * provides auto cleaning and auto backup by two different threads.
 * 
 * @author Giuseppe Ravagnani
 * @version 1.0
 */
public class MinerApplication {
    public static void main(String[] args) {
        // -p port -b backup

        System.out.println("Miner application started");

        final int DEFAULT_PORT_REG = 7867;
        int portMiner = -1;
        int numberThread = 1;
        boolean fromBackup = false;
        Miner min = null;
        ArrayList<InetSocketAddress> listReg = new ArrayList<>();

        // read parameters from command line
        if (args.length % 2 != 0) {
            throw new MalformedParametersException();
        }

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-p")) {
                portMiner = Integer.valueOf(args[++i]);
            } else if (args[i].equals("-r")) {
                String reg = args[++i];
                String[] val = reg.split(":");
                if (val.length > 0) {
                    listReg.add(new InetSocketAddress(val[0], Integer.valueOf(val[1])));
                } else {
                    listReg.add(new InetSocketAddress(val[0], DEFAULT_PORT_REG));
                }
            } else if (args[i].equals("-t")) {
                numberThread = Integer.valueOf(args[++i]);
            }
        }
        if (portMiner == -1) {
            portMiner = Miner.DEFAULT_PORT;
        }

        try {
            // very bad mistake calling calss Registry while exists another calss Registry
            // on java RMI
            java.rmi.registry.Registry r = LocateRegistry.createRegistry(portMiner);
            min = new Miner();
            // porta di default presa automaticamente se non viene settata qua
            // messa nel costruttore del miner
            min.setPort(portMiner);
            min.setNumberMinerThread(numberThread);
            r.rebind("miner", min);
            // System.setProperty("java.rmi.server.hostname", "192.168.1.224");
            // System.out.println("......." + InetAddress.getLocalHost().getHostAddress());
            // InetAddre
            System.out
                    .println("Miner bound at //" + Miner.getMyAddress().getHostAddress() + ":" + portMiner + "/miner");
            /*
             * if(fromBackup) { System.out.println("Restoring registry from backup");
             * reg.restore(new File(path));
             * System.out.println("Registry restored from backup"); }
             */
        } catch (RemoteException re) {
            re.printStackTrace();
            System.exit(1);
        }

        /*
         * int portReg = 7867; String ip = "192.168.1.72";
         */
        for (int i = 0; i < listReg.size(); i++) {
            RegistryInterface reg = null;
            try {
                reg = (RegistryInterface) Naming
                        .lookup("//" + listReg.get(i).getHostName() + ":" + listReg.get(i).getPort() + "/registry");
            } catch (RemoteException re) {
                re.printStackTrace();
                reg = null;
            } catch (NotBoundException nbe) {
                nbe.printStackTrace();
                reg = null;
            } catch (MalformedURLException mue) {
                mue.printStackTrace();
                reg = null;
            }
            if (reg != null) {
                min.addRegistry(reg);
            }
        }

        min.startThreads();

        //min.chooseBlockchain();

        /*
         * RegistryApplication ra = new RegistryApplication(); CleanThread ct = ra.new
         * CleanThread(reg); Thread cleanThread = new Thread(ct);
         * cleanThread.setDaemon(true); cleanThread.start(); BackupThread bt = ra.new
         * BackupThread(reg); Thread backupThread = new Thread(bt);
         * backupThread.setDaemon(true); backupThread.start();
         */
    }

}