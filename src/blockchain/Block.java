package blockchain;

import java.sql.Timestamp;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.*;
import java.util.*;
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

    public Block() {
        listTransactions = new ArrayList<>(LIST_LENGTH);
        previousHash = null;
        hash = null;
        //nonce = null;
        tempNonce = new AtomicLong();
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

    public List<Transaction> getListTransactions() {
        return listTransactions;
    }

    public Timestamp getMinedTime() {
        return minedTime;
    }

    private void calculateMerkleRoot() {
        // merkle of one single transaction is the hash itself
        final int SHA256LENGTH = 32;
        byte[][] pendingHash = new byte[listTransactions.size()][SHA256LENGTH];
        for(int i = 0; i < pendingHash.length; i++) {
            pendingHash[i] = listTransactions.get(i).getTransactionHash();
        }
        while(pendingHash.length > 1) {
            String[] calculated = new String[(listTransactions.size() + 1) / 2];
            int i = 0;
            while(i < calculated.length - 1) {
                int j = 0;
                String s = pendingHash[i++];
                if(i < calculated.length) {
                    s = s + pendingHash[i];
                }
                i++;
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    calculated[j] = new String(digest.digest(s.getBytes(StandardCharsets.UTF_8)));
                } catch(NoSuchAlgorithmException nsae) {
                    nsae.printStackTrace();
                    System.exit(1);
                }
                j++;
            }
            pendingHash = calculated;
        }
        merkleRoot = pendingHash[0];
    }

    private static String calculateHash(String previousHash, String merkleRoot, long nonce) {
        // previous merkle root nonce
        String s = previousHash + merkleRoot + nonce;
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch(NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
            System.exit(1);
        }
        return new String(digest.digest(s.getBytes(StandardCharsets.UTF_8)));
    }

    private static Boolean verifyHash(String hash, int difficulty) {
        for(int i = 0; i < difficulty; i++) {
            if(hash.charAt(i) != '0') {
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

        minedTime = new Timestamp(System.currentTimeMillis());
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



    private class MinerThread implements Runnable {

        private int difficulty;

        public MinerThread(int difficulty) {
            this.difficulty = difficulty;
        }

        public void run() {

            while(true) {

                if(minedTime != null) {
                    return;
                }

                long nonceCopy = tempNonce.getAndIncrement();

                // calculate hash
                String tempHash = calculateHash(previousHash, merkleRoot, nonceCopy);

                // verify correctness
                if(! verifyHash(tempHash, difficulty)) {
                    continue;
                }

                synchronized(minedTime) {
                    // check if not already found correct hash
                    if(minedTime != null) {
                        return;
                    }

                    nonce = nonceCopy;
                    hash = tempHash;

                    return;

                }


            }

        }

    }

}