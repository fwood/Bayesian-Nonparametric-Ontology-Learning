/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.stat.wood.hdp;

import java.io.Serializable;
import java.util.Random;

/**
 * @author nicholasbartlett
 */
public class RND implements Serializable {

    private static Random rng;
    private static LogSterlingNumber lsn = new LogSterlingNumber();

    /***************static methods*********************************************/

    /**
     * Sets the random number generator underlying the methods of this class.
     * @param rng random number generator
     */
    public static final void setRNG(Random rng) {
        RND.rng = rng;
    }

    /**
     * Gets a random sample from a Gamma distribution with the given parameters.
     * @param alpha shape parameter
     * @param beta scale parameter
     * @return random sample
     */
    public static final double sampleGamma(double alpha, double beta) {
        if(alpha <= 0d || beta <= 0d){
            throw new IllegalArgumentException("Shape and scale must be positive");
        }
        
        boolean accept = false;
        if (alpha < 1) {
            // Weibull algorithm
            double c = (1 / alpha);
            double d = ((1 - alpha) * Math.pow(alpha, (alpha / (1 - alpha))));
            double u, v, z, e, x;
            do {
                u = rng.nextDouble();
                v = rng.nextDouble();
                z = -Math.log(u);
                e = -Math.log(v);
                x = Math.pow(z, c);
                if ((z + e) >= (d + x)) {
                    accept = true;
                }
            } while (!accept);
            return (x * beta);
        } else {
            // Cheng's algorithm
            double b = (alpha - Math.log(4));
            double c = (alpha + Math.sqrt(2 * alpha - 1));
            double lam = Math.sqrt(2 * alpha - 1);
            double cheng = (1 + Math.log(4.5));
            double u, v, x, y, z, r;
            do {
                u = rng.nextDouble();
                v = rng.nextDouble();
                y = ((1 / lam) * Math.log(v / (1 - v)));
                x = (alpha * Math.exp(y));
                z = (u * v * v);
                r = (b + (c * y) - x);
                if ((r >= ((4.5 * z) - cheng))
                        || (r >= Math.log(z))) {
                    accept = true;
                }
            } while (!accept);
            return (x * beta);
        }
    }

    /**
     * Samples from a Dirichlet distribution with given parameters.
     * @param parameters parameters of Dirichlet distribution
     * @return random sample from Dirichlet distribution
     */
    public static final double[] sampleDirichlet(double[] parameters){
        double[] sample = new double[parameters.length];

        double sum = 0d, gamrnd;
        int index = 0;
        for(double param : parameters){
            if (param > 0d) {
                gamrnd = sampleGamma(param, 1d);
                sum += gamrnd;
            } else {
                gamrnd = Double.NEGATIVE_INFINITY;
            }
            sample[index++] = gamrnd;
        }

        index = 0;
        for (double samp : sample){
            sample[index++] = samp / sum;
        }

        return sample;
    }

    /**
     * Draws a sample from a Beta distribution.
     * @param alpha first parameter
     * @param beta second parameter
     * @return random sample
     */
    public static final double sampleBeta(double alpha, double beta){
        double rndAlpha = sampleGamma(alpha, 1d);
        return rndAlpha / (rndAlpha + sampleGamma(beta, 1d));
    }

    /**
     * Samples a multinomial, which is an indices in [0, weights.length)
     * @param weights weights of multinomial
     * @return sample
     */
    public static final int sampleMultinomial(double[] weights){
        double totalWeight = 0d;
        for (double weight : weights){
            if (weight < 0d) {
                throw new IllegalArgumentException("all weights must be non-negative");
            } else {
                totalWeight += weight;
            }
        }

        double cuSum = 0d;
        double randomNumber = rng.nextDouble();

        for(int i = 0; i < weights.length; i++){
            cuSum += weights[i] / totalWeight;
            if (cuSum > randomNumber) {
                return i;
            }
        }

        throw new RuntimeException("should never get to here, YIKES!");
    }

    /**
     * Assumes all weights are positive and sum to 1.
     * @param weights weights of distribution
     * @return indices of sample
     */
    public static final int sampleMultinomialFast(double[] weights){
        double cuSum = 0d;
        double randomNumber = rng.nextDouble();

        for(int i = 0; i < weights.length; i++){
            cuSum += weights[i];
            if (cuSum > randomNumber) {
                return i;
            }
        }

        throw new RuntimeException("should never get to here, YIKES!");
    }

    public static double logDirichletLikelihood(double[] x, double[] alpha) {
        if (x.length != alpha.length) {
            throw new IllegalArgumentException("Args must have same length");
        } else {
            double logLik = 0d;
            double sum = 0d, a, l = x.length;

            for (int i = 0; i < l; i++){
                a = alpha[i];

                assert a > 0 : "a was instead" + a;

                sum += a;
                logLik -= logGamma(a);
                logLik += (a - 1d) * Math.log(x[i]);
            }

            logLik += logGamma(sum);
            
            return logLik;
        }
    }

    public static double logGamma(double x) {
        double tmp = (x - 0.5) * Math.log(x + 4.5) - (x + 4.5);
        double ser = 1.0 + 76.18009173 / (x + 0) - 86.50532033 / (x + 1)
                + 24.01409822 / (x + 2) - 1.231739516 / (x + 3)
                + 0.00120858003 / (x + 4) - 0.00000536382 / (x + 5);
        return tmp + Math.log(ser * Math.sqrt(2 * Math.PI));
    }

    private static double gamma(double x) {
        return Math.exp(logGamma(x));
    }

    public static double logGammaLikelihood(double x, double alpha, double beta) {
        double logLik = 0d;
        logLik -= logGamma(alpha);
        logLik -= alpha * Math.log(beta);
        logLik += (alpha - 1d) * Math.log(x);
        logLik -= x / beta;
        return logLik;
    }

    /**
     * Samples the number of tables given the number of customers in a dirichlet
     * process distribution.
     * @param n number of customers
     * @param alpha concentration parameter
     * @param beta base distribution probability of type
     * @return number of tables
     */
    public static int sampleDirichletProcessTables(int n, double alpha, double beta) {

        double c0 = logGamma(alpha * beta) - logGamma(alpha * beta + (double) n);
        double c1 = Math.log(alpha) + Math.log(beta);

        double cuSum = 0;
        double r = rng.nextDouble();

        int m = 0;
        for (int i = 1; i <= n; i++) {
            cuSum += Math.exp(c0 + lsn.get(n, i) + i * c1);
            if (cuSum > r) {
                m = i;
                break;
            }
        }

        assert m > 0;

        return m;
    }

    /**
     * Gets the log probability of the given number of tables in the Dirichlet
     * process distribution.
     * @param m tables
     * @param n count
     * @param alpha concentration
     * @param beta probability of type in base distribution
     * @return score
     */
    public static double logProbTablesDirichletProcess(int m, int n, double alpha, double beta) {
        double c0 = logGamma(alpha * beta) - logGamma(alpha * beta + (double) n);
        double c1 = Math.log(alpha) + Math.log(beta);

        return c0 + lsn.get(n, m) + m * c1;
    }
}