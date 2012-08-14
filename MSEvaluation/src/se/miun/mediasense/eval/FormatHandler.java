package se.miun.mediasense.eval;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class FormatHandler {

	/*
     *  Format phone number by removing dashes and pluses
     *  
     *  @ param number	String number to format
     *  @ return		String formatted number 
     */
    public static String formatPhoneNumber(String number){
    	return number.replace("-", "").replace("+", "00");
    }
	
	public static double[] listsDoubleToArray(ArrayList<ArrayList<Double>> lists){
		int index = 0;
		double[] array = new double[lists.get(0).size()*4];
		for(ArrayList<Double> aList: lists){
			for(int i=index; i<aList.size(); index++)
				array[i] = aList.get(i);
		}
		return array;
	}

	public static double[] listDoubleToArray(ArrayList<Double> list){
		double[] levels = new double[list.size()];
		for(int i=0; i<list.size(); i++)
			levels[i] = list.get(i);
		return levels;
	}
	
	public static int[] listIntToArray(ArrayList<Integer> list){
		int[] levels = new int[list.size()];
		for(int i=0; i<list.size(); i++)
			levels[i] = list.get(i);
		return levels;
	}
	
	public static double getMinutesFromStart(double[] start){
		Calendar calendar = Calendar.getInstance(Locale.FRANCE);
		return ((double)calendar.get(Calendar.MINUTE)*60000+((double)calendar.get(Calendar.SECOND))*1000+((double)calendar.get(Calendar.MILLISECOND)))-
				(start[0]*60000+start[1]*1000+start[2]);
	}
}
