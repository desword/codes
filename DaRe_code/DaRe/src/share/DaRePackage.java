/*
 * this class must be designed carefully and specifically.
 * I will use this class to serialize the pakcage into bytes array
 * which contains less than 110 bytes, so here, I will just calculate 
 * them when you apply this class into your own project, you should use 
 * the parameters you specific.
 * 
 * you can find more information about the object serialization here:
 * http://www.oracle.com/technetwork/articles/java/javaserial-1536170.html
 *
 * as the serialization may contain some basic object information, so
 * I need to implement my own package indeed.
 *
 */
package share;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author smile
 */
public class DaRePackage {

//    public final static short DEFAULT_HEADER_SIZE = 20;
    public final static short DEFAULT_HEADER_SIZE = 36;
    public final static short DEFAULT_BLOCK_SIZE = 80;

    public final static short CRC_DATA_OFFSET = 0;
    public final static short FILE_SIZE_OFFSET = 2;
    public final static short BLOCK_SIZE_OFFSET = 10;
    public final static short BLOCK_SEED_OFFSET = 12;
    
    public final static short BLOCK_RATE_OFFSET = 20;
    public final static short BLOCK_WINDOW_OFFSET = 22;
    public final static short BLOCK_DEGREE_OFFSET = 24;
    public final static short BLOCK_UNIT_OFFSET = 26;
    public final static short BLOCK_ID_OFFSET = 28;
    
    public final static short BLOCK_DATA_OFFSET = 36;

    private final long file_size;
    private final short block_size;
    private final long block_seed;
    
    private byte[] R ; // Rate is denoted first is the up, second is the bottom.  first/second.
    private short W ;
    private byte[] degree;
    private short block_unit_size;
    private long block_id;
    
    private final byte[] block_data;

    /*
     *  |   CRC 16  | file size | block size | seed    | data |
     *  |   2 bytes |  8 bytes  |   2 bytes  | 8 bytes | 80 bytes |
     *  |   short   |   long    |   short    |  long   | byte[] |
     *
     *
     */
    
    /*  
     *   new packet structure, CRC is only the packet header cRC.
     *  |   CRC 16  | file size | block size | seed    | rate R |  window size W | degree |   block unit size| pktID      |   data |
     *  |   2 bytes |  8 bytes  |   2 bytes  | 8 bytes | 2 bytes|    2 bytes     |  2 byts|     2 bytes      |  8 bytes   | block_size * R[1] bytes |
     *  |   short   |   long    |   short    |  long   | byte[2]|    short       |  byte[2]|       short     |	long	  |	byte[] |
     *
     *
     */
    public DaRePackage(long fsize, short bsize, long bseed, byte[] bdata) {
        file_size = fsize;
        block_size = bsize;
        block_seed = bseed;
        block_data = bdata.clone();
    }
    
    public DaRePackage(long fsize, short bsize, long bseed, byte[] bdata, byte[] R, short W, byte[] degree, short block_unit_size, long block_id) {
        file_size = fsize;
        block_size = bsize;
        block_seed = bseed;
        block_data = bdata.clone();
        
        this.R = R;
        this.W = W;
        this.degree = degree.clone();
        this.block_unit_size = block_unit_size;
        this.block_id = block_id;
    }

    public long getFileSize() {
        return file_size;
    }

    public short getBlockSize() {
        return block_size;
    }

    public long getBlockSeed() {
        return block_seed;
    }

    public byte[] getBlockData() {
        return block_data.clone();
    }
    
    public List<byte[]> getSrcBlockData(){
        int num_srcBlocks = R[0];
        int num_parityBlocks = R[1] - R[0];
        
        int inner_blockSize = block_data.length/R[1];
    	
        List<byte[]> srcBlockList = new ArrayList<byte[]>();
    	for(int i=0 ;i<num_srcBlocks ; i++)
    	{
    		byte[] block = new byte[inner_blockSize];
    		System.arraycopy(block_data, i*inner_blockSize, block, 0, inner_blockSize);
    		srcBlockList.add(block);
    	}
    	
    	return srcBlockList;
    }
    
    public List<byte[]> getParityBlockData(){
        int num_srcBlocks = R[0];
        int num_parityBlocks = R[1] - R[0];
        
        int inner_blockSize = block_data.length/R[1];
        
        List<byte[]> parityBlockList = new ArrayList<byte[]>();
    	for(int i=0 ;i<num_parityBlocks ; i++)
    	{
    		byte[] block = new byte[inner_blockSize];
    		System.arraycopy(block_data, (num_srcBlocks+i)*inner_blockSize, block, 0, inner_blockSize);
    		parityBlockList.add(block);
    	}
    	
    	return parityBlockList;
        
    }
    
    
    
    public byte[] getRate(){
    	return R.clone();
    }
    
    public short getWindow(){
    	return W;
    }
    
    public byte[] getDegree(){
    	return degree.clone();
    }
    
    public short getBlockUnitSize(){
    	return block_unit_size;
    }

    public long getBlockId(){
    	return block_id;
    }
    
    /**
     *
     * @param bytes receive bytes to restore the package
     * @return LTPackage
     */
    public static DaRePackage fromBytes(byte[] bytes) {

        return PackageRestore.restore(bytes);
    }

    /**
     *
     * @param builder package builder
     * @return current package bytes, send to other networks
     */
    public static byte[] toBytes(PackageBuilder builder) {

        return builder.build();
    }

    public static class PackageBuilder {

        private long file_size = 0L;
        private short block_size = DEFAULT_BLOCK_SIZE;
        private long block_seed = 4671836L;
        private byte[] $;
        
        private byte[] R ; // Rate is denoted first is the up, second is the bottom.  first/second.
        private short W ;
        private byte[] degree;
        private short block_unit_size;
        private long block_id;

        public void fileSize(long size) {
            file_size = size;
        }

        public void blockSize(short size) {
            block_size = size;
        }

        public void blockSeed(long seed) {
            block_seed = seed;
        }

        
        public void blockRate(byte[] iR){
        	R = iR.clone();
        }
        
        public void blockWindow(short iW){
        	W = iW;
        }
        
        public void blockDegree(byte[] idegree){
        	degree = idegree.clone();
        }
        
        
        public void blockUnitSize(short usize){
        	block_unit_size = usize;
        }
        
        public void blockId(long bId){
        	block_id = bId;
        }
        
        public void blockData(byte[] bytes) {

            // get memory first
            $ = new byte[DEFAULT_HEADER_SIZE + block_size];

            // set file size
            setNumberInByteArray($, file_size, FILE_SIZE_OFFSET,
                    BLOCK_SIZE_OFFSET - FILE_SIZE_OFFSET);
            // set block size
            setNumberInByteArray($, block_size, BLOCK_SIZE_OFFSET,
                    BLOCK_SEED_OFFSET - BLOCK_SIZE_OFFSET);
            // set block seed
            setNumberInByteArray($, block_seed, BLOCK_SEED_OFFSET,
            		BLOCK_RATE_OFFSET - BLOCK_SEED_OFFSET);
            
            // set the rate, window and degree.
            System.arraycopy(R, 0, $, BLOCK_RATE_OFFSET, R.length);
                        
            setNumberInByteArray($, W, BLOCK_WINDOW_OFFSET,
            		2);
            
            System.arraycopy(degree, 0, $, BLOCK_DEGREE_OFFSET, degree.length);
            
            
            setNumberInByteArray($, block_unit_size, BLOCK_UNIT_OFFSET,
            		2);
            
            setNumberInByteArray($, block_id, BLOCK_ID_OFFSET,
            		8);
            
            System.arraycopy(bytes, 0, $, BLOCK_DATA_OFFSET, block_size);
        }

        public void showInformation() {

            System.err.println("file size: " + file_size);
            System.err.println("block size: " + block_size);
            System.err.println("block seed: " + block_seed);

        }

        private void setCRC16() {

            byte[] package_bytes
                    = new byte[DEFAULT_HEADER_SIZE - FILE_SIZE_OFFSET];

            System.arraycopy($, FILE_SIZE_OFFSET, package_bytes, 0,
                    package_bytes.length);

            int crc16 = CRC16.getCRC16(package_bytes);

            setNumberInByteArray($, crc16, CRC_DATA_OFFSET,
                    FILE_SIZE_OFFSET - CRC_DATA_OFFSET);
        }

        private byte[] build() {
            setCRC16();
            return $.clone();
        }

        private void setNumberInByteArray(byte[] bytes,
                long number, int start, int len) {

            for (int shift = len - 1; shift >= 0; shift--) {
                long getter;
                getter = 0xFF;
                getter = (getter << shift * 8 & number) >> shift * 8;
                bytes[start++] = (byte) getter;
            }
        }
    }

    public static class PackageRestore {

        private static int getCRC16(byte[] bytes) {
            return (int) getNumberInByteArray(bytes, CRC_DATA_OFFSET,
                    FILE_SIZE_OFFSET - CRC_DATA_OFFSET);
        }

        private static long getFileSize(byte[] bytes) {
            return getNumberInByteArray(bytes, FILE_SIZE_OFFSET,
                    BLOCK_SIZE_OFFSET - FILE_SIZE_OFFSET);
        }

        private static short getBlockSize(byte[] bytes) {
            return (short) getNumberInByteArray(bytes, BLOCK_SIZE_OFFSET,
                    BLOCK_SEED_OFFSET - BLOCK_SIZE_OFFSET);
        }

        private static long getBlockSeed(byte[] bytes) {
            return getNumberInByteArray(bytes, BLOCK_SEED_OFFSET,
                    BLOCK_DATA_OFFSET - BLOCK_SEED_OFFSET);
        }

        
        // get the rate, w, and degree
        private static byte[] getRate(byte[] bytes){
        	
        	byte[] rate = new byte[BLOCK_WINDOW_OFFSET - BLOCK_RATE_OFFSET];
        	
        	System.arraycopy(bytes, BLOCK_RATE_OFFSET, rate, 0, BLOCK_WINDOW_OFFSET - BLOCK_RATE_OFFSET);
        	
        	return rate;
        }
        
        
        private static short getWindow(byte[] bytes){
        	return (short) getNumberInByteArray(bytes, BLOCK_WINDOW_OFFSET,
        			BLOCK_DEGREE_OFFSET - BLOCK_WINDOW_OFFSET);
        }
        
        private static byte[] getDegree(byte[] bytes){
        	byte[] degree = new byte[2];
        	
        	System.arraycopy(bytes, BLOCK_DEGREE_OFFSET, degree, 0, 2);
        	
        	return degree;      	
        	
        	
        }
        
        private static short getBlockUnitSize(byte[] bytes){
        	return (short) getNumberInByteArray(bytes, BLOCK_UNIT_OFFSET,
        			2);
        	
        }
        
        private static long getBlockId(byte[] bytes){
        	return getNumberInByteArray(bytes, BLOCK_ID_OFFSET,
        			8);
        }
        
        
        private static byte[] getBlockData(byte[] bytes, int len) {

            byte[] data = new byte[len];

            System.arraycopy(bytes, BLOCK_DATA_OFFSET, data, 0, len);

            return data;
        }

        private static boolean check(byte[] bytes, int size, int crc) {

            byte[] package_bytes = new byte[DEFAULT_HEADER_SIZE - FILE_SIZE_OFFSET];

            System.arraycopy(bytes, FILE_SIZE_OFFSET, package_bytes, 0,
                    package_bytes.length);

            return CRC16.getCRC16(package_bytes) == crc;

        }

        public static final DaRePackage restore(byte[] bytes) {

            int crc = getCRC16(bytes);

            long file_size = getFileSize(bytes);

            short block_size = getBlockSize(bytes);

            long block_seed = getBlockSeed(bytes);

            
            // get the rate, W, and degree
            byte[] R = getRate(bytes);
            short W = getWindow(bytes);
            byte[] degree = getDegree(bytes);           		
            short blockUnitSize = getBlockUnitSize(bytes);
            long blockId = getBlockId(bytes);
            		
            byte[] block_data = getBlockData(bytes, block_size);

            if (!check(bytes, block_size, crc)) {
                return null;
            }
            
            
            return new DaRePackage(file_size, block_size, block_seed, block_data, R, W, degree, blockUnitSize, blockId);
            // return redefine darepackage

//            return new DaRePackage(file_size, block_size, block_seed, block_data);
        }

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

    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {

        PackageBuilder builder = new PackageBuilder();

        builder.fileSize(542342);
        builder.blockSize((short) 2);
        builder.blockSeed(41234);
        builder.blockData(new byte[]{0x01, 0x02});

        byte[] pack = DaRePackage.toBytes(builder);

        DaRePackage pack_ = DaRePackage.fromBytes(pack);

        // check fails
        if (null == pack_) {
            return;
        }

        System.out.println("file size: " + pack_.getFileSize());
        System.out.println("block size:" + pack_.getBlockSize());
        System.out.println("block seed:" + pack_.getBlockSeed());
        byte[] data = pack_.getBlockData();
        System.out.println("block data:");
        for (byte b : data) {
            System.out.println("\t" + b);
        }
    }
}
