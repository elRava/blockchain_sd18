
import java.security.*;
import java.util.*;

import blockchain.Block;
import blockchain.Transaction;
import java.sql.Timestamp;
import java.net.InetAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.*;

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
        
        /*KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024);
        keyPair kpAlice = kpg.genKeyPair();
        System.out.println("Chiave public Alice is "+convPubintoStringAlice);
        System.out.println("Chiave private Alice is "+convPriintoStringAlice);
*/
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
        /*
        System.out.println("Chiave public Bob is "+convPubintoStringBob);
        System.out.println("Chiave private Bob is "+convPriintoStringBob);
        */
        Transaction t1 = new Transaction(5, pubAlice, pubBob);
        t1.sign(priAlice);
        System.out.println("Corretta? "+t1.verify());
        System.out.println("HASH t1 "+Block.hashToString(t1.getTransactionHash()));

        Transaction t2 = new Transaction(15, pubAlice, pubBob);
        t2.sign(priAlice);
        System.out.println("Corretta? "+t2.verify());
        
        System.out.println("HASH t2 "+Block.hashToString(t2.getTransactionHash()));
        String con = Block.hashToString(t1.getTransactionHash()) + Block.hashToString(t2.getTransactionHash());
        System.out.println("Stringa merkle root "+con);
        Block b = new Block();
        b.addTransaction(t1);
        b.calculateMerkleRoot();
        //b.getMerkleRoot();
        System.out.println("Merkle con solo una transazione is "+Block.hashToString(b.getMerkleRoot()));
        b.addTransaction(t2);
        b.calculateMerkleRoot();
        System.out.println("Aggiunta t2");
        System.out.println("Merkle con due transazioni is "+Block.hashToString(b.getMerkleRoot()));

    }

    
    

}