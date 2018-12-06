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

import java.net.*;
import java.net.UnknownHostException;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.*;



public class provaMine{
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
         Block genesis = new Block().genesisBlock();

         Block b = new Block();
         Transaction t = new Transaction(10, pubAlice, pubBob);
         t.sign(priAlice);
         b.addTransaction(t);
         b.setPreviousHash(genesis.getHash());
         //Transazione aggiunta
         System.out.println("Transazione aggiunta");
         //b.setPreviousHash(previousHash);
         b.mineBlock(3, 1);
         System.out.println("Blocco minato con successo");
    

    }

}