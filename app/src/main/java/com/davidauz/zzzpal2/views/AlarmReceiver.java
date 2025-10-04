package com.davidauz.zzzpal2.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import com.davidauz.zzzpal2.service.AlarmService;
import com.davidauz.zzzpal2.service.AppLogger;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
            AppLogger.getInstance().log("Alarm received!");
            Intent service = new Intent(context, AlarmService.class);
            service.putExtras(intent);
            ContextCompat.startForegroundService(context, service);
        }

}


