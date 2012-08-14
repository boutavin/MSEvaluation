package se.miun.mediasense.eval;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;


public class MSEvaluationActivity extends Activity implements OnClickListener {
    
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
}