package blockchain;

import java.rmi.*;
import java.util.LinkedList;

/**
 * Interface of a remote object that defines a miner
 * @author Giuseppe Ravagnani
 * @version 1.0
 */
public interface MinerInterface extends Remote {

    public Blockchain getBlockchain() throws RemoteException;

    public byte[] getBlockchainHash() throws RemoteException;

    public void sendTransaction(Transaction transaction) throws RemoteException;

    public void sendBlock(Block block) throws RemoteException;

    public LinkedList<Block> getMissingBlocks(byte[] hash) throws RemoteException;
    
}
