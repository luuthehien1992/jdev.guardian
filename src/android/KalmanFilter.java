package jdev.guardian.core;

/**
 *
 * @author Jeremy
 */
public class KalmanFilter {

    private static final double Q = 1e-3;
    private static final double R = 1;
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
