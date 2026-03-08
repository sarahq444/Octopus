package com.octopus.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        // Re-fire the app's WebView briefly to let JS reschedule
        // We store pending alarms in SharedPreferences as a backup
        SharedPreferences prefs = context.getSharedPreferences("octopus_alarms", Context.MODE_PRIVATE);
        String alarmsJson = prefs.getString("alarms", null);
        if (alarmsJson == null) return;

        try {
            org.json.JSONArray alarms = new org.json.JSONArray(alarmsJson);
            for (int i = 0; i < alarms.length(); i++) {
                org.json.JSONObject alarm = alarms.getJSONObject(i);
                int id = alarm.getInt("id");
                String time = alarm.getString("time");
                String title = alarm.getString("title");
                String body = alarm.getString("body");
                boolean repeat = alarm.optBoolean("repeat", false);

                String[] parts = time.split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);

                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(java.util.Calendar.HOUR_OF_DAY, hour);
                cal.set(java.util.Calendar.MINUTE, minute);
                cal.set(java.util.Calendar.SECOND, 0);
                if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
                }

                Intent notifIntent = new Intent(context, NotificationReceiver.class);
                notifIntent.putExtra("notif_id", id);
                notifIntent.putExtra("title", title);
                notifIntent.putExtra("body", body);
                notifIntent.putExtra("repeat", repeat);

                android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(
                    context, id, notifIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
                );

                android.app.AlarmManager am = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (am != null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                    } else {
                        am.setExact(android.app.AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
