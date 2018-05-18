/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lubytransformcode;

import LTCode.LTDecoder;
import LTCode.LTEncoder;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author smile
 */
public class LubyTransformCode {

    public static void generate(String filename) throws IOException {

        LTEncoder encoder = new LTEncoder(filename);

        OutputStream out = System.out;

        int index = 0;
        while (index < 20000) {

            byte[] pack;
            pack = encoder.getNextPackage();
            out.write(pack);
            index++;
        }
    }

    public static void decode(String filename) throws IOException {

        LTDecoder decoder = new LTDecoder(filename);

        decoder.decode(System.in);
    }

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {

        if (2 <= args.length) {
            if (null != args[0]) {
                switch (args[0]) {
                    case "encode":
                        LubyTransformCode.generate(args[1]);
                        break;
                    case "decode":
                        LubyTransformCode.decode(args[1]);
                        break;
                    default:
                        System.err.println("Usage: ");
                        System.err.println("    encode sourcefile encode the give file");
                        System.err.println("    decode savefile   decode the buffer");
                        break;
                }
            }
        }

    }

}
