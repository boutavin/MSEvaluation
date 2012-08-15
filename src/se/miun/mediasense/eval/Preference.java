package se.miun.mediasense.eval;

import se.miun.mediasense.eval.R;
import android.os.Bundle;

public class Preference extends android.preference.PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

}
