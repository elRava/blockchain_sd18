package blockchain;

import java.security.*;

public class Transaction {

    private Object data;
    private Timestamp time;
    private PublicKey keySrc;
    private PublicKey keyDst;
    private String signature;

    public Transaction(Object data, PublicKey keySrc, PublicKey keyDst) {
        
    }

    public void sign(PrivateKey privateKey) {

    }

    public boolean verify() {
        // decriptare signature con chiave pubblica src e ottengo hash di data e le due public key
        // piu altri controlli
        return true;
    }


}