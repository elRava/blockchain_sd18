package blockchain;

import java.sql.Timestamp;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a block in a blockchain
 * @author Giuseppe Ravagnani
 * @version 1.0
 */
public class Block implements Serializable {

    // list of fixed length. Fill with at most LIST_LENGTH transactions
    public static final int LIST_LENGTH = 4;
    // all information stored on a block
    private List<Transaction> listTransactions;
    private byte[] previousHash;
    private byte[] hash;
    private long nonce;
    private byte[] merkleRoot;
    private Timestamp minedTime;
    // information used only for mining
    private AtomicLong tempNonce;
    private boolean isMining;
    private AtomicBoolean isMined;

    /**
     * Creates an empty Block. 
     * It will be filled in a second moment.
     */
    public Block() {
        listTransactions = new ArrayList<>(LIST_LENGTH);
        previousHash = null;
        hash = null;
        //nonce = null;
        tempNonce = new AtomicLong((new Random()).nextLong());
        isMined = new AtomicBoolean(false);
        merkleRoot = null;
        minedTime = null;
        isMining = false;
    }

    /**
     * Create the genesis block
     * @return the genesis block
     */
    public Block genesisBlock() {
        Block b = new Block();
        b.hash = hexStringToByteArray("000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f");   
        b.isMined = new AtomicBoolean();
        b.isMined.set(true);
        b.listTransactions = new ArrayList<>();
        b.previousHash = null;
        return b;
    }

    /**
     * Set the hash of the previous block in the blockchain on block information
     * @param previousHash the hash of the previous block
     */
    public void setPreviousHash(byte[] previousHash) {
        this.previousHash = previousHash;
    }

    /**
     * Get the hash of the previous block in the blockchain
     * @return the hash of the previous block in the blockchain
     */
    public byte[] getPreviousHash() {
        return previousHash;
    }

    /**
     * Get the hash of the block
     * @return the hash of the block
     */
    public byte[] getHash() {
        return hash;
    }

    /**
     * Get the merkle root calculated before mining
     * @return the merkle root
     */
    public byte[] getMerkleRoot() {
        return merkleRoot;
    }

    /**
     * Get the nonce found mining the block
     * @return the nonce
     */
    public long getNonce() {
        return nonce;
    }

    /**
     * Get the list of the transactions saved on the block
     * @return the list of the transactions
     */
    public List<Transaction> getListTransactions() {
        return listTransactions;
    }

    /**
     * Get the timestamp on when the block is successufully mined
     * @return the timestamp on when the block is mined
     */
    public Timestamp getMinedTime() {
        return minedTime;
    }
    
    /**
     * Override Object equals method
     * Two blocks are considered equal if they have the same hash
     * @param o the object that we want to compare
     * @return if the object is equal to this
     */
    @Override
    public boolean equals(Object o) {
        if(! (o instanceof Block)) {
            return false;
        }
        if(hashToString(this.hash).equals(hashToString(((Block) o).getHash()))) {
            return true;
        }
        return false;
    }

    /**
     * TODO: check if it is better to make this method static
     * Calculate the merkle root. It is calculated from the list of the hashes of the transactions
     * take two by two and calculate the hash of the concatenation of the hashes
     * then repeat the same on the list of the hashes, and so on
     */
    public static byte[] calculateMerkleRoot(List<Transaction> listTransactions) {
        // merkle of one single transaction is the hash itself
        // use tho lists: one of the hashes that have to be grouped
        // the other of the hashes that have been calculated
        // then let the second be the first and iterate until the list has only one element
        if(listTransactions.isEmpty()) {
            throw new NoSuchElementException();
        }
        final int SHA256LENGTH = 32;
        byte[][] pendingHash = new byte[listTransactions.size()][SHA256LENGTH];
        for(int i = 0; i < pendingHash.length; i++) {
            //Transaction temp = (Transaction)listTransactions.get(i);
            pendingHash[i] = listTransactions.get(i).getTransactionHash();
            //String s = temp.getTransactionHash();
            //System.out.println("PD "+s);
            //pendingHash[i] = temp.tranByte();
        }
        while(pendingHash.length > 1) {
            byte[][] calculated = new byte[(pendingHash.length + 1) / 2][SHA256LENGTH];
            
            for(int i = 0; i < calculated.length; i++) {

                if(2*i + 1 < pendingHash.length) {
                    byte[] s = new byte[2 * SHA256LENGTH];
                    for(int k = 0; k < SHA256LENGTH; k++) {
                        s[k] = pendingHash[2*i][k];
                        s[k + SHA256LENGTH] = pendingHash[2*i+1][k];
                    }
                    try {
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        calculated[i] = digest.digest(s);
                    }catch(NoSuchAlgorithmException nsae) {
                        nsae.printStackTrace();
                        System.exit(1);
                    }
                } else {
                    calculated[i] = pendingHash[2*i];
                }
                
            }

            pendingHash = calculated;

        }
        return pendingHash[0];
    }

    /**
     * Calculate the hash of a block. That is calculated on the previous hash, the merkle root and the nonce
     * the merkle root has been calculated also on transaction timestamp
     * @param previousHash hash of the previous block on blockchain
     * @param merkleRoot merkle roou calculated on list of transactions
     * @param nonce simple long used to solve the prove of work problem
     * @return the hash of the block
     */
    public static byte[] calculateHash(byte[] previousHash, byte[] merkleRoot, long nonce) {
        // previous merkle root nonce
        //String s = previousHash + merkleRoot + nonce;
        // concatenate previous hash, merkle root and nonce and then calcuate the hash
        ByteBuffer buff = ByteBuffer.allocate(Long.BYTES);
        buff.putLong(nonce);
        byte[] nonceByte = buff.array();
        byte[] s = new byte[previousHash.length + merkleRoot.length + nonceByte.length];
        for(int i = 0; i < previousHash.length; i++) {
            s[i] = previousHash[i];
        }
        for(int i = 0; i < merkleRoot.length; i++) {
            s[i + previousHash.length] = merkleRoot[i];
        }
        for(int i = 0; i < nonceByte.length; i++) {
            s[i + previousHash.length + merkleRoot.length] = nonceByte[i];
        }

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch(NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
            System.exit(1);
        }
        return digest.digest(s);
    }

    /**
     * Check if hash of the block is accepted by the blockchain.
     * The hash must have the fists difficulty-characters equal to '0'
     * @param hash the hash that has to be verified
     * @param difficulty the number of initial zeros
     * @return if the hash is accepted or not
     */
    private static Boolean verifyHash(byte[] hash, int difficulty) {
        String hashString = hashToString(hash);
        for(int i = 0; i < difficulty; i++) {
            if(hashString.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }

    /**
     * Add a transaction to the block.
     * It is possible if the block is not in the mining state
     * and if the list of transactions is not full
     * @param transaction the transaction that has to be added to the list
     * @return if the transaction is added to the list or not
     */
    public boolean addTransaction(Transaction transaction) {
        // synchronize in order to be sure that are added only one transaction at time
        synchronized(listTransactions) {
            if(listTransactions.size() < LIST_LENGTH && !isMining) {
                listTransactions.add(transaction);
                return true;
            }
            return false;
        }
    }

    /**
     * Method used to mine the block
     * It must fine a value of nonce such that the hash of the block is verified
     * @param difficulty the number of initial '0' of the hash of the block
     * @param numThread the number of threads that will work
     */
    public void mineBlock(int difficulty, int numThread) {
        // se abbiamo tempo evoglia dire quanti thread dedicare
        // crea thread che mina. se numero di transazioni aumenta ricomincia
        isMining = true;
        merkleRoot = calculateMerkleRoot(listTransactions);

        MinerThread mt = new MinerThread(difficulty);
        for(int i = 0; i < numThread; i++) {
            Thread t = new Thread(mt);
            t.setDaemon(true);
            t.start();
        }
        // only one thread at time checks if the hash calculated is verified
        synchronized(isMined){
            while(!isMined.get()){
                try{
                    isMined.wait();
                }catch(Exception e){
                    e.printStackTrace();
                }    
            }
        }
        isMining = false;
        
    }

    /**
     * Verify the block. Check if the hash is correct anc if all the transactions are verified.
     * @param difficulty the number of initial '0' that tha hash of the block should have
     * @return if the block is verified
     */
    public boolean verifyBlock(int difficulty) {
        // number of initial <difficulty> chars must be all zeros
        for (Transaction t : listTransactions) {
            if(! t.verify()) {
                return false;
            }
        }
        if(! verifyHash(hash, difficulty)) {
            return false;
        }
        return true;
    }

    /**
     * Utility method used to convert a SHA256 hash to string
     * @param hash the hash that should be converted
     * @return the correspondent string
     */
    public static String hashToString(byte[] hash) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Utility method to get a byte array from a hex string
     * @param s hex string
     * @return hex string in byte array format
     */ 
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    /**
     * Class that implements a thread that mines a block.
     * This thread should find the correct nonce such that verifies the block hash
     * Implemented in this way because we will mine the block in multi-thread way
     * since in some architectures is more efficient ad faster
     * @author Giuseppe Ravagnani
     * @version 1.0
     */
    private class MinerThread implements Runnable {

        private int difficulty;

        /**
         * Create an instance of thread
         * @param difficulty the number of initial '0' that the hash of the block should have
         */
        public MinerThread(int difficulty) {
            this.difficulty = difficulty;
        }

        /**
         * Method that defines what the thread should do.
         * Increment the tempNonce and check if with this number the hash is verified.
         * If no, then iterate, if yes, then save the last information in the block and exit.
         * All synchronized between threads
         */
        public void run() {

            System.out.println("Creato " + Thread.currentThread().getName());

            while(true) {

                if(isMined.get()) {
                    //System.out.println("aaa");
                    return;
                }

                long nonceCopy = tempNonce.getAndIncrement();

                // calculate hash
                byte[] tempHash = calculateHash(previousHash, merkleRoot, nonceCopy);
                //System.out.println(Thread.currentThread().getName() + "   nonce " + nonceCopy + "   " + hashToString(tempHash));

                // verify correctness
                if(! verifyHash(tempHash, difficulty)) {
                    continue;
                }

                // only one thread at time that verifies the hash can make the modification
                synchronized(isMined) {
                    // check if not already found correct hash
                    if(isMined.get()) {
                        return;
                    }

                    isMined.set(true);
                    nonce = nonceCopy;
                    hash = tempHash;
                    minedTime = new Timestamp(System.currentTimeMillis());
                    isMined.notifyAll();
                    return;

                }

            }

        }

    }

}