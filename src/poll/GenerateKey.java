package poll;

import java.io.*;
import java.security.*;
import java.util.*;
import java.lang.reflect.MalformedParametersException;
import java.security.spec.*;

/**
 * Application useful to generate the file with the key
 * Main generates a txt for each user with public and private key
 * It generates also a txt with a list with all the public key
 * @author Marco Sansoni
 * @version 1.0 first version
 */
public class GenerateKey {

    /**
     * Main for the application
     * @param args -n for the number of key to be generated
     */
    public static void main(String[] args) {

        int numKey = 10;//default value

        if (args.length % 2 != 0) {
            throw new MalformedParametersException();
        }

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-n")) {
                numKey = Integer.valueOf(args[++i]);
            }
        }

        //Path where store the txt file
        //printOne will be used for each txt with private and public key
        String path = "poll/key/key";
        File oneKey = null;
        PrintWriter printOne = null;

        //complete will be used only to print the txt with the whole public key
        String pathComplete = "poll/key/allPublic.txt";
        File fileComplete = new File(pathComplete);
        PrintWriter complete = null;

        try {
            complete = new PrintWriter(fileComplete);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }


        for (int i = 0; i < numKey; i++) {
            Key pub = null;
            Key pri = null;
            try {
                //It will be used to generate the key with rsa encryption
                KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
                
                //Initialize the key generator
                keygen.initialize(2048);
                KeyPair kp = keygen.genKeyPair();
                pub = kp.getPublic();
                pri = kp.getPrivate();

                //I ricompute the new path for the following txt
                oneKey = new File(path + "" + i + ".txt");
                printOne = new PrintWriter(oneKey);

                //print on the file public and private key, then i close the connection
                printOne.println(Base64.getEncoder().encodeToString(pub.getEncoded()));
                printOne.println(Base64.getEncoder().encodeToString(pri.getEncoded()));
                printOne.flush();

                //I add to txt the public key of the previous user
                complete.println(Base64.getEncoder().encodeToString(pub.getEncoded()));

                printOne.close();

            } catch (IOException io) {
                io.printStackTrace();
            } catch (NoSuchAlgorithmException nsae) {
                nsae.printStackTrace();
            }

        }
        //After the generation I close alse the complete printwriter
        complete.flush();
        complete.close();

    }

    /**
     * Static method used to return the private key stored as txt in a file
     * @param path the path of the file to be read
     * @return the private key read from the file
     */
    public static PrivateKey getPrivateFromFile(String path) {
        //filename will be store the private key in the string format
        BufferedReader reader = null;
        String filename = null;
        //Privetekey will be the second row on the file generated using main
        try{
            reader = new BufferedReader(new FileReader(path));
            reader.readLine();
            filename = reader.readLine();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
        //PrivateKey stored on a file is on PKCS8 Encoding
        //First I transform in a byte array the key
        //Using the encoder I store the private key in the privateKey format
        byte[] bytes = Base64.getDecoder().decode(filename);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(bytes);
        KeyFactory kf = null;
        PrivateKey pk = null;
        try{
            kf = KeyFactory.getInstance("RSA");
            pk = kf.generatePrivate(privateKeySpec);
        }catch(NoSuchAlgorithmException nsae){
            nsae.printStackTrace();
        }catch(InvalidKeySpecException ikse){
            ikse.printStackTrace();
        }      
        return pk;

    }

    /**
     * Static method used to return the public key stored as txt in a file
     * @param path the path of the file to be read
     * @return the public key read from the file
     */
    public static PublicKey getPublicFromFile(String path)  {
        //filename will be store the public key in the string format
        BufferedReader reader = null;
        String filename = null;
        //Public key will be on the first row on the file with the ket
        try{
            reader = new BufferedReader(new FileReader(path));            
            filename = reader.readLine();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
        //PublicKey stored on a file is on X509 Encoding
        //First I transform in a byte array the key
        //Using the encoder I store the public key in the publicKey format
        byte[] keyBytes = Base64.getDecoder().decode(filename);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = null;
        PublicKey pk = null;
        try{
            kf = KeyFactory.getInstance("RSA");
            pk = kf.generatePublic(spec);
        }catch(NoSuchAlgorithmException nsae){
            nsae.printStackTrace();
        }catch(InvalidKeySpecException ikse){
            ikse.printStackTrace();
        }
        return pk;
    }

}