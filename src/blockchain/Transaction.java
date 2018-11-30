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
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.*;



public class Transaction {

    private Object payload;
    private Timestamp creationTime;
    private PublicKey keySrc;
    private PublicKey keyDst;
    private byte[] signature;
    private byte[] transactionHash;
    //private String hash;

    //solo a scopo di debug
    /*public Transaction(){
        setCurrentTime();
        System.out.println(creationTime+"");
    }*/
    
    public Transaction(Object payload, PublicKey keySrc, PublicKey keyDst) {
        this.payload = payload;
        this.keySrc = keySrc;
        this.keyDst = keyDst;
        this.signature = null;
        this.transactionHash = null;
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
    public byte[] getSignature() {
        return signature;
    }

    //hash 256 di
    //timestamp + data + publickeydest + publickeysource conccatenati senza spazi
    private void setHash(){
        String keyDestString = Base64.getEncoder().encodeToString(keyDst.getEncoded());
        String keySrcString = Base64.getEncoder().encodeToString(keySrc.getEncoded());
        String s = creationTime.toString()+payload.toString()+keyDestString+keySrcString;
        //System.out.println("Stringa da hashare "+s);
        try {
            /*
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(s.getBytes("UTF-8"));
            transactionHash = Base64.getEncoder().encodeToString(hash);
            */
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            transactionHash = digest.digest(s.getBytes("UTF-8"));
            
            /*System.out.println("Lunghezza "+hash.length);
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {
              String hex = Integer.toHexString(0xff & hash[i]);
              if(hex.length() == 1) hexString.append('0');
              hexString.append(hex);
            }

            //transactionHash = hexString.toString();
            transactionHash = hash;*/

        }catch(NoSuchAlgorithmException nsae){
            nsae.printStackTrace();
        }catch(UnsupportedEncodingException uee){
            uee.printStackTrace();
        } 
    }

    public void sign(PrivateKey privateKey) {
        //imposto l'hash della transazione
        setHash();

        //byte output[] = new byte[0]; //conterrà l'hash finale crittografato con la mia chiave privata
        try{
            Signature ecdsa = Signature.getInstance("SHA1WithRSA");
            ecdsa.initSign(privateKey);
            //trasfomro l'input in un flusso di byte
            //byte[] input = transactionHash;
            //aggiorno nella signature i valori da essere criptati
            ecdsa.update(transactionHash);
            signature = ecdsa.sign();
            //byte[] realSig = ecdsa.sign();
            //output = realSig; //in realsign è contenuto l'array di byte del messaggio cifrato con la chiave privata
        }catch(NoSuchAlgorithmException nsae){
            nsae.printStackTrace();
        }catch(InvalidKeyException ike){
            ike.printStackTrace();
        }catch(SignatureException se){
            se.printStackTrace();
        }
        //salvo il messaggio cifrato in string con codifica utf8
        //signature = output;
                   
    }

    public boolean verify() {
        setHash();
        boolean verified = false;
        try {
			Signature ecdsaVerify = Signature.getInstance("SHA1WithRSA");
			ecdsaVerify.initVerify(keySrc);
			ecdsaVerify.update(transactionHash);
			verified = ecdsaVerify.verify(signature);
		}catch(NoSuchAlgorithmException nsae){
            nsae.printStackTrace();
        }catch(InvalidKeyException ike){
            ike.printStackTrace();
        }catch(SignatureException se){
            se.printStackTrace();
        }    
        return verified;
    }

    
    public byte[] getTransactionHash(){       
        return transactionHash;
    }


}