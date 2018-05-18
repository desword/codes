/*
 * this file work compitable with the LTEncoder.java
 * this can decode the source file out with enough 
 * data packages.
 */
package LTCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;
import share.CodeHelper;
import share.GraphResolver;
import share.LTPackage;

/**
 *
 * @author smile
 */
public class LTDecoder {

    private long file_size;
    private short block_size;

    private CodeHelper helper;
    private GraphResolver resolver;

    private final String file_name;
    
    private long block_num_for_restore_data = 0;
    
    private double C = 0.1;
    private double Delta = 0.5;

    public LTDecoder(String filename) throws IOException {

        file_size = 0;
        block_size = 0;

        file_name = filename;
        createFileIfNotExist(filename);
    }

    public void setC(double iC){
    	C = iC;
    }
    
    public void setDelta(double iDelta){
    	Delta = iDelta;
    }
    
    public long getNeededBlocks(){
    	return block_num_for_restore_data;
    }
    
    /**
     *
     * @param in data transform stream
     * @throws java.io.IOException
     */
    public void decode(InputStream in) throws IOException {

        // read until we can decode the source file
        while (in.available() > 0) {

        	block_num_for_restore_data++;
        	
            LTPackage current_package = readAPackage(in);

            if (null != current_package) {

                // initialazition, 
            	//file_size: the total length(bytes) of all of data.  
            	//block_size: the number of bytes per block.
            	// The following -if- condition is used for the first time extraction of the necessary info for create the code helper about how the blocks are XORed.
                if (0 == file_size || 0 == block_size) {

                    file_size = current_package.getFileSize();
                    block_size = current_package.getBlockSize();
                    
                    long k = (long) Math.ceil(file_size / block_size);
                    k = k * block_size >= file_size? k : k + 1;
                    
                    // create helper, reverse the procedure of encoder to create the code helper. According to the number of source blocks k.
                    helper = new CodeHelper(k, C, Delta);
                    // create solver
                    resolver = new GraphResolver(k);
                }

                // set the seed just follow the encoder to create the xored blocks into packets.
                helper.setSeed(current_package.getBlockSeed());
                // get the blocks index to be xored
                Set<Integer> block_array = helper.getSrcBlocks();

                // try to decode. The resulted block data is the obtained block data.
                byte[] block_data = current_package.getBlockData();

                // handle. Periodically read the received XORed blocks until we can totally resolve all of the source blocks.
                if (resolver.addBlock(block_array, block_data)) {
//                	System.out.printf("needed block size: %d\n", block_num_for_restore_data);
                    break;
                }
            }
        }

        if (0 != file_size || 0 != block_size) {
            // get data from resolver
            try (FileOutputStream fout = new FileOutputStream(file_name)) {

                long index = 0;
                int remain = (int) (file_size % block_size);
                
                long k = Math.round(file_size / block_size);
                k = k * block_size >= file_size? k : k + 1;
                
                Iterator<byte[]> block_interator = resolver.iterator();
                while (block_interator.hasNext()) {

                    if (0 == remain || index++ < k - 1) {
                        fout.write(block_interator.next());
                    } else {
                        fout.write(block_interator.next(), 0, remain);
                    }
                }
            }
        }
    }

    /**
     *
     * @param name the file to be checked
     */
    private void createFileIfNotExist(String name) throws IOException {

        File current_file = new File(name);

        if (!current_file.exists() && !current_file.isDirectory()) {
            current_file.createNewFile();
        }
    }

    /**
     *
     * @param in data transform stream
     * @return a package
     * @throws IOException
     */
    private LTPackage readAPackage(InputStream in) throws IOException {

        byte[] header = readPackageHeader(in);

        short size = getBlockSize(header);

        byte[] current_package = new byte[header.length + size];

        // copy header
        System.arraycopy(header, 0, current_package, 0, header.length);

        // copy data
        in.read(current_package, LTPackage.BLOCK_DATA_OFFSET, size);

        return LTPackage.fromBytes(current_package);
    }

    /**
     *
     * @return return the package header, this byte[] is define in LTpackage
     */
    private byte[] readPackageHeader(InputStream in) throws IOException {

        byte[] header = new byte[LTPackage.DEFAULT_HEADER_SIZE];

        in.read(header, 0, header.length);

        return header;
    }

    /**
     *
     * @param bytes header byte[]
     * @return block size in this package
     */
    private short getBlockSize(byte[] bytes) {

        return (short) getNumberInByteArray(bytes, LTPackage.BLOCK_SIZE_OFFSET,
                LTPackage.BLOCK_SEED_OFFSET - LTPackage.BLOCK_SIZE_OFFSET);
    }

    /**
     *
     * @param bytes package byte array
     * @param start offset of the value
     * @param len length of the value
     * @return
     */
    private static long getNumberInByteArray(byte[] bytes,
            int start, int len) {

        long number = 0L;

        for (int shift = len - 1; shift >= 0; shift--) {
            long current;
            current = bytes[start++] & 0xFF;    // here you must use & 0xff
            number |= current << shift * 8;
        }

        return number;
    }

    /**
     * @throws java.io.IOException
     * @brief test this module
     * @param args
     */
    public static void main(String[] args) throws IOException {
//      LTDecoder decoder = new LTDecoder("./copy.txt");
//        decoder.decode(System.in);
    	
    	LTDecoder decoder = new LTDecoder("D:\\AndroidW\\LT_code\\src\\LTCode\\copy.txt");

    	FileInputStream inputStream  =new FileInputStream(new File("D:\\AndroidW\\LT_code\\src\\LTCode\\channel.txt"));
    	decoder.decode(inputStream);
    	


    }
}
