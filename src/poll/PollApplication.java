package poll;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.MalformedParametersException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.*;

import registry.*;
import blockchain.*;

/**
 * 
 * @author Giuseppe Ravagnani
 */
public class PollApplication {

    public static void main(String[] args) {

        final int DEFAULT_PORT_REG = 7867;
        ArrayList<InetSocketAddress> listReg = new ArrayList<>();
        ArrayList<String> listParties = new ArrayList<>();
        ArrayList<String> listKeys = new ArrayList<>();
        ArrayList<String> listSeats = new ArrayList<>();
        String seatsPath = null;
        String keysPath = null;
        String partiesPath = null;
        String outputPath = null;
        Blockchain blockchain = null;

        System.out.println("Poll Application Started");

        // read parameters from command line
        if (args.length % 2 != 0) {
            throw new MalformedParametersException();
        }
        // acquire parameters from command line
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-r")) {
                String reg = args[++i];
                String[] val = reg.split(":");
                if (val.length > 0) {
                    listReg.add(new InetSocketAddress(val[0], Integer.valueOf(val[1])));
                } else {
                    listReg.add(new InetSocketAddress(val[0], DEFAULT_PORT_REG));
                }
            } else if (args[i].equals("-s")) {
                seatsPath = args[++i];
            } else if (args[i].equals("-k")) {
                keysPath = args[++i];
            } else if (args[i].equals("-p")) {
                partiesPath = args[++i];
            } else if (args[i].equals("-o")) {
                outputPath = args[++i];
            }
        }

        if(seatsPath == null) {
            System.out.println("Missing seats list file");
            System.exit(1);
        }
        if(keysPath == null) {
            System.out.println("Missing public keys list file");
            System.exit(1);
        }
        if(partiesPath == null) {
            System.out.println("Missing parties list file");
            System.exit(1);
        }
        if(listReg.isEmpty()) {
            System.out.println("Missing registry");
            System.exit(1);
        }

        // get the blockchain and take the "best"
        Map<byte[], MinerInterface> minersBlockchain = new HashMap<>();

        for (InetSocketAddress addr : listReg) {

            RegistryInterface reg = null;

            try {
                reg = (RegistryInterface) Naming.lookup("//" + addr.getAddress().getHostAddress() + ":" + addr.getPort() + "/registry");
                ArrayList<InetSocketAddress> listMiners = reg.getIPSet();
                for (InetSocketAddress minAdd : listMiners) {
                    MinerInterface min = (MinerInterface) Naming.lookup("//" + minAdd.getAddress().getHostAddress() + ":" + minAdd.getPort() + "/miner");
                    minersBlockchain.put(min.getBlockchainHash(), min);
                }
            } catch (RemoteException re) {
                re.printStackTrace();
            } catch (MalformedURLException mue) {
                mue.printStackTrace();
            } catch (NotBoundException nbe) {
                nbe.printStackTrace();
            }

        }

        // take the max occurrency of blockchain hashes
        Map<byte[], Integer> occurrencies = new HashMap<>();

        for (byte[] hash : minersBlockchain.keySet()) {
            if (occurrencies.containsKey(hash)) {
                occurrencies.replace(hash, occurrencies.get(hash) + 1);
            } else {
                occurrencies.put(hash, 1);
            }
        }

        byte[] freq = null;
        int max = 0;

        for (Map.Entry<byte[], Integer> e : occurrencies.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                freq = e.getKey();
            }
        }

        // finally get the blockchain
        try {
            blockchain = minersBlockchain.get(freq).getBlockchain();
        } catch (RemoteException re) {
            re.printStackTrace();
            System.exit(1);
        }

        // fill the list of parties from file
        BufferedReader br1 = null;
        try {
            br1 = new BufferedReader(new FileReader(partiesPath));
        } catch (FileNotFoundException fnfe) {
            System.out.println("Parties File not found. Exit.");
            System.exit(1);
        }

        String line1 = null;
        try {
            while ((line1 = br1.readLine()) != null) {
                listParties.add(line1);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // fill list of public keys
        BufferedReader br2 = null;
        try {
            br2 = new BufferedReader(new FileReader(keysPath));
        } catch (FileNotFoundException fnfe) {
            System.out.println("Parties File not found. Exit.");
            System.exit(1);
        }

        String line2 = null;
        try {
            while ((line2 = br2.readLine()) != null) {
                listKeys.add(line2);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // fill list of seats
        BufferedReader br3 = null;
        try {
            br3 = new BufferedReader(new FileReader(seatsPath));
        } catch (FileNotFoundException fnfe) {
            System.out.println("Parties File not found. Exit.");
            System.exit(1);
        }

        String line3 = null;
        try {
            while ((line3 = br3.readLine()) != null) {
                listSeats.add(line3);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // now I have the blockchain, the list of seats, the list of
        Map<String, Integer> votes = new HashMap<>();

        Iterator<Block> it = blockchain.getIterator();

        while (it.hasNext()) {
            Block b = it.next();

            for (Transaction t : b.getListTransactions()) {

                Vote v = (Vote) t.getPayload();
                String vote = v.getVote();
                String seat = v.getSeat();
                String pubKey = Base64.getEncoder().encodeToString(v.getPublicKey().getEncoded());

                //System.out.println("vote:" + vote);

                // check if the public key is contained in the list of the keys
                // and the seat is contained on the list of the seats
                if (listParties.contains(vote) && listKeys.contains(pubKey) && listSeats.contains(seat)) {
                    if (votes.containsKey(vote)) {
                        votes.replace(vote, votes.get(vote) + 1);
                    } else {
                        votes.put(vote, 1);
                    }
                    listKeys.remove(pubKey);
                }

            }

        }

        // now print the report
        PrintWriter pw = null;
        try {
            System.out.println("Poll Report");
            if (outputPath != null) {
                pw = new PrintWriter(outputPath);
                pw.println("Poll Report");
            }

            for (Map.Entry<String, Integer> e : votes.entrySet()) {
                System.out.println(e.getKey() + " - " + e.getValue());
                if (outputPath != null) {
                    pw.println(e.getKey() + " - " + e.getValue());
                }
            }

            if(outputPath != null) {
                pw.close();
            }


        } catch(FileNotFoundException fnfe) {
            System.out.println("Cannot print the report since the file does not exist");
        }

    }

}