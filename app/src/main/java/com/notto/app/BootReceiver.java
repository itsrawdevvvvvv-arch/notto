package com.notto.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Re-arms the daily reminder after a reboot (alarms do not survive it). */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        if (context.getSharedPreferences(MainActivity.PREFS, 0).getBoolean("remOn", false)) {
            int h = context.getSharedPreferences(MainActivity.PREFS, 0).getInt("remHour", 20);
            int m = context.getSharedPreferences(MainActivity.PREFS, 0).getInt("remMin", 0);
            ReminderReceiver.schedule(context, h, m);
        }
        MainActivity.refreshWidgets(context);
    }
}
