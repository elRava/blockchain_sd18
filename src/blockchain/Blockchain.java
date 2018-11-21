package blockchain;

public class Blockchain {

    Ring first;
    Ring last;


    public Blockchain() {
    
    }

    public boolean addBlock(Block block) {
        // creare ring e attaccare al posto giusto
        return true;
    }



    private class Ring {

        private Ring father;
        private List sons;
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