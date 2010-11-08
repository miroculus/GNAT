package gnat.utils;

import java.util.List;

/**
 * Offers some mathematical functions.
 * */
public class MathHelper {


	/**
	 * Log(x) divided by ln2=0.693146d
	 * @param x
	 * @return log2
	 */
	public static double log2 (double x) {
		return Math.log(x) / 0.693146d;  // divided by ln2
	}


	/**
	 * Log(x) divided by ln10=2.3025d
	 * @param x
	 * @return log10
	 */
	public static double log10 (double x){
	  return Math.log(x) / 2.3025d;  // divided by ln10
	}


	/**
	 * Rounds a float to two digits after the decimal point
	 * @param number
	 * @return
	 */
	public static float round2 (float number) {
		number = number * 100.0f;
		int i = Math.round(number);
		return (float)i / 100.0f;
	}


	/**
	 * Returns the sigmoid: 1 / (1+Math.pow(2.718, -val))
	 * @param val
	 * @return sigmoid
	 */
	public static double sigmoid (float val){
	  return 1 / (1+Math.pow(2.718, -val));
	}


	/**
	 * Returns the tangens hyperbolicus.
	 * @param x
	 * @return tanh
	 */
	public static double tanh(float x){
	  double powBig = Math.pow(2.718,  x);
	  double powSml = Math.pow(2.718, -x);
	  return (powBig - powSml) / (powBig + powSml);
	}

	/***/
	public static double mean(List<Double> values){
		double mean = 0;
		for (Double value : values) {
	        mean += value;
        }
		return mean/(double)values.size();
	}

	/***/
	public static double variance(List<Double> values){
		double mean = mean(values);
		float variance = 0;
		for (Double value : values) {
	        variance += Math.pow(value-mean, 2);
        }
		return variance/(double)values.size();
	}

	/***/
	public static double variance(List<Integer> values, double mean, int totalValues){
		double variance = 0;
		for (Integer value : values) {
	        variance += Math.pow(value-mean, 2);
        }
		return variance/(double)totalValues;
	}


	/***/
	public static int sum(List<Integer> values){
		int sum = 0;
		for (Integer integer : values) {
			sum += integer;
        }
		return sum;
	}

	/***/
	public static double sum(double[] values){
		double sum = 0;
		for (double d : values) {
			sum += d;
        }
		return sum;
	}


	/***/
	public static double tValue(double controlMean, double testMean, double controlVariance, double testVariance, int testSamples, int controlSamples){
		double tValue = (testMean - controlMean) / Math.sqrt( (testVariance/(double)testSamples) + (controlVariance/(double)controlSamples) );
		if(Double.isInfinite(tValue)){
			System.out.println("MathHelper.tValue:  testMean="+testMean+", testVariance="+testVariance+", controlMean="+controlMean+", controlVariance="+controlVariance);
		}
		return tValue;
	}

}
