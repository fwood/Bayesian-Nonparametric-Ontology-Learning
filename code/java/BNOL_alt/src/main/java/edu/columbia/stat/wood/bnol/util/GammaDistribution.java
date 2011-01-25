/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol.util;

import java.io.Serializable;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.GammaDistributionImpl;
import org.apache.commons.math.special.Gamma;

/**
 *
 * @author nicholasbartlett
 *
 * Gamma distribution only for evaluation of the pdf.  We use the parameterization
 * with x^(alpha - 1) (exp(-x/beta)/(G(alpha)beta^alpha)).  The mean is alpha * beta
 * and the variance is alpha * beta^2.
 */
public class GammaDistribution implements Serializable{

    static final long serialVersionUID = 1 ;
    
    private double alpha, beta, logGammaAlpha;
    private GammaDistributionImpl gammaDist;

    /**
     * Constructor with shape and scale parameters
     * @param alpha shape parameter
     * @param beta scale parameter
     */
    public GammaDistribution(double alpha, double beta){
        if(alpha <= 0.0 || beta <= 0.0){
            throw new IllegalArgumentException("alpha and beta parameters must be positive");
        }
        
        this.alpha = alpha;
        this.beta = beta;
        logGammaAlpha = Gamma.logGamma(alpha);
        gammaDist = new GammaDistributionImpl(alpha, beta);
    }

    /**
     * Get the density at given value.
     * @param x value at which to find density
     * @return density value at x
     */
    public double density(double x){
        return Math.pow(x, alpha - 1.0) * Math.exp(-x / beta) / (Math.exp(logGammaAlpha) * Math.pow(beta, alpha));
    }

    /**
     * Get the logarithm of the density at given value.
     * @param x value at which to find logarithm of density
     * @return logarithm of density at x
     */
    public double logDensity(double x){
        return (alpha - 1.0) * Math.log(x) - (x / beta) - logGammaAlpha - alpha * Math.log(beta);
    }

    /**
     * Get a value which is proportional to the density with respect to x at x.
     * @param x value at which to get the proportional density
     * @return value proportional to density at x
     */
    public double proportionalToDensity(double x){
        return Math.pow(x, alpha - 1.0) * Math.exp(-x / beta);
    }

    /**
     * Get a value off by an additive constant from the log density at x.
     * @param x value at which to get the log density off by an additive constant
     * @return log density off by an additive constant
     */
    public double logProportionalToDensity(double x){
        return (alpha - 1.0) * Math.log(x) - (x / beta);
    }

    /**
     * Generate a random value from this distribution.
     * @param rng random number generator
     * @return random value from this distribution
     */
    public double generate(MersenneTwisterFast rng){
        try {
            return gammaDist.inverseCumulativeProbability(rng.nextDouble());
        } catch (MathException e){
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
        
        return -1d;
    }
}
