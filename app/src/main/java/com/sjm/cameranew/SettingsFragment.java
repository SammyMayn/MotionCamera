package com.sjm.cameranew;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Context m_context;
    private static Map<String, String> m_listRT;
    private String m_newAlarmSummary;
    private String m_currentAlarmSummary;
    private String m_ringtonePicked;
    private Ringtone m_ringtone;
    private Integer m_ringtoneSelectedIndex = 0;
    //public SettingsFragment() {
    // Required empty public constructor
    //}

    //@Override
    //public View onCreateView(LayoutInflater inflater, ViewGroup container,
    //                         Bundle savedInstanceState) {
    //    TextView textView = new TextView(getActivity());
    //    textView.setText(R.string.hello_blank_fragment);
    //    return textView;
    //}

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        String sMsg = "";
        String pw = "";
        addPreferencesFromResource(R.xml.preferences);
        //setPreferencesFromResource(R.xml.preferences,rootKey);
        CheckBoxPreference pref = (CheckBoxPreference) findPreference("key_checkbox_email");
        CheckBoxPreference chkAlarm = (CheckBoxPreference) findPreference("key_checkbox_alarm");

        EditTextPreference keyalarm = (EditTextPreference) findPreference("key_alarm");
        //String s = keyalarm.getText();
        //String s1 = keyalarm.getSummary().toString();
        //String s2 = keyalarm.getTitle().toString();
        EditTextPreference email = (EditTextPreference) findPreference("key_email_address");
        EditTextPreference emailPW = (EditTextPreference) findPreference("key_email_PW");
        EditTextPreference emailNum = (EditTextPreference) findPreference("key_email_number");
        ListPreference tolerance = (ListPreference) findPreference("key_sensitivity");
        sMsg = "Select Motion Sensitivity - less is more sensitive. Currently set to " + tolerance.getValue();
        tolerance.setSummary(sMsg);
        email.setSummary(email.getText());
        pw = setAsterisks(emailPW.getText().length());
        emailPW.setSummary(pw);
        //emailPW.setSummary(emailPW.getText());
        emailNum.setSummary("Currently set to " + emailNum.getText() + " video frames");
        email.setVisible(pref.isChecked());
        emailPW.setVisible(pref.isChecked());
        emailNum.setVisible(pref.isChecked());

        SharedPreferences settings = getContext().getSharedPreferences("PrefAlarm", 0); // 0 - for private mode
        String currentToneString = settings.getString("currentTone", null);
        String currentToneTitle = settings.getString("currentToneTitle", null);
        if (currentToneString != null && currentToneTitle != null) {
            //Uri currentTone = Uri.parse(currentToneString);
            //Ringtone rtone = RingtoneManager.getRingtone(getContext(), currentTone);
            //chkAlarm.setSummary(currentTone.toString());
            //String sTitle = rtone.getTitle(getContext());

            chkAlarm.setSummary(currentToneTitle);
            //Log.d("[DEBUG]", "CURRENT ALARM_X ");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 1:
                case 0:
                    Uri ringtone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    final Ringtone ringtonePicked = RingtoneManager.getRingtone(getContext(), ringtone);

                    //Set volume -> max
                    AudioManager mobilemode = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
                    mobilemode.setStreamVolume(AudioManager.STREAM_RING, mobilemode.getStreamMaxVolume(AudioManager.STREAM_RING), 0);

                    m_newAlarmSummary = ringtonePicked.getTitle(getContext());
                    m_ringtone = ringtonePicked;
                    String sTitle = m_ringtone.getTitle(getContext());
                    //ringtonePicked.play();
                    m_ringtoneSelectedIndex = ListRingtones(sTitle);
                    m_ringtonePicked = ringtonePicked.toString();
                    com.sjm.cameranew.GetContext gc = new com.sjm.cameranew.GetContext();

                    // Write Ringtone so we can recall it next time we come into Preferences
                    SharedPreferences pref = getContext().getSharedPreferences("PrefAlarm", 0); // 0 - for private mode
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("currentTone", m_ringtonePicked);
                    editor.putString("currentToneTitle", m_newAlarmSummary);
                    editor.commit();

                    gc.setRingTone(ringtonePicked);
                    refreshPreferences();

                    break;
                default:
                    break;
            }
        }
    }

    private int ListRingtones(String SelectedRingtoneTitle) {
        Integer selIndex = 0;
        RingtoneManager manager = new RingtoneManager(getContext());
        manager.setType(RingtoneManager.TYPE_NOTIFICATION);
        Cursor cursor = manager.getCursor();

        Map<String, String> list = new HashMap<>();
        int Index = 0;
        while (cursor.moveToNext()) {
            String notificationTitle = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
            String notificationUri = cursor.getString(RingtoneManager.URI_COLUMN_INDEX) + "/" + cursor.getString(RingtoneManager.ID_COLUMN_INDEX);
            String sTitle = notificationTitle;
            list.put(notificationTitle, notificationUri);
            if (notificationTitle.equals(SelectedRingtoneTitle))
                selIndex = Index;
            Index++;
        }
        return selIndex;
    }

    private void refreshPreferences() {
        //if (m_alarmTitle == null) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            onSharedPreferenceChanged(sp, getPreferenceScreen().getPreference(i).getKey());
        }
        //}
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String sMsg = "";
        String pw = "";
        if (key.equals("key_checkbox_alarm")) {
            CheckBoxPreference setAlarm = (CheckBoxPreference) findPreference("key_checkbox_alarm");
            com.sjm.cameranew.GetContext gc = new com.sjm.cameranew.GetContext();
            EditTextPreference ringtoneName = (EditTextPreference) findPreference("key_alarm");
            ringtoneName.setVisible(setAlarm.isChecked());

            if (setAlarm.isChecked()) {
                //CheckBoxPreference setChkAlarm = (CheckBoxPreference) findPreference("key_checkbox_alarm");
                EditTextPreference keyAlarm = (EditTextPreference) findPreference("key_alarm");
                m_currentAlarmSummary = setAlarm.getSummary().toString();
                //Log.d("[DEBUG]", "CURRENT ALARM : " + sendMail.getSummary() );
                if (m_currentAlarmSummary != m_newAlarmSummary && m_newAlarmSummary != null) {
                    //setAlarm.setSummary(m_newAlarmSummary);
                    //keyAlarm.setSummary(m_ringtoneSelectedIndex.toString());

                    //setAlarm.setTitle(m_newAlarmSummary);

                    //keyAlarm.setText(m_newAlarmSummary);


                    String s = setAlarm.getSummary().toString();
                    String s1 = keyAlarm.getSummary().toString();
                    String s2 = setAlarm.getTitle().toString();
                    String s3 = keyAlarm.getTitle().toString();
                    String s4 = keyAlarm.getText();

                    //setAlarm.setSummary(keyAlarm.getSummary());
                    //setAlarm.setTitle(keyAlarm.getSummary());
                    setAlarm.setSummary(m_newAlarmSummary);
                    setAlarm.setTitle(m_newAlarmSummary);
                    keyAlarm.setText(m_ringtoneSelectedIndex.toString());
                    keyAlarm.setTitle(m_ringtoneSelectedIndex.toString());
                    keyAlarm.setSummary(m_ringtoneSelectedIndex.toString());

                    Log.d("[DEBUG]", "CURRENT ALARM : ");
                }

                if (m_newAlarmSummary != m_currentAlarmSummary && m_newAlarmSummary != null) {
                    return;
                }
                m_currentAlarmSummary = m_newAlarmSummary;
                final Intent ringtone = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                ringtone.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                ringtone.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                ringtone.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

                startActivityForResult(ringtone, 0);

            }

        }


        if (key.equals("key_checkbox_email")) {
            // Set summary to be the user-description for the selected value

            //Preference exercisesPref = findPreference(key);
            try {
                CheckBoxPreference sendMail = (CheckBoxPreference) findPreference("key_checkbox_email");
                Log.d("[DEBUG]", "eMail Check state: " + sendMail.isChecked());
                //now make visible the email address and number of notifications edittexts
                EditTextPreference email = (EditTextPreference) findPreference("key_email_address");
                EditTextPreference emailPW = (EditTextPreference) findPreference("key_email_PW");
                EditTextPreference emailNum = (EditTextPreference) findPreference("key_email_number");

                email.setSummary(email.getText());
                pw = setAsterisks(emailPW.getText().length());
                emailPW.setSummary(pw);
                //emailNum.setSummary("Currently set to send " + emailNum.getText() + " notifications");
                emailNum.setSummary("An email will be sent with " + emailNum.getText() + " frame captures");
                email.setVisible(sendMail.isChecked());
                emailPW.setVisible(sendMail.isChecked());
                emailNum.setVisible(sendMail.isChecked());

            } catch (Exception e) {
                Log.d("[DEBUG]", "Error onSharedPreferenceChanged: " + e);
                e.printStackTrace();
            }
        }
        if (key.equals("key_email_address")) {
            EditTextPreference email = (EditTextPreference) findPreference("key_email_address");
            email.setSummary(email.getText());
            //Log.d("[DEBUG]", "eMail changed: " + email.getText());
        }
        if (key.equals("key_email_PW")) {
            EditTextPreference emailPW = (EditTextPreference) findPreference("key_email_PW");
            pw = setAsterisks(emailPW.getText().length());
            emailPW.setSummary(pw);

        }
        if (key.equals("key_email_number")) {
            EditTextPreference emailNum = (EditTextPreference) findPreference("key_email_number");
            String num = emailNum.getText();
            Log.d("[DEBUG]", "eMail Number changed: " + num);
            emailNum.setSummary("Currently set to " + num + " video frames");
        }
        if (key.equals("key_sensitivity")) {
            ListPreference tolerance = (ListPreference) findPreference("key_sensitivity");
            sMsg = "Select Motion Sensitivity - less is more sensitive. Currently set to " + tolerance.getValue();
            tolerance.setSummary(sMsg);
        }
        if (key.equals("key_alarm")) {
            EditTextPreference keyAlarm = (EditTextPreference) findPreference("key_alarm");
            //keyAlarm.setText("test");
            //String s = keyAlarm.getText();
            //String s1 = keyAlarm.getTitle().toString();
            //String s2 = keyAlarm.getSummary().toString();
            //s2 = "";
            //keyAlarm.setSummary(keyAlarm.getText());
        }
//            ListPreference ringtoneList = (ListPreference) findPreference("key_list_ringtone");
//
//            ringtoneList.setSummary(ringtoneList.getValue()); //this is the file to play
//            ringtoneList.setSummary(ringtoneList.getEntry()); //this is it's name
///*****************************************************/
//            final Intent ringtone = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
//            ringtone.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
//            ringtone.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
//            ringtone.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
//                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
//            startActivityForResult(ringtone, 0);
//
//            //if (requestCode == 0 && resultCode == RESULT_OK) {
//                final Uri uri = ringtone.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
//                final Ringtone ringtonePicked = RingtoneManager.getRingtone(getContext(), uri);
//                // Get your title here `ringtone.getTitle(this)`
//            //ringtoneList.setSummary(ringtonePicked.getTitle(getContext()));
//            //}
///****************************************************/
////            String strTone = ringtoneList.getValue();
////            int rToneIdx = strTone.lastIndexOf("/");
////            int rTone = Integer.parseInt(ringtoneList.getValue().substring(rToneIdx+1)) ;
////            Uri notification = RingtoneManager.getDefaultUri(rTone);
////            Uri myUri = Uri.parse(ringtoneList.getValue());
////
//            Log.d("[DEBUG]", "RingtoneListValue: "  + ringtonePicked);
////            RingtoneManager ringtoneMgr = new RingtoneManager(getContext());
////            ringtoneMgr.setType(RingtoneManager.TYPE_ALL);
////            Ringtone r  = ringtoneMgr.getRingtone(getContext(), notification);
//            //Ringtone r = RingtoneManager.getRingtone(getContext(), notification);
//            //r.setType(RingtoneManager.TYPE_ALARM);
//            AudioManager mobilemode = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
//            mobilemode.setStreamVolume(AudioManager.STREAM_RING,mobilemode.getStreamMaxVolume(AudioManager.STREAM_RING),0);
//            //r.play();
//            //ringtonePicked.play();
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            ringtonePicked.stop();
//        }

    }

    private String setAsterisks(int length) {
        StringBuilder sb = new StringBuilder();
        for (int s = 0; s < length; s++) {
            sb.append("*");
        }
        return sb.toString();
    }

    protected static void setListPreferenceData(ListPreference lp) {
        //m_listRT
        CharSequence[] cs = m_listRT.keySet().toArray(new CharSequence[m_listRT.size()]);
        CharSequence[] csValues = m_listRT.values().toArray(new CharSequence[m_listRT.size()]);
        lp.setEntries(cs);
        lp.setDefaultValue("");
        lp.setEntryValues(csValues);
    }
}
