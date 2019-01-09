package blockchain;

import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    public LinkedList<Block> getFromDepth(int depth){
        LinkedList<Block> missingBlock = new LinkedList<>();
        int currentLength = last.depth;
        Ring currentRing = last;
        while(currentLength>=depth){
            missingBlock.add(currentRing.block);
            currentRing = currentRing.father;
            currentLength = currentRing.depth;
        }
        return missingBlock;
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

            /*
             * System.out.println("Analisi corrente:");
             * System.out.println("Hash target da inserire: "+Block.hashToString(target));
             * System.out.println("Corrente hash da valutare: "+Block.hashToString(current.
             * block.getHash()));
             */

            // Normally the block will be linked to the last block added to the blockchain
            if (!Arrays.equals(target, current.block.getHash())) { // If it's not..
                //If i looking for same sibling of the genesis, previous is not in the blockchain
                if(current.father == null){
                    return false;
                }        
                // I get the list of my siblings
                if(current.father == null) {
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
                current.addSon(ring);
                // the last block reference is related to bigger depth
                if (ring.depth >= last.depth) {
                    this.last = ring;
                }
                // The block is properly added to the chain
                isAdd = true;
                // System.out.println("Blocco corrente con hash
                // "+Block.hashToString(ring.block.getHash())+" appena inserito ha
                // "+ring.sons.size()+" figli");
            }
            // uodate the current node
            current = current.father;
        }
        return isAdd;

    }

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

    public boolean contains(Block block) {
        LinkedList<Ring> queue = new LinkedList<>();
        queue.add(first);
        while(!queue.isEmpty()){
            Ring current = queue.remove(0);
            if(current.block.equals(block)){
                return true;
            }
            for(Ring r: current.sons){
                queue.add(r);
            }
        }
        return false;
    }

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

    public byte[] getHash() {
        computeHash();
        return this.hash;
    }

    public int length() {
        return last.depth+1;
    }

    /*
     * public boolean contains(Block b){ Iterator<Block> iter = this.getIterator();
     * byte[] target = b.getHash(); while(iter.hasNext()){ byte[] current =
     * iter.next().getHash(); boolean same = true; for(int i=0; i< target.length;
     * i++){ if(current[i]!=target[i]){ same = false; break; } } if(same){
     * 
     * }
     * 
     * 
     * public boolean contains(Transaction t){ Iterator<Block> iter =
     * this.getIterator(); byte[] target = t.getTransactionHash();
     * while(iter.hasNext()){ Block currentBlock = iter.next(); List<Transaction>
     * allTran = currentBlock.getListTransactions(); Iterator<Transaction> iterTran
     * = allTran.iterator(); while(iterTran.hasNext()){ byte[] currentTran =
     * iterTran.next().getTransactionHash(); boolean same = true; for(int i=0; i<
     * cur.length; i++){ if(current[i]!=target[i]){ same = false; break; } } }
     * if(same){ return true; } } }
     * 
     * }
     */

    // private method used with addBlock
    private boolean DFS(Block block, Ring root) {
        // target is the same of addNode
        byte[] target = block.getPreviousHash();
        boolean isAdd = false;

        /*
         * System.out.println("Analisi corrente:");
         * System.out.println("Hash target da inserire: "+Block.hashToString(target));
         * System.out.println("Corrente hash da valutare: "+Block.hashToString(root.
         * block.getHash()));
         */

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


    public void print(String path) {
        PrintStream write = null;
        // FileOutputStream f = null;
        try {
            File print = new File(path);
            print.createNewFile();
            write = new PrintStream(print);
            Iterator<Block> iter = this.getIterator();
            System.out.println("Print Blockchain");
            int depth = this.length();
            while (iter.hasNext()) {
                Block b = iter.next();
                String s = "";
                s += "b_" + depth + ": " + Block.hashToString(b.getHash()) + " - ";
                depth--;
                for (int i = 0; i < b.getListTransactions().size()-1; i++) {
                    s += "t" + i + ": " + Block.hashToString(b.getListTransactions().get(i).getHash()) + ", ";
                }
                int lastIndex = b.getListTransactions().size()-1;
                s += "t" + lastIndex + ": " + Block.hashToString(b.getListTransactions().get(lastIndex).getHash());
                write.println(s);
            }

            write.close();
        } catch (IOException e) {
            System.out.println("Error: " + e);
            // System.exit(1);
        }
    }

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
            /*
             * System.out.println("Padre "+Block.hashToString(this.block.getHash())
             * +" prima della chiamata del metodo ha "+sons.size()+" figli"); for(int
             * i=0;i<sons.size();i++){
             * System.out.println("Figlio "+i+" ha hash "+Block.hashToString(sons.get(i).
             * block.getHash())); }
             */
            sons.add(ring);
            ring.father = this;
            ring.depth = depth + 1;
            // System.out.println("Padre dopo la chiamata del metodo ha "+sons.size()+"
            // figli");
            /*
             * System.out.println("Padre "+Block.hashToString(this.block.getHash())+" ha "
             * +sons.size()+" figli"); for(int i=0;i<sons.size();i++){
             * System.out.println("Figlio "+i+" ha hash "+Block.hashToString(sons.get(i).
             * block.getHash())); }
             */
        }

        /**
         * Used to get the sibling of the node
         * 
         * @return a list of ring with all the siblings
         */
        private List<Ring> getSiblings() {
            List<Ring> siblings = new LinkedList<Ring>();
            byte[] currentHash = this.block.getHash();
            // System.out.println("Il padre con hash
            // "+Block.hashToString(this.father.block.getHash())+" ha
            // "+this.father.sons.size()+" figli");
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