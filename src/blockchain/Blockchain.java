package blockchain;

import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.*;

public class Blockchain {

    //Ring first;
    Ring last;


    public Blockchain() {
        Block genesis = new Block().genesisBlock(); //blocco iniziale aggiunto alla blockchain
        //unico blocco in cui non faccio setFather, viene impostato automaticamente a null, con il corrispondente depth a 0
        Ring firstBlock = new Ring(genesis);
        this.last = firstBlock;
    }

    public boolean addBlock(Block block) {
        // creare ring e attaccare al posto giusto
        // va tenuto consistente il riferimento all'ultimo blocco, sarà quello con depth massima
        // in caso di pareggio tra depth, considero last quello appena attaccato
        byte[] target = block.getPreviousHash(); //attacco in funzione del previuos hash
        //if(last.getBlock().getHash()!=target)
        boolean isAdd = false;
        Ring current = last;
        //finisce se lo trova, oppure se sono arrivato alla cima senza un nodo valido corrispondente
        while(!isAdd && current!=null){    
            System.out.println("Analisi corrente:");
            System.out.println("Hash target da inserire: "+Block.hashToString(target));
            System.out.println("Corrente hash da valutare: "+Block.hashToString(current.block.getHash()));       
            if(!Arrays.equals(target,current.block.getHash())){ // non è giusto attaccarlo a last
                System.out.println("Cerco nei siblings");
                List<Ring> sibling = last.getSiblings();
                for(int i=0; i<sibling.size() && !isAdd; i++){
                    isAdd = isAdd || DFS(block,sibling.get(i));
                }
            }else{
                System.out.println("Proprio quello giusto");
                //si attacca alla radice
                Ring ring = new Ring(block);
                ring.setFather(current);
                //mantengo la consistenza con last
                if(ring.depth>=last.depth){
                    this.last = ring;
                }
                isAdd = true;  
            }
            current = current.father;           
        }    
        return isAdd;
         
    }

    private boolean DFS(Block block, Ring root){
        //provo ad attaccarla come un altro figlio del root
        byte[] target = block.getPreviousHash();
        boolean isAdd = false;
        if(!Arrays.equals(target,root.block.getHash())){
            List<Ring> sons = root.sons;     
            //appena mi ritorna true un sottoramo esco dalla ricerca
            for(int i=0; i<sons.size() && !isAdd; i++){
                isAdd = isAdd || DFS(block,sons.get(i));
            }
        }else{
            //il nodo corrispondente corrisponde
            Ring ring = new Ring(block);
            ring.setFather(root);
            //mantengo la consistenza con last
            if(ring.depth>=last.depth){
                this.last = ring;
            }
            return true; 
        }
        //se non ha figli e il nodo corrisondente non corrisponde e non c'è nel sottoalbero allora è false
        //se è presente nel sottoalbero allora è true
        return false || isAdd;          
    }

    public Block lastBlock(){
        return last.block;
    }

    private Iterator<Block> getIterator() {
        return new BlockchainIterator();
    }


    private class BlockchainIterator implements Iterator<Block> {

        // per iteratore
        //il cursore corrisponde al blocco che si ottiene facendo next(che concettualmente sarebbe previous)
        //iteratore va da coda a cima
        private Ring cursor = last;

        //genesis Block è un blocco valido a tutti gli effetti, torna false solamente quando si punta al blocco precedente al genesis
        @Override
        public boolean hasNext() {
            return cursor!=null;
        }
    
        @Override
        public Block next() {
            Block next = last.block;
            //scorre al successivo
            cursor = last.father;
            return next;
        }
           

    }


    private class Ring {

        private Ring father;
        private List<Ring> sons;
        private Block block;
        private int depth;

        private Ring(Block block) {
            this.block = block;
            List<Ring> sons = new LinkedList<Ring>();
            //se non gli imposto il padre successivamente è un blocco che punta all'origine
            // viene utilizzato solo per il GenesisBlock
            father = null;
            depth = 0;
        }

        //metodo unico per fare il setFather per il figlio e automaticamente aggiunge il figlio alla lista del padre 
        private void setFather(Ring ring){
            this.father = ring;
            ring.sons.add(this);
            this.depth = ring.depth+1;
        }


        private List<Ring> getSiblings(){
            List<Ring> siblings = new LinkedList<Ring>();
            for(Ring r : this.father.sons){
                if(r!=this){//da controllare se funziona o specificare meglio il "diverso da"
                    siblings.add(r);
                }
            }
            return siblings;
        }

    }

}