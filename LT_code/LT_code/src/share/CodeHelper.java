/*
 * this class help the decode and encode process to deal with random number
 */
package share;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author smile
 */
public class CodeHelper {

    public double DEFAULT_C = 0.001;
    public double DEFAULT_DELTA = 0.5;

    // members
    private final long K;
    private final PRNG random_generator;
    private final ArrayList<Double> cdf_set;
 
    
    
    public CodeHelper(long K) {

        this.K = K;
        this.random_generator = new PRNG();
        this.cdf_set = RobustSolitonDistribution.genRsd(K,
                DEFAULT_DELTA, DEFAULT_C);
    }

    public CodeHelper(long K, double delta, double c) {

        this.K = K;
        this.random_generator = new PRNG();
        this.cdf_set = RobustSolitonDistribution.genRsd(K, delta, c);
    }

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
    public Set<Integer> getSrcBlocks() {

        // the number generate use the state above
        int degree = getSampleDegree();

        int getton = 0;
        Set<Integer> blocks = new HashSet<>();
        while (getton < degree) {
            int number;

            number = (int) (random_generator.nextInt() % K);
            if (!blocks.contains(number)) {
                blocks.add(number);
                getton++;
            }
        }

        return blocks;
    }

    /**
     * @brief the degree relay on the K, delta, c, seed
     *
     * @return degree of the package
     */
    private int getSampleDegree() {

        double p = random_generator.getProbability();

        int index = 0;
        int size = cdf_set.size();

        while (index < size) {

            if (cdf_set.get(index) > p) {
                return index + 1;
            }

            index++;
        }

        return index + 1;
    }
}
