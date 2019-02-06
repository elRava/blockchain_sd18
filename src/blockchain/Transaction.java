package blockchain;

import java.sql.Timestamp;
import java.security.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.charset.*;

/**
 * Class of a transaction of a blockchain
 * Transaction contains the vote and information related the source and time
 * @author Marco Sansoni
 * @version 1.0
 */
public class Transaction implements Serializable{

    private Verifiable payload;
    private Timestamp creationTime;
    private PublicKey keySrc;
    // private PublicKey keyDst;
    private byte[] signature;
    private byte[] transactionHash;

    /**
     * Constructor of the transaction
     * 
     * @param paylod content of the transaction
     * @param keySrc public key of the source
     * @param keyDst public key of the destination
     */
    public Transaction(Verifiable payload, PublicKey keySrc) {
        this.payload = payload;
        this.keySrc = keySrc;
        // this.keyDst = keyDst;
        this.signature = null;
        this.transactionHash = null;
        this.creationTime = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Get the hash of the transaction
     * 
     * @return the hash of the transaction after it is calculated
     */
    public byte[] getHash() {
        return transactionHash;
    }

    /**
     * 
     */
    public Verifiable getPayload() {
        return payload;
    }

    /**
     * Override Object equals method Two Transactions are considered equal if they
     * have the same hash
     * 
     * @param o the object that we want to compare
     * @return if the object is equal to this
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Transaction)) {
            return false;
        }
        // unique interface says that the payload of the transaction must be unique on
        // the blockchain
        // with respect to .equals implemented on the payload class
        if (this.payload instanceof Unique) {
            return this.payload.equals(((Transaction) o).payload);
        }
        if (Block.hashToString(this.transactionHash).equals(Block.hashToString(((Transaction) o).getHash()))) {
            return true;
        }
        return false;
    }

    /**
     * Signature of the transaction
     * @return the singature
     */
    public byte[] getSignature() {
        return signature;
    }

    /**    
     * Set the hash of the transaction. 
     * Hash will be timestamp + payload + keySrc 
     */
    private void setHash() {
        // String keyDestString =
        // Base64.getEncoder().encodeToString(keyDst.getEncoded());
        String keySrcString = Base64.getEncoder().encodeToString(keySrc.getEncoded());
        // String s =
        // creationTime.toString()+payload.toString()+keyDestString+keySrcString;
        String s = creationTime.toString() + payload.toString() + keySrcString;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            transactionHash = digest.digest(s.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        }
    }

    /**
     * Sign the transaction hash
     * 
     * @param privateKey the source private key
     */
    public void sign(PrivateKey privateKey) {
        // set the hash of the transaction
        setHash();
        try {
            // use RSA
            Signature ecdsa = Signature.getInstance("SHA1WithRSA");
            // sign the messagge with the privat key
            ecdsa.initSign(privateKey);
            ecdsa.update(transactionHash);
            // signature is the encrypted transaction hash
            signature = ecdsa.sign();
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        } catch (InvalidKeyException ike) {
            ike.printStackTrace();
        } catch (SignatureException se) {
            se.printStackTrace();
        }

    }

    /**
     * Verify if the transaction is valid
     * 
     * @return true if it is valid, false otherwise
     */
    public boolean verify() {
        // look if same field are modified, hashing again the transactio
        setHash();
        boolean verified = false;
        try {
            Signature ecdsaVerify = Signature.getInstance("SHA1WithRSA");
            ecdsaVerify.initVerify(keySrc);
            ecdsaVerify.update(transactionHash);
            verified = ecdsaVerify.verify(signature);
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        } catch (InvalidKeyException ike) {
            ike.printStackTrace();
        } catch (SignatureException se) {
            se.printStackTrace();
        }
        //System.out.println("Verified:" + verified + " payload.verify:" + payload.verify());
        return verified && this.payload.verify();
    }

    /**
     * Get the hash of the transaction
     * 
     * @return hash
     */
    public byte[] getTransactionHash() {
        return transactionHash;
    }

}