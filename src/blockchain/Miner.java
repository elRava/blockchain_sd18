package blockchain;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.registry.Registry;
import java.util.*;

import org.omg.CORBA.CurrentHelper;

import java.net.*;
import registry.*;
import java.rmi.server.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class Miner extends UnicastRemoteObject implements MinerInterface {

    public static final int DIFFICULTY = 6;
    public static final int DEFAULT_PORT = 7392;
    public static final int DEFAULT_MINER_THREAD = 1;
    public static final int TRANSACTIONS_PER_BLOCK = 4;

    private Blockchain blockchain;

    private List<RegistryInterface> registryList;
    private List<MinerInterface> minersIPList;

    private List<Transaction> transactionToSend;
    private List<Transaction> pendingTransactions;

    private List<Block> blockToSend;
    // private List<Block> pendingBlock;

    private Thread updateRegistry;
    private Thread transactionsThread;
    private Thread blocksThread;
    private Thread minerThread;

    private int myPort;

    private int numberMinerThread;

    private boolean firstConnection = true;

    public Miner() throws RemoteException {
        super();
        transactionToSend = new LinkedList<Transaction>();
        pendingTransactions = new LinkedList<Transaction>();
        blockToSend = new LinkedList<>();
        // pendingBlock = new LinkedList<>();
        registryList = new LinkedList<>();
        minersIPList = new LinkedList<>();
        blockchain = new Blockchain();
        // chooseBlockchain();
        myPort = DEFAULT_PORT;

        numberMinerThread = DEFAULT_MINER_THREAD;
    }

    public LinkedList<Block> getBlocksGivenLength(int depth) throws RemoteException{
        return blockchain.getFromDepth(depth);
    }
    

    public Blockchain getBlockchain() throws RemoteException {
        return blockchain;
    }

    public void setNumberMinerThread(int numberMinerThread) {
        if (numberMinerThread <= 0) {
            throw new NumberFormatException("The number of miner threads must be > 0");
        }
        this.numberMinerThread = numberMinerThread;
    }

    public void sendTransaction(Transaction transaction) throws RemoteException {
        synchronized (transactionToSend) {
            transactionToSend.add(transaction);
            transactionToSend.notifyAll();
            // System.out.println("Transazione ricevuta " +
            // Block.hashToString(transaction.getHash()));
            // delega tutti i controlli e le verifiche al thread che aggiunge le transazioni
            // e le manda a tutti
        }
    }

    public void sendBlock(Block block) throws RemoteException {
        // stesso modello delle transactions applicate ai blocchi
        synchronized (blockToSend) {
            blockToSend.add(block);
            blockToSend.notifyAll();
            // delega tutti i controlli e le verifiche al thread che aggiunge i blocchi alla
            // blockchain e le manda a tutti
        }
    }

    public void addRegistry(RegistryInterface reg) {
        synchronized (registryList) {
            registryList.add(reg);
        }
    }

    public void setPort(int port) {
        myPort = port;
    }

    public void startThreads() {

        updateRegistry = new Thread(new UpdateRegistry(20000, 10));
        transactionsThread = new Thread(new TransactionsThread());
        blocksThread = new Thread(new BlocksThread());
        minerThread = new Thread(new MinerThread(DIFFICULTY, numberMinerThread));

        updateRegistry.start();
        transactionsThread.start();
        blocksThread.start();
        minerThread.start();

    }

    private void chooseBlockchain() {
        synchronized (blockchain) {
            ArrayList<Triplet<byte[], MinerInterface, Integer>> hashMiner = new ArrayList<>();
            synchronized (minersIPList) {
                Iterator<MinerInterface> iterMiner = minersIPList.iterator();
                while (iterMiner.hasNext()) {
                    try {
                        // listHash.add(iterMiner.next().getBlockchain().getHash());
                        MinerInterface currentMiner = iterMiner.next();
                        byte[] currentHash = currentMiner.getBlockchain().getHash();
                        boolean found = false;
                        for (int i = 0; i < hashMiner.size(); i++) {
                            if (Arrays.equals(currentHash, hashMiner.get(i).first)) {
                                hashMiner.get(i).third ++;
                                found = true;
                            }
                        }
                        if (!found) {
                            hashMiner.add(new Triplet<byte[], MinerInterface, Integer>(currentHash, currentMiner, 1));
                        }
                    } catch (RemoteException re) {
                        re.printStackTrace();
                    }
                }
            }
            System.out.println("Ho preso " + hashMiner.size() + " differenti blockchain");
            if (hashMiner.isEmpty()) {
                return;
            }
            // Iterator<byte[]> occurance = setByte.iterator();
            byte[] longer = hashMiner.get(0).first;
            int max = hashMiner.get(0).third;
            MinerInterface bestMiner = hashMiner.get(0).second;
            for (int i = 1; i < hashMiner.size(); i++) {
                if (hashMiner.get(i).third > max) {
                    longer = hashMiner.get(i).first;
                    max = hashMiner.get(i).third;
                    bestMiner = hashMiner.get(i).second;
                }
            }
            System.out.println(
                    "Hash vincitore is " + Block.hashToString(longer) + " che si trova "+   max + "  volte");
            System.out.println("Inizio il download");
            try{
                blockchain = bestMiner.getBlockchain();
            }catch(RemoteException re){
                System.err.println("Run again");
                System.exit(1);
            }    
            System.out.println("Fine download");

            // I need to find which blockchain is the most frequent

            /*
             * while(iterByte.hasNext()){
             * 
             * }
             * 
             * 
             * Map<byte[], Integer> map = new HashMap<byte[], Integer>(); Iterator<byte[]>
             * iterByte = listHash.iterator(); while (iterByte.hasNext()) { byte[] current =
             * iterByte.next(); if (!map.containsKey(current)) { map.put(current, 1); } else
             * { int previousOccurance = map.get(current); previousOccurance++;
             * map.put(current, previousOccurance); } }
             * 
             * Set<byte[]> setByte = map.keySet();
             * 
             * System.out.println("Ho " + setByte.size() +
             * " possibili scelte di hash della blockchain");
             * 
             * if (setByte.isEmpty()) { return; }
             * 
             * Iterator<byte[]> occurance = setByte.iterator(); byte[] longer =
             * occurance.next(); int occ = map.get(longer); while (occurance.hasNext()) {
             * byte[] current = occurance.next(); if (map.get(current) > occ) { longer =
             * current; occ = map.get(current); } }
             */

            // i find the hash of the blockchain
            /*
             * synchronized (minersIPList) { Iterator<MinerInterface> minerIter =
             * minersIPList.iterator(); while (minerIter.hasNext()) { MinerInterface
             * currentMiner = minerIter.next(); try { if
             * (Arrays.equals(currentMiner.getBlockchain().getHash(), longer)) { //
             * synchronized (blockchain) { System.out
             * .println("Ho trovato il miner da cui scaricare la blockchain, inizio il download"
             * ); blockchain = currentMiner.getBlockchain(); break; // } } } catch
             * (RemoteException re) { re.printStackTrace(); } } }
             */

            // System.out.println("Blockchain is aggiornata, rilascio il synchronized");
        }

    }

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
                // System.out.println(i.getHostAddress());
            }
        }
        return null;

    }

    // thread che aggiorna registro
    private class UpdateRegistry implements Runnable {

        private long delayTime;
        private int numberConnection;

        // size sono il numero di miner cui voglio connettermi
        // se voglio size ip sono interessato al fatto che non ne voglio di uguali
        public UpdateRegistry(long time, int numberConnection) {
            this.delayTime = time;
            this.numberConnection = numberConnection;
        }

        public void run() {
            while (true) {
                // it contains all the InetSocketAddress obtained from all the registry
                List<InetSocketAddress> updatedMinerList = new LinkedList<InetSocketAddress>();

                // InetSocketAddress of the miner

                InetSocketAddress myAddress = null;
                myAddress = new InetSocketAddress(getMyAddress(), myPort);

                synchronized (registryList) {
                    Iterator<RegistryInterface> regIter = registryList.iterator();
                    while (regIter.hasNext()) {
                        RegistryInterface actual = regIter.next();
                        List<InetSocketAddress> addressFromThis = null; // inetsocketaddress from the actual registry
                        try {
                            actual.register(myAddress);
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
                }

                // System.out.println("Tutti i miner che ricevo dal registry sono: " +
                // updatedMinerList.size());

                // now i want to connect to a fixed number to miner
                // if i don't know so many address i will connect to all
                // -1 because we don't want to consider itself, problem if it is the only one
                int numberMiner = Math.min(updatedMinerList.size() - 1, numberConnection);

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
                    if (chose.getAddress().getHostAddress().equals(Miner.getMyAddress().getHostAddress())
                            && chose.getPort() == myPort) {
                        // System.out.println("Ho rimosso il mio");
                        continue;
                    }

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
                if (chosedMiner.size() > 0) {
                    synchronized (minersIPList) {
                        minersIPList = chosedMiner;
                    }
                }

                if (firstConnection) {
                    chooseBlockchain();
                    firstConnection = false;
                }

                try {
                    Thread.sleep(delayTime);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }

            }

        }

    }

    /**
     * thread che pensa alle transazioni
     */
    private class TransactionsThread implements Runnable {

        @SuppressWarnings("deprecation")
        public void run() {
            // while(lista non è vuota)
            // sincronized
            // wait
            // ottengo l'elemento dalla lista delle trnsazioni da inviare
            // lo cancello dalla lista
            // esco dal sincronized
            // con la transazione la verifico
            // valida se non compare mai nella blockchain, e se il verify delle chiavi
            // private e pubbliche ritorna true
            // non deve già essere presente nella pending list
            // se va tutto bene lo mando a tutti
            // alla fine lo metto nella mia lista

            // faccio il notify all per svegliare il miner che dormiva se non c'erano
            // transazioni
            // se avevo già 4 transazioni pendenti nella pendingTransactions, non rompo il
            // azzo al miner
            // se ne avevo di meno devo ristartarlo
            // ricomincio da capo

            while (true) {

                List<Transaction> tempList = new LinkedList<>();
                synchronized (transactionToSend) {
                    while (transactionToSend.isEmpty()) {
                        try {
                            transactionToSend.wait();
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                            System.exit(1);
                        }
                    }
                    // copy list
                    while (!transactionToSend.isEmpty()) {
                        tempList.add(transactionToSend.remove(0));
                    }
                }

                LinkedList<MinerInterface> listMiners = null;
                synchronized (minersIPList) {
                    // just clone
                    listMiners = new LinkedList<MinerInterface>();
                    for (MinerInterface mi : minersIPList) {
                        listMiners.add(mi);
                    }
                }

                while (!tempList.isEmpty()) {

                    Transaction t = tempList.remove(0);

                    System.out.println("Transaction received: " + Block.hashToString(t.getHash()));

                    // possibile errore è blockchain contains quando sta ancora facendo
                    // choseBlockchain
                    if (t.verify() && !pendingTransactions.contains(t) && !blockchain.contains(t)) {
                        // send to every miner
                        for (MinerInterface mi : listMiners) {
                            try {
                                mi.sendTransaction(t);
                            } catch (RemoteException re) {
                                re.printStackTrace();
                            }
                        }

                        synchronized (pendingTransactions) {
                            if (!pendingTransactions.contains(t)) {
                                pendingTransactions.add(t);
                            }
                            // pendingTransactions.notifyAll();
                        }

                        // restart miner thread
                        // @SuppressWarnings("deprecation")
                        if (pendingTransactions.size() < TRANSACTIONS_PER_BLOCK) {
                            minerThread.stop();

                            // minerThread.start();
                            minerThread = new Thread(new MinerThread(DIFFICULTY, numberMinerThread));
                            minerThread.start();
                        }

                    }

                }

            }

        }

    }

    /**
     * thread che pensa ai blocchi
     */
    private class BlocksThread implements Runnable {

        @SuppressWarnings("deprecation")
        public void run() {
            // killa il miner, da vedere se possibile con setDeamond
            // da vedere semmai se killare il miner solo quando un blocco lo si trova valido

            // while(empty)
            // sincronized
            // wait
            // prendo in maniera sincronizzata un solo blocco dalla blocktosend
            // lo rimuovo dalla lista
            // effettuo la verifica del blocco
            // verifica è hash valido, transazioni non esistano da nessuna altra parte e
            // ransazioni valide

            // se tutto valido mando il blocco a tutti gli altri
            // aggiungo in maniera sincronizzata su blockchain il blocco alla mia blockchain
            // ricomincio da capo
            // se non c'è niente prima di addormentarmi nell'attesa di qualche nuovo blocco
            // veglio il miner
            // mi addormento

            while (true) {

                List<Block> tempList = new LinkedList<>();
                synchronized (blockToSend) {
                    while (blockToSend.isEmpty()) {
                        try {
                            // System.out.println("wait");
                            blockToSend.wait();
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                            System.exit(1);
                        }
                    }
                    // System.out.println("wake up");
                    // block arrived, stop mining!

                    minerThread.stop();

                    // copy list
                    while (!blockToSend.isEmpty()) {
                        Block b = blockToSend.remove(0);
                        tempList.add(b);
                        System.out.println("Block received: " + Block.hashToString(b.getHash()));
                    }
                }

                LinkedList<MinerInterface> listMiners = null;
                synchronized (minersIPList) {
                    // just clone
                    listMiners = new LinkedList<MinerInterface>();
                    for (MinerInterface mi : minersIPList) {
                        listMiners.add(mi);
                    }
                }
                System.out.println("Temp list " + tempList.size());
                while (!tempList.isEmpty()) {
                    Block b = tempList.remove(0);

                    // System.out.println("Block hash: " + Block.hashToString(b.getHash()));
                    // System.out.println("Previous Block hash: " +
                    // Block.hashToString(b.getPreviousHash()));
                    boolean valid = false;
                    // blockchain
                    synchronized (blockchain) {
                        // System.out.println("Entro");
                        System.out.println("Contenuta? " + blockchain.contains(b));
                        if (b.verifyBlock(DIFFICULTY) && !blockchain.contains(b)) {
                            valid = true;
                            for (Transaction t : b.getListTransactions()) {
                                if (blockchain.contains(t)) {
                                    continue;
                                }
                            }

                            blockchain.addBlock(b);

                            // remove block's transaction from pending transactions
                            synchronized (pendingTransactions) {
                                for (Transaction t : b.getListTransactions()) {
                                    // if not present does nothing
                                    pendingTransactions.remove(t);
                                }
                            }

                            System.out.println("Blockchain hash: " + Block.hashToString(blockchain.getHash()));
                            System.out.println("Blockchain length: " + blockchain.length());
                            System.out.println(
                                    "Blockchain last block: " + Block.hashToString(blockchain.lastBlock().getHash()));

                            blockchain.print("blockchain/print/M" + myPort + ".txt");
                        }
                    }
                    // System.out.println("esco");
                    if (valid) {
                        // send to every miner
                        for (MinerInterface mi : listMiners) {
                            try {
                                mi.sendBlock(b);
                            } catch (RemoteException re) {
                                re.printStackTrace();
                            }
                        }
                    }
                }

                // TODO: does not work well, look after minerThread debug
                try {
                    // minerThread.start();
                    minerThread = new Thread(new MinerThread(DIFFICULTY, numberMinerThread));
                    minerThread.start();
                } catch (IllegalThreadStateException itse) {
                    itse.printStackTrace();
                    continue;
                }
            }

        }

    }

    /**
     * thread che mina e basta
     */
    private class MinerThread implements Runnable {

        private int difficulty;
        private int numThread;

        public MinerThread(int difficulty, int numThread) {
            this.difficulty = difficulty;
            this.numThread = numThread;
        }

        public void run() {

            System.out.println("Miner Thread start");

            // while(true) {

            List<Transaction> actual = new LinkedList<Transaction>();
            synchronized (pendingTransactions) {
                while (pendingTransactions.isEmpty()) {
                    try {
                        System.out.println("dorme num Transact: " + pendingTransactions.size());
                        pendingTransactions.wait();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
                System.out.println("sveglio num Transact: " + pendingTransactions.size());

                // there are at least one transaction
                Iterator<Transaction> iter = pendingTransactions.iterator();
                while (iter.hasNext() && actual.size() < TRANSACTIONS_PER_BLOCK) { // no more than 4 transactions for
                                                                                   // block
                    Transaction t = iter.next();
                    if (t.verify() && !blockchain.contains(t)) {
                        System.out.println("Aggiunta transazione a lista");
                        actual.add(t);
                    } else {
                        System.out.println("Transazione eliminata");
                        iter.remove();
                    }
                }
            }
            // I will mine this block

            Block b = new Block();

            // Iterator through the list of transaction
            Iterator<Transaction> iter = actual.iterator();
            while (iter.hasNext()) { // no more than 4 transactions for block
                System.out.println("Aggiunta transazione a blocco");
                b.addTransaction(iter.next());
            }

            System.out.println("lista blocco size: " + b.getListTransactions().size());

            // I need to set the previous hash on the block
            b.setPreviousHash(blockchain.lastBlock().getHash());

            System.out.println("inizia mining");
            // The block is ready, i can start mining
            b.mineBlock(difficulty, numThread);

            System.out.println("##### WINNER #####");
            System.out.println("Hash blocco: " + Block.hashToString(b.getHash()));

            // clean used transactions
            synchronized (pendingTransactions) {
                for (int i = 0; i < b.getListTransactions().size(); i++) {
                    System.out.println("removed transaction");
                    pendingTransactions.remove(0);
                }
            }

            // when I mine the block I can add it to the block to send list
            synchronized (blockToSend) {
                blockToSend.add(b);
                blockToSend.notifyAll();
            }

            // b = null;

            // }
        }

    }

    private class Triplet<T, U, V> {

        public T first;
        public U second;
        public V third;

        public Triplet(T first, U second, V third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }

    }

}