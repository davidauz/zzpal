package com.davidauz.zzpal.service;


import static com.davidauz.zzpal.views.AlarmReceiver.wakeLock;

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
import com.davidauz.zzpal.MainActivity;
import com.davidauz.zzpal.R;
import com.davidauz.zzpal.views.AlarmReceiver;

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


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            long alarmId = intent.getLongExtra("ALARM_ID", -1);

//            startForeground(NOTIFICATION_ID, notification);
            if (-1 != alarmId) {
                Notification notification = createNotification(alarmId);
                AppLogger.getInstance().log("AlarmService Got alarm #" + alarmId);
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
            }
        }catch(Exception e){
            Log.e("AlarmService", e.getMessage());
        }finally{
            AlarmReceiver.releaseWakeLock();
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
//        AppLogger.getInstance().log("AlarmService onCreate");
        startForegroundWithValidNotification();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    private void startForegroundWithValidNotification() {
        Notification notification = buildValidNotification();
        startForeground(NOTIFICATION_ID, notification);
    }


    private Notification buildValidNotification() {
        // Create a valid notification builder with proper channel
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_alarm);
        builder.setContentTitle("Alarm Service");
        builder.setContentText("Running in background");
        builder.setOngoing(true);
        builder.setShowWhen(false);
        builder.setAutoCancel(false);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setCategory(Notification.CATEGORY_SERVICE);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        return builder.build();
    }

    private void createNotificationChannel() {
        try {
//            AppLogger.getInstance().log("AlarmService createNotificationChannel");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Alarm Channel",
                        NotificationManager.IMPORTANCE_HIGH
                );
                NotificationManager manager = getSystemService(NotificationManager.class);
                manager.createNotificationChannel(channel);
            }
        } catch (Exception e) {
            Log.e("AlarmService", e.getMessage());
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


    private Notification createNotification() {
        return new NotificationCompat.Builder(this, "alarm_channel")
            .setContentTitle("Alarm Service")
            .setContentText("Alarm service is running")
            .setSmallIcon(R.drawable.ic_alarm)
            .build();
    }

    private Notification createNotification(long alarmId) {
// Create intent to open app when notification is tapped
        AppLogger.getInstance().log("AlarmService createNotification");
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
        AppLogger.getInstance().log("AlarmService onBind");
        return null;
    }

    @Override
    public void onDestroy() {
//        AppLogger.getInstance().log("AlarmService onDestroy");
        super.onDestroy();
//must release wakelock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        stopAlarm();
    }

//    @Override not useful anyway
//    public void onTaskRemoved(Intent rootIntent) {
//        AppLogger.getInstance().log("AlarmService onTaskRemoved");
//        Intent restartService = new Intent(this, AlarmService.class);
//        restartService.setPackage(getPackageName());
//        startService(restartService);
//        super.onTaskRemoved(rootIntent);
//    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // 🔥 This is called when app is swiped from recents
        // You can restart service here if needed
        Intent restartService = new Intent(this, AlarmService.class);
        startService(restartService);
        super.onTaskRemoved(rootIntent);
    }

}
