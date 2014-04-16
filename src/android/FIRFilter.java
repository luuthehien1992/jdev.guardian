package jdev.guardian.core;

import java.util.List;

/**
 *
 * @author Jeremy
 */
public class FIRFilter {

    private static final double[] b
            = {
                3.69421067777138e-19,
                0.00132722289691100,
                0.00465025225065800,
                0.0117498026081420,
                0.0237792219561070,
                0.0407234511898748,
                0.0611569261162345,
                0.0824036741883591,
                0.101082211156672,
                0.113897378800498,
                0.118459717673086,
                0.113897378800498,
                0.101082211156672,
                0.0824036741883591,
                0.0611569261162345,
                0.0407234511898748,
                0.0237792219561070,
                0.0117498026081424,
                0.00465025225065795,
                0.00132722289691133,
                3.69421067777138e-19
            };

    public static double filter(List<Double> valueList, Double value) {
        valueList.add(value);

        double result = 0;
        int j = 0;
        int i = valueList.size() - 1;
        while (i >= 0) {
            result += b[j] * valueList.get(i);
            i--;
            j++;
        }

        valueList.remove(valueList.size() - 1);

        return result;
    }
}
