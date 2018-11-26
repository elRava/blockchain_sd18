package blockchain;

import java.rmi.*;
import java.util.List;

public class Miner implements MinerInterface {

    public Miner() {

    }

    public Blockchain getBlockchain() throws RemoteException {
        return null;
    }

    public boolean sendTransaction(Transaction transaction) throws RemoteException {
        return true;
    }

    public boolean sendBlock(Block block) throws RemoteException {
        return true;
    }

    public void startThreads() {
        // thread che aggiorna registro
        Thread reg = new Thread() {
            public void run() {

            }
        };
        reg.start();
        // thread che pensa alle transazioni
        Thread tr = new Thread() {
            public void run() {

            }
        };
        tr.start();
        // thread che pensa ai blocchi
        Thread bl = new Thread() {
            public void run() {

            }
        };
        bl.start();
        // thread che mina e basta
        Thread min = new Thread() {
            public void run() {

            }
        };
        min.start();
    }

    private Blockchain chooseBlockchain(List<Blockchain> list) {
        return null;
    }


}