/*
 * this class help the decode and encode process to deal with random number
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
public class CodeHelper {

    public final double DEFAULT_C = 0.001;
    public final double DEFAULT_DELTA = 0.5;

    // members
    private final long K;
    private final PRNG random_generator;
    private final byte[] degree;
    private final byte[] R;
    private final short W;
    private final long num_selected_bl_in_W; 
//    private final ArrayList<Double> cdf_set;

    public CodeHelper(long K, byte[] degree, byte[] R, short W) {

        this.K = K;
        this.random_generator = new PRNG();
        this.degree = degree;
        this.R= R.clone();
        this.W = W;
        this.num_selected_bl_in_W = (long) (W * degree[0] *1.0 /degree[1]);
        // select blocks randomly from the window size W, with the fixed degree. 
        
        
//        this.cdf_set = RobustSolitonDistribution.genRsd(K,
//                DEFAULT_DELTA, DEFAULT_C);
    }

//    public CodeHelper(long K, double delta, double c) {
//
//        this.K = K;
//        this.random_generator = new PRNG();
//        this.cdf_set = RobustSolitonDistribution.genRsd(K, delta, c);
//    }

    /**
     *
     * @param seed set the seed to random generator
     */
    public void setSeed(long seed) {

        random_generator.setSeed(seed);
    }

    /**
     *
     * @return the state of random generator
     */
    public long getSeed() {

        return random_generator.getState();
    }

    
    
    
    /**
     *
     * @brief this will generate the source block index of current package
     *
     * @return block array of source block index
     */
    public Set<Integer> getParityBlocks(long cur_block_index) {

        // the number generate use the state above
//        int degree = getSampleDegree();
    	
    	Set<Integer> blocks = new HashSet<>();
    	
        int getton = 0;
        long stBlockIndex = cur_block_index - W;
        long endBlockIndex = cur_block_index - 1;
    	
    	if(cur_block_index == 0	){
    		blocks.add((int) cur_block_index);
    		return blocks;
    	}
    	
    	if(endBlockIndex -0 + 1 <= num_selected_bl_in_W)// if the number of blocks within window is smaller than the targeted randomly selected blocks, then just add all of the blocks in W.
    	{	
    		for(int i=0 ; i< endBlockIndex+1 ;i++)
    		{
    			blocks.add(i);
    		}
    	}
    	else
    	{
    		for(int i=0 ; i< num_selected_bl_in_W ;)
    		{
                int number;

                if(stBlockIndex < 0){
                	stBlockIndex = 0;
                }
                
                
                number = (int) ((int) (random_generator.nextInt() % W) + stBlockIndex);
                if (!blocks.contains(number)) {
                    blocks.add(number);
                    i++;
                }
    		}
    	}

        return blocks;
    }

    
    public List<Set<Integer>> getDataBlocks(long cur_block_index, int num_srcBlocks) {
    	
    	List<Set<Integer>> blocks = new ArrayList<Set<Integer>>();   	
    	
    	for (long i=cur_block_index ;i<cur_block_index + num_srcBlocks ; i++)
    	{
    		if(i > K - 1)// if the src blocks are consumed.
    		{
    			break;
    		}
    		Set<Integer> block = new HashSet<Integer>();    		
    		block.add((int) i);
    		blocks.add(block);
    	}
    	return blocks;    	
    	
    }
    
    
    
    
    
    /**
     * @brief the degree relay on the K, delta, c, seed
     *
     * @return degree of the package
     */
//    private int getSampleDegree() {
//
//        double p = random_generator.getProbability();
//
//        int index = 0;
//        int size = cdf_set.size();
//
//        while (index < size) {
//
//            if (cdf_set.get(index) > p) {
//                return index + 1;
//            }
//
//            index++;
//        }
//
//        return index + 1;
//    }
}
