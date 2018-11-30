package blockchain;

import java.sql.Timestamp;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


public class Block {

    public static final int LIST_LENGTH = 4;

    private List<Transaction> listTransactions;
    private byte[] previousHash;
    private byte[] hash;
    private long nonce;
    private byte[] merkleRoot;
    private AtomicLong tempNonce;
    private Timestamp minedTime;
    private boolean isMining;

    private AtomicBoolean isMined;

    public Block() {
        listTransactions = new ArrayList<>(LIST_LENGTH);
        previousHash = null;
        hash = null;
        //nonce = null;
        tempNonce = new AtomicLong();
        isMined = new AtomicBoolean(false);
        merkleRoot = null;
        minedTime = null;
        isMining = false;
    }

    public void setPreviousHash(byte[] previousHash) {
        this.previousHash = previousHash;
    }

    public byte[] getPreviousHash() {
        return previousHash;
    }

    public byte[] getHash() {
        return hash;
    }

    public byte[] getMerkleRoot() {
        return merkleRoot;
    }

    public long getNonce() {
        return nonce;
    }

    public List<Transaction> getListTransactions() {
        return listTransactions;
    }

    public Timestamp getMinedTime() {
        return minedTime;
    }

    public void calculateMerkleRoot() {
        // merkle of one single transaction is the hash itself
        final int SHA256LENGTH = 32;
        byte[][] pendingHash = new byte[listTransactions.size()][SHA256LENGTH];
        for(int i = 0; i < pendingHash.length; i++) {
            pendingHash[i] = listTransactions.get(i).getTransactionHash();
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
        merkleRoot = pendingHash[0];
    }

    public static byte[] calculateHash(byte[] previousHash, byte[] merkleRoot, long nonce) {
        // previous merkle root nonce
        //String s = previousHash + merkleRoot + nonce;
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

    private static Boolean verifyHash(byte[] hash, int difficulty) {
        String hashString = hashToString(hash);
        for(int i = 0; i < difficulty; i++) {
            if(hashString.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }

    public boolean addTransaction(Transaction transaction) {
        synchronized(listTransactions) {
            if(listTransactions.size() < LIST_LENGTH && !isMining) {
                listTransactions.add(transaction);
                return true;
            }
            return false;
        }
    }

    public void mineBlock(int difficulty, int numThread) {
        // se abbiamo tempo evoglia dire quanti thread dedicare
        // crea thread che mina. se numero di transazioni aumenta ricomincia
        isMining = true;
        calculateMerkleRoot();

        MinerThread mt = new MinerThread(difficulty);
        for(int i = 0; i < numThread; i++) {
            new Thread(mt).start();
        }
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


    public static String hashToString(byte[] hash) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }



    private class MinerThread implements Runnable {

        private int difficulty;

        public MinerThread(int difficulty) {
            this.difficulty = difficulty;
        }

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
                System.out.println(Thread.currentThread().getName() + "   nonce " + nonceCopy + "   " + hashToString(tempHash));

                // verify correctness
                if(! verifyHash(tempHash, difficulty)) {
                    continue;
                }

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