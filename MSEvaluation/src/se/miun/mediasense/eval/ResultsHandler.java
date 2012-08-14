package se.miun.mediasense.eval;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class ResultsHandler {

	public static void writeResults(String fileName, int[] results){
		File root = Environment.getExternalStorageDirectory();
        File file = new File(root, fileName+".dat");
        try {
        	if (root.canWrite()){
                FileWriter filewriter = new FileWriter(file, false);
                BufferedWriter out = new BufferedWriter(filewriter);
                for (int i=0; i<results.length; i++)
            		out.write(results[i] + " ");
                Log.i("WRITE", "results for "+fileName+" saved into "+ file.getAbsolutePath());
                out.close();
            }
        } catch (IOException e) {
            Log.e("TAG", "Could not write file " + e.getMessage());
        }
	}
	
	public static void writeResults(String fileName, double[] results){
		File root = Environment.getExternalStorageDirectory();
        File file = new File(root, fileName+".dat");
        try {
        	if (root.canWrite()){
                FileWriter filewriter = new FileWriter(file, false);
                BufferedWriter out = new BufferedWriter(filewriter);
                for (int i=0; i<results.length; i++)
            		out.write(results[i] + " ");
                Log.i("WRITE", "results for "+fileName+" saved into "+ file.getAbsolutePath());
                out.close();
            }
        } catch (IOException e) {
            Log.e("TAG", "Could not write file " + e.getMessage());
        }
	}
	
	public static Intent getEmailIntentWithResults(String subject, String[] files){
		Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"alekspboutavin@aim.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, subject);
        ArrayList<Uri> uris = new ArrayList<Uri>();
        for(String file: files)
        	uris.add(Uri.fromFile(new File("/sdcard/"+file+"-"+subject+".dat")));
        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        return emailIntent;
	}
	
}
