import registry.*;
import java.security.*;
import java.util.*;
import blockchain.*;
import java.sql.Timestamp;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.net.UnknownHostException;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.*;
import java.rmi.*;

public class TestClass {


    public static void main(String[] args) {
           
        //Transaction t = new Transaction();
        PublicKey pubAlice = null;
        PrivateKey priAlice = null;
        //creo chiave pubblica e privata
        
        try{
            KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
            //Inizializzo il key generator
            keygen.initialize(1024);
            KeyPair kp = keygen.genKeyPair();
            pubAlice = kp.getPublic();
            priAlice = kp.getPrivate();
        }catch(Exception nsae){
            nsae.printStackTrace();
        }
        
        String convPubintoStringAlice = Base64.getEncoder().encodeToString(pubAlice.getEncoded());
        String convPriintoStringAlice = Base64.getEncoder().encodeToString(priAlice.getEncoded());
        
        //KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        //kpg.initialize(1024);
        //KeyPair kpAlice = kpg.genKeyPair();
        /*System.out.println("Chiave public Alice is "+convPubintoStringAlice);
        System.out.println("Chiave private Alice is "+convPriintoStringAlice);
        */
        System.out.println("Chiavi di Alice generate");

        PublicKey pubBob = null;
        PrivateKey priBob = null;
        
        try{
            KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
            //Inizializzo il key generator
            keygen.initialize(1024);
            KeyPair kp = keygen.genKeyPair();
            pubBob = kp.getPublic();
            priBob = kp.getPrivate();
        }catch(Exception nsae){
            nsae.printStackTrace();
        }

        String convPubintoStringBob = Base64.getEncoder().encodeToString(pubBob.getEncoded());
        String convPriintoStringBob = Base64.getEncoder().encodeToString(priBob.getEncoded());
        
        /*System.out.println("Chiave public Bob is "+convPubintoStringBob);
        System.out.println("Chiave private Bob is "+convPriintoStringBob);*/
        System.out.println("Chiavi di Bob generate");
   
        System.out.println("Creo b1");

        Blockchain chain = new Blockchain();
        System.out.println("Blockchain creata");
        //System.out.println("Ultimo hash, che sarebbe il primo: "+Block.hashToString(chain.lastBlock().getHash()));
        Block b1 = new Block();

        //chain.lastBlock().getHash();
        b1.setPreviousHash(chain.lastBlock().getHash());



        Transaction t1 = new Transaction(5, pubAlice, pubBob);
        t1.sign(priAlice);
        b1.addTransaction(t1);
        //System.out.println("Corretta? "+t1.verify());
        System.out.println("HASH t1 "+Block.hashToString(t1.getTransactionHash()));
        //b.calculateMerkleRoot();
        //Block.calculateMerkleRoot(listTransactions)
        //System.out.println("merkle "+Block.hashToString(b.getMerkleRoot()));
/*
        Transaction t2 = new Transaction(15, pubAlice, pubBob);
        t2.sign(priAlice);
        b.addTransaction(t2);
        //System.out.println("Corretta? "+t1.verify());
        System.out.println("HASH t2 "+Block.hashToString(t2.getTransactionHash()));
        b.calculateMerkleRoot();
        System.out.println("merkle "+Block.hashToString(b.getMerkleRoot()));

        Transaction t3 = new Transaction(25, pubAlice, pubBob);
        t3.sign(priAlice);
        b.addTransaction(t3);
        //System.out.println("Corretta? "+t1.verify());
        System.out.println("HASH t3 "+Block.hashToString(t3.getTransactionHash()));
        b.calculateMerkleRoot();
        System.out.println("merkle "+Block.hashToString(b.getMerkleRoot()));

        Transaction t4 = new Transaction(35, pubAlice, pubBob);
        t4.sign(priAlice);
        b.addTransaction(t4);
        //System.out.println("Corretta? "+t1.verify());
        System.out.println("HASH t4 "+Block.hashToString(t4.getTransactionHash()));
        b.calculateMerkleRoot();
        System.out.println("merkle "+Block.hashToString(b.getMerkleRoot()));

/*
        byte[] m2 = new byte[t1.getTransactionHash().length + t2.getTransactionHash().length];
        for(int i = 0; i < t1.getTransactionHash().length; i++) {
            m2[i] = t1.getTransactionHash()[i];
            m2[i+t1.getTransactionHash().length] = t2.getTransactionHash()[i];
        }
        byte[] merkle2 = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            merkle2 = digest.digest(m2);
            System.out.println("Test merkle 2 " + Block.hashToString(merkle2));
        }catch(NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
            System.exit(1);
        }

        byte[] m3 = new byte[merkle2.length + t3.getTransactionHash().length];
        for(int i = 0; i < merkle2.length; i++) {
            m3[i] = merkle2[i];
            m3[i+merkle2.length] = t3.getTransactionHash()[i];
        }
        byte[] merkle3 = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            merkle3 = digest.digest(m3);
            System.out.println("Test merkle 3 " + Block.hashToString(merkle3));
        }catch(NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
            System.exit(1);
        }


        b.setPreviousHash(t1.getTransactionHash());

        System.out.println("prev " + Block.hashToString(b.getPreviousHash()));
        //System.out.println("merk " + Block.hashToString(b.getMerkleRoot()));
        //System.out.println("prev " + Block.hashToString(b.getPreviousHash()));
        */
        //b.setPreviousHash();

        //long begin = System.currentTimeMillis();
        
        b1.mineBlock(3, 3);
        //long end = System.currentTimeMillis();

        //System.out.println("Block hash " + Block.hashToString(b.getHash()));
        //System.out.println("Mining Time " + (end-begin) + "ms");


        //System.out.println("Verify hash " + Block.hashToString(b.calculateHash(b.getPreviousHash(), b.getMerkleRoot(), b.getNonce())));
        byte[] hashb1 = b1.getHash();
        System.out.println("Blocco b1 ha hash: "+Block.hashToString(hashb1));

        chain.addBlock(b1);
        System.out.println("Blocco b1 aggiunto alla blockchain");
        Iterator<Block> iter = chain.getIterator();
        int i = 0;
        byte[] primo;
        while(iter.hasNext()){
            
            
            System.out.println("Blocco "+i+" ha hash: "+Block.hashToString(iter.next().getHash()));
            i++;
        }
        iter = chain.getIterator();
        iter.next();
        primo = iter.next().getHash();
        
        
        Transaction t2 = new Transaction(15, pubAlice, pubBob);
        t2.sign(priAlice);

        System.out.println("\n\n\n\n\n\n\n Creo b2");
        System.out.println("Aggiungo un nuovo blocco");
        
        Block b2 = new Block();
        b2.addTransaction(t2);

        b2.setPreviousHash(chain.lastBlock().getHash());


        b2.mineBlock(4,3);

        byte[] hashb2 = b2.getHash();
        System.out.println("Blocco b2 ha hash: "+Block.hashToString(hashb2));

        chain.addBlock(b2);
        System.out.println("Blocco b2 aggiunto alla blockchain");

        i=0;
        iter  = chain.getIterator();
        while(iter.hasNext()){
            //int i = 0;
            
            System.out.println("Blocco "+i+" ha hash: "+Block.hashToString(iter.next().getHash()));
            i++;
        }

        System.out.println("\n\n\n\n\n\n\n Creo b3");
        System.out.println("Aggiungo un nuovo blocco, si collega al primo, posizione sbagliata");
        
        Block b3 = new Block();
        b3.addTransaction(t2);

        b3.setPreviousHash(primo);

        b3.mineBlock(3,2);

        byte[] hashb3 = b3.getHash();
        System.out.println("Blocco b3 ha hash: "+Block.hashToString(hashb3));

        chain.addBlock(b3);
        System.out.println("Blocco b3 aggiunto alla blockchain");

        i=0;
        iter  = chain.getIterator();
        while(iter.hasNext()){
            //int i = 0;
            
            System.out.println("Blocco "+i+" ha hash: "+Block.hashToString(iter.next().getHash()));
            i++;
        }

        System.out.println("\n\n\n\n\n\n\n Creo b4");
        System.out.println("Aggiungo un nuovo blocco, si collega al siblings di b1");
        
        Block b4 = new Block();
        b4.addTransaction(t2);

        b4.setPreviousHash(hashb3);

        b4.mineBlock(3,2);

        byte[] hashb4 = b4.getHash();
        System.out.println("Blocco b4 ha hash: "+Block.hashToString(hashb4));

        boolean add4 = chain.addBlock(b4);
        System.out.println("Blocco b4 aggiunto alla blockchain? "+add4);

        i=0;
        iter  = chain.getIterator();
        while(iter.hasNext()){
            //int i = 0;
            
            System.out.println("Blocco "+i+" ha hash: "+Block.hashToString(iter.next().getHash()));
            i++;
        }

        System.out.println("\n\n\n\n\n\n\n Creo b5");
        System.out.println("Aggiungo un nuovo blocco, si collega come figlio a b2");
        Block b5 = new Block();
        b5.addTransaction(t2);

        b5.setPreviousHash(hashb2);

        b5.mineBlock(3,2);

        byte[] hashb5 = b5.getHash();
        System.out.println("Blocco b5 ha hash: "+Block.hashToString(hashb5));

        boolean add5 = chain.addBlock(b5);
        System.out.println("Blocco b5 aggiunto alla blockchain? "+add5);

        i=0;
        iter  = chain.getIterator();
        while(iter.hasNext()){
            //int i = 0;
            
            System.out.println("Blocco "+i+" ha hash: "+Block.hashToString(iter.next().getHash()));
            i++;
        }

        System.out.println("\n\n\n\n\n\n\n Creo b6");
        System.out.println("Aggiungo un nuovo blocco, nuova biforcazione del genesis");
        Block b6 = new Block();

        Transaction t3 = new Transaction(55, pubAlice, pubBob);
        t3.sign(priAlice);

        b6.addTransaction(t3);

        b6.setPreviousHash(primo);

        b6.mineBlock(3,2);

        byte[] hashb6 = b6.getHash();
        System.out.println("Blocco b6 ha hash: "+Block.hashToString(hashb6));

        boolean add6 = chain.addBlock(b6);
        System.out.println("Blocco b6 aggiunto alla blockchain? "+add6);

        i=0;
        iter  = chain.getIterator();
        while(iter.hasNext()){
            //int i = 0;
            
            System.out.println("Blocco "+i+" ha hash: "+Block.hashToString(iter.next().getHash()));
            i++;
        }
        //System.out.println()

        System.out.println("\n\n\n\n\n\n\n Creo b7");
        System.out.println("Aggiungo un nuovo blocco, dopo b6");
        Block b7 = new Block();
        b7.addTransaction(t2);

        b7.setPreviousHash(hashb6);

        b7.mineBlock(3,2);

        byte[] hashb7 = b7.getHash();
        System.out.println("Blocco b7 ha hash: "+Block.hashToString(hashb7));

        boolean add7 = chain.addBlock(b7);
        System.out.println("Blocco b7 aggiunto alla blockchain? "+add7);

        i=0;
        iter  = chain.getIterator();
        while(iter.hasNext()){
            //int i = 0;
            
            System.out.println("Blocco "+i+" ha hash: "+Block.hashToString(iter.next().getHash()));
            i++;
        }



        /*


        

        try {
            r = (RegistryInterface) Naming.lookup("//localhost:7867/registry");
        } catch(Exception re) {
            re.printStackTrace();
            System.exit(1);
        }


        BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));


        while(true) {
            String line = null;
            try {
                while((line = bf.readLine()) != null) {
                    try {
                        r.register(new InetSocketAddress(line, 1234));
                        ArrayList<InetSocketAddress> l = r.getIPSet();
                        for (InetSocketAddress a : l) {
                            System.out.println("... " + a.getHostName());
                        }
                    } catch(RemoteException re) {
                        re.printStackTrace();
                        System.exit(1);
                    }
                }
            } catch(IOException ioe) {
                ioe.printStackTrace();
                System.exit(1);
            }
            

        }
        */



    }

}