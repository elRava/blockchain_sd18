
import java.security.*;
import java.util.*;

import blockchain.Transaction;

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
*/
        System.out.println("Chiave public Alice is "+convPubintoStringAlice);
        System.out.println("Chiave private Alice is "+convPriintoStringAlice);

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

        System.out.println("Chiave public Bob is "+convPubintoStringBob);
        System.out.println("Chiave private Bob is "+convPriintoStringBob);
        
        Transaction t = new Transaction(5, pubAlice, pubBob);
        t.sign(priBob);
        System.out.println("Corretta? "+t.verify());



    }

    
    

}