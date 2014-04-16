package jdev.guardian.core;

/**
 *
 * @author Jeremy
 */
public class KalmanFilter {

    private static final double Q = 1e-5;
    private static final double R = 2;
    private static double xpre = 0;
    private static double Ppre = 1;

    public static double filter(double logD) {
        double K = Ppre * (1 / (Ppre + R));
        xpre = xpre + K * (logD - xpre);
        double P = (1 - K) * Ppre;
        Ppre = P + Q;

        return xpre;
    }
}
