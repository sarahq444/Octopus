package com.octopus.app;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra("notif_id", 0);
        String title = intent.getStringExtra("title");
        String body = intent.getStringExtra("body");
        boolean repeat = intent.getBooleanExtra("repeat", false);

        if (title == null) title = "Octopus 🐙";
        if (body == null) body = "Don't forget your task!";

        showNotification(context, id, title, body);

        // If it's a one-shot but should repeat daily, reschedule for tomorrow
        if (repeat) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, 1);

            Intent newIntent = new Intent(context, NotificationReceiver.class);
            newIntent.putExtra("notif_id", id);
            newIntent.putExtra("title", title);
            newIntent.putExtra("body", body);
            newIntent.putExtra("repeat", true);

            PendingIntent pi = PendingIntent.getBroadcast(
                context, id, newIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                }
            }
        }
    }

    private void showNotification(Context context, int id, String title, String body) {
        Intent openApp = new Intent(context, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent tapIntent = PendingIntent.getActivity(
            context, id, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .setVibrate(new long[]{0, 300, 100, 300});

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(id, builder.build());
    }
}
