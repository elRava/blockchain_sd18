package blockchain;

import java.net.InetAddress;
import java.rmi.*;
import java.util.*;
import registry.*;
import java.rmi.server.*;

public class Miner implements MinerInterface {

    private Blockchain blockchain;

    private List<InetAddress> registry;

    private List<Transaction> transactionToSend;
    private List<Transaction> pendingTransactions;

    private List<Block> blockToSend;
    private List<Block> pendingBlock;

    public Miner() throws RemoteException{
        super();
        transactionToSend = new LinkedList<Transaction>();
        pendingTransactions = new LinkedList<Transaction>();
        blockToSend = new LinkedList<Block>();
        pendingBlock = new LinkedList<Block>();
        registry = null;
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

    public boolean sendBlock(Block block) throws RemoteException {
        //stesso modello delle transactions applicate ai blocchi
        synchronized(blockToSend){
            blockToSend.add(block);
            blockToSend.notifyAll();
            //delega tutti i controlli e le verifiche al thread che aggiunge i blocchi alla blockchain e le manda a tutti
        }
        //non c'è nessuna verifica
        return true;
    }

    public void startThreads() {
        // thread che aggiorna registro
        Thread reg = new Thread() {
            public void run() {
                //aggiorna il registro, ottenendo la lista di tutti gli inetaddress dal getregistry
                //while true
                //getIpaddress
                //thread sleep
            }
        };
        reg.start();
        // thread che pensa alle transazioni
        Thread tr = new Thread() {
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
        };
        tr.start();
        // thread che pensa ai blocchi
        Thread bl = new Thread() {
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
        };
        bl.start();
        // thread che mina e basta
        Thread min = new Thread() {
            public void run() {
                //mi creo una lista privata con le transazioni da fare il mining
                //se non ci sono mi addormento, mi risveglia il thread delle transazioni
                
                //inizio il miner del blocco

                //se ho successo e trovo aggiungo al blocktosend
                //lui si occuperà di tirare via le transazioni minate da pendingTransactions
            }
        };
        min.start();
    }

    private Blockchain chooseBlockchain(List<Blockchain> list) {
        //prendo gli hash della blockchain da tutti quelli cui sono attaccato
        //quello che ha occorrenze maggiori è il vincitore
        //da lui scarico la blockchain
        //controllo hash blockchain per vedere se è consistente con quello che mi ha appena inviato
        //faccio tutte le operazioni su synchronized blockchain
        return null;
    }


}