package se.miun.mediasense.eval;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

import se.miun.mediasense.addinlayer.AddInManager;
import se.miun.mediasense.addinlayer.extensions.publishsubscribe.PublishSubscribeExtension;
import se.miun.mediasense.addinlayer.extensions.publishsubscribe.SubscriptionEventListener;
import se.miun.mediasense.disseminationlayer.communication.CommunicationInterface;
import se.miun.mediasense.disseminationlayer.disseminationcore.DisseminationCore;
import se.miun.mediasense.disseminationlayer.disseminationcore.GetEventListener;
import se.miun.mediasense.disseminationlayer.disseminationcore.GetResponseListener;
import se.miun.mediasense.disseminationlayer.disseminationcore.ResolveResponseListener;
import se.miun.mediasense.disseminationlayer.lookupservice.LookupServiceInterface;
import se.miun.mediasense.interfacelayer.MediaSensePlatform;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ResponseDelayActivity extends Activity implements OnClickListener, GetEventListener, ResolveResponseListener, GetResponseListener, OnSharedPreferenceChangeListener, SubscriptionEventListener {
    
	private TextView txtName, txtNumber; // Name of the user, Phone number of the user
	private TextView ctName, ctNumber, resultEval; // Name of the contact, Phone number of the contact, Status of the contact, Results of evaluation
	private Button button;
	private SharedPreferences prefs;
	
	private String username;
	
	private Handler handler; 
	private ResolveThread resolveThread;
	private int delay = 0;
	private CopyOnWriteArrayList<Long> evalList;
	private boolean clicked = false;
	
	// MediaSense Platform Application Interfaces
	private MediaSensePlatform platform;
	private DisseminationCore core;
	private PublishSubscribeExtension pse;
    //////////////////////////////////////////
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.resp);
        
        // Create components
        txtName = (TextView) findViewById(R.id.headerName); 
        txtNumber = (TextView) findViewById(R.id.headerNumber);
        ctName = (TextView) findViewById(R.id.contactName); 
        ctNumber = (TextView) findViewById(R.id.contactNumber);
        resultEval = (TextView) findViewById(R.id.resultEval);
        resultEval.setMovementMethod(new ScrollingMovementMethod());
        button = (Button) findViewById(R.id.resp_StopButton);
        button.setOnClickListener(this);
        ///////////////////////////////////////
        
        // Header initialization from Preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        username = prefs.getString("user_name", "Alexandre");
        txtName.setText(username);
        txtNumber.setText(prefs.getString("user_number", "0703612088"));
        ctName.setText(prefs.getString("contact_name", "Johan"));
        ctNumber.setText(prefs.getString("contact_number", "0768248156"));
        ///////////////////////////////////////
        
        // MediaSense Platform initialization
        platform = new MediaSensePlatform(); // Create the platform itself
        // Initialize the platform with chosen LookupService type and chosen Communication type. 
        platform.initalize(LookupServiceInterface.SERVER, CommunicationInterface.TCP); // For Server Lookup and TCP P2P communication
        core = platform.getDisseminationCore(); // Extract the core for accessing the primitive functions
        core.setGetEventListener(this); // Set the event listeners
        core.setResolveResponseListener(this); // Set the response listeners
        core.setGetResponseListener(this);
        AddInManager addInManager = platform.getAddInManager();
        pse = new PublishSubscribeExtension();
        addInManager.loadAddIn(pse);
        pse.setSubscriptionEventListener(this);
        //////////////////////////////////////
        
//        if(username.equalsIgnoreCase("Alexandre")){
        	// Create and start a Thread to register the user's status to MediaSense
            Thread registerThread = new Thread(new Runnable() {
    			@Override
    			public void run() {
    				core.register(FormatHandler.formatPhoneNumber(txtNumber.getText().toString())+"@evaluation.se/respDelay");
    			}
    		});
            registerThread.start();
            /////////////////////////////////////
//        }
        
        
        
        // Start performing resolve calls
        evalList = new CopyOnWriteArrayList<Long>(); // Create evaluation ArrayList do foster, times and time differences to perform some statistics later
        handler = new Handler(); // Create Handler to hold a queue of threads (ResolveThread)
        delay = 100; // set delay 
        resolveThread = new ResolveThread();
        handler.postDelayed(resolveThread, 100); // Run a ResolveThread after 100 milliseconds
        /////////////////////////////////////
        	
    }
    
    /*
     * Perform resolve call thanks to an asynchronous task each 'delay' 
     */
    private class ResolveThread implements Runnable {
    	@Override
    	public void run() {
    		String uci = ctNumber.getText().toString()+"@evaluation.se/respDelay";
			core.resolve(uci); // Resolve/get call to the correct UCI
    		if(evalList.size() < 500)
    			handler.postAtTime(this, SystemClock.uptimeMillis()+delay); // A ResolveThread is added to the Handler's queue with a specific delay
    		else
    			button.performClick();
    	}
    }
    
    /*
     * When the 'Stop' button is clicked, all callbacks from the ResolveThread are removed, so that no other resolve call is made.
     * Display the evaulation list
     */
	@Override
	public void onClick(View v) {
		handler.removeCallbacks(resolveThread);
		resultEval.setText(evalList.toString());
		clicked = true;
		double[] array = Statistics.extractDifferences(evalList);
		resultEval.append("\nDiff:"+Arrays.toString(array)+
				"\nMean:"+Statistics.mean(evalList)+
				"\nDeviation:"+Statistics.standardDeviation(evalList)+
				"\nMeanFromArray:"+Statistics.mean(array)+
				"\nDeviationFromArray:"+Statistics.standardDeviation(array));
		writeResults(array);
        sendEmailWithResults();
		performToast("Evaluation completed");
	}
	
	private void writeResults(double [] results){
		File root = Environment.getExternalStorageDirectory();
        File file = new File(root, "data.dat");
        try {
        	if (root.canWrite()){
                FileWriter filewriter = new FileWriter(file, false);
                BufferedWriter out = new BufferedWriter(filewriter);
                for (int i=0; i<results.length; i++)
            		out.write(results[i] + " ");
                Log.i("WRITE", "results saved into "+ file.getAbsolutePath());
                out.close();
            }
        } catch (IOException e) {
            Log.e("TAG", "Could not write file " + e.getMessage());
        }
	}
	
	private void sendEmailWithResults(){
		Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"alekspboutavin@aim.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "data");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "data");
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///sdcard/data.dat"));
        startActivity(emailIntent);
	}
	
	/*
	 * When the response from the get call is received, add difference (in milliseconds) between the received time and the last time added into the ArrayList
	 */
	@Override
	public void getResponse(String uci, String _value) {
		final long answerDelay = new Date().getTime()-evalList.get(evalList.size()-1);
		evalList.add(answerDelay);
		Log.i("GET-RESPONSE", ""+evalList.size());
		if(evalList.size() < 2000 && !clicked)
			handler.postAtTime(new ResolveThread(), SystemClock.uptimeMillis()); // A ResolveThread is added to the Handler's queue with a specific delay
		else{
			handler.removeCallbacks(resolveThread);
			performToast("Evaluation completed");
			double[] array = Statistics.extractDifferences(evalList);
			writeResults(array);
	        sendEmailWithResults();
		}
	}

	// Perform GET call
	@Override
	public void resolveResponse(String uci, String ip) {
		if(!ip.equalsIgnoreCase("null")){
			core.get(uci, ip);
			if(evalList.size()%2 == 0) // Save current time to the ArrayList only if first or difference added earlier  
				evalList.add(new Date().getTime());
		} else{
			evalList.add(0L);
		}
	}
	
	// When a GET request is received --> send back the count value to the source of the call
	@Override
	public void getEvent(String source, String uci) {
		platform.getDisseminationCore().notify(source, uci, "");
		Log.i("GET-EVENT", "received get call");
	}
	
	/* 
	 * Perform Toast on UI thread
	 *
	 * @param _text		String to be display in a Toast
	 * @see				Toast with text 			
	 */
	private void performToast(String _text){
		final String text = _text;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(ResponseDelayActivity.this, text, Toast.LENGTH_SHORT).show();
			}
		});
	}

	/*
	 * When SharedPreferences has changed, update the TextViews with right names and numbers
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if(key.equalsIgnoreCase("user_name"))
    		txtName.setText(sharedPreferences.getString("user_name", "Alex"));
    	if(key.equalsIgnoreCase("user_number"))
    		txtNumber.setText(FormatHandler.formatPhoneNumber(sharedPreferences.getString("user_number", "070XXXXXXX")));
    	if(key.equalsIgnoreCase("contact_name"))
    		ctName.setText(sharedPreferences.getString("contact_name", "Johan"));
    	if(key.equalsIgnoreCase("contact_number"))
    		ctNumber.setText(FormatHandler.formatPhoneNumber(sharedPreferences.getString("contact_number", "0768238156")));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 1, 1, "Settings").setIcon(android.R.drawable.ic_menu_preferences); // Settings
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
			case 1: // Settings selected
				startActivity(new Intent(getApplicationContext(), Preference.class));
				break;
		}
		return true;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		core.shutdown();
		platform.shutdown();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// MediaSense Platform initialization
        platform = new MediaSensePlatform(); // Create the platform itself
        // Initialize the platform with chosen LookupService type and chosen Communication type. 
        platform.initalize(LookupServiceInterface.SERVER, CommunicationInterface.TCP); // For Server Lookup and TCP P2P communication
        core = platform.getDisseminationCore(); // Extract the core for accessing the primitive functions
        core.setGetEventListener(this); // Set the event listeners
        core.setResolveResponseListener(this); // Set the response listeners
        core.setGetResponseListener(this);
        //////////////////////////////////////
	}

	@Override
	public void subscriptionEvent(String uci, String value) {
		
	}
}