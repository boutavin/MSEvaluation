package se.miun.mediasense.eval;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import se.miun.mediasense.addinlayer.extensions.publishsubscribe.PublishSubscribeExtension;
import se.miun.mediasense.addinlayer.extensions.publishsubscribe.SubscriptionEventListener;
import se.miun.mediasense.disseminationlayer.disseminationcore.DisseminationCore;
import se.miun.mediasense.disseminationlayer.disseminationcore.GetEventListener;
import se.miun.mediasense.disseminationlayer.disseminationcore.GetResponseListener;
import se.miun.mediasense.disseminationlayer.disseminationcore.ResolveResponseListener;
import se.miun.mediasense.interfacelayer.MediaSensePlatform;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.TrafficStats;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class DataTrafficActivity extends Activity implements 
OnClickListener, GetEventListener, ResolveResponseListener, GetResponseListener, 
OnSharedPreferenceChangeListener, SubscriptionEventListener, OnItemSelectedListener, SensorEventListener {
    
	// UI Components
	private TextView txtName, txtNumber; // Name of the user, Phone number of the user, Status of the user
	private TextView ctName, ctNumber, resultEval; // Name of the contact, Phone number of the contact, Status of the contact, Results of evaluation
	private Button buttonStop, buttonSub;
	private Spinner spinnerNotifDelay;
	private SharedPreferences prefs;
	//////////////////////////////////////////
	
	// Threads
	private Handler handler; 
	private boolean hasSubscribers = false, accRegistered = false;
	private int dummy = 0;
	//////////////////////////////////////////
	
	// Accelerometer
	private SensorManager mSensorManager;
    private Sensor mAccelerometer;
	private int SENSOR_DELAY = SensorManager.SENSOR_DELAY_FASTEST, notifDelay;
	private TextView tvLocalX, tvLocalY, tvLocalZ, tvRemoteX, tvRemoteY, tvRemoteZ;
	//////////////////////////////////////////
	
	// Dummy
	private TextView localDummyTv, remoteDummyTv;
	//////////////////////////////////////////
	
	// Battery level information
	private BroadcastReceiver br;
	private ArrayList<Integer> levelList;
	/////////////////////////////////////////
	
	// Notification characteristics
	private ArrayList<Double> callTimeList;
	private ArrayAdapter<String> myArrayAdapter;
	/////////////////////////////////////////
	
	// Traffic statistics
	private ArrayList<Double> trafficRxList, trafficTxList, trafficTimeList, trafficRxFromStartList, trafficTxFromStartList,
								trafficRxPacketsList, trafficTxPacketsList, trafficRxPacketsFromStartList, trafficTxPacketsFromStartList;
	private double lastRxBytes, lastTxBytes, currentRxBytes, currentTxBytes, rxBytesOnStart, txBytesOnStart,
					lastRxPackets, lastTxPackets, currentRxPackets, currentTxPackets, rxPacketsOnStart, txPacketsOnStart,
					minOnStart, secOnStart, milliOnStart;
	private Calendar calendar;
	/////////////////////////////////////////
      
	// Scenario
	boolean isAccScenario;
	//////////////////////////////////////////
	
	// MediaSense Platform Application Interfaces
	private MediaSensePlatform platform;
	private DisseminationCore core;
	private PublishSubscribeExtension pse;
	private HashMap<String, String> subscriptionMap;
    //////////////////////////////////////////
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set scenario
        isAccScenario = getIntent().getBooleanExtra("isAccScenario", true);
        
        if(isAccScenario)
        	setContentView(R.layout.acc);
        else
        	setContentView(R.layout.dummy);
        
        // Create components
        txtName = (TextView) findViewById(R.id.headerName); 
        txtNumber = (TextView) findViewById(R.id.headerNumber);
        ctName = (TextView) findViewById(R.id.contactName); 
        ctNumber = (TextView) findViewById(R.id.contactNumber);
        resultEval = (TextView) findViewById(R.id.resultEval);
        resultEval.setMovementMethod(new ScrollingMovementMethod());
        spinnerNotifDelay = (Spinner) findViewById(R.id.spinnerNotifDelay);
        buttonSub = (Button) findViewById(R.id.buttonSub);
        buttonSub.setOnClickListener(this);
        buttonStop = (Button) findViewById(R.id.buttonStop);
        buttonStop.setOnClickListener(this);
        ///////////////////////////////////////
        
        // Header and contact initialization from Preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        txtName.setText(prefs.getString("user_name", "Alexandre"));
        txtNumber.setText(prefs.getString("user_number", "0703612088"));
        ctName.setText(prefs.getString("contact_name", "Johan"));
        ctNumber.setText(prefs.getString("contact_number", "0768248156"));
        if(isAccScenario)
        	myArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,
        	new String[]{"60","80","100","160","200","500","1000","2000"}); // acc scenario
        else
        	myArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, 
        			new String[]{"50","100","150","200","500","1000","2000"}); //dummy scenario
        spinnerNotifDelay.setAdapter(myArrayAdapter);
        spinnerNotifDelay.setOnItemSelectedListener(this);
        spinnerNotifDelay.setSelection(myArrayAdapter.getPosition(prefs.getString("notif_delay", "100")));
        ///////////////////////////////////////
        
        // Handler init
        subscriptionMap = new HashMap<String, String>(); // HashMap holding all the subscription with their uci and ip
        handler = new Handler(); // Create Handler to hold a queue of threads (ResolveThread)
        /////////////////////////////////////
        
        // MediaSense initialization
        MediaSenseManager msManager = new MediaSenseManager();
        platform = msManager.getPlatform();
        core = msManager.getCore();
        if(isAccScenario)
        	msManager.registerUCI(FormatHandler.formatPhoneNumber(txtNumber.getText().toString())+"@evaluation.se/acc");
        else
        	msManager.registerUCI(FormatHandler.formatPhoneNumber(txtNumber.getText().toString())+"@evaluation.se/dummy");
        handler.postAtTime(new TrafficStatsThread(), SystemClock.uptimeMillis());
        core.setGetEventListener(this); // Set the event listeners
        core.setResolveResponseListener(this); // Set the response listeners
        core.setGetResponseListener(this);
        msManager.loadPubSubExtension();
        pse = msManager.getPubSubExt();
        pse.setSubscriptionEventListener(this);
        ///////////////////////////////////////
        
        // Accelerometer sensor or dummy initialization
        if(isAccScenario){
	        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
	        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	        mSensorManager.registerListener(this, mAccelerometer, SENSOR_DELAY);
	        resultEval.setText("Accelerometer registered");
	        notifDelay = Integer.parseInt(spinnerNotifDelay.getSelectedItem().toString());
	        tvLocalX= (TextView)findViewById(R.id.m_x_axis);
	    	tvLocalY= (TextView)findViewById(R.id.m_y_axis);
	    	tvLocalZ= (TextView)findViewById(R.id.m_z_axis);
	    	tvRemoteX= (TextView)findViewById(R.id.r_x_axis);
	    	tvRemoteY= (TextView)findViewById(R.id.r_y_axis);
	    	tvRemoteZ= (TextView)findViewById(R.id.r_z_axis);
        } else{
        	notifDelay = Integer.parseInt(spinnerNotifDelay.getSelectedItem().toString());
	        resultEval.setText("Prepared to share dummy value");
	        localDummyTv = (TextView)findViewById(R.id.local_dummy);
	        localDummyTv.setText(""+dummy);
	        remoteDummyTv = (TextView)findViewById(R.id.remote_dummy);
	        remoteDummyTv.setText("-");
        }
        /////////////////////////////////////
        
        // Traffic stats initialization
        trafficRxList = new ArrayList<Double>();
        trafficTxList = new ArrayList<Double>();
        trafficRxFromStartList = new ArrayList<Double>();
        trafficTxFromStartList = new ArrayList<Double>();
        trafficRxPacketsList = new ArrayList<Double>();
        trafficTxPacketsList = new ArrayList<Double>();
        trafficRxPacketsFromStartList = new ArrayList<Double>();
        trafficTxPacketsFromStartList = new ArrayList<Double>();
        lastRxBytes = TrafficStats.getTotalRxBytes();
        lastTxBytes = TrafficStats.getTotalTxBytes();
        rxBytesOnStart = lastRxBytes;
        txBytesOnStart = lastTxBytes;
        lastRxPackets = TrafficStats.getTotalRxPackets();
        lastTxPackets = TrafficStats.getTotalTxPackets();
        rxPacketsOnStart = lastRxPackets;
        txPacketsOnStart = lastTxPackets;
        trafficTimeList = new ArrayList<Double>();
        calendar = Calendar.getInstance(Locale.FRANCE);
        minOnStart = (double)calendar.get(Calendar.MINUTE);
        secOnStart = (double)calendar.get(Calendar.SECOND);
        milliOnStart = (double)calendar.get(Calendar.MILLISECOND);
        /////////////////////////////////////
        
        callTimeList = new ArrayList<Double>();
        
        // Battery level monitoring setup  
		levelList = new ArrayList<Integer>();
        br = new BroadcastReceiver() {
        	int level;
			@Override
			public void onReceive(Context context, Intent intent) {
				level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				levelList.add(level);
				Log.i("BATTERY", level+"/100");
			}
		};
		registerReceiver(br, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		/////////////////////////////////////
    }
    
    @Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    /*
     * When sensor value changed, update UI and notify subscribers with the new values
     * @see UI update of the device's acc values
     */
    @Override
	public void onSensorChanged(SensorEvent event) {
		// Update UI with acc values
    	final float x = event.values[0];
    	final float y = event.values[1];
    	final float z = event.values[2];
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				tvLocalX.setText(""+x);
				tvLocalY.setText(""+y);
				tvLocalZ.setText(""+z);
			}
		});
	}

    /*
     * Perform resolve call 
     */
    private class ResolveThread implements Runnable {
    	@Override
    	public void run() {
    		String uci;
    		if(isAccScenario)
    			uci = ctNumber.getText().toString()+"@evaluation.se/acc";
    		else
    			uci = ctNumber.getText().toString()+"@evaluation.se/dummy";
			core.resolve(uci); // Resolve/get call to the correct UCI
			handler.postAtTime(new TrafficStatsThread(), SystemClock.uptimeMillis()); // Start monitoring data traffic
			Log.i("RESOLVETHREAD", "resolving");
    	}
    }
	
	/*
     * Perform notify call
     */
    private class NotificationThread implements Runnable {
    	String value, x, y, z;
    	public NotificationThread(String value) {
    		this.value = value;
    		if(isAccScenario){
	    		x = tvLocalX.getText().toString();
				y = tvLocalY.getText().toString();
				z = tvLocalZ.getText().toString();
    		}
		}
    	@Override
    	public void run() {
    		String uci;
    		if(isAccScenario)
    			uci = txtNumber.getText().toString()+"@evaluation.se/acc";
    		else{
    			uci = txtNumber.getText().toString()+"@evaluation.se/dummy";
    			localDummyTv.setText(""+dummy++);
    		}
			pse.notifySubscribers(uci, value); // Notify the subscribers of the new context value

			Log.i("SUBTHREAD", "notify "+uci+" in "+value);
			
			// Perform another notification after specific delay
			if(isAccScenario){
				if(accRegistered)
					handler.postDelayed(new NotificationThread(x+":"+y+":"+z), notifDelay);
			} else {
				if(hasSubscribers)
					handler.postDelayed(new NotificationThread(""+dummy), notifDelay);
			}
    	}
    }
    
    // Thread to gather traffic statistics (Time, TotalRxBytes, TotalTxBytes, TotalRxPackets and TotalTxPackets)
    private class TrafficStatsThread implements Runnable {
    	@Override
		public void run() {
    		if(hasSubscribers || accRegistered){
	    		trafficTimeList.add(FormatHandler.getMinutesFromStart(new double[]{minOnStart, secOnStart, milliOnStart}));
	    		currentRxBytes = TrafficStats.getTotalRxBytes();
	    		currentTxBytes = TrafficStats.getTotalTxBytes();
	    		trafficRxList.add(currentRxBytes-lastRxBytes);
	    		trafficRxFromStartList.add(currentRxBytes-rxBytesOnStart);
	    		trafficTxList.add(currentTxBytes-lastTxBytes);
	    		trafficTxFromStartList.add(currentTxBytes-txBytesOnStart);
	    		lastRxBytes = currentRxBytes;
	    		lastTxBytes = currentTxBytes;
	    		currentRxPackets = TrafficStats.getTotalRxPackets();
	    		currentTxPackets = TrafficStats.getTotalTxPackets();
	    		trafficRxPacketsList.add(currentRxPackets-lastRxPackets);
	    		trafficRxPacketsFromStartList.add(currentRxPackets-rxPacketsOnStart);
	    		trafficTxPacketsList.add(currentTxPackets-lastTxPackets);
	    		trafficTxPacketsFromStartList.add(currentTxPackets-txPacketsOnStart);
	    		lastRxPackets = currentRxPackets;
	    		lastTxPackets = currentTxPackets;
	    		callTimeList.add(FormatHandler.getMinutesFromStart(new double[]{minOnStart, secOnStart, milliOnStart}));
    		}
    		handler.postDelayed(new TrafficStatsThread(), 30000);
		}
    }
	
    /*
     * When 'Subscribe' is clicked, resolve UCI to start subscription
     * 
     * When the 'Stop' button is clicked, 
     * all callbacks from the threads are removed, so that no other call is made and the accelerometer sensor is unregistered.
     * if clicked a second time then register again the accelerometer sensor
     */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.buttonSub:
				handler.postAtTime(new ResolveThread(), SystemClock.uptimeMillis()); // Run a ResolveThread to perform subscription
				break;
			case R.id.buttonStop:
				if(hasSubscribers || accRegistered){
					if(isAccScenario){
						mSensorManager.unregisterListener(this);
						resultEval.setText("Accelerometer unregistered");
					} else 
						resultEval.setText("Stopped sharing dumy value");
					String currentNotifDelay = spinnerNotifDelay.getSelectedItem().toString();
					ResultsHandler.writeResults("level-"+currentNotifDelay, FormatHandler.listIntToArray(levelList));
					ResultsHandler.writeResults("rx-"+currentNotifDelay, FormatHandler.listDoubleToArray(trafficRxList));
					ResultsHandler.writeResults("tx-"+currentNotifDelay, FormatHandler.listDoubleToArray(trafficTxList));
					ResultsHandler.writeResults("rxStart-"+currentNotifDelay, FormatHandler.listDoubleToArray(trafficRxFromStartList));
					ResultsHandler.writeResults("txStart-"+currentNotifDelay, FormatHandler.listDoubleToArray(trafficTxFromStartList));
					ResultsHandler.writeResults("rxPackets-"+currentNotifDelay, FormatHandler.listDoubleToArray(trafficRxPacketsList));
					ResultsHandler.writeResults("txPackets-"+currentNotifDelay, FormatHandler.listDoubleToArray(trafficTxPacketsList));
					ResultsHandler.writeResults("rxPacketsStart-"+currentNotifDelay, FormatHandler.listDoubleToArray(trafficRxPacketsFromStartList));
					ResultsHandler.writeResults("txPacketsStart-"+currentNotifDelay, FormatHandler.listDoubleToArray(trafficTxPacketsFromStartList));
					ResultsHandler.writeResults("time-"+currentNotifDelay, FormatHandler.listDoubleToArray(trafficTimeList));
					startActivity(ResultsHandler.getEmailIntentWithResults(currentNotifDelay,
							new String[]{"level","rx","tx","rxStart","txStart","rxPackets","txPackets", "rxPacketsStart","txPacketsStart","time"}));
					hasSubscribers = false;
					accRegistered = false;
				} else{
					if(isAccScenario){
						mSensorManager.registerListener(this, mAccelerometer, SENSOR_DELAY);
						resultEval.setText("Accelerometer registered");
						accRegistered = true;
					} else {
						hasSubscribers = true;
						dummy = 0;
						resultEval.setText("Stopped sharing dumy value");
					}
				}
				break;
		}
	}

	/*
	 * When the response from the get call is received, do nothing
	 */
	@Override
	public void getResponse(String uci, String value) {
		Log.i("getResponse", value);
	}

	// Perform GET call --> start subscription on the UCI
	@Override
	public void resolveResponse(String uci, String ip) {
		// If not already subscribed, start subscription
		if(!subscriptionMap.containsKey(uci)){
			pse.startSubscription(uci, ip);
			subscriptionMap.put(uci, ip);
			Log.i("resolveResponse", "subscribing to :"+uci+" - "+ip);
		}
		// Perform GET call
		if(!ip.equalsIgnoreCase("null")){
			core.get(uci, ip);
			Log.i("resolveResponse", uci+"-"+ip);
		} else Log.i("resolveResponse", "uci doesn't exist");
	}
	
	// When a GET request is received --> start notifying acc or dummy
	@Override
	public void getEvent(String source, String uci) {
		platform.getDisseminationCore().notify(source, uci, "subscribed");
		if(isAccScenario)
			accRegistered = true;
		else
			hasSubscribers = true;
		handler.postAtTime(new TrafficStatsThread(), SystemClock.uptimeMillis());
		if(isAccScenario)
			handler.postAtTime(new NotificationThread("0:0:0"), SystemClock.uptimeMillis());
		else
			handler.postAtTime(new NotificationThread(""+dummy++), SystemClock.uptimeMillis());
		Log.i("getEvent", source+"-"+uci+" / isAccScenario:"+isAccScenario+", accRegistered"+accRegistered);
	}

	/*
	 * When notification received, extract value and present it on the screen
	 * @see UI update of the remote acc values
	 */
	@Override
	public void subscriptionEvent(String uci, String value) {
		Log.i("SubEvent", value);
		float x = 0, y = 0, z = 0;
		if(isAccScenario){
			String[] values = value.split(":"); // Get x, y and z values from the notification in an Array
			x = Float.valueOf(values[0]);
	    	y = Float.valueOf(values[1]);
	    	z = Float.valueOf(values[2]);
		}
    	final float finalX = x;
    	final float finalY = y;
    	final float finalZ = z;
    	
		final String myValue = value; // Get remote dummy value
			
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if(isAccScenario){ // Show remote acc values
					tvRemoteX.setText(""+finalX);
					tvRemoteY.setText(""+finalY);
					tvRemoteZ.setText(""+finalZ);
				} else // Show remote dummy values
					remoteDummyTv.setText(myValue);
			}
		});
	}

	/*
	 * When SharedPreferences has changed, update the TextViews with right names and numbers
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if(key.equalsIgnoreCase("user_name")) txtName.setText(sharedPreferences.getString("user_name", "Alex"));
    	if(key.equalsIgnoreCase("user_number")) txtNumber.setText(FormatHandler.formatPhoneNumber(sharedPreferences.getString("user_number", "070XXXXXXX")));
    	if(key.equalsIgnoreCase("contact_name")) ctName.setText(sharedPreferences.getString("contact_name", "Johan"));
    	if(key.equalsIgnoreCase("contact_number")) ctNumber.setText(FormatHandler.formatPhoneNumber(sharedPreferences.getString("contact_number", "0768238156")));
    	if(key.equalsIgnoreCase("notif_delay")) spinnerNotifDelay.setSelection(myArrayAdapter.getPosition(sharedPreferences.getString("notif_delay", "100")));
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
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(br);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		prefs.edit().putString("notif_delay", spinnerNotifDelay.getSelectedItem().toString()).commit();
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) { }
	
}