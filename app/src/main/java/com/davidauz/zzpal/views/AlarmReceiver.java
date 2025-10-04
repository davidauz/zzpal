package com.davidauz.zzpal.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import com.davidauz.zzpal.service.AlarmService;
import com.davidauz.zzpal.service.AppLogger;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
            AppLogger.getInstance().log("Alarm received!");
            Intent service = new Intent(context, AlarmService.class);
            service.putExtras(intent);
            ContextCompat.startForegroundService(context, service);
        }

}


