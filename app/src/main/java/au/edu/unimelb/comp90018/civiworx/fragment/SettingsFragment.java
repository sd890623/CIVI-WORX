package au.edu.unimelb.comp90018.civiworx.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import au.edu.unimelb.comp90018.civiworx.R;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

}