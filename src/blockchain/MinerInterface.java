package blockchain;

import java.rmi.*;

public interface MinerInterface extends Remote {

    public Blockchain getBlockchain() throws RemoteException;

    public void sendTransaction(Transaction transaction) throws RemoteException;

    public void sendBlock(Block block) throws RemoteException;

}