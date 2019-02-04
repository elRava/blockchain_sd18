package blockchain;

import java.util.*;
import java.io.*;
import java.security.*;

/**
 * Blockchain class, contains the real implementation of a blockchain
 * @author Marco Sansoni
 * @version 1.0
 */
public class Blockchain implements Serializable {

    private Ring last;
    private byte[] hash;
    private Ring first;

    /**
     * Constructor of the blockchain It creates the genesis block at the beginning
     * of the blockchain
     */
    public Blockchain() {
        // first block of the blockchain, genesis block
        Block genesis = new Block().genesisBlock();
        // it is the only block with father set to null and depth set to 0
        Ring firstBlock = new Ring(genesis);
        this.last = firstBlock;
        this.first = firstBlock;
    }


    /**
     * Method used to add a mined block on the blockchain It will be insert
     * according to its previous hash
     * 
     * @param block it is the block to be added
     * @return true if the block is succesfully added, false otherwise
     */
    public boolean addBlock(Block block) {
        // Block will be link to the blockchain according to the previous hash field of
        // the block
        byte[] target = block.getPreviousHash();
        // In the following I look for the block with this hash
        boolean isAdd = false;
        Ring current = last;
        // Repeat until it finds block or it arrives at genesis block without finding it
        while (!isAdd && current != null) {
            // Normally the block will be linked to the last block added to the blockchain
            if (!Arrays.equals(target, current.block.getHash())) { // If it's not..
                // If i looking for same sibling of the genesis, previous is not in the
                // blockchain
                if (current.father == null) {
                    return false;
                }
                // I get the list of my siblings
                if (current.father == null) {
                    return false;
                }
                List<Ring> sibling = current.getSiblings();
                // I search recursively in all the subtree with the sibling as root
                // It is more probable to find there instead of looking deeper
                for (int i = 0; i < sibling.size() && !isAdd; i++) {
                    isAdd = isAdd || DFS(block, sibling.get(i));
                }
            } else {
                // The last block is the real last
                // Generate the ring and set father and sons properly
                Ring ring = new Ring(block);              
                //Check if the transactions in block are already in the blockchain
                List<Transaction> toAdd = block.getListTransactions();
                Ring temp = current;
                while(temp.father!=null){
                    Block currentBlock = temp.block;
                    for(Transaction t: toAdd){
                        if(currentBlock.getListTransactions().contains(t)){
                            return false;
                        }
                    }
                    temp = temp.father;
                }
                current.addSon(ring);
                // the last block reference is related to bigger depth
                if (ring.depth > last.depth) {
                    this.last = ring;
                }
                // The block is properly added to the chain
                isAdd = true;
            }
            // update the current node
            current = current.father;
        }
        return isAdd;
    }

    
    /**
     * Private method used in add
     * @param block to be added
     * @param root the block which i start the research
     * @return true if I add the block, false otherwise
     */
    private boolean DFS(Block block, Ring root) {
        // target is the same of addNode
        byte[] target = block.getPreviousHash();
        boolean isAdd = false;
        // I am looking for the hash in the subtree from root
        if (!Arrays.equals(target, root.block.getHash())) {
            // If the root is not the block who i am looking for
            // method is ran recursively on the son
            // System.out.println("Entro nel if");
            List<Ring> sons = root.sons;
            for (int i = 0; i < sons.size() && !isAdd; i++) {
                isAdd = isAdd || DFS(block, sons.get(i));
            }
        } else {
            // I find the proper block
            // Like in addNode I create a ring and set its father
            Ring ring = new Ring(block);
            //Check if the transactions in block are already in the blockchain
            List<Transaction> toAdd = block.getListTransactions();
            Ring temp = root;
            while(temp.father!=null){
                Block currentBlock = temp.block;
                for(Transaction t: toAdd){
                    if(currentBlock.getListTransactions().contains(t)){
                        return false;
                    }
                }
                temp = temp.father;
            }
            root.addSon(ring);
            // update the last Ring
            if (ring.depth >= last.depth) {
                this.last = ring;
            }
            return true;
        }
        // false if the root has no child or if the subtree does not contain the block
        return false || isAdd;
    }

    /**
     * Determine if a transaction belong to the blockchain
     * A transaction is contained on the blockchain of it is belong on the main branch
     * If a transaction belong to an orphan block will not be considered into the blockchain
     * @param transaction to analyzed
     * @return true if it is contained, false otherwise
     */
    public boolean contains(Transaction transaction) {
        Iterator<Block> it = this.getIterator();
        while (it.hasNext()) {
            Block b = it.next();
            if (b.getListTransactions().contains(transaction)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deterime if a block belongs to the main branch of a blockchain
     * If it exists on a orphan branch will be considered not on the blockchain
     * @param block to be analyzed
     * @return true if it contained, false otherwise
     */
    public boolean contains(Block block) {
        LinkedList<Ring> queue = new LinkedList<>();
        queue.add(first);
        while (!queue.isEmpty()) {
            Ring current = queue.remove(0);
            if (current.block.equals(block)) {
                return true;
            }
            for (Ring r : current.sons) {
                queue.add(r);
            }
        }
        return false;
    }

    /**
     * Compute the hash of the blockchain
     * I concatenate different hash of the block in order to compute hash of a blockchain
     */
    public void computeHash() {
        Iterator<Block> iter = this.getIterator();
        final int SHA256LENGTH = 32;
        byte[] finalhash = iter.next().getHash();
        while (iter.hasNext()) {
            byte[] current = iter.next().getHash();
            byte[] temp = new byte[2 * SHA256LENGTH];
            for (int i = 0; i < SHA256LENGTH; i++) {
                temp[i] = finalhash[i];
                temp[i + SHA256LENGTH] = current[i];
            }
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                finalhash = digest.digest(finalhash);
            } catch (NoSuchAlgorithmException nsae) {
                nsae.printStackTrace();
                System.exit(1);
            }
        }
        this.hash = finalhash;
    }

    /**
     * Hash of the blockchain
     * @return hash of blockchain
     */
    public byte[] getHash() {
        computeHash();
        return this.hash;
    }

    /**
     * Compute the hash of the blockchain
     * @return the length
     */
    public int length() {
        return last.depth + 1;
    }

    /**
     * Given an hash, it return all the blocks from there until the last block
     * @param hash the hash value of the last block
     * @return the list of the block
     */
    public LinkedList<Block> getMissingBlocks(byte[] hash) {

        //DFS queue to explore the whole blockchain
        LinkedList<Ring> queue = new LinkedList<>();
        //list that will be returned
        LinkedList<Block> returnList = new LinkedList<>();
        Ring iter = this.last;
        while (iter.father != null) {
            //I add the block to the returnlist and to the queue
            returnList.addFirst(iter.block);
            queue.addLast(iter);
            //check if we find the hash
            while (!queue.isEmpty()) {
                Ring current = queue.removeFirst();
                if (current.block.getHash().equals(hash)) {
                    //I found the block
                    return returnList;
                }
                //I look in all the siblings of the block
                for (Ring son : current.sons) {
                    if (!son.block.getHash().equals(returnList.getFirst().getHash())) {
                        queue.addLast(son);
                    }
                }
            }
            iter = iter.father;
        }
        //If it do not contains the hash i return the whole blockchain
        return returnList;
    }

    
    /**
     * Get the reference to the last block on the chain. It will be used to link to
     * its the following block
     * 
     * @return the block
     */
    public Block lastBlock() {
        return last.block;
    }

    /**
     * Get the iterator on the blockchain. Iterator stats from the last block,
     * reaching the genesis block using only the reference to the father
     * 
     * @return the iterator on the blockchain
     */
    public Iterator<Block> getIterator() {
        return new BlockchainIterator();
    }

    /**
     * Method used to store the blockchain on a file, in a visualizable format
     * @param path of the file to be saved
     */
    public void print(String path) {
        PrintStream write = null;
        // FileOutputStream f = null;
        try {
            File print = new File(path);
            print.createNewFile();
            write = new PrintStream(print);
            Iterator<Block> iter = this.getIterator();
            //System.out.println("Print Blockchain");
            int depth = this.length();
            while (iter.hasNext()) {
                Block b = iter.next();
                String s = "";
                s += "b_" + depth + ": " + Block.hashToString(b.getHash()) + " - ";
                depth--;
                for (int i = 0; i < b.getListTransactions().size(); i++) {
                    s += "t" + i + ": " + Block.hashToString(b.getListTransactions().get(i).getHash()) + ", ";
                }
                write.println(s);
            }
            write.close();
        } catch (IOException e) {
            System.out.println("Error: " + e);
            // System.exit(1);
        }
    }

    /**
     * Backup of the blockchain on a file
     * @param path of the file to be saved
     */
    public void backup(String path) {
        synchronized (this) {
            ObjectOutputStream oos = null;
            try {
                File backup = new File(path);
                backup.createNewFile();
                oos = new ObjectOutputStream(new FileOutputStream(backup));
                oos.writeObject(this);
                oos.flush();
                oos.close();
            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
                System.exit(1);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Restore the blockchain saved previously on a file
     * @param backup the file used to restore the blockchain
     * @return the blockchain
     */
    public static Blockchain restore(File backup) {
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(backup));
            Blockchain bc = (Blockchain) ois.readObject();
            ois.close();
            return bc;
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
            System.exit(1);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(1);
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
            System.exit(1);
        }
        return new Blockchain();

    }

    /**
     * Class used to implement the iterator through the blockchain
     * @author Marco Sansoni
     * @version 1.0
     */
    private class BlockchainIterator implements Iterator<Block> {
        // current position on the chain
        private Ring cursor = last;

        /**
         * Return if exits a next block, if the father is not null
         * 
         * @return true if exists, false otherwise
         */
        @Override
        public boolean hasNext() {
            return cursor != null;
        }

        /**
         * Method to get the next block on the chain
         * 
         * @return the current block on the list
         */
        @Override
        public Block next() {
            Block next = cursor.block;
            cursor = cursor.father;
            return next;
        }

    }

    /**
     * Class ring, used as a wrapper of a block
     * @author Marco Sansoni
     * @version 1.0
     */
    private class Ring implements Serializable {

        private Ring father;
        private List<Ring> sons;
        private Block block;
        // depth is the distance from the genesis block
        private int depth;

        /**
         * Constructor of the ring
         * 
         * @param block it is the block to be added on the chain
         */
        private Ring(Block block) {
            this.block = block;
            sons = new LinkedList<Ring>();
            // used only for the genesis block
            father = null;
            depth = 0;
        }

        /**
         * Add a son to this ring
         * 
         * @param ring the ring to be added as a son
         */
        private void addSon(Ring ring) {          
            sons.add(ring);
            ring.father = this;
            ring.depth = depth + 1;            
        }

        /**
         * Used to get the sibling of the node
         * 
         * @return a list of ring with all the siblings
         */
        private List<Ring> getSiblings() {
            List<Ring> siblings = new LinkedList<Ring>();
            byte[] currentHash = this.block.getHash();          
            for (Ring r : this.father.sons) {
                // Return only the ring with block's hash different from this block's hash
                if (r.block.getHash() != currentHash) {
                    siblings.add(r);
                }
            }
            return siblings;
        }

    }

}