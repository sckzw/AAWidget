package com.gitlab.sckzw.aawidget;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_main );

        getSupportFragmentManager()
                .beginTransaction()
                .replace( R.id.layout_preference, new PreferenceFragment() )
                .commit();
    }

    public static class PreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences( Bundle savedInstanceState, String rootKey ) {
            setPreferencesFromResource( R.xml.root_preferences, rootKey );
        }
    }
}