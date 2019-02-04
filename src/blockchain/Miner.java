package blockchain;

import java.net.*;
import java.rmi.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.io.*;
import registry.*;
import java.rmi.server.*;
import java.text.SimpleDateFormat;

/**
 * Class that defines a remote object Miner. It uses multiple threads.
 * 
 * @author Giuseppe Ravagnani
 * @author Marco Sansoni
 * @version 1.0
 */
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

    private Thread updateRegistry;
    private Thread transactionsThread;
    private Thread blocksThread;
    private Thread minerThread;
    private Thread askBlockThread;

    private int myPort;

    private int numberMinerThread;

    private Semaphore askBlocksSemaphore;
    private Semaphore blocksRetrievedSemaphore;
    private Semaphore startGettingBlocks;

    private int blocksDoNotSend = 0;

    /**
     * Constructor of class Miner
     * @throws RemoteException
     */
    public Miner() throws RemoteException {
        super();
        transactionToSend = new LinkedList<Transaction>();
        pendingTransactions = new LinkedList<Transaction>();
        blockToSend = new LinkedList<>();
        registryList = new LinkedList<>();
        minersIPList = new LinkedList<>();
        blockchain = new Blockchain();
        myPort = DEFAULT_PORT;

        // on first run, ask blocks (don't wait on the semaphore)
        askBlocksSemaphore = new Semaphore(1);
        blocksRetrievedSemaphore = new Semaphore(0);
        startGettingBlocks = new Semaphore(0);

        numberMinerThread = DEFAULT_MINER_THREAD;
    }


    public Blockchain getBlockchain() throws RemoteException {
        return blockchain;
    }

    public byte[] getBlockchainHash() throws RemoteException {
        return blockchain.getHash();
    }

    public void setBlockchain(Blockchain blockchain) {
        synchronized (blockchain) {
            this.blockchain = blockchain;
        }
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

    public LinkedList<Block> getMissingBlocks(byte[] hash) throws RemoteException {
        return blockchain.getMissingBlocks(hash);
    }

    public void addRegistry(RegistryInterface reg) {
        synchronized (registryList) {
            registryList.add(reg);
        }
    }

    public void clearRegistry() {
    /*
     * public LinkedList<Block> getBlocksGivenLength(int depth) throws
     * RemoteException { return blockchain.getFromDepth(depth); }
     * 
     * public int depthOfTheBlock(byte[] hash) throws RemoteException { return
     * blockchain.depthOfTheBlock(hash); }
     */
        synchronized (registryList) {
            registryList.clear();
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
        askBlockThread = new Thread(new AskBlocksThread());

        updateRegistry.start();
        transactionsThread.start();
        blocksThread.start();
        minerThread.start();
        askBlockThread.start();

    }

    /*
     * private void chooseBlockchain() { synchronized (blockchain) {
     * ArrayList<Triplet<byte[], MinerInterface, Integer>> hashMiner = new
     * ArrayList<>(); synchronized (minersIPList) { Iterator<MinerInterface>
     * iterMiner = minersIPList.iterator(); while (iterMiner.hasNext()) { try { //
     * listHash.add(iterMiner.next().getBlockchain().getHash()); MinerInterface
     * currentMiner = iterMiner.next(); byte[] currentHash =
     * currentMiner.getBlockchain().getHash(); boolean found = false; for (int i =
     * 0; i < hashMiner.size(); i++) { if (Arrays.equals(currentHash,
     * hashMiner.get(i).first)) { hashMiner.get(i).third ++; found = true; } } if
     * (!found) { hashMiner.add(new Triplet<byte[], MinerInterface,
     * Integer>(currentHash, currentMiner, 1)); } } catch (RemoteException re) {
     * re.printStackTrace(); } } } System.out.println("Ho preso " + hashMiner.size()
     * + " differenti blockchain"); if (hashMiner.isEmpty()) { return; } //
     * Iterator<byte[]> occurance = setByte.iterator(); byte[] longer =
     * hashMiner.get(0).first; int max = hashMiner.get(0).third; MinerInterface
     * bestMiner = hashMiner.get(0).second; for (int i = 1; i < hashMiner.size();
     * i++) { if (hashMiner.get(i).third > max) { longer = hashMiner.get(i).first;
     * max = hashMiner.get(i).third; bestMiner = hashMiner.get(i).second; } }
     * System.out.println( "Hash vincitore is " + Block.hashToString(longer) +
     * " che si trova "+ max + "  volte"); System.out.println("Inizio il download");
     * try{ blockchain = bestMiner.getBlockchain(); }catch(RemoteException re){
     * System.err.println("Run again"); System.exit(1); }
     * System.out.println("Fine download");
     * 
     * blockchain.print("blockchain/print/M" + myPort + "_dopochooseblock.txt");
     * 
     * // I need to find which blockchain is the most frequent
     * 
     * /* while(iterByte.hasNext()){
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
    // }

    // }

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
                            // re.printStackTrace();
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
                    // fnfe.printStackTrace();
                    // First iteration the file has not already been created
                    // System.exit(1);
                } catch (IOException ioe) {
                    // ioe.printStackTrace();
                    //System.err.println("Invalid address while reading from backup");
                    // System.exit(1);
                }

                // save the list on file
                // String str = "World";
                File f = new File(backup);
                // FileWriter fileWriter = new FileWriter(f);
                PrintWriter printWriter = null;
                // BufferedWriter writer = new BufferedWriter(new FileWriter(f));
                Iterator<InetSocketAddress> iter = updatedMinerList.iterator();
                try {
                    printWriter = new PrintWriter(f);
                    while (iter.hasNext()) {
                        InetSocketAddress current = iter.next();
                        printWriter.println("" + current.getAddress().getHostAddress() + ":" + current.getPort());
                        // printWriter.wrte
                        // printWriter.
                    }
                    // System.out.println("Successfully writed on a file");
                    printWriter.flush();
                    printWriter.close();
                } catch (FileNotFoundException fnf) {
                    //System.err.println("Invalid Address of local backup");
                    // fnf.printStackTrace();
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

                    }
                    // I compute at avery itaration the possible number of miner

                    // numberMiner = Math.min(updatedMinerList.size() , numberConnection -
                    // chosedMiner.size());
                    // System.out.println("Number Miner da raggiungere "+numberMiner);

                }

                System.out.println("Total connected miner: " + chosedMiner.size());

                // list with minerInterface is completed
                if (chosedMiner.size() > 0) {
                    synchronized (minersIPList) {
                        minersIPList = chosedMiner;
                    }
                }

                startGettingBlocks.release();

                /*
                 * if (firstConnection) { chooseBlockchain(); firstConnection = false; }
                 */
                try {
                    Thread.sleep(delayTime);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                

            }

        }

    }

    /**
     * Thread that manages Transactions
     * @author Giuseppe Ravagnani
     * @version 1.0
     */
    private class TransactionsThread implements Runnable {

        @SuppressWarnings("deprecation")
        public void run() {
            // I have a list of transactions to send to other miners
            // every time I receive a transaction from a client or another miner I put the transaction
            // into the queue (only if valid and not yet in blockchain). While the transaction is not empty I take some transations and I put them in a block
            // then start the mining thread that mnes the block. (these done in other threads)

            while (true) {

                // use a temp list in order to don't keep the lock too much time
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
                // use a temp list in order to keep the lock only a few time
                LinkedList<MinerInterface> listMiners = null;
                synchronized (minersIPList) {
                    // just clone
                    listMiners = new LinkedList<MinerInterface>();
                    for (MinerInterface mi : minersIPList) {
                        listMiners.add(mi);
                    }
                }

                // do operations on temp list of transactions
                // send transactions to other miners
                while (!tempList.isEmpty()) {

                    Transaction t = tempList.remove(0);

                    System.out.println("Transaction received: " + Block.hashToString(t.getHash()));

                    if (t.verify() && !pendingTransactions.contains(t) && !blockchain.contains(t)) {
                        // send to every miner
                        for (MinerInterface mi : listMiners) {
                            try {
                                mi.sendTransaction(t);
                            } catch (RemoteException re) {
                                // re.printStackTrace();
                            }
                        }

                        // put the transaction in the list of transactions where the block thread takes the transactions
                        // to put on the block
                        synchronized (pendingTransactions) {
                            if (!pendingTransactions.contains(t)) {
                                pendingTransactions.add(t);
                            }
                        }

                        // restart miner thread if other transactions can be put on the block
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
     * Thread that manages Blocks
     * @author Giuseppe Ravagnani
     * @version 1.0
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

            // for each receved block (from other miners) send it. flooding
            // if a new block arrives, stop mining brutally

            while (true) {

                // temp list in order to take the lock only a few time
                List<Block> tempList = new LinkedList<>();
                synchronized (blockToSend) {
                    while (blockToSend.isEmpty()) {
                        try {
                            blockToSend.wait();
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                            System.exit(1);
                        }
                    }

                    // block arrived, stop mining!
                    minerThread.stop();

                    // copy list
                    while (!blockToSend.isEmpty()) {
                        Block b = blockToSend.remove(0);
                        tempList.add(b);
                        System.out.println("Block received: " + Block.hashToString(b.getHash()));
                    }
                }

                // copy list of miners in order to take the loc only few time
                LinkedList<MinerInterface> listMiners = null;
                synchronized (minersIPList) {
                    // just clone
                    listMiners = new LinkedList<MinerInterface>();
                    for (MinerInterface mi : minersIPList) {
                        listMiners.add(mi);
                    }
                }

                System.out.println("Pending transactions: " + tempList.size());

                // do operations on the temp list
                // for each received block: verify, send to other miners and add to the blockchain
                while (!tempList.isEmpty()) {
                    Block b = tempList.remove(0);
                    boolean valid = false;
                    // blockchain
                    synchronized (blockchain) {
                        // verify block and check if blockchain already contains it or some of its transactions
                        if (b.verifyBlock(DIFFICULTY) && !blockchain.contains(b)) {
                            valid = true;
                            for (Transaction t : b.getListTransactions()) {
                                if (blockchain.contains(t)) {
                                    continue;
                                }
                            }
                            // if cannot add the block to the blockchain for some reasons
                            // wake up the thread that asks blocks
                            if (blockchain.addBlock(b) == false) {
                                askBlocksSemaphore.release();
                                try {
                                    blocksRetrievedSemaphore.acquire();
                                } catch (InterruptedException ie) {
                                    ie.printStackTrace();
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

                            System.out.println("Blockchain length: " + blockchain.length() + " with last block hash: "
                                    + Block.hashToString(blockchain.lastBlock().getHash()));

                            blockchain.print("blockchain/print/M" + myPort + ".txt");
                            String fileName = new SimpleDateFormat("yyyyMMddHHmmss")
                                    .format(new Date(System.currentTimeMillis()));
                            blockchain.backup(
                                    "blockchain/backup/blockchain/M" + myPort + "_blockchain" + fileName + ".txt");
                        }
                    }
                    
                    // blockdonotsend is the number of blocks asket to other miners, so they must not be sent again
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

                try {
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

            //System.out.println("Start Miner Thread ");

            // while(true) {

            List<Transaction> actual = new LinkedList<Transaction>();
            synchronized (pendingTransactions) {
                while (pendingTransactions.isEmpty()) {
                    try {
                        //System.out.println("dorme num Transact: " + pendingTransactions.size());
                        System.out.println("Miner sleeping");
                        pendingTransactions.wait();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
                //System.out.println("sveglio num Transact: " + pendingTransactions.size());

                // there are at least one transaction
                Iterator<Transaction> iter = pendingTransactions.iterator();
                while (iter.hasNext() && actual.size() < TRANSACTIONS_PER_BLOCK) { // no more than 4 transactions for
                                                                                   // block
                    Transaction t = iter.next();
                    if (t.verify() && !blockchain.contains(t)) {
                        //System.out.println("Aggiunta transazione a lista");
                        actual.add(t);
                    } else {
                        //System.out.println("Transazione eliminata");
                        iter.remove();
                    }
                }
            }
            // I will mine this block
            
            Block b = new Block();

            // Iterator through the list of transaction
            Iterator<Transaction> iter = actual.iterator();
            while (iter.hasNext()) { // no more than 4 transactions for block
                //System.out.println("Aggiunta transazione a blocco");
                b.addTransaction(iter.next());
            }

            //System.out.println("lista blocco size: " + b.getListTransactions().size());

            // I need to set the previous hash on the block
            b.setPreviousHash(blockchain.lastBlock().getHash());

            System.out.println("Start Mining with "+numberMinerThread+" thread");
            // The block is ready, i can start mining
            b.mineBlock(difficulty, numThread);

            System.out.println("##### WINNER ##### \n Block Hash: " + Block.hashToString(b.getHash()));
            

            // clean used transactions
            synchronized (pendingTransactions) {
                for (int i = 0; i < b.getListTransactions().size(); i++) {
                    //System.out.println("removed transaction");
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

    /**
     * Thread that asks blocks to other miners. waits until other threads start.
     * @author Giuseppe Ravagnani
     * @version 1.0
     */
    private class AskBlocksThread implements Runnable {

        public void run() {

            try {
                // wait all other threads ready. only on start up
                startGettingBlocks.acquire();
            } catch(InterruptedException ie) {

            }

            // at each iteration stop until there is need to ask new blocks.
            // generally if the miner receives a block that cannot attached then ask blocks
            while (true) {

                try {
                    // wait until there is need to ask blocks
                    askBlocksSemaphore.acquire();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                    System.exit(1);
                }

                // get blocks and add them to blockToSend on top of the queue. Do not send again the blocks received
                synchronized (blockToSend) {

                    for (MinerInterface miner : minersIPList) {
                        LinkedList<Block> blockList = new LinkedList<>();
                        try {
                            blockList = miner.getMissingBlocks(blockchain.lastBlock().getHash());
                        } catch (RemoteException re) {
                            re.printStackTrace();
                        }

                        while (!blockList.isEmpty()) {
                            ((LinkedList<Block>) blockToSend).addFirst(blockList.removeLast());
                            blocksDoNotSend++;
                        }
                    }

                }

                synchronized(blockToSend) {
                    blockToSend.notify();
                }
                blocksRetrievedSemaphore.release();

            }

        }

    }

}