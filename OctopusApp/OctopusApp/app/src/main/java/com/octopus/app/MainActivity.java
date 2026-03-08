package com.octopus.app;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    public static final String CHANNEL_ID = "octopus_notifications";
    public static final String CHANNEL_NAME = "Octopus Reminders";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        webView = findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setDatabaseEnabled(true);

        // Inject the Android bridge so JS can call it
        webView.addJavascriptInterface(new NotificationBridge(this), "AndroidNotif");

        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Task and motivation reminders from Octopus");
            channel.enableVibration(true);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    // -------------------------------------------------------
    // JavaScript Bridge
    // -------------------------------------------------------
    public class NotificationBridge {
        private Context context;

        NotificationBridge(Context ctx) { this.context = ctx; }

        /**
         * Called from JS to schedule a single notification at a specific time.
         * time: "HH:MM" (24h), title: string, body: string, id: unique int
         */
        @JavascriptInterface
        public void scheduleAt(int id, String timeHHMM, String title, String body) {
            try {
                String[] parts = timeHHMM.split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                // If time already passed today, schedule for tomorrow
                if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }

                scheduleAlarm(id, cal.getTimeInMillis(), title, body, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Schedule a repeating notification every day at the given time.
         */
        @JavascriptInterface
        public void scheduleDailyAt(int id, String timeHHMM, String title, String body) {
            try {
                String[] parts = timeHHMM.split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }

                scheduleAlarm(id, cal.getTimeInMillis(), title, body, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Schedule multiple times for spam mode.
         * timesJson: JSON array of "HH:MM" strings
         */
        @JavascriptInterface
        public void scheduleMultiple(String timesJson, String title, String body, int baseId) {
            try {
                JSONArray times = new JSONArray(timesJson);
                for (int i = 0; i < times.length(); i++) {
                    String t = times.getString(i);
                    scheduleAt(baseId + i, t, title, body);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Cancel a previously scheduled notification by id.
         */
        @JavascriptInterface
        public void cancel(int id) {
            Intent intent = new Intent(context, NotificationReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(
                context, id, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pi != null) {
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (am != null) am.cancel(pi);
                pi.cancel();
            }
        }

        /**
         * Returns true so JS can detect Android bridge is present.
         */
        @JavascriptInterface
        public boolean isAvailable() { return true; }
    }

    private void scheduleAlarm(int id, long triggerMs, String title, String body, boolean repeat) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("notif_id", id);
        intent.putExtra("title", title);
        intent.putExtra("body", body);
        intent.putExtra("repeat", repeat);

        PendingIntent pi = PendingIntent.getBroadcast(
            this, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        if (repeat) {
            am.setRepeating(AlarmManager.RTC_WAKEUP, triggerMs, AlarmManager.INTERVAL_DAY, pi);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerMs, pi);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
