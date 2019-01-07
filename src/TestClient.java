import registry.*;
import java.security.*;
import java.util.*;
import blockchain.*;
import java.sql.Timestamp;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.net.UnknownHostException;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.*;
import java.rmi.*;
import java.lang.reflect.MalformedParametersException;

public class TestClient {

    public static void main(String[] args) {

        final int DEFAULT_PORT_REG = 7867;
        // mi collego ai miner

        List<RegistryInterface> registryList = new LinkedList<>();
        ArrayList<InetSocketAddress> listReg = new ArrayList<>();
        // read parameters from command line
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
            }
        }

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
                registryList.add(reg);
            }
        }

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
        int numberConnection = 100;
        // now i want to connect to a fixed number to miner
        // if i don't know so many address i will connect to all
        // -1 because we don't want to consider itself, problem if it is the only one
        int numberMiner = Math.min(updatedMinerList.size(), numberConnection);

        // random generator
        Random r = new Random();

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
            int find = r.nextInt(updatedMinerList.size());
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
                System.out.println("valid? " + valid);
                // I create the reference to the remote object, if it is possible
                MinerInterface m = null;
                String ip = chose.getAddress().getHostAddress();
                int portMiner = chose.getPort();
                // System.out.println("Mi provo a collegare a IP " + ip + " e porta " +
                // portMiner);
                try {
                    m = (MinerInterface) Naming.lookup("//" + ip + ":" + portMiner + "/miner");
                    System.out.println("Successfully connected to IP " + ip + ":" + portMiner);
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
                System.out.println("Total connected miner: " + chosedMiner.size());
            }
            // I compute at avery itaration the possible number of miner

            // numberMiner = Math.min(updatedMinerList.size() , numberConnection -
            // chosedMiner.size());
            // System.out.println("Number Miner da raggiungere "+numberMiner);

        }

        // list with minerInterface is completed
        /*
         * if (chosedMiner.size() > 0) { synchronized (minersIPList) { minersIPList =
         * chosedMiner; } }
         * 
         * try { Thread.sleep(delayTime); } catch (InterruptedException ie) {
         * ie.printStackTrace(); }
         * 
         */

        // Transaction t = new Transaction();
        PublicKey pubAlice = null;
        PrivateKey priAlice = null;
        // creo chiave pubblica e privata

        try {
            KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
            // Inizializzo il key generator
            keygen.initialize(1024);
            KeyPair kp = keygen.genKeyPair();
            pubAlice = kp.getPublic();
            priAlice = kp.getPrivate();
        } catch (Exception nsae) {
            nsae.printStackTrace();
        }

        String convPubintoStringAlice = Base64.getEncoder().encodeToString(pubAlice.getEncoded());
        String convPriintoStringAlice = Base64.getEncoder().encodeToString(priAlice.getEncoded());

        System.out.println("Chiavi di Alice generate");

        PublicKey pubBob = null;
        PrivateKey priBob = null;

        try {
            KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
            // Inizializzo il key generator
            keygen.initialize(1024);
            KeyPair kp = keygen.genKeyPair();
            pubBob = kp.getPublic();
            priBob = kp.getPrivate();
        } catch (Exception nsae) {
            nsae.printStackTrace();
        }

        String convPubintoStringBob = Base64.getEncoder().encodeToString(pubBob.getEncoded());
        String convPriintoStringBob = Base64.getEncoder().encodeToString(priBob.getEncoded());

        /*
         * System.out.println("Chiave public Bob is "+convPubintoStringBob);
         * System.out.println("Chiave private Bob is "+convPriintoStringBob);
         */
        System.out.println("Chiavi di Bob generate");

        System.out.println("Creo b1");

        Transaction t1 = new Transaction(5, pubAlice, pubBob);
        t1.sign(priAlice);

        // test block thread
        //Block b = new Block();
        //b.addTransaction(t1);
        //b.setPreviousHash(new Block().genesisBlock().getHash());
        //b.mineBlock(4, 4);

        for (MinerInterface m : chosedMiner) {
            try {
                m.sendTransaction(t1);
                //m.sendBlock(b);
            } catch (RemoteException re) {
                System.out.println("Errore re");
            }
        }

    }

}