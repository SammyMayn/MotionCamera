package com.sjm.cameranew;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class SettingsActivity extends AppCompatActivity {
    public static final String
            KEY_PREF_EXAMPLE_SWITCH = "example_switch";
    public static final String
            KEY_PREF_SENSITIVITY = "capture_sensitivity";
    public static final String
            KEY_PREF_PREVIEW = "key_preview";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new com.sjm.cameranew.SettingsFragment())
                .commit();
        //setDivider(ContextCompat.getDrawable(this, R.drawable.circlebutton));
        //setDividerHeight(your_height);
        //Set this application context in GetContext class, SettingsFragment object will pick it up
        com.sjm.cameranew.GetContext gc = new com.sjm.cameranew.GetContext();
        gc.setAppContext(getApplicationContext());
        //Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        //Ringtone r = RingtoneManager.getRingtone(this, notification);
        //r.play();
        /*************************************************************************************/

        //Preference preference = findPreference("key_label_email");
        //preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        //    @Override
        //    public boolean onPreferenceClick(Preference preference) {
        //        Toast.makeText(SettingsActivity.this, "Clicked", Toast.LENGTH_SHORT).show();
        //        return true;
        //    }
        //});
    }

}
