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

/**
 *
 * @author smile
 */
public class LTPackage {

    public final static short DEFAULT_HEADER_SIZE = 20;
    public final static short DEFAULT_BLOCK_SIZE = 80;

    public final static short CRC_DATA_OFFSET = 0;
    public final static short FILE_SIZE_OFFSET = 2;
    public final static short BLOCK_SIZE_OFFSET = 10;
    public final static short BLOCK_SEED_OFFSET = 12;
    public final static short BLOCK_DATA_OFFSET = 20;

    private final long file_size;
    private final short block_size;
    private final long block_seed;
    private final byte[] block_data;

    /*
     *  |   CRC 16  | file size | block size | seed    | data |
     *  |   2 bytes |  8 bytes  |   2 bytes  | 8 bytes | 80 bytes |
     *  |   short   |   long    |   short    |  long   | byte[] |
     *
     *
     */
    public LTPackage(long fsize, short bsize, long bseed, byte[] bdata) {
        file_size = fsize;
        block_size = bsize;
        block_seed = bseed;
        block_data = bdata.clone();
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

    /**
     *
     * @param bytes receive bytes to restore the package
     * @return LTPackage
     */
    public static LTPackage fromBytes(byte[] bytes) {

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

        public void fileSize(long size) {
            file_size = size;
        }

        public void blockSize(short size) {
            block_size = size;
        }

        public void blockSeed(long seed) {
            block_seed = seed;
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
                    BLOCK_DATA_OFFSET - BLOCK_SEED_OFFSET);

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

        public static final LTPackage restore(byte[] bytes) {

            int crc = getCRC16(bytes);

            long file_size = getFileSize(bytes);

            short block_size = getBlockSize(bytes);

            long block_seed = getBlockSeed(bytes);

            byte[] block_data = getBlockData(bytes, block_size);

            if (!check(bytes, block_size, crc)) {
                return null;
            }

            return new LTPackage(file_size, block_size, block_seed, block_data);
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

        byte[] pack = LTPackage.toBytes(builder);

        LTPackage pack_ = LTPackage.fromBytes(pack);

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
