package blockchain;

import java.util.List;
import java.util.Iterator;

public class Blockchain {

    Ring first;
    Ring last;


    public Blockchain() {
        // crea anche blocco nullo
    }

    public boolean addBlock(Block block) {
        // creare ring e attaccare al posto giusto
        return true;
    }

    private Iterator<Block> getIterator() {
        return null;
    }


    private class BlockchainIterator implements Iterator<Block> {

        // per iteratore
        private Ring cursor = first;

        @Override
        public boolean hasNext() {
            return true;
        }
    
        @Override
        public Block next() {
            return null;
        }
    
        @Override
        public void remove() {
    
        }

    }


    private class Ring {

        private Ring father;
        private List<Ring> sons;
        private Block block;

        private Ring(Block block) {
            this.block = block;
        }

        private void addSon(Ring ring) {

        }

        private void setFather(Ring ring) {

        }

    }

}