/*
 * this file work compitable with the LTEncoder.java
 * this can decode the source file out with enough 
 * data packages.
 */
package DaReCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import share.CodeHelper;
import share.GraphResolver;
import share.DaRePackage;

/**
 *
 * @author smile
 */
public class DaReDecoder {

    private long file_size;
    private short block_size;
    private short block_unitSIze;

    private byte[] R = {1, 2}; // Rate is denoted first is the up, second is the bottom.  first/second.
    private short W;
    private byte[] degree;
    
    
    private CodeHelper helper;
    private GraphResolver resolver;

    private final String file_name;
    
    private long block_num_for_restore_data = 0;
    private long cur_block_index =0;

    
    public int getRevolsedBlocks(){
    	return resolver.getResolvedBlocks();
    }
    
    
    public DaReDecoder(String filename) throws IOException {

        file_size = 0;
        block_size = 0;

        file_name = filename;
        createFileIfNotExist(filename);
    }

    /**
     *
     * @param in data transform stream
     * @throws java.io.IOException
     */
    public void decode(InputStream in) throws IOException {

    	
        int num_srcBlocks = R[0];
        int num_parityBlocks = R[1] - R[0];
        
        
        // read until we can decode the source file
        while (in.available() > 0) {

        	block_num_for_restore_data++;
        	
            DaRePackage current_package = readAPackage(in);

            
         // check whether the packet is received correctly first.! if the header is not correct, then will return null.
            if (null != current_package) {
            	
                // initialazition, 
            	//file_size: the total length(bytes) of all of data.  
            	//block_size: the number of bytes per block.
            	// The following -if- condition is used for the first time extraction of the necessary info for create the code helper about how the blocks are XORed.
                if (0 == file_size || 0 == block_size) {

                    file_size = current_package.getFileSize();
                    block_size = current_package.getBlockSize();                    
                    block_unitSIze = current_package.getBlockUnitSize();
                    
                    
                    
                    R = current_package.getRate();
                    W = current_package.getWindow();
                    degree = current_package.getDegree();
                    
                    
                    num_srcBlocks = R[0];
                    num_parityBlocks = R[1] - R[0];
                    
                    long k = (long) Math.ceil(file_size / block_unitSIze);
                    k = k * block_unitSIze >= file_size? k : k + 1;
                    
                    // create helper, reverse the procedure of encoder to create the code helper. According to the number of source blocks k.
                    helper = new CodeHelper(k, degree, R, W);
                    // create solver
                    resolver = new GraphResolver(k);
                }

                // get the block id.
                cur_block_index = current_package.getBlockId();
                
                // set the seed just follow the encoder to create the xored blocks into packets.
                helper.setSeed(current_package.getBlockSeed());                
                
                List<Set<Integer>> blockSrc = helper.getDataBlocks(cur_block_index, num_srcBlocks);

                // get the parity check blocks.
                List<Set<Integer>> blockParity = new ArrayList<Set<Integer>>();        
                for(int i=0 ; i< num_parityBlocks ; i++)
                {
                	Set<Integer> blocksSingleParity = helper.getParityBlocks( cur_block_index);
                	blockParity.add(blocksSingleParity);                	
                }     
                                
                // get the blocks index to be xored
//                Set<Integer> block_array = helper.getSrcBlocks();

                // try to decode. The resulted block data is the obtained block data.
//                byte[] block_data = current_package.getBlockData();

                List<byte[]> srcBlockData = current_package.getSrcBlockData();
                List<byte[]> parityBlockData = current_package.getParityBlockData();
                
                
                for(int i=0 ; i< blockSrc.size() ;i++)
                {
                    if (resolver.addBlock(blockSrc.get(i),srcBlockData.get(i) )) {
//                    	System.out.printf("needed block size: %d\n", block_num_for_restore_data);
                        break;
                    }
                }
                
                for(int i=0 ; i< blockParity.size();i++)
                {
                    if (resolver.addBlock(blockParity.get(i),parityBlockData.get(i) )) {
                    	
//                    	System.out.printf("needed block size: %d\n", block_num_for_restore_data);
                        break;
                    }
                }
                
                
                
                // handle. Periodically read the received XORed blocks until we can totally resolve all of the source blocks.
//                if (resolver.addBlock(block_array, block_data)) {
//                	System.out.printf("needed block size: %d\n", block_num_for_restore_data);
//                    break;
//                }
                
//                cur_block_index = cur_block_index + num_srcBlocks;
            }
        }

        // write currently restored data.
        // [@TODO] currently, resotre data is not so that important, we only accoding to the resolved blocks to determine the overhead.
//        if (0 != file_size || 0 != block_size) {
//            // get data from resolver
//            try (FileOutputStream fout = new FileOutputStream(file_name)) {
//
//                long index = 0;
//                int remain = (int) (file_size % block_size);
//                
//                long k = Math.round(file_size / block_size);
//                k = k * block_size >= file_size? k : k + 1;
//                
//                Iterator<byte[]> block_interator = resolver.iterator();
//                while (block_interator.hasNext()) {
//
//                    if (0 == remain || index++ < k - 1) {
//                        fout.write(block_interator.next());
//                    } else {
//                        fout.write(block_interator.next(), 0, remain);
//                    }
//                }
//            }
//            
//            int txBlocks = (int) (block_num_for_restore_data* R[1]);
//            int recBlocks = resolver.getResolvedBlocks();
//            
//            
////            System.out.printf("TxBlocks: %d,  RecBLocks:%d,  TO: %f \n", txBlocks, recBlocks, txBlocks*1.0/ recBlocks);
//        }
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
    private DaRePackage readAPackage(InputStream in) throws IOException {

        byte[] header = readPackageHeader(in);

        short size = getBlockSize(header);

        byte[] current_package = new byte[header.length + size];

        // copy header
        System.arraycopy(header, 0, current_package, 0, header.length);

        // copy data
        in.read(current_package, DaRePackage.BLOCK_DATA_OFFSET, size);

        return DaRePackage.fromBytes(current_package);
    }

    /**
     *
     * @return return the package header, this byte[] is define in LTpackage
     */
    private byte[] readPackageHeader(InputStream in) throws IOException {

        byte[] header = new byte[DaRePackage.DEFAULT_HEADER_SIZE];

        in.read(header, 0, header.length);

        return header;
    }

    /**
     *
     * @param bytes header byte[]
     * @return block size in this package
     */
    private short getBlockSize(byte[] bytes) {

        return (short) getNumberInByteArray(bytes, DaRePackage.BLOCK_SIZE_OFFSET,
                DaRePackage.BLOCK_SEED_OFFSET - DaRePackage.BLOCK_SIZE_OFFSET);
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
    	
    	DaReDecoder decoder = new DaReDecoder("D:\\AndroidW\\DaRe\\src\\DaReCode\\copy.txt");

    	FileInputStream inputStream  =new FileInputStream(new File("D:\\AndroidW\\DaRe\\src\\DaReCode\\channel.txt"));
    	decoder.decode(inputStream);

    	
    }
}
