package se.miun.mediasense.eval;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;


public class MSEvaluationActivity extends Activity implements OnClickListener {
    
	Handler handler; // Handler to check internet connection

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		Button responseDelay = (Button) findViewById(R.id.button1);
		Button accScenario = (Button) findViewById(R.id.button2);
		Button dummyScenario = (Button) findViewById(R.id.button3);
		responseDelay.setOnClickListener(this);
		accScenario.setOnClickListener(this);
		dummyScenario.setOnClickListener(this);

		handler = new Handler();
        handler.postAtTime(new ConnectionThread(), SystemClock.uptimeMillis());
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.button1:
				startActivity(new Intent(getApplicationContext(), ResponseDelayActivity.class));
				break;
			case R.id.button2:
				startActivity(new Intent(getApplicationContext(), DataTrafficActivity.class).putExtra("isAccScenario", true));
				break;
			case R.id.button3:
				startActivity(new Intent(getApplicationContext(), DataTrafficActivity.class).putExtra("isAccScenario", false));
				break;
		}
	}

	/*
	 * Thread checking the internet connection each 10 seconds
	 * 
	 * @see	if no internet connection, display AlertDialog 
	 */
	private class ConnectionThread implements Runnable{
		@Override
		public void run() {
			if(!isOnline()){
				AlertDialog.Builder dialog = new AlertDialog.Builder(MediaSenseAndroidExampleActivity.this);
		        dialog.
		        setMessage("No Internet Connection!").
				setNeutralButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						handler.postDelayed(new ConnectionThread(), 10000); // Check internet connection in 10 seconds
					}
				}).show();
			}
		}
	}

	/*
	 * Check if phone has internet connection
	 * 
	 * @result	true if is online, false if not online
	 */
	public boolean isOnline() {
	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

	    return cm.getActiveNetworkInfo() != null && 
	       cm.getActiveNetworkInfo().isConnectedOrConnecting();
	}
}