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
        /*    
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
        System.out.println("Chiave public Alice is "+convPubintoStringAlice);
        System.out.println("Chiave private Alice is "+convPriintoStringAlice);

        PublicKey pubBob = null;
        PrivateKey priBob = null;
        
        try{
            KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
            //Inizializzo il key generator
            keygen.initialize(10
import blockchain.Transaction;24);
            KeyPair kp = keygen.genKeyPair();
            pubBob = kp.getPublic();
            priBob = kp.getPrivate();
        }catch(Exception nsae){
            nsae.printStackTrace();
        }

        String convPubintoStringBob = Base64.getEncoder().encodeToString(pubBob.getEncoded());
        String convPriintoStringBob = Base64.getEncoder().encodeToString(priBob.getEncoded());
        
        System.out.println("Chiave public Bob is "+convPubintoStringBob);
        System.out.println("Chiave private Bob is "+convPriintoStringBob);
   




        Block b = new Block();

/*

        Transaction t1 = new Transaction(5, pubAlice, pubBob);
        t1.sign(priAlice);
        b.addTransaction(t1);
        //System.out.println("Corretta? "+t1.verify());
        System.out.println("HASH t1 "+Block.hashToString(t1.getTransactionHash()));
        b.calculateMerkleRoot();
        System.out.println("merkle "+Block.hashToString(b.getMerkleRoot()));

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
        System.out.println("merk " + Block.hashToString(b.getMerkleRoot()));
        //System.out.println("prev " + Block.hashToString(b.getPreviousHash()));


        long begin = System.currentTimeMillis();
        b.mineBlock(5, 3);
        long end = System.currentTimeMillis();

        System.out.println("Block hash " + Block.hashToString(b.getHash()));
        System.out.println("Mining Time " + (end-begin));


        System.out.println("Verify hash " + Block.hashToString(b.calculateHash(b.getPreviousHash(), b.getMerkleRoot(), b.getNonce())));
        */

        RegistryInterface r = null;

        try {
            r = (RegistryInterface) Naming.lookup("//" + args[0] + "/registry");
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




    }

}