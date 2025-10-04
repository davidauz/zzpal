package com.davidauz.zzzpal2.service;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.os.HandlerCompat;

import com.davidauz.zzzpal2.MainActivity;
import com.davidauz.zzzpal2.R;

import java.io.IOException;

/*
A Service is a component designed to perform long-running operations in the background,
even when the app is not in the foreground (or even closed).
Unlike Activity (which is tied to the UI and stops when the app is minimized), a Service runs
independently of the user interface, making it useful for tasks that need to continue working
when the app is in the background or terminated.

Services run on the main thread by default (but can spawn background threads for heavy work).
The lifecycle is separate from activities (can outlive the app’s UI).
Can be started with startService() (runs until explicitly stopped) or bound with bindService()
 (for ongoing interactions with other components).

How a Service Solves the "Foreground-Only Alarm" Problem:
Alarms (triggered via AlarmManager or WorkManager) can fail to fire reliably when the app is
in the background or killed, due to Android’s power-saving features (e.g., Doze mode, app standby)
or system constraints on background processes. A Service helps address this by:
* Maintaining a persistent background presence: Ensuring the app has a active component running,
making it less likely for the system to ignore alarm triggers.
* Handling alarm logic reliably: Receiving alarm intents and executing critical actions (e.g.,
showing notifications, vibrating) even when the app is not visible.
*/
public class AlarmService extends Service {
    private static final int NOTIFICATION_ID = 123;
    private static final String CHANNEL_ID = "ALARM_CHANNEL";
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private static final String WAKE_LOCK_TAG = "zzzpal:AlarmWakeLock";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
// Acquire Wake Lock to ensure that the CPU stays on while the alarm is being set up and run.
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
// Acquire for the needed time
        wakeLock.acquire(8 * 60 * 60 * 1000L);
//                               |   |    |     |
//                               |   |    |     +---one second
//                               |   |    +---one minute
//                               |   +---one hour
//                               +---hours

// ***************************************************************
// **IMMEDIATELY** CALL startForeground()
// this has to be at the absolute top of onStartCommand
        createNotificationChannel();
        Notification notification = createNotification(intent.getLongExtra("ALARM_ID", -1));
        startForeground(NOTIFICATION_ID, notification);
// ***************************************************************
// Now the service is hopefully running in the foreground, protected
// from being killed, so it is safe to process the rest of the data
        AppLogger.getInstance().log("onStartCommand!");

        long alarmId = intent.getLongExtra("ALARM_ID", -1);
        AppLogger.getInstance().log("Got alarm #"+alarmId);
        int duration = intent.getIntExtra("DURATION", 60); // Default 60s
        boolean vibrate = intent.getBooleanExtra("VIBRATE", false);
        String audioUri = intent.getStringExtra("AUDIO_URI");

        startAlarmSound(audioUri);
        if (vibrate)
            startVibration(duration);

        Handler handler = HandlerCompat.createAsync(Looper.getMainLooper());
        handler.postDelayed(() -> {
            stopAlarm();
            stopSelf();
        }, duration * 1000L);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Alarm Channel",
                NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }






    private void startAlarmSound(String audioUri) {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(this, android.net.Uri.parse(audioUri));
            AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
            mediaPlayer.setAudioAttributes(attributes);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error playing alarm sound", Toast.LENGTH_SHORT).show();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void startVibration(int duration) {
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(
                        duration * 1000L, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(duration * 1000L);
            }
        }
    }

    private void stopAlarm() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException e) {
                Log.w("AlarmService", "MediaPlayer in invalid state", e);
            } finally {
                mediaPlayer.release();
                mediaPlayer = null; // Prevent reuse
            }
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    private Notification createNotification(long alarmId) {
        // Create intent to open app when notification is tapped
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Alarm Ringing")
                .setContentText("Alarm ID: " + alarmId)
                .setSmallIcon(R.drawable.ic_alarm) // Replace with your icon
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .build();
    }



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//must release wakelock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        stopAlarm();
    }
}
