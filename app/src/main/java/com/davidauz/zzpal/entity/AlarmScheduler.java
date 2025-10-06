package com.davidauz.zzpal.entity;

import static com.davidauz.zzpal.entity.Alarm.TYPE_ELAPSED;
import static com.davidauz.zzpal.entity.Alarm.TYPE_FIXED;
import static com.davidauz.zzpal.entity.Alarm.TYPE_RECURRING;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.davidauz.zzpal.MainActivity;
import com.davidauz.zzpal.service.AppLogger;
import com.davidauz.zzpal.views.AlarmReceiver;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

public class AlarmScheduler {

    private Context context;

    public AlarmScheduler(Context context) {
        this.context = context.getApplicationContext();
    }

    public void scheduleAlarm(Alarm alarm) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            AppLogger.getInstance().log("AlarmManager is null");
            return;
        }
//intent for triggered alarm
        Intent callback = new Intent(context, AlarmReceiver.class);
        callback.putExtra("ALARM_ID", alarm.id);
        callback.putExtra("DURATION", alarm.durationSeconds);
        callback.putExtra("VIBRATE", alarm.vibrate);
        callback.putExtra("AUDIO_URI", alarm.audioUri);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            (int) alarm.id, // Unique per alarm
            callback,
            flags
        );

        // Intent for when user taps the alarm in status bar
        Intent showIntent = new Intent(context, MainActivity.class);
        showIntent.putExtra("ALARM_ID", alarm.id);

        PendingIntent showPendingIntent = PendingIntent.getActivity(context,
                (int)alarm.id, showIntent, PendingIntent.FLAG_UPDATE_CURRENT |PendingIntent.FLAG_IMMUTABLE);

        LocalDateTime nowdt=LocalDateTime.now()
        , targetdt = LocalDateTime.now()
        ;
        switch(alarm.type){
            case TYPE_FIXED-> {
                targetdt = LocalDateTime.of(nowdt.getYear(), nowdt.getMonth(), nowdt.getDayOfMonth(), alarm.hours, alarm.minutes);
                if(LocalDateTime.now().isAfter(targetdt))
                    targetdt=targetdt.plusDays(1);// if time already past then set for tomorrow
            }
            case TYPE_ELAPSED -> {
                targetdt = nowdt.plusHours(alarm.hours).plusMinutes(alarm.minutes);
            }
            case TYPE_RECURRING->{targetdt = nowdt.plusHours(alarm.hours).plusMinutes(alarm.minutes);}
        }

        long millis=targetdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        AppLogger.getInstance().log("#"+alarm.getId()+": "+formatter.format(millis));
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
// https://developer.android.com/training/monitoring-device-state/doze-standby :
// Alarms set with setAlarmClock() continue to fire normally. The system exits Doze shortly
// before those alarms fire.
//                AppLogger.getInstance().log("using setAlarmClock");
                   AlarmManager.AlarmClockInfo alarmInfo = new AlarmManager.AlarmClockInfo(millis, showPendingIntent);
                alarmManager.setAlarmClock(alarmInfo, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, millis, pendingIntent);
            }
        }
    }

    public void cancelAlarm(long alarmId) {
        AppLogger.getInstance().log("Stopping alarm #"+alarmId);
        Intent intent = new Intent(context, AlarmReceiver.class); // targets the receiver and not the service
        intent.putExtra("ALARM_ID", alarmId);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            (int) alarmId,
            intent,
            flags
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }
}
