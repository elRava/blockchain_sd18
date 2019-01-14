package poll;

import java.lang.reflect.MalformedParametersException;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class PollApplication {

    public static void main(String[] args) {

        final int DEFAULT_PORT_REG = 7867;
        ArrayList<InetSocketAddress> listReg = new ArrayList<>();
        String seatsPath = null;
        String keysPath = null;

        System.out.println("Poll Application Started");

        // read parameters from command line
        if (args.length % 2 != 0) {
            throw new MalformedParametersException();
        }

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-r")) {
                String reg = args[++i];
                String[] val = reg.split(":");
                if (val.length > 0) {
                    listReg.add(new InetSocketAddress(val[0], Integer.valueOf(val[1])));
                } else {
                    listReg.add(new InetSocketAddress(val[0], DEFAULT_PORT_REG));
                }
            } else if (args[i].equals("-s")) {
                seatsPath = args[++i];
            } else if (args[i].equals("-k")) {
                keysPath = args[++i];
            }
        }
        
    }

}