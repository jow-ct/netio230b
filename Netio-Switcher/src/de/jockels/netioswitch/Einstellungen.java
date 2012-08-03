package de.jockels.netioswitch;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Einstellungen extends PreferenceActivity {
	@SuppressWarnings("unused") private final static String TAG = "Einstellungen";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.cfg);
	}
	
}
