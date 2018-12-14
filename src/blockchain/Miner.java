package blockchain;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.registry.Registry;
import java.util.*;
import registry.*;
import java.rmi.server.*;

public class Miner implements MinerInterface {

    private Blockchain blockchain;

    private List<RegistryInterface> registryList;
    private List<MinerInterface> minersIPList;

    private List<Transaction> transactionToSend;
    private List<Transaction> pendingTransactions;

    private List<Block> blockToSend;
    //private List<Block> pendingBlock;

    private Thread updateRegistry;
    private Thread transactionsThread;
    private Thread blocksThread;
    private Thread minerThread;

    public Miner() throws RemoteException{
        super();
        transactionToSend = new LinkedList<Transaction>();
        pendingTransactions = new LinkedList<Transaction>();
        blockToSend = new LinkedList<>();
        //pendingBlock = new LinkedList<>();
        registryList = new LinkedList<RegistryInterface>();
        minersIPList = null;
        //chooseBlockchain(list)
    }

    public Blockchain getBlockchain() throws RemoteException {
        return blockchain;
    }

    public void sendTransaction(Transaction transaction) throws RemoteException {
        synchronized(transactionToSend){
            transactionToSend.add(transaction);
            transactionToSend.notifyAll();
            //delega tutti i controlli e le verifiche al thread che aggiunge le transazioni e le manda a tutti
        }
    }

    public void sendBlock(Block block) throws RemoteException {
        //stesso modello delle transactions applicate ai blocchi
        synchronized(blockToSend){
            blockToSend.add(block);
            blockToSend.notifyAll();
            //delega tutti i controlli e le verifiche al thread che aggiunge i blocchi alla blockchain e le manda a tutti
        }
    }

    public void addRegistry(RegistryInterface reg) {
        synchronized(registryList){
            registryList.add(reg);
        }
    }

    public void startThreads() {

        updateRegistry = new Thread(new UpdateRegistry(0,0));
        updateRegistry.start();

        transactionsThread = new Thread(new TransactionsThread());
        transactionsThread.start();

        blocksThread = new Thread(new BlocksThread());
        blocksThread.start();

        minerThread = new Thread(new MinerThread(3,3));
        minerThread.start();
        
    }

    private void chooseBlockchain() {
        List<byte[]> listHash = new LinkedList<byte[]>();
        synchronized(minersIPList){
            Iterator<MinerInterface> iterMiner = minersIPList.iterator();           
            while(iterMiner.hasNext()){ 
                try{   
                    listHash.add(iterMiner.next().getBlockchain().getHash());
                }catch(RemoteException re){
                    re.printStackTrace();
                }    
            }
        }

        //I need to find which blockchain is the most frequent
        Map<byte[],Integer> map = new HashMap<byte[],Integer>();
        Iterator<byte[]> iterByte = listHash.iterator();
        while(iterByte.hasNext()){
            byte[] current = iterByte.next();
            if(!map.containsKey(current)){
                map.put(current, 1);
            }else{
                int previousOccurance = map.get(current);
                previousOccurance++;
                map.put(current, previousOccurance);
            }
        }

        Set<byte[]> setByte = map.keySet();
        Iterator<byte[]> occurance = setByte.iterator();
        byte[] longer = occurance.next();
        int occ = map.get(longer);
        while(occurance.hasNext()){
           byte[] current = occurance.next();
           if(map.get(current)>occ){
               longer=current;
               occ = map.get(current);
           } 
        }

        
        //i find the hash of the blockchain 
        synchronized(minersIPList){
            Iterator<MinerInterface> minerIter = minersIPList.iterator();
            while(minerIter.hasNext()){
                MinerInterface currentMiner = minerIter.next();
                try{
                    if(Arrays.equals(currentMiner.getBlockchain().getHash(), longer)){
                        synchronized(blockchain){
                            blockchain = currentMiner.getBlockchain();
                            break;
                        }    
                    }   
                }catch(RemoteException re){
                    re.printStackTrace();
                }               
            }
        }

    }

    // thread che aggiorna registro
    private class UpdateRegistry implements Runnable {

        private long delayTime;
        private int numberConnection;

        //size sono il numero di miner cui voglio connettermi
        //se voglio size ip sono interessato al fatto che non ne voglio di uguali
        public UpdateRegistry(long time, int numberConnection) {
            this.delayTime = time;
            this.numberConnection = numberConnection;
        }

        public void run() {
            while(true){
                //it contains all the InetSocketAddress obtained from all the registry
                List<InetSocketAddress> updatedMinerList = new LinkedList<InetSocketAddress>(); 

                //InetSocketAddress of the miner
                int myPort = 7687;
                InetSocketAddress myAddress = null;
                try{
                    myAddress = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), myPort);            
                }catch(UnknownHostException uhe){
                    uhe.printStackTrace();
                }
                

                synchronized(registryList){  
                    Iterator<RegistryInterface> regIter = registryList.iterator();
                    while (regIter.hasNext()) {
                        RegistryInterface actual = regIter.next();
                        List<InetSocketAddress> addressFromThis = null; // inetsocketaddress from the actual registry
                        try {
                            actual.register(myAddress);
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
                   
                //now i want to connect to a fixed number to miner
                //if i don't know so many address i will connect to all
                int numberMiner = Math.min(updatedMinerList.size(), numberConnection);

                //random generator
                Random r = new Random();
                
                //the final list of all the working connection
                List<MinerInterface> chosedMiner = new LinkedList<MinerInterface>();

                List<InetSocketAddress> addressChosedMiner = new LinkedList<InetSocketAddress>();

                //I continue until i reach the target number of connection
                while(chosedMiner.size()<numberMiner){
                    int find = r.nextInt(chosedMiner.size());
                    InetSocketAddress chose = updatedMinerList.get(find);
                    boolean valid = true;

                    //avoid to keep the same twice
                    Iterator<InetSocketAddress> iterAdd = addressChosedMiner.iterator();
                    while(iterAdd.hasNext()){
                        InetSocketAddress actual = iterAdd.next();
                        if(actual.getAddress().getHostAddress().equals(chose.getAddress().getHostAddress())
                            && actual.getPort() == chose.getPort()){
                                valid = false;
                                break;
                        }
                    }
                    if(valid){  
                        //I create the reference to the remote object, if it is possible
                        MinerInterface m = null;
                        String ip = chose.getAddress().getHostAddress();
                        int port =chose.getPort();
                        try{
                            m = (MinerInterface) Naming.lookup("//" + ip+":"+port + "/miner"); 
                        }catch(RemoteException re){
                            re.printStackTrace();
                            m=null;
                        }catch(NotBoundException nbe){
                            nbe.printStackTrace();
                            m=null;
                        }catch(MalformedURLException mue){
                            mue.printStackTrace();
                            m=null;
                        }
                        //if i am able to connect, i add to the list
                        //only condition to add the miner to the list
                        if(m!=null){
                            chosedMiner.add(m);
                            addressChosedMiner.add(chose);
                        }
                    }    
                    
                }

                //list with minerInterface is completed
                synchronized(minersIPList){
                    minersIPList = chosedMiner;
                }

                try{
                    Thread.sleep(delayTime);
                }catch(InterruptedException ie){
                    ie.printStackTrace();
                }
                
            }
            
        }

    }

    // thread che pensa alle transazioni
    private class TransactionsThread implements Runnable {

        public void run() {
            //while(lista non è vuota)
            //sincronized
            //wait
            //ottengo l'elemento dalla lista delle trnsazioni da inviare
            //lo cancello dalla lista
            //esco dal sincronized
            //con la transazione la verifico
            //valida se non compare mai nella blockchain, e se il verify delle chiavi private e pubbliche ritorna true
            //non deve già essere presente nella pending list
            //se va tutto bene lo mando a tutti
            //alla fine lo metto nella mia lista

            //faccio il notify all per svegliare il miner che dormiva se non c'erano transazioni
            //se avevo già 4 transazioni pendenti nella pendingTransactions, non rompo il cazzo al miner
            //se ne avevo di meno devo ristartarlo
            //ricomincio da capo               
        }

    }

    // thread che pensa ai blocchi
    private class BlocksThread implements Runnable {

        public void run() {
            //killa il miner, da vedere se possibile con setDeamond
            //da vedere semmai se killare il miner solo quando un blocco lo si trova valido

            //while(empty)
            //sincronized
            //wait
            //prendo in maniera sincronizzata un solo blocco dalla blocktosend
            //lo rimuovo dalla lista
            //effettuo la verifica del blocco
            //verifica è hash valido, transazioni non esistano da nessuna altra parte e transazioni valide

            //se tutto valido mando il blocco a tutti gli altri
            //aggiungo in maniera sincronizzata su blockchain il blocco alla mia blockchain
            //ricomincio da capo
            //se non c'è niente prima di addormentarmi nell'attesa di qualche nuovo blocco sveglio il miner
            //mi addormento
        }

    }

    // thread che mina e basta
    private class MinerThread implements Runnable {

        private int difficulty;
        private int numThread;

        public MinerThread(int difficulty, int numThread){
            this.difficulty = difficulty;
            this.numThread = numThread;
        }

        public void run() {
            List<Transaction> actual = new LinkedList<Transaction>();
            synchronized(pendingTransactions){
                while(pendingTransactions.isEmpty()){
                    try{
                        wait();
                    }catch(InterruptedException ie){
                        ie.printStackTrace();
                    }    
                }
                //there are at least one transaction
                Iterator<Transaction> iter = pendingTransactions.iterator();
                while(iter.hasNext() && actual.size()<4){ //no more than 4 transactions for block                 
                    actual.add(iter.next());
                }
            }
            //I will mine this block
            Block b = new Block();
            
            //Iterator through the list of transaction
            Iterator<Transaction> iter = actual.iterator();
            while (iter.hasNext()) { // no more than 4 transactions for block
                b.addTransaction(iter.next());
            }

            //I need to set the previous hash on the block
            b.setPreviousHash(blockchain.lastBlock().getHash());

            //The block is ready, i can start mining
            b.mineBlock(difficulty, numThread);

            //when I mine the block I can add it to the block to send list
            synchronized(blockToSend){
                blockToSend.add(b);
            }
        }

    }


}