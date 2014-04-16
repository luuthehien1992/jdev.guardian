
package jdev.guardian.core;

/**
 *
 * @author Jeremy
 */
public class Calculator {

    public static double calculateLogD(double rssi, double A1, double B1) {
        
        return (1 / B1) * (rssi - A1);
    }

    public static double calculateDistance(double logD) {
        return Math.pow(10, logD);
    }
}
