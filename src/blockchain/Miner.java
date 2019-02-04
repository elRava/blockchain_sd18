package blockchain;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.omg.CORBA.portable.RemarshalException;

import java.io.BufferedReader;
import java.io.*;
import java.io.ObjectInputStream;
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
    private Thread askBlockThread;

    private int myPort;

    private int numberMinerThread;

    // private boolean firstConnection = true;

    private Semaphore askBlocksSemaphore;
    private Semaphore blocksRetrievedSemaphore;
    private Semaphore startGettingBlocks;

    private int blocksDoNotSend = 0;

    // private int lengthToSend = 0;

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

        // on first run, ask blocks
        askBlocksSemaphore = new Semaphore(1);
        blocksRetrievedSemaphore = new Semaphore(0);
        startGettingBlocks = new Semaphore(0);

        numberMinerThread = DEFAULT_MINER_THREAD;
    }

    /**
     * Remote method used to ontain the blockchain
     * @return the blockchain
     * @throws RemoteException
     */
    public Blockchain getBlockchain() throws RemoteException {
        return blockchain;
    }

    /**
     * Remote method used to return the hash of the blockchain
     * @return hash of the blockchain
     * @throws RemoteException
     */
    public byte[] getBlockchainHash() throws RemoteException {
        return blockchain.getHash();
    }

    /**
     * Used to set the current blockchain
     * @param blockchain that we set on the miner
     */
    public void setBlockchain(Blockchain blockchain) {
        synchronized (blockchain) {
            this.blockchain = blockchain;
        }
    }

    /**
     * Set the number of thread used for mining on the miner
     * @param numberMinerThread number of thread deducated to mining
     */
    public void setNumberMinerThread(int numberMinerThread) {
        if (numberMinerThread <= 0) {
            throw new NumberFormatException("The number of miner threads must be > 0");
        }
        this.numberMinerThread = numberMinerThread;
    }

    /**
     * Remote method used to send transaction to all the connected miner
     * @param transaction to be send
     * @throws RemoteException
     */
    public void sendTransaction(Transaction transaction) throws RemoteException {
        synchronized (transactionToSend) {
            transactionToSend.add(transaction);
            transactionToSend.notifyAll();
            //All the check will be done on the thrad that handle transaction
        }
    }

    /**
     * Remote method used to send block to all the connected miner
     * @param block to be sent
     * @throws RemoteException
     */
    public void sendBlock(Block block) throws RemoteException {
        //we use the same model of the trasaction on the block
        synchronized (blockToSend) {
            blockToSend.add(block);
            blockToSend.notifyAll();          
        }
    }

    /**
     * Remote method that return list of block from the block containing hash until the end of the blockchain
     * @param hash of the block which start the list to be returned
     * @return list of all blocks
     * @throws RemoteException
     */
    public LinkedList<Block> getMissingBlocks(byte[] hash) throws RemoteException {
        return blockchain.getMissingBlocks(hash);
    }

    /**
     * Add a registry
     * @param reg the registry to be added
     */
    public void addRegistry(RegistryInterface reg) {
        synchronized (registryList) {
            registryList.add(reg);
        }
    }

    /**
     * Clear the list of the registry
     */
    public void clearRegistry() {
        synchronized (registryList) {
            registryList.clear();
        }
    }

    /**
     * Set the port of the current miner
     * @param port
     */
    public void setPort(int port) {
        myPort = port;
    }

    /**
     * It runs all the thread on the miner
     */
    public void startThreads() {

        updateRegistry = new Thread(new UpdateRegistry(20000, 10));
        transactionsThread = new Thread(new TransactionsThread());
        blocksThread = new Thread(new BlocksThread());
        minerThread = new Thread(new MinerThread(DIFFICULTY, numberMinerThread));
        askBlockThread = new Thread(new AskBlocksThread());

        updateRegistry.start();
        transactionsThread.start();
        blocksThread.start();
        minerThread.start();
        askBlockThread.start();

    }

    /**
     * IP address of the current miner
     * @return the InetAdress
     */
    public static InetAddress getMyAddress() {
        Enumeration e = null;
        //List of all the network interfaces on computer
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
                //value is valid if it is not a loopback address and if it is an instance of Inet4address
                if (!i.isLoopbackAddress() && i instanceof Inet4Address) {
                    return i;
                }
            }
        }
        return null;

    }

    /**
     * Thread that handle the update of the registry and the connection the other miner
     * @author Marco Sansoni
     * @version 1.0
     */
    private class UpdateRegistry implements Runnable {

        private long delayTime;
        private int numberConnection;

        /**
         * It defines the parameters necessary to update the connection to the registry
         * @param time between diffenrent connections
         * @param numberConnection it is the max number of registry to connect
         */
        public UpdateRegistry(long time, int numberConnection) {
            this.delayTime = time;
            this.numberConnection = numberConnection;
        }

        public void run() {
            while (true) {
                // it contains all the InetSocketAddress obtained from all the registry
                List<InetSocketAddress> updatedMinerList = new LinkedList<InetSocketAddress>();
                InetSocketAddress myAddress = null;
                myAddress = new InetSocketAddress(getMyAddress(), myPort);
                synchronized (registryList) {
                    Iterator<RegistryInterface> regIter = registryList.iterator();
                    //I store all the miner gained from the registry
                    while (regIter.hasNext()) {
                        RegistryInterface actual = regIter.next();
                        List<InetSocketAddress> addressFromThis = null; 
                        try {
                            actual.register(myAddress);
                            addressFromThis = actual.getIPSet();
                        } catch (RemoteException re) {
                           
                        }
                        // if a remoteexcpetion has been thrown address from list will be null
                        if (addressFromThis != null) {
                            updatedMinerList.addAll(addressFromThis);
                        }
                    }
                }
                // read the list from the file, adding to updateMinerList
                String backup = "blockchain/backup/miner/M" + myPort + "_miner.txt";
                BufferedReader reader = null;
                String line = null;
                //I store on a file all the miner, to be restore later
                try {
                    reader = new BufferedReader(new FileReader(backup));
                    while ((line = reader.readLine()) != null) {
                        String[] min = line.split(":");
                        InetSocketAddress isa = new InetSocketAddress(min[0], Integer.parseInt(min[1]));
                        if (!updatedMinerList.contains(isa)) {
                            updatedMinerList.add(isa);
                        }
                    }
                    reader.close();
                } catch (FileNotFoundException fnfe) {
                   
                } catch (IOException ioe) {
                   
                }
                // save the list on file               
                File f = new File(backup);               
                PrintWriter printWriter = null;
                Iterator<InetSocketAddress> iter = updatedMinerList.iterator();
                try {
                    printWriter = new PrintWriter(f);
                    while (iter.hasNext()) {
                        InetSocketAddress current = iter.next();
                        printWriter.println("" + current.getAddress().getHostAddress() + ":" + current.getPort());                        
                    }
                    printWriter.flush();
                    printWriter.close();
                } catch (FileNotFoundException fnf) {
                   
                }

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
                while (chosedMiner.size() < numberMiner && updatedMinerList.size() > 0) {
                    int find = r.nextInt(updatedMinerList.size());
                    InetSocketAddress chose = updatedMinerList.get(find);
                    updatedMinerList.remove(find);
                    if (chose.getAddress().getHostAddress().equals(Miner.getMyAddress().getHostAddress())
                            && chose.getPort() == myPort) {
                        
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
                    // I compute at avery itaration the possible number of miner
                }
                System.out.println("Total connected miner: " + chosedMiner.size());

                // list with minerInterface is completed
                if (chosedMiner.size() > 0) {
                    synchronized (minersIPList) {
                        minersIPList = chosedMiner;
                    }
                }

                //After that I can receive the missing blocks from other miner
                //Other thread are waiting the release of the semaphore
                startGettingBlocks.release();

                //wait the delay time
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
                            // System.exit(1);
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
                                // re.printStackTrace();
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
                System.out.println("Pending transactions: " + tempList.size());

                while (!tempList.isEmpty()) {
                    Block b = tempList.remove(0);

                    // System.out.println("Block hash: " + Block.hashToString(b.getHash()));
                    // System.out.println("Previous Block hash: " +
                    // Block.hashToString(b.getPreviousHash()));
                    boolean valid = false;
                    // blockchain
                    synchronized (blockchain) {
                        // System.out.println("Entro");
                        // System.out.println("Contenuta? " + blockchain.contains(b));
                        if (b.verifyBlock(DIFFICULTY) && !blockchain.contains(b)) {
                            valid = true;
                            for (Transaction t : b.getListTransactions()) {
                                if (blockchain.contains(t)) {
                                    continue;
                                }
                            }

                            if (blockchain.addBlock(b) == false) {
                                askBlocksSemaphore.release();
                                try {
                                    blocksRetrievedSemaphore.acquire();
                                } catch (InterruptedException ie) {
                                    ie.printStackTrace();
                                    // System.exit(1);
                                }
                            }

                            // remove block's transaction from pending transactions
                            // only if last added block is actually the last block
                            if(blockchain.lastBlock().equals(b)) {
                                synchronized (pendingTransactions) {
                                    for (Transaction t : b.getListTransactions()) {
                                        // if not present does nothing
                                        pendingTransactions.remove(t);
                                    }
                                }
                            }

                            // System.out.println("Blockchain hash: " +
                            // Block.hashToString(blockchain.getHash()));
                            System.out.println("Blockchain length: " + blockchain.length() + " with last block hash: "
                                    + Block.hashToString(blockchain.lastBlock().getHash()));
                            // System.out.println(
                            // "Blockchain last block: " + );

                            blockchain.print("blockchain/print/M" + myPort + ".txt");
                            String fileName = new SimpleDateFormat("yyyyMMddHHmmss")
                                    .format(new Date(System.currentTimeMillis()));
                            blockchain.backup(
                                    "blockchain/backup/blockchain/M" + myPort + "_blockchain" + fileName + ".txt");
                        }
                    }
                    // System.out.println("esco");
                    // blockDoNotSend sono blocchi inseriti in blockToSend che arrivano da altri
                    // miner quando chiedo blocchi
                    // non devi riinviarli
                    if (valid && blocksDoNotSend == 0) {
                        // send to every miner
                        for (MinerInterface mi : listMiners) {
                            try {
                                mi.sendBlock(b);
                            } catch (RemoteException re) {
                                // re.printStackTrace();
                            }
                        }
                    }

                    if (blocksDoNotSend > 0) {
                        blocksDoNotSend--;
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
     * Thread that mine block
     * @author Marco Sansoni
     * @version 1.0
     */
    private class MinerThread implements Runnable {

        private int difficulty;
        private int numThread;

        /**
         * It defines parameters useful for mining
         * @param difficulty the number of zero at the beginning of the hash
         * @param numThread number of thread allocated for mining
         */
        public MinerThread(int difficulty, int numThread) {
            this.difficulty = difficulty;
            this.numThread = numThread;
        }

        /**
         * Run the miner if exists same block in block to be added to the blockchain
         */
        public void run() {
            List<Transaction> actual = new LinkedList<Transaction>();
            synchronized (pendingTransactions) {
                //If no transaction thread will sleep
                while (pendingTransactions.isEmpty()) {
                    try {
                        System.out.println("Miner sleeping");
                        pendingTransactions.wait();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
                // there are at least one transaction
                Iterator<Transaction> iter = pendingTransactions.iterator();
                while (iter.hasNext() && actual.size() < TRANSACTIONS_PER_BLOCK) { // no more than 4 transactions for
                                                                                   // block
                    Transaction t = iter.next();
                    if (t.verify() && !blockchain.contains(t)) {
                        //Add the transaction to transaction to be added in a block
                        actual.add(t);
                    } else {                    
                        iter.remove();
                    }
                }
            }
            // I will mine this block  
            Block b = new Block();

            // Iterator through the list of transaction
            Iterator<Transaction> iter = actual.iterator();
            while (iter.hasNext()) { // no more than 4 transactions for block
                b.addTransaction(iter.next());
            }

            // I need to set the previous hash on the block
            b.setPreviousHash(blockchain.lastBlock().getHash());

            System.out.println("Start Mining with "+numberMinerThread+" thread");
            // The block is ready, i can start mining
            b.mineBlock(difficulty, numThread);
            //Finish mining
            System.out.println("##### WINNER ##### \n Block Hash: " + Block.hashToString(b.getHash()));
            
            // clean used transactions
            synchronized (pendingTransactions) {
                for (int i = 0; i < b.getListTransactions().size(); i++) {
                    pendingTransactions.remove(0);
                }
            }

            // when I mine the block I can add it to the block to send list
            synchronized (blockToSend) {
                blockToSend.add(b);
                blockToSend.notifyAll();
            }
        }

    }

    /**
     * 
     */
    private class AskBlocksThread implements Runnable {

        public void run() {

            try {
                // wait all other threads ready
                startGettingBlocks.acquire();
            } catch(InterruptedException ie) {

            }

            while (true) {

                try {
                    askBlocksSemaphore.acquire();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                    System.exit(1);
                }
                /*
                 * // Mappa profondità frequenza Map<Integer, Integer> poll = new HashMap<>();
                 * 
                 * 
                 * //complessità folle, da sistemare in futuro //non ci sincronizziamo su
                 * minerList, non è un problema perdere un miner perchè non più attivo int
                 * maxDepth = -1; int currentDepth = blockchain.length()-1; while (maxDepth ==
                 * -1) { for (MinerInterface miner : minersIPList) { try { int depth =
                 * miner.depthOfTheBlock(blockchain.hashGivenDepth(currentDepth)); if
                 * (poll.containsKey(depth)) { poll.replace(depth, poll.get(depth) + 1); } else
                 * { poll.put(depth, 1); } } catch (RemoteException re) { re.printStackTrace();
                 * System.exit(1); } } maxDepth = -1; int maxFreq = 0; for (Integer i :
                 * poll.keySet()) { if (poll.get(i) > maxFreq) { maxDepth = i; } }
                 * currentDepth--; }
                 */

                // chiedi tutti blocchi a partire dall'ultimo che ho e inseriscili in testa a
                // blocktosend
                // ocio deadlock!!!!!!
                // synchronized(minersIPList) {
                synchronized (blockToSend) {

                    for (MinerInterface miner : minersIPList) {
                        LinkedList<Block> blockList = new LinkedList<>();
                        try {
                            // System.out.println("Chiedo lista blocchi a miner");
                            blockList = miner.getMissingBlocks(blockchain.lastBlock().getHash());

                        } catch (RemoteException re) {
                            re.printStackTrace();
                            //System.exit(1);
                        }

                        while (!blockList.isEmpty()) {
                            ((LinkedList<Block>) blockToSend).addFirst(blockList.removeLast());
                            blocksDoNotSend++;
                        }
                    }

                }
                // }

                blocksRetrievedSemaphore.release();

            }

        }

    }

    /*
     * private class Triplet<T, U, V> {
     * 
     * public T first; public U second; public V third;
     * 
     * public Triplet(T first, U second, V third) { this.first = first; this.second
     * = second; this.third = third; }
     * 
     * }
     */

}