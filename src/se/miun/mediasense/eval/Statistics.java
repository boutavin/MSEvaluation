package se.miun.mediasense.eval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class Statistics {

	public static double mean(CopyOnWriteArrayList<Long> list){
		DescriptiveStatistics stats = new DescriptiveStatistics(extractDifferences(list));
		return stats.getMean();
	}

	public static double mean(double[] array){
		DescriptiveStatistics stats = new DescriptiveStatistics(array);
		return stats.getMean();
	}
	
	public static double standardDeviation(CopyOnWriteArrayList<Long> list){
		DescriptiveStatistics stats = new DescriptiveStatistics(extractDifferences(list));
		return stats.getStandardDeviation();
	}
	
	public static double standardDeviation(double[] array){
		DescriptiveStatistics stats = new DescriptiveStatistics(array);
		return stats.getStandardDeviation();
	}
	
	public static double[] extractDifferences(CopyOnWriteArrayList<Long> list){
		double[] array = new double[list.size()/2];
		int arrayIndex = 0;
		for(int i=1; i<list.size(); i+=2){
			array[arrayIndex] = list.get(i);
			arrayIndex++;
		}
		Arrays.sort(array);
		return keepDifferencesBetweenDeviationBounds(array);
	}
	
	// [10, 20, 400, 450, 470, 500, 657, 1000, 1200, 1400] mean:600 deviation:500
	// [400, 450, 470, 500, 657, 1000]
	
	private static double[] keepDifferencesBetweenDeviationBounds(double[] arrayToTrim){
		DescriptiveStatistics stats = new DescriptiveStatistics(arrayToTrim);
		double mean = stats.getMean();
		double dev = stats.getStandardDeviation();
		ArrayList<Double> trimmedList = new ArrayList<Double>();
		for(int i=0; i<arrayToTrim.length; i++){
			if(arrayToTrim[i] > (mean - dev) && arrayToTrim[i] < (mean + dev)){
				trimmedList.add(arrayToTrim[i]);
			}
		}
		double[] trimmedArray = new double[trimmedList.size()];
		for(int i=0; i<trimmedList.size(); i++)
			trimmedArray[i] = trimmedList.get(i);
//		DescriptiveStatistics stats2 = new DescriptiveStatistics(trimmedArray);
//		trimmedArray[trimmedList.size()] = stats2.getMean();
//		trimmedArray[trimmedList.size()+1] = stats2.getStandardDeviation();
		return trimmedArray;
	}
	
}
