package com.auri.cesta;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }
        DataStore store = new DataStore(context);
        for (DataStore.FutureItem item : store.getFuture()) {
            if (item.notify && item.whenMillis > System.currentTimeMillis()) {
                ReminderScheduler.schedule(context, item);
            }
        }
    }
}
