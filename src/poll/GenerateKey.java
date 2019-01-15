package poll;

import java.io.*;
import java.security.*;
import java.util.*;
import java.lang.reflect.MalformedParametersException;


import java.security.spec.*;

public class GenerateKey {

    public static void main(String[] args) {

        int numKey = 10;// default value

        if (args.length % 2 != 0) {
            throw new MalformedParametersException();
        }

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-n")) {
                numKey = Integer.valueOf(args[++i]);
            }
        }

        String path = "poll/key/key";
        File oneKey = null;
        PrintWriter printOne = null;

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
                KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
                // Inizializzo il key generator
                keygen.initialize(2048);
                KeyPair kp = keygen.genKeyPair();
                pub = kp.getPublic();
                pri = kp.getPrivate();

                oneKey = new File(path + "" + i + ".txt");
                printOne = new PrintWriter(oneKey);

                printOne.println(Base64.getEncoder().encodeToString(pub.getEncoded()));
                printOne.println(Base64.getEncoder().encodeToString(pri.getEncoded()));
                printOne.flush();

                complete.println(Base64.getEncoder().encodeToString(pub.getEncoded()));

                printOne.close();

             

            } catch (IOException io) {
                io.printStackTrace();

            } catch (NoSuchAlgorithmException nsae) {
                nsae.printStackTrace();
            }

        }
        complete.flush();
        complete.close();

        // Provo a leggere da file
        /*
        BufferedReader reader = null;
        String line = null;
        PublicKey pu = null;
        PrivateKey pr = null;
        try {
            reader = new BufferedReader(new FileReader(path + "1.txt"));
            line = reader.readLine();
            pu = getPub(line);
            System.out.println("Public " + Base64.getEncoder().encodeToString(pu.getEncoded()));
            line = reader.readLine();
            pr = getPri(line);
            System.out.println("Private " + Base64.getEncoder().encodeToString(pr.getEncoded()));
            reader.close();
        } catch (Exception fnfe) {
            fnfe.printStackTrace();
        }*/

    }

    public static PrivateKey getPrivateFromFile(String path) {

        BufferedReader reader = null;
        String filename = null;
        try{
            reader = new BufferedReader(new FileReader(path));
            reader.readLine();
            filename = reader.readLine();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
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

    public static PublicKey getPublicFromFile(String path)  {
        BufferedReader reader = null;
        String filename = null;
        try{
            reader = new BufferedReader(new FileReader(path));            
            filename = reader.readLine();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
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