
import java.security.*;
import java.util.*;

import blockchain.Transaction;

public class TestClass {


    public static void main(String[] args) {
        Transaction t = new Transaction();
        PublicKey pub = null;
        PrivateKey pri = null;
        //creo chiave pubblica e privata
        /*try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA","BC");
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
			// Initialize the key generator and generate a KeyPair
			keyGen.initialize(ecSpec, random);   //256 bytes provides an acceptable security level
	        	KeyPair keyPair = keyGen.generateKeyPair();
	        	// Set the public and private keys from the keyPair
	        	privateKey = keyPair.getPrivate();
	        	publicKey = keyPair.getPublic();
		}catch(Exception e) {
			throw new RuntimeException(e);
        }
        */
        try{
            KeyPairGenerator keygen = KeyPairGenerator.getInstance("DSA","SUN");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            //Inizializzo il key generator
            keygen.initialize(1024, random);
            KeyPair kp = keygen.genKeyPair();
            pub = kp.getPublic();
            pri = kp.getPrivate();
        }catch(Exception nsae){
            nsae.printStackTrace();
        }
        
        String convPubintoString = Base64.getEncoder().encodeToString(pub.getEncoded());
        String convPriintoString = Base64.getEncoder().encodeToString(pri.getEncoded());

        System.out.println("Chiave public is "+convPubintoString);
        System.out.println("Chiave private is"+convPriintoString);

    }

    

}