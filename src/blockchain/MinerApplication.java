package blockchain;

import java.rmi.*;
import java.rmi.registry.*;
import java.net.*;
import java.io.*;
import java.util.*;

import registry.RegistryInterface;

import java.lang.reflect.MalformedParametersException;

/**
 * Class that defines the Application of the miner Run with parameters
 * -p port if not present default port 7392
 * -b backup file
 * -r registry where to connect. can connect to more registries. if port not defined, default 7867
 * -t number of threads for mining
 * IP localhost. Also provides auto cleaning and auto backup by two different threads.
 *
 * @author Giuseppe Ravagnani
 * @version 1.0
 */
public class MinerApplication {

    /**
     * 
     * @param args the list of arguments -p port -b backup -r registry -t number threads
     */
    public static void main(String[] args) {

        System.out.println("Miner application started");

        final int DEFAULT_PORT_REG = 7867;
        int portMiner = -1;
        int numberThread = 1;
        boolean fromBackup = false;
        Miner min = null;
        ArrayList<InetSocketAddress> listReg = new ArrayList<>();
        Blockchain bcToRestore = null;

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
            } else if (args[i].equals("-b")) {
                String path = args[++i];
                bcToRestore = Blockchain.restore(new File(path));
            }
        }
        if (portMiner == -1) {
            portMiner = Miner.DEFAULT_PORT;
        }

        boolean firstTime = true;

        try {
            // very bad mistake calling calss Registry while exists another calss Registry
            // on java RMI
            java.rmi.registry.Registry r = LocateRegistry.createRegistry(portMiner);
            min = new Miner();

            // porta di default presa automaticamente se non viene settata qua
            // messa nel costruttore del miner
            min.setPort(portMiner);
            min.setNumberMinerThread(numberThread);
            if (bcToRestore != null) {
                min.setBlockchain(bcToRestore);
            }
            r.rebind("miner", min);

            System.out
                    .println("Miner bound at //" + Miner.getMyAddress().getHostAddress() + ":" + portMiner + "/miner");

        } catch (RemoteException re) {
            re.printStackTrace();
            System.exit(1);
        }

        while (true) {

            // It cleans the registry list on the miner
            // it's updated with only the working registry every iteration
            synchronized (min) {
                min.clearRegistry();
                for (int i = 0; i < listReg.size(); i++) {
                    RegistryInterface reg = null;
                    String IPReg = listReg.get(i).getAddress().getHostAddress() + ":" + listReg.get(i).getPort();
                    try {
                        reg = (RegistryInterface) Naming.lookup("//" + IPReg + "/registry");
                    } catch (RemoteException re) {
                        System.err.println("Registry " + IPReg + " not reachable");
                        reg = null;
                    } catch (NotBoundException nbe) {
                        System.err.println("Registry " + IPReg + " not reachable");
                        reg = null;
                    } catch (MalformedURLException mue) {
                        System.err.println("Registry " + IPReg + " not reachable");
                        reg = null;
                    }
                    if (reg != null) {
                        min.addRegistry(reg);
                    }
                }
                // Start threads only first time
                if (firstTime) {
                    min.startThreads();
                }
                firstTime = false;
            }
            try {
                // update every 20 seconds
                Thread.sleep(20000);
            } catch (InterruptedException ie) {
            }

        }

    }

}
