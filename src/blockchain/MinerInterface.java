package blockchain;

import java.rmi.*;
import java.util.LinkedList;

public interface MinerInterface extends Remote {

    public Blockchain getBlockchain() throws RemoteException;

    public void sendTransaction(Transaction transaction) throws RemoteException;

    public void sendBlock(Block block) throws RemoteException;

    public LinkedList<Block> getBlocksGivenLength(int depth) throws RemoteException;

    public int depthOfTheBlock(byte[] hash) throws RemoteException;
}