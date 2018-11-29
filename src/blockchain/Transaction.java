package blockchain;

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


public class Transaction {

    private Object payload;
    private Timestamp creationTime;
    private PublicKey keySrc;
    private PublicKey keyDst;
    private String signature;
    private String transactionHash;

    public Transaction(){
        setCurrentTime();
        System.out.println(creationTime+"");
    }
    
    public Transaction(Object payload, PublicKey keySrc, PublicKey keyDst) {
        this.payload = payload;
        this.keySrc = keySrc;
        this.keyDst = keyDst;
        setCurrentTime();
    }

    private void setCurrentTime(){
        /*String TIME_SERVER = "time-a.nist.gov";   //server a cui chiedere orario
        NTPUDPClient timeClient = new NTPUDPClient();   //client che fa richiesta per il Network time protocol
        InetAddress inetAddress = InetAddress.getByName(TIME_SERVER);   //collegamento al server
        TimeInfo timeInfo = timeClient.getTime(inetAddress);    //ora che ritorna dal server è nell'oggetto TimeInfo
        //long returnTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();
        long returnTime = timeInfo.getMessage().getReceiveTimeStamp().getTime();
        this.creationTime = returnTime;
        //InetAddress*/
        this.creationTime = new Timestamp(System.currentTimeMillis());        
    }

    public void sign(PrivateKey privateKey) {
        //Effettuo l'hash
        byte output[] = new byte[0]; //conterrà l'hash finale crittografato con la mia chiave privata
        try{
            Signature ecdsa = Signature.getInstance("ECDSA", "BC");
            ecdsa.initSign(privateKey);
            //trasfomro l'input in un flusso di byte
            byte[] input = transactionHash.getBytes();
            //aggiorno nella signature i valori da essere criptati
            ecdsa.update(input);
            byte[] realSig = ecdsa.sign();
            output = realSig; //in realsign è contenuto l'array di byte del messaggio cifrato con la chiave privata
        }catch(NoSuchAlgorithmException nsae){
            nsae.printStackTrace();
        }catch(NoSuchProviderException nspe){
            nspe.printStackTrace();
        }catch(InvalidKeyException ike){
            ike.printStackTrace();
        }catch(SignatureException se){
            se.printStackTrace();
        }
        //salvo il messaggio cifrato in string con codifica utf8
        try{
            signature = new String(output,"UTF-8");
        }catch(UnsupportedEncodingException uee){
            uee.printStackTrace();
        }    
        
    }

    public boolean verify() {
        boolean verified = false;
        try {
			Signature ecdsaVerify = Signature.getInstance("ECDSA", "BC");
			ecdsaVerify.initVerify(keySrc);
			ecdsaVerify.update(transactionHash.getBytes());
			verified = ecdsaVerify.verify(signature.getBytes());
		}catch(NoSuchAlgorithmException nsae){
            nsae.printStackTrace();
        }catch(InvalidKeyException ike){
            ike.printStackTrace();
        }catch(SignatureException se){
            se.printStackTrace();
        }catch(NoSuchProviderException nspe){
            nspe.printStackTrace();
        }
        return verified;
    }

    //hash 256 di
    //timestamp + data + publickeydest + publickeysource conccatenati senza spazi
    public String getTransactionHash(){
        return null;
    }


}