package com.notto.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Fires a timer or alarm: posts the notification and shows the in-app takeover. */
public class ClockReceiver extends BroadcastReceiver {

    static final String CH = "notto.alarms";

    @Override
    public void onReceive(Context context, Intent intent) {
        String label = intent == null ? null : intent.getStringExtra("label");
        fire(context, label == null ? "Alarm" : label);
    }

    static void fire(Context context, String label) {
        ensureChannel(context);

        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        open.putExtra("notto_alarm", label);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getActivity(context, 11, open, flags);

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b = new Notification.Builder(context, CH);
        } else {
            b = new Notification.Builder(context);
        }
        b.setSmallIcon(R.drawable.ic_stat_notto)
                .setContentTitle(label)
                .setContentText("Time is up.")
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_MAX);
        try {
            b.setFullScreenIntent(pi, true);
        } catch (Exception ignored) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            b.setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            try {
                nm.notify(77, b.build());
            } catch (Exception ignored) {
            }
        }

        // If the app is already open, show the takeover straight away.
        MainActivity.ringIfAlive(label);
    }

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm =
                    (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CH) == null) {
                NotificationChannel ch = new NotificationChannel(CH, "Alarms & timers",
                        NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("Alarms and timers from Notto");
                ch.enableVibration(true);
                nm.createNotificationChannel(ch);
            }
        }
    }
}
