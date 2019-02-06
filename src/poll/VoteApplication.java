package poll;

import java.util.*;
import blockchain.*;
import registry.*;
import java.net.*;
import java.io.*;
import java.lang.reflect.MalformedParametersException;
import java.rmi.*;
import java.security.*;

/**
 * Application used to the specific vote
 * @author Marco Sansoni
 * @version 1.0
 */
public class VoteApplication {

    /**
     * Main application
     * The setting to the network will be defined as input
     * @param args -r registry in the format ip:port, -s the name of the seat,
     *             -k the path of the txt containing the key to validate the vote
     */
    public static void main(String[] args) {

        //default port if it is not setted on args
        final int DEFAULT_PORT_REG = 7867;

        //List of the registry used to spread my vote
        List<RegistryInterface> registryList = new LinkedList<>();
        //Registry writed in Ip:Port format
        ArrayList<InetSocketAddress> listReg = new ArrayList<>();

        String seat = null;
        String keyPath = null;
        PublicKey pub = null;
        PrivateKey pri = null;

        if (args.length % 2 != 0) {
            throw new MalformedParametersException();
        }

        //I set all the parameters obtained in input
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-r")) {
                String reg = args[++i];
                String[] val = reg.split(":");
                //If i do not set the port i used as port registry the default one
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

        //Check if the input is complete
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

        //I obtain the value of public and private key
        pub = GenerateKey.getPublicFromFile(keyPath);
        pri = GenerateKey.getPrivateFromFile(keyPath);

        //I try to connect to all the registry on input
        
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
            //If i get an exception i do not add to the list
            if (reg != null) {
                registryList.add(reg);
            }
        }

        //List of the inetsocketaddress of the miner
        List<InetSocketAddress> updatedMinerList = new LinkedList<InetSocketAddress>();

        //Iterator on all registry
        Iterator<RegistryInterface> regIter = registryList.iterator();
        while (regIter.hasNext()) {
            RegistryInterface actual = regIter.next();
            List<InetSocketAddress> addressFromThis = null; // inetsocketaddress from the actual registry
            //From each registry i store all the miner in its list
            try {               
                addressFromThis = actual.getIPSet();
            } catch (RemoteException re) {
                re.printStackTrace();
            }
            // if a remoteexcpetion has been thrown address from list will be null
            if (addressFromThis != null) {
                updatedMinerList.addAll(addressFromThis);
            }
        }
       
        //I set the number of the connection
        int numberConnection = 3;
        // now i want to connect to a fixed number to miner
        // if i don't know so many address i will connect to all
        // -1 because we don't want to consider itself, problem if it is the only one
        int numberMiner = Math.min(updatedMinerList.size(), numberConnection);

        // random generator
        Random ra = new Random();

        //the final list of all the working connection
        List<MinerInterface> chosedMiner = new LinkedList<MinerInterface>();
        List<InetSocketAddress> addressChosedMiner = new LinkedList<InetSocketAddress>();

        // I continue until i reach the target number of connection
        while (chosedMiner.size() < numberMiner && updatedMinerList.size() > 0) {
            int find = ra.nextInt(updatedMinerList.size());
            InetSocketAddress chose = updatedMinerList.get(find);
            updatedMinerList.remove(find);
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

            //I connect to the miner in the list
            if (valid) {              
                // I create the reference to the remote object, if it is possible
                MinerInterface m = null;
                String ip = chose.getAddress().getHostAddress();
                int portMiner = chose.getPort();
               
                try {
                    m = (MinerInterface) Naming.lookup("//" + ip + ":" + portMiner + "/miner");
                } catch (RemoteException re) {
                    m = null;
                } catch (NotBoundException nbe) {                    
                    m = null;
                } catch (MalformedURLException mue) {
                    m = null;
                }

                // if i am able to connect, i add to the list
                // only condition to add the miner to the list
                if (m != null) {
                    chosedMiner.add(m);
                    addressChosedMiner.add(chose);
                }               
            }
           
        }
        //List of all candidates
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
        //I print on screen the possible parties with number related to it
        //Number will be used in order to define our reference
        for (int i = 0; i < allParties.size(); i++) {
            System.out.println((i + 1) + " - " + allParties.get(i));
        }
        Scanner in = new Scanner(System.in);
        boolean valid = false;
        int r = -1;
        while (!valid) {
            System.out.println("Which party would you like to vote? \n Write the number related to it");
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
        //r will contain our preference, party related will be on position r-1
        String party = allParties.get(r-1);
        
        //Create the transaction and send to all miner
        Vote v = new Vote(party, seat, pub);
        v.sign(pri);
        Transaction t = new Transaction(v, pub);
        t.sign(pri);
        for (MinerInterface m : chosedMiner) {
            try {
                m.sendTransaction(t);
            } catch (RemoteException re) {
                re.printStackTrace();
            }
        }

    }
}