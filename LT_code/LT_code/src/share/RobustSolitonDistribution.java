/*
 * @brief 
 *      calculate the rho, mu, cdf before the de/en coding process start
 */
package share;

import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author smile
 */
public class RobustSolitonDistribution {

    /**
     * rho(1) = 1 / K, d = 1 
     * rho(d) = 1 / d*(d-1) d = 2, 3, ..., K
     *
     * @param k -- number of source block
     * @return list of the rho value
     */
    private static ArrayList<Double> genRho(long k) {

        ArrayList<Double> rho_set = new ArrayList<>();

        // add d = 1
        rho_set.add(1.0 / k);
        // add d >= 2
        for (long d = 2; d <= k; d++) {
            rho_set.add(1.0 / (d - 1) * 1.0 / d);
        }

        return rho_set;
    }

    /**
     *
     * @param s s = c * ln( K / delta ) * sqrt( K )
     * @param k the number of source blocks
     * @param delta delta is a bound on the probability that the decoding fails
     * @return list of tau
     */
    private static ArrayList<Double> genTau(double s, long k, double delta) {

        ArrayList<Double> tau_set = new ArrayList<>();
        
        long pivot = (int) Math.floor(k / s);
        
        for(long d=1; d<=k; d++){
            if(d < pivot){
                tau_set.add( s / k * 1 / d);
            }else if(d == pivot){
                tau_set.add(s / k * Math.log(s / delta));
            }else{
                tau_set.add(0.0);
            }
        }

        return tau_set;
    }
    /**
     * 
     * @param list the list to calculate summation
     * @return the summation of the list
     */
    private static double getSumOfList(ArrayList<Double> list, long size){
        
        long counter = 0;
        double sum = 0.0;
        
        for(double item: list){
            if(size ==  counter){
                break;
            }
            sum += item;
            counter += 1;
        }
        
        return sum;
    }

    /**
     *
     * @param k the number of source blocks  
     * @param delta delta is a bound on the probability that the decoding fails
     * @param c c is a constant of order 1
     * @return list of mu 
     */
    private static ArrayList<Double> genMu(long k, double delta, double c) {

        ArrayList<Double> mu_set = new ArrayList<>();
        
        double s = c * Math.log(k / delta) * Math.sqrt(k);
        
        ArrayList<Double> rho_set = genRho(k);
        ArrayList<Double> tau_set = genTau(s, k, delta);
        
        double normalizer = getSumOfList(rho_set, rho_set.size()) + 
                            getSumOfList(tau_set, tau_set.size());
        
        Iterator<Double> rho = rho_set.iterator();
        Iterator<Double> tau = tau_set.iterator();
        
        while(rho.hasNext() && tau.hasNext()){
            mu_set.add((rho.next() + tau.next()) / normalizer);
        }
        
        // release the buffer
        rho_set = null;
        tau_set = null;
        
        return mu_set;
    }

    /**
     *
     * @param k number of source blocks
     * @param delta delta is a bound on the probability that the decoding fails
     * @param c c is a constant of order 1
     * @return list of rsd
     */
    public static ArrayList<Double> genRsd(long k, double delta, double c) {
        
        ArrayList<Double> rsd_set = new ArrayList<>();
        
        ArrayList<Double> mu_set = genMu(k, delta, c);
        
        for(long d=1; d<=k; d++){
            rsd_set.add(getSumOfList(mu_set, d));
        }

        // release the mu_set
        mu_set = null;
        
        return rsd_set;
    }
    
    /**
     * @brief test this module, this module test successful now
     * @param args 
     */
    public static void main(String args[]){
        
        double delta = 0.5;
        double c = 0.1;
        long k = 10003;
        
        ArrayList<Double> rsd = RobustSolitonDistribution.genRsd(k, delta, c);
        
        // here do not use java 8 stream 
        for(double s: rsd){
            System.out.println(s);
        }
    }
}
