/*
 * this class encode the source file block and continue send the package
 * out until the decoder decode the source file out.
 *
 * this file use the java 8 stream api, so you need to use the java version 8
 * if you want to use the lower version of java, just implement the same
 * logical.
 *
 */
package DaReCode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

import share.CodeHelper;
import share.DaRePackage;

/**
 *
 * @author smile
 */
public class DaReEncoder {

	// [issue] currently, we do not deal with the block that has the size lower than BLOCK_SIZE.
    private short BLOCK_SIZE = 40;

    private Map<Integer, byte[]> source_blocks;

    private long file_size;
    private long block_seed;

    private CodeHelper helper;
    
    //[ISSUE] currently, only K%R[1] == 0 case can correctly be decoded. Other case will not.
    private byte[] R = {1, 2}; // Rate is denoted first is the up, second is the bottom.  first/second. 
    private short W = 4;
    private byte[] degree = {3, 4};
//    private double degree = 0.75;

    
    private long cur_block_index;
    
    
    public byte[] getR(){
    	return R.clone();
    }
    
    public short getW(){
    	return W;
    }
    
    public byte[] getDegree(){
    	return degree.clone();
    }
    
    public short getBlockUnitSize(){
    	return BLOCK_SIZE;
    }
    
    public void setR(byte[] iR){
    	R = iR.clone();
    }
    
    public void setW(short iW){
    	W = iW;
    }
    
    public void setDegree(byte[] idegree){
    	degree = idegree.clone();
    }
    
    public void setBlockUniSize(short ibuSize){
    	BLOCK_SIZE = ibuSize;
    }
    
    
    public DaReEncoder(){}
    
    // default
    public DaReEncoder(String filename)
            throws IOException {

        source_blocks = spliteSourceFile(filename);

        long k = source_blocks.size();     
        
        this.cur_block_index = 0;
        
        helper = new CodeHelper(k, degree, R, W );
        
//        byte[] tem = source_blocks.get(k-1);
        System.out.printf("Num. of Source block:%d, with block length:%d. TOtal file length:%d\n", k,  BLOCK_SIZE, file_size );
        
    }

    public void initial(String filename) throws IOException {
        source_blocks = spliteSourceFile(filename);

        long k = source_blocks.size();     
        
        this.cur_block_index = 0;
        
        helper = new CodeHelper(k, degree, R, W );
        
//        byte[] tem = source_blocks.get(k-1);
        System.out.printf("Num. of Source block:%d, with block length:%d. TOtal file length:%d\n", k,  BLOCK_SIZE, file_size );
    }
    
    
    // with delta, c
//    public DaReEncoder(String filename, double delta, double c)
//            throws IOException {
//
//        source_blocks = spliteSourceFile(filename);
//
//        long k = source_blocks.size();
//
//        helper = new CodeHelper(k, delta, c);
//    }

    // with seed
//    public DaReEncoder(String filename, double delta, double c, long seed)
//            throws IOException {
//
//        source_blocks = spliteSourceFile(filename);
//
//        long k = source_blocks.size();
//
//        helper = new CodeHelper(k, delta, c);
//
//        helper.setSeed(seed);
//    }

    /**
     *
     * @return the package bytes, this bytes array will send in network
     */
    public byte[] getNextPackage() {    	
    	
        block_seed = helper.getSeed();
        
        int num_srcBlocks = R[0];
        int num_parityBlocks = R[1] - R[0];
        
        if(cur_block_index > source_blocks.size() - 1)// if the src blocks are encoded over, then the blocks are transmitted over.
        {
        	return null;
        }
        

        // get source block first, source block index actually.
        List<Set<Integer>> blockSrc = helper.getDataBlocks(cur_block_index, num_srcBlocks);

        // get the parity check blocks.
        List<Set<Integer>> blockParity = new ArrayList<Set<Integer>>();        
        for(int i=0 ; i< num_parityBlocks ; i++)
        {
        	Set<Integer> blocksSingleParity = helper.getParityBlocks( cur_block_index);
        	blockParity.add(blocksSingleParity);
        }     
                
        
        
        // Then we build all above into one package and send out.
        
        short total_block_unit_c = (short) (blockSrc.size() + blockParity.size());       
        short total_block_size = (short) (BLOCK_SIZE * total_block_unit_c);
        
        byte[] block_data = new byte[BLOCK_SIZE * total_block_unit_c];        
                
        int copy_block_index = 0;
        // copy the src data
        for( Set<Integer> bl: blockSrc)
        {
        	byte[] block_data_single = new byte[BLOCK_SIZE];
        	for(int b:bl)
        	{
        		block_data_single = xorOperation(
                        source_blocks.get(b), 
                        block_data_single,
                        BLOCK_SIZE);
        	}
        	System.arraycopy(block_data_single, 0, block_data, copy_block_index * BLOCK_SIZE, block_data_single.length);
        	copy_block_index++;
        }
        
        // copy the parity blocks
        for( Set<Integer> bl: blockParity)
        {
        	byte[] block_data_single = new byte[BLOCK_SIZE];
        	for(int b:bl)
        	{
        		block_data_single = xorOperation(
                        source_blocks.get(b), 
                        block_data_single,
                        BLOCK_SIZE);
        	}
        	System.arraycopy(block_data_single, 0, block_data, copy_block_index * BLOCK_SIZE, block_data_single.length);
        	copy_block_index++;
        }
        
        
        // build the package
        DaRePackage.PackageBuilder builder = new DaRePackage.PackageBuilder();

        builder.fileSize(file_size);
        builder.blockSize(total_block_size);
        builder.blockSeed(block_seed);
        
        builder.blockRate(R);
        builder.blockWindow(W);
        builder.blockDegree(degree);     
        builder.blockUnitSize(BLOCK_SIZE);
        builder.blockId(cur_block_index);
        
        builder.blockData(block_data);

        
        cur_block_index = cur_block_index + num_srcBlocks;// move to the next src block index based on the number of encoded src blocks
        
        
        return DaRePackage.toBytes(builder);
    }

    /**
     *
     * @param a byte[] operand, this parameter can be changed.
     * @param b byte[] operand, this parameter can be changed.
     * @param size the length of the byte [], this parameter can be changed.
     * @return the byte[] of length size
     */
    private byte[] xorOperation(
            final byte[] a,
            final byte[] b,
            final int size) {

        byte[] return_value = new byte[size];

        for (int index = 0; index < size; index++) {
            return_value[index] = (byte) (a[index] ^ b[index]);
        }

        return return_value;
    }

    /**
     *
     * @param file source file to send
     * @return block map contain the source file data
     */
    private Map<Integer, byte[]> spliteSourceFile(String filename)
            throws FileNotFoundException, IOException {

        Map<Integer, byte[]> blocks_map = new HashMap<>();

        // set the file size
        File file = new File(filename);

        file_size = file.length();

        // get the data block map
        int index = 0;
        int remain = 0;
        FileInputStream fin = new FileInputStream(file);

        while ((remain = fin.available()) > 0) {
            byte[] block = new byte[BLOCK_SIZE];
            if (remain >= BLOCK_SIZE) {
                fin.read(block, 0, BLOCK_SIZE);
                blocks_map.put(index, block);
            } else {
                fin.read(block, 0, remain);
                blocks_map.put(index, block);
                System.out.printf("The last block length is:%d\n", remain);
            }

            index++;
        }

        return blocks_map;
    }

    public void printBlock(byte[] pack){
    	
    	int lens = pack.length;
    	for (int i=0 ;i< lens ; i++)
    	{
    		System.out.printf("%x", pack[i]);
    	}
    }
    
    
    /**
     * @throws java.io.IOException
     * @brief test this module
     *
     * @param args
     */
    public static void main(String[] args) throws IOException {

//        if (args.length > 0) {
//
//            LTEncoder encoder = new LTEncoder(args[0]);            
//            
//            OutputStream out = System.out;
//
//            int index = 0;
//            while (index < 10000) {
//
//                byte[] pack;
//                pack = encoder.getNextPackage();
//                out.write(pack);
//                index++;
//            }
//        }
        
        DaReEncoder encoder = new DaReEncoder("D:\\AndroidW\\DaRe\\src\\DaReCode\\send.txt");
//        OutputStream out = System.out;

        FileOutputStream outputStream  =new FileOutputStream(new File("D:\\AndroidW\\DaRe\\src\\DaReCode\\channel.txt"));
//        outputStream.write(data);       
        
        
        int index = 0;
        while (index < 1000) {

            byte[] pack;
            pack = encoder.getNextPackage();
            if(pack == null)// if all of the src blocks have been consumed.
            {
            	break;
            }
            outputStream.write(pack);
            
            int pi = 0;
//            System.out.printf("\nblockSize:%d, b[%d]:%x\n", pack.length, pi, pack[pi]);
//            encoder.printBlock(pack);
            
//            pack[5] = 11;
//            System.out.printf("\nerror after\n");
//            encoder.printBlock(pack);
            index++;
        }
        outputStream.close();
        System.out.print("\nover");
        
    }
}
