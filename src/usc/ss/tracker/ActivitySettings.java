package usc.ss.tracker;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

public class ActivitySettings extends PreferenceActivity
{
	SharedPreferences sharedPreferences;

	Preference cbEnableAssistance;
	Preference lvAssistanceType;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		sharedPreferences = getSharedPreferences(getResources().getString(R.string.preferences_key_bustrackr), Activity.MODE_PRIVATE);

		cbEnableAssistance = (Preference) findPreference(getResources().getString(R.string.preference_assistance));
		lvAssistanceType = (Preference) findPreference(getResources().getString(R.string.preference_input_type));

		lvAssistanceType.setSummary(getResources().getString(R.string.summary_input_type)
				+ sharedPreferences.getString(getResources().getString(R.string.preference_input_type),
						getResources().getString(R.string.default_input_type)));

		cbEnableAssistance.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				return Utility.saveMyPreference(getApplicationContext(), getResources().getString(R.string.preference_assistance),
						(Boolean) newValue);
			}
		});

		lvAssistanceType.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				boolean isSuccessful;

				// Update the preferences
				isSuccessful = Utility.saveMyPreference(getApplicationContext(), getResources().getString(R.string.preference_input_type),
						(String) newValue);
				
				// Update the activity view
				if (isSuccessful)
				{
					lvAssistanceType.setSummary(getResources().getString(R.string.summary_input_type) + ((String) newValue));
				}

				return isSuccessful;
			}
		});
	}
}
