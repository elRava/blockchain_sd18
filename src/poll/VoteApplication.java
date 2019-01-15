package poll;

import java.util.*;
import blockchain.*;
import registry.*;
import java.net.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.MalformedParametersException;
import java.rmi.*;
import java.security.*;
import java.io.*;

public class VoteApplication {

    public static void main(String[] args) {

        final int DEFAULT_PORT_REG = 7867;

        List<RegistryInterface> registryList = new LinkedList<>();
        ArrayList<InetSocketAddress> listReg = new ArrayList<>();

        String seat = null;
        String keyPath = null;
        PublicKey pub = null;
        PrivateKey pri = null;

        if (args.length % 2 != 0) {
            throw new MalformedParametersException();
        }

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-r")) {
                String reg = args[++i];
                String[] val = reg.split(":");
                if (val.length > 0) {
                    listReg.add(new InetSocketAddress(val[0], Integer.valueOf(val[1])));
                } else {
                    listReg.add(new InetSocketAddress(val[0], DEFAULT_PORT_REG));
                }
            } else if (args[i].equals("-s")) { // seat's name
                seat = args[++i];
            } else if (args[i].equals("-k")) { // key
                keyPath = args[++i];
            }
        }

        if (seat == null) {
            System.err.println("Seat is missing");
            System.exit(1);
        } else if (keyPath == null) {
            System.err.println("Key is missing");
            System.exit(1);
        } else if (listReg.isEmpty()) {
            System.err.println("Registry is missing");
            System.exit(1);
        }

        pub = GenerateKey.getPublicFromFile(keyPath);
        pri = GenerateKey.getPrivateFromFile(keyPath);

        for (int i = 0; i < listReg.size(); i++) {
            RegistryInterface reg = null;
            try {
                reg = (RegistryInterface) Naming
                        .lookup("//" + listReg.get(i).getHostName() + ":" + listReg.get(i).getPort() + "/registry");
            } catch (RemoteException re) {
                System.err.println(
                        "Registry " + listReg.get(i).getHostName() + ":" + listReg.get(i).getPort() + " not reachable");
                reg = null;
            } catch (NotBoundException nbe) {
                System.err.println(
                        "Registry " + listReg.get(i).getHostName() + ":" + listReg.get(i).getPort() + " not reachable");
                reg = null;
            } catch (MalformedURLException mue) {
                System.err.println(
                        "Registry " + listReg.get(i).getHostName() + ":" + listReg.get(i).getPort() + " not reachable");
                reg = null;
            }
            if (reg != null) {
                registryList.add(reg);
            }
        }

        // Connection to the miner
        List<InetSocketAddress> updatedMinerList = new LinkedList<InetSocketAddress>();

        // InetSocketAddress of the miner

        // InetSocketAddress myAddress = null;
        // myAddress = new InetSocketAddress(getMyAddress(), myPort);

        // synchronized (registryList) {
        Iterator<RegistryInterface> regIter = registryList.iterator();
        while (regIter.hasNext()) {
            RegistryInterface actual = regIter.next();
            List<InetSocketAddress> addressFromThis = null; // inetsocketaddress from the actual registry
            try {
                // actual.register(myAddress);
                // System.out.println("Correttamente registrato");
                addressFromThis = actual.getIPSet();
            } catch (RemoteException re) {
                re.printStackTrace();
            }
            // if a remoteexcpetion has been thrown address from list will be null
            if (addressFromThis != null) {
                updatedMinerList.addAll(addressFromThis);
            }
        }
        // }

        // System.out.println("Tutti i miner che ricevo dal registry sono: " +
        // updatedMinerList.size());
        int numberConnection = 3;
        // now i want to connect to a fixed number to miner
        // if i don't know so many address i will connect to all
        // -1 because we don't want to consider itself, problem if it is the only one
        int numberMiner = Math.min(updatedMinerList.size(), numberConnection);

        // random generator
        Random ra = new Random();

        // the final list of all the working connection
        List<MinerInterface> chosedMiner = new LinkedList<MinerInterface>();

        List<InetSocketAddress> addressChosedMiner = new LinkedList<InetSocketAddress>();

        // I continue until i reach the target number of connection
        // System.out.println("Attualmente connesso a " + chosedMiner.size() + "
        // Miner");
        while (chosedMiner.size() < numberMiner && updatedMinerList.size() > 0) {

            // System.out.println("Attualmente connesso a " + chosedMiner.size() + "
            // Miner");
            // System.out.println("Possibili miner ancora da testare " +
            // updatedMinerList.size());
            int find = ra.nextInt(updatedMinerList.size());
            InetSocketAddress chose = updatedMinerList.get(find);
            updatedMinerList.remove(find);

            /*
             * if (chose.getAddress().getHostAddress().equals(Miner.getMyAddress().
             * getHostAddress()) && chose.getPort() == myPort) { //
             * System.out.println("Ho rimosso il mio"); continue; }
             */

            boolean valid = true;

            // avoid to keep the same twice
            Iterator<InetSocketAddress> iterAdd = addressChosedMiner.iterator();
            while (iterAdd.hasNext()) {
                InetSocketAddress actual = iterAdd.next();
                if (actual.getAddress().getHostAddress().equals(chose.getAddress().getHostAddress())
                        && actual.getPort() == chose.getPort()) {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                // System.out.println("valid? " + valid);
                // I create the reference to the remote object, if it is possible
                MinerInterface m = null;
                String ip = chose.getAddress().getHostAddress();
                int portMiner = chose.getPort();
                // System.out.println("Mi provo a collegare a IP " + ip + " e porta " +
                // portMiner);
                try {
                    m = (MinerInterface) Naming.lookup("//" + ip + ":" + portMiner + "/miner");
                    // System.out.println("Successfully connected to IP " + ip + ":" + portMiner);
                } catch (RemoteException re) {
                    // re.printStackTrace();
                    m = null;
                } catch (NotBoundException nbe) {
                    // nbe.printStackTrace();
                    m = null;
                } catch (MalformedURLException mue) {
                    // mue.printStackTrace();
                    m = null;
                }
                // if i am able to connect, i add to the list
                // only condition to add the miner to the list
                if (m != null) {
                    chosedMiner.add(m);
                    addressChosedMiner.add(chose);
                }
                // System.out.println("Total connected miner: " + chosedMiner.size());
            }
            // I compute at avery itaration the possible number of miner

            // numberMiner = Math.min(updatedMinerList.size() , numberConnection -
            // chosedMiner.size());
            // System.out.println("Number Miner da raggiungere "+numberMiner);

        }
        // Print list of all Candidates
        String path = "poll/parties.txt";
        BufferedReader reader = null;
        String line = null;
        ArrayList<String> allParties = new ArrayList<>();
        try {
            reader = new BufferedReader(new FileReader(path));
            while ((line = reader.readLine()) != null) {
                allParties.add(line);
            }
            reader.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        for (int i = 0; i < allParties.size(); i++) {
            System.out.println((i + 1) + " - " + allParties.get(i));
        }
        Scanner in = new Scanner(System.in);
        boolean valid = false;
        int r = -1;
        while (!valid) {
            System.out.println("Which party would you like to vote? \n Write the number realted to it");
            try {
                r = Integer.parseInt(in.nextLine());
                if (r > 0 && r <= allParties.size()) {
                    valid = true;
                }
            } catch (NumberFormatException nfe) {
                System.err.println("Invalid choice");
                valid = false;
            }
        }
        String party = allParties.get(r-1);
        
        //Create the transaction and send to all miner
        Vote v = new Vote(party, seat, pub);
        v.sign(pri);
        Transaction t = new Transaction(v, pub);
        t.sign(pri);


        for (MinerInterface m : chosedMiner) {
            try {
                m.sendTransaction(t);
                // m.sendBlock(b);
            } catch (RemoteException re) {
                re.printStackTrace();
            }
        }

    }
}