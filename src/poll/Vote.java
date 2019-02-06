package poll;

import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import blockchain.*;


/**
 * Class that defines a Vote entity.
 * implements the interface verifiable because we want to verify if the Vote is correct
 * implements the interface unique because we want that a vote is unique in the blockchain
 * 
 * @author Giuseppe Ravagnani
 * @version 1.0
 */
@SuppressWarnings("overrides")
public class Vote implements Verifiable, Unique, Serializable {

    // vote = the actual vote
    private String vote;
    private String seat; // seggio
    private PublicKey publicKey;
    private byte[] signature;

    /**
     * Constructor of class vote. Define all in the constructor
     * 
     * @param vote the vote
     * @param seat the seat from which the vote is created
     * @param publicKey the public key of the "person" that votes
     */
    public Vote(String vote, String seat, PublicKey publicKey) {
        this.vote = vote;
        this.seat = seat;
        this.publicKey = publicKey;
    }

    /**
     * Get the vote
     * 
     * @return the vote
     */
    public String getVote() {
        return vote;
    }

    /**
     * Get the seat
     * 
     * @return the seat from which the vote is created
     */
    public String getSeat() {
        return seat;
    }

    /**
     * Get the public key of the "voter"
     * 
     * @return the public key
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Get the signature of the vote = the vote encripted with the private key
     * 
     * @return the signature of the vote
     */
    public byte[] getSignature() {
        return signature;
    }

    /**
     * Verify the vote
     * 
     * @return if the vote is verified
     */
    public boolean verify() {

        boolean isVerified = false;
        try {
            Signature ecdsaVerify = Signature.getInstance("SHA1WithRSA");
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update((new String(this.seat + this.vote)).getBytes());
            isVerified = ecdsaVerify.verify(signature);
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        } catch (InvalidKeyException ike) {
            ike.printStackTrace();
        } catch (SignatureException se) {
            se.printStackTrace();
        }
        return isVerified;
    }

    /**
     * Sign the vote. Encrypt the seat and the vote
     * 
     * @param privateKey the private key with which encrypt the vote
     */
    public void sign(PrivateKey privateKey) {
        try {
            // use RSA
            Signature ecdsa = Signature.getInstance("SHA1WithRSA");
            // sign the messagge with the privat key
            ecdsa.initSign(privateKey);
            ecdsa.update((new String(this.seat + this.vote)).getBytes());
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
     * Override the Object's equal method
     * 
     * @param o the object to compare
     * @return if o is equal to this
     */
    @Override
    public boolean equals(Object o) {
        if(! (o instanceof Vote)) {
            return false;
        }
        // I consider two votes of the same public key = the same person, equals
        return publicKey.equals(((Vote) o).publicKey);
    }

    /**
     * Overrides the toString method
     * 
     * @return the toString
     */
    public String toString() {
        return vote + " " + seat + " " + publicKey.toString();
    }

}