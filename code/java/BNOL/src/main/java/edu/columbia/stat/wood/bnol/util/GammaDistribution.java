/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol.util;

import org.apache.commons.math.special.Gamma;

/**
 *
 * @author nicholasbartlett
 *
 * Gamma distribution only for evaluation of the pdf.  We use the parameterization
 * with x^(alpha - 1) (exp(-x/beta)/(G(alpha)beta^alpha)).  The mean is alpha * beta
 * and the variance is alpha * beta^2.
 */
public class GammaDistribution {
    
    private double alpha, beta, logGammaAlpha;

    public GammaDistribution(double alpha, double beta){
        if(alpha <= 0.0 || beta <= 0.0){
            throw new IllegalArgumentException("alpha and beta parameters must be positive");
        }
        
        this.alpha = alpha;
        this.beta = beta;
        logGammaAlpha = Gamma.logGamma(alpha);
    }

    public double density(double x){
        return Math.pow(x, alpha - 1.0) * Math.exp(-x / beta) / (Math.exp(logGammaAlpha) * Math.pow(beta, alpha));
    }

    public double logDensity(double x){
        return (alpha - 1.0) * Math.log(x) - (x / beta) - logGammaAlpha - alpha * Math.log(beta);
    }

    public double proportionalToDensity(double x){
        return Math.pow(x, alpha - 1.0) * Math.exp(-x / beta);
    }

    public double logProportionalToDensity(double x){
        return (alpha - 1.0) * Math.log(x) - (x / beta);
    }
}
