package com.davidauz.zzpal.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import androidx.core.content.ContextCompat;
import com.davidauz.zzpal.service.AlarmService;
import com.davidauz.zzpal.service.AppLogger;

public class AlarmReceiver extends BroadcastReceiver {
    public static PowerManager.WakeLock wakeLock;
    private static final String WAKE_LOCK_TAG = "zzpal:AlarmWakeLock";

    @Override
    public void onReceive(Context context, Intent intent) {
        acquireWakeLock(context);
        AppLogger.getInstance().log("Alarm received!");
        Intent service = new Intent(context, AlarmService.class);
        service.putExtras(intent); //  ! this starts the service only upon receiving the alarm

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
        } catch (Exception e) {
            AppLogger.getInstance().log("Error starting service: " + e.getMessage());
            releaseWakeLock();
        }
    }

    private static void acquireWakeLock(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
        wakeLock.acquire(          1 * 60); // not ringing for more than one minute
//                               |   |    |     |
//                               |   |    |     +---one second
//                               |   |    +---one minute
//                               |   +---one hour
//                               +---hours
    }

    public static void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

}


