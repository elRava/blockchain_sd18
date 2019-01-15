package poll;

import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import blockchain.*;

@SuppressWarnings("overrides")
public class Vote implements Verifiable, Unique, Serializable {

    private String vote;
    private String seat; // seggio
    private PublicKey publicKey;
    private byte[] signature;

    public Vote(String vote, String seat, PublicKey publicKey) {
        this.vote = vote;
        this.seat = seat;
        this.publicKey = publicKey;
    }

    public String getVote() {
        return vote;
    }

    public String getSeat() {
        return seat;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public byte[] getSignature() {
        return signature;
    }

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

    @Override
    public boolean equals(Object o) {
        if(! (o instanceof Vote)) {
            return false;
        }
        // I consider two votes of the same public key = the same person, equals
        return publicKey.equals(((Vote) o).publicKey);
    }

}