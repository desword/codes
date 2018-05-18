/*
 * @brief
 *      this class will generate the random number for the LT de/en coder
 */
package share;

/**
 *
 * @author smile
 */
public class PRNG {

    private final static int PRNG_A = 16807;
    private final static int PRNG_M = (1 << 31) - 1;
    private final static int PRNG_MAX_RAND = PRNG_M - 1;

    private long state;

    /**
     * @brief make the state a random number
     */
    public PRNG() {

        state = 2067261;
    }

    /**
     *
     * @param seed state to set
     */
    public void setSeed(long seed) {
        
        this.state = seed;
    }

    /**
     *
     * @return current state
     */
    public long getState() {
        
        return state;
    }
    
    /**
     *
     * @return random next integer number
     */
    public int nextInt() {

        state = state * PRNG_A % PRNG_M;
        return (int) state;
    }

    public double getProbability() {

        // the probability 
        return nextInt() / (PRNG_MAX_RAND * 1.0);
    }

    /**
     * @param args
     * @brief test the pseudo random number generation
     */
    public static void main(String args[]) {

        PRNG prng = new PRNG();

        for (int i = 0; i < 10; i++) {
            System.out.println(prng.nextInt());
        }
    }
}
