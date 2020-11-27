package com.sjm.camera;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class GetContext extends Application {

    private static Context context;
    private static Context m_context;
    private static Ringtone m_ringtone;

    public void onCreate() {
        super.onCreate();
        GetContext.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return GetContext.context;
    }

    public static void setAppContext(Context context) {
        GetContext.m_context = context;
    }

    public static Ringtone getRingtone() {
        return m_ringtone;
    }

    public void setRingTone(Ringtone rtone) {
        m_ringtone = rtone;
    }

    public Map<String, String> getNotifications() {

        Map<String, String> list = new HashMap<>();
        try {
            //RingtoneManager manager = new RingtoneManager(this);
            RingtoneManager manager = new RingtoneManager(m_context);
            manager.setType(RingtoneManager.TYPE_ALARM);
            Cursor cursor = manager.getCursor();
            while (cursor.moveToNext()) {
                String notificationTitle = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
                String notificationUri = cursor.getString(RingtoneManager.URI_COLUMN_INDEX) + "/" + cursor.getString(RingtoneManager.ID_COLUMN_INDEX);
                Uri uri = Uri.parse(notificationUri + "/" + cursor.getString(RingtoneManager.ID_COLUMN_INDEX));
                //list.put(notificationTitle, notificationUri);
                list.put(notificationTitle, uri.toString());
            }
        } catch (Exception e) {
            Log.d("[DEBUG]", "ERROR IN GetContext class: " + e);
            e.printStackTrace();
        }

        return list;
    }
}