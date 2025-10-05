package com.davidauz.zzpal;

import static com.davidauz.zzpal.entity.Alarm.TYPE_ELAPSED;
import static com.davidauz.zzpal.entity.Alarm.TYPE_FIXED;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.davidauz.zzpal.entity.Alarm;
import com.davidauz.zzpal.entity.AlarmScheduler;
import com.davidauz.zzpal.service.AlarmService;
import com.davidauz.zzpal.service.AppLogger;
import com.davidauz.zzpal.ui.AlarmAdapter;
import com.davidauz.zzpal.views.AlarmViewModel;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

/*
Application life cycle:

initialized -> onCreate ->
    created -> onStart (onRestart) ->
        started (is visible)-> onResumed ->
            resumed (has focus)-> onPause ->
        started (is visible)-> onStop ->
    created-> onDestroy ->
destroyed


This is what this app is all about:
* Schedule alarms with AlarmManager: it persists when the app is killed
* Start AlarmService only when an alarm fires: it is a temporary execution
* Stop the service immediately after, to preserve battery

Key points:

* When the app is swiped away the service is stopped, but AlarmManager alarms remains alert
* When an alarm fires, the system creates a new process, calls the receiver, who in turn starts the service
* After the service stops, the processes are killed again

*/

public class MainActivity extends ComponentActivity {
    private AlarmViewModel viewModel;
    private AlarmScheduler alarmScheduler;
    private LinearLayout mainLayout;
    private LinearLayout logsLayout;
    private String selectedRingtoneUri = Settings.System.DEFAULT_ALARM_ALERT_URI.toString()
    ,   selectedRingtoneText;
    private TextView audioSelectionText;
    private ActivityResultLauncher<String[]> multiplePermissionLauncher;
    private ActivityResultLauncher<Intent> ringtonePickerLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppLogger.init(this.getApplicationContext());
        setContentView(R.layout.activity_main);

        mainLayout = findViewById(R.id.main_layout);
        logsLayout = findViewById(R.id.logs_layout);

        RecyclerView alarmList = findViewById(R.id.alarm_list);
        Button addButtonFixed = findViewById(R.id.add_button_fixed)
        ,   addButtonElapsed = findViewById(R.id.add_button_elapsed)
        ,   startAllButton = findViewById(R.id.add_button_startall)
        ,   stopAllButton = findViewById(R.id.add_button_stopall)
        ,   showLogsButton = findViewById(R.id.show_logs)
        ,   clearLogsBtn = findViewById(R.id.button_clear)
        ,   backButton = logsLayout.findViewById(R.id.button_back)
        ,   btnDumpDb=logsLayout.findViewById(R.id.btn_dumpdb)
        ,   btnFixes=logsLayout.findViewById(R.id.btnFixes)
        ;

        alarmList.setLayoutManager(new LinearLayoutManager(this));

        AlarmAdapter adapter = new AlarmAdapter
        (   (alarm, enabled) -> toggleAlarm(alarm, enabled)
        ,   alarm -> deleteAlarm(alarm)
        );
        alarmList.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(AlarmViewModel.class);
        alarmScheduler = new AlarmScheduler(this.getApplicationContext());

        multiplePermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                (Map<String, Boolean> permissions) -> {
                    Boolean postGranted = permissions.getOrDefault("android.permission.POST_NOTIFICATIONS", false)
                    , specuseGranted=permissions.getOrDefault("android.permission.FOREGROUND_SERVICE_SPECIAL_USE", false)
                    ;

                    if (!postGranted || //!wakeLockGranted ||
                    !specuseGranted) {
                        Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
                    }
                });

        requestMultiplePermissions();

        viewModel.getAllAlarms().observe(this, alarms -> {
            adapter.submitList(alarms); // Update RecyclerView
        });

        addButtonFixed.setOnClickListener(v -> showDialogForNewFixedAlarm());
        addButtonElapsed.setOnClickListener(v -> showDialogForNewElapsedAlarm());
//        addBtnRecurring.setOnClickListener(v -> showDialogForNewRecurringAlarm());
        startAllButton.setOnClickListener(v -> startAllActiveAlarms());
        stopAllButton.setOnClickListener(v -> stopAllActiveAlarms());
        showLogsButton.setOnClickListener(v->showLogsLayout());
        clearLogsBtn.setOnClickListener(v->clearLogs());
        backButton.setOnClickListener(v->showMainlayout());
        btnDumpDb.setOnClickListener(v-> dumpdb());
        btnFixes.setOnClickListener(v-> btnFixes());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!isExactAlarmPermissionGranted()) {
                requestNeededPermissions();
            }
        }

        ringtonePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.class);
                        if (uri != null) {
                            selectedRingtoneUri = uri.toString();
                            Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
                            String title = ringtone.getTitle(this);
                            audioSelectionText.setText(title);
                            selectedRingtoneText=title;
                        } else {
                            selectedRingtoneUri = Settings.System.DEFAULT_ALARM_ALERT_URI.toString();
                            audioSelectionText.setText("Silent");
                            selectedRingtoneText="Silent";
                        }
                    }
                }
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (canScheduleExactAlarms()) {
                AppLogger.getInstance().log("Can schedule exact alarms (good)");
            }else{
                AppLogger.getInstance().log("CANNOT SCHEDULE EXACT ALARMS");
                showExactAlarmPermissionRationale();
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                AppLogger.getInstance().log("MISSING 'IgnoringBatteryOptimizations': must allow background activity in Settings");
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);

                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else
                AppLogger.getInstance().log("Ignoring Battery Optimizations (good)");
        }

        ActivityManager am = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        if( am.isBackgroundRestricted() )
            AppLogger.getInstance().log("Background restricted (not good)");
        else
            AppLogger.getInstance().log("Not background restricted (good)");

        NotificationManager nm = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm.isNotificationPolicyAccessGranted())
            AppLogger.getInstance().log("Notifications access granted (can override Do Not Disturb)");
        else
            AppLogger.getInstance().log("Notifications access NOT granted (can NOT override Do Not Disturb)");

        UsageStatsManager usm = (UsageStatsManager) getApplicationContext().getSystemService(Context.USAGE_STATS_SERVICE);
        AppLogger.getInstance().log("App Standby Bucket="+usm.getAppStandbyBucket()+" (low=best)");

        if ( NotificationManagerCompat.from(getApplicationContext()).areNotificationsEnabled() )
            AppLogger.getInstance().log("Notifications enabled (good)");
        else
            AppLogger.getInstance().log("Notifications NOT enabled (not good)");

        BroadcastReceiver dozeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if( pm.isDeviceIdleMode() )
                    AppLogger.getInstance().log("Entering DOZE");
                else
                    AppLogger.getInstance().log("Not in DOZE");
            }
        };
        IntentFilter filter = new IntentFilter(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
        getApplicationContext().registerReceiver(dozeReceiver, filter);
        
        startPersistentService();
    }

    private void startPersistentService() {
//so the service lives even when the app is swiped out
        Intent serviceIntent = new Intent(this, AlarmService.class);
        AppLogger.getInstance().log("MainActivity startPersistentService");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void dumpdb() {
        List<Alarm> alarms = viewModel.getAllAlarms().getValue();
        AppLogger.getInstance().log("Starting DB dump");
        StringBuilder sb = new StringBuilder();
        for (Alarm alr : alarms) {
            sb.append("#").append(alr.getId().toString()).append(" : type=").append(alr.getTypeDescr()).append("\n");
            sb.append("\tTime: ").append(alr.hours).append("h:").append(alr.minutes).append("m").append("\n");
            sb.append("\tDuration: ").append(alr.durationSeconds).append("s").append("\n");
            sb.append("\tVibration:").append(alr.vibrate ? "yes" : "no").append("\n");
            sb.append("\tSound: ").append(alr.audioText).append("\n");
            sb.append("\tEnbled: ").append(alr.enabled?"ON":"OFF");
            AppLogger.getInstance().log(sb.toString());
            sb.setLength(0);
        }
        AppLogger.getInstance().log("DB dump ended");
    }

    private void btnFixes() {
        String fileContent="";
        try {
            AssetManager assetManager = getAssets();
            InputStream inputStream = assetManager.open("blurb.txt"); // Replace with your file name
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
            fileContent = stringBuilder.toString();
            // Use fileContent as needed
            inputStream.close();
        } catch (Exception e) {
            fileContent=e.getMessage();
        }
        AppLogger.getInstance().log(fileContent);
    }

    private void showMainlayout() {
        logsLayout.setVisibility(View.GONE);
        mainLayout.setVisibility(View.VISIBLE);
    }

    private void clearLogs() {
        AppLogger.getInstance().clearLogTextView(true);
    }

    private void showLogsLayout() {
        TextView logs=findViewById(R.id.log_tw);
        AppLogger.getInstance().setLogTextView(logs);

        mainLayout.setVisibility(View.GONE);
        logsLayout.setVisibility(View.VISIBLE);
    }

    private void requestMultiplePermissions() {
        multiplePermissionLauncher.launch(new String[]
                {   "android.permission.POST_NOTIFICATIONS"
//                ,   "android.permission.WAKE_LOCK"
                ,   "android.permission.FOREGROUND_SERVICE_SPECIAL_USE"
        });
    }

    @TargetApi(Build.VERSION_CODES.S)
    private boolean canScheduleExactAlarms() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        return alarmManager.canScheduleExactAlarms();
    }

    private void showExactAlarmPermissionRationale() {
        new AlertDialog.Builder(this)
        .setTitle("Exact Alarm Permission Required")
        .setMessage("This app needs to schedule exact alarms to work properly. Please enable it in settings.")
        .setPositiveButton("Go to Settings", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            startActivity(intent);
        })
        .setNegativeButton("Cancel", null)
        .show();
    }

    private void startAllActiveAlarms() {
        AppLogger.getInstance().log("MainActivity Starting all alarms");
        List<Alarm> alarms = viewModel.getAllAlarms().getValue();
        if (alarms != null) {
            for (Alarm alarm : alarms) {
                if (alarm.enabled) {
                    alarmScheduler.scheduleAlarm(alarm);
                }
            }
        }
        Toast.makeText(this, "All alarms started!", Toast.LENGTH_SHORT).show();
    }


    private void stopAllActiveAlarms() {
        List<Alarm> alarms = viewModel.getAllAlarms().getValue();
        AppLogger.getInstance().log("MainActivity Stopping all alarms");
        if (alarms != null) {
            for (Alarm alarm : alarms) {
                if (alarm.enabled) {
                    alarmScheduler.cancelAlarm(alarm.id);
                }
            }
        }
        Toast.makeText(this, "All alarms stopped!", Toast.LENGTH_SHORT).show();
    }

    private void showDialogForNewFixedAlarm() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_new_alarm_fixed, null);
        builder.setView(dialogView);

        TimePicker timePicker = dialogView.findViewById(R.id.timePicker);
        NumberPicker durationInput = dialogView.findViewById(R.id.numberPickerDurationf);
        durationInput.setMinValue(0);
        durationInput.setMaxValue(30);
        durationInput.setValue(10);
        durationInput.setWrapSelectorWheel(false);

        Switch vibrateSwitch = dialogView.findViewById(R.id.switchVibrate);
        audioSelectionText = dialogView.findViewById(R.id.textViewAudioSelection);
        Button selectAudioButton = dialogView.findViewById(R.id.buttonSelectAudio);
        timePicker.setIs24HourView(true);
        selectedRingtoneUri = Settings.System.DEFAULT_ALARM_ALERT_URI.toString();
        audioSelectionText.setText("Default Alarm");

        selectAudioButton.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_ALARM_ALERT_URI);
            ringtonePickerLauncher.launch(intent);
        });

        builder.setTitle("New Alarm (fixed)")
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
            .setPositiveButton("Submit", (dialog, which) -> {
		            Alarm ala = new Alarm
                    (   TYPE_FIXED
                    ,   timePicker.getHour()
                    ,   timePicker.getMinute()
                    ,   durationInput.getValue()
                    ,   vibrateSwitch.isChecked()
                    ,   selectedRingtoneUri
                    ,   selectedRingtoneText
                    ,   false);
                    saveNewAlarm(ala);
            });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDialogForNewElapsedAlarm() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_new_alarm_elapsed, null);
        builder.setView(dialogView);

        NumberPicker numberPickerHours = dialogView.findViewById(R.id.numberPickerHours);
        numberPickerHours.setMinValue(0);
        numberPickerHours.setMaxValue(10);
        numberPickerHours.setValue(5);
        numberPickerHours.setWrapSelectorWheel(false);

        NumberPicker numberPickerMins = dialogView.findViewById(R.id.numberPickerMinutes);
        numberPickerMins.setMinValue(0);
        numberPickerMins.setMaxValue(60);
        numberPickerMins.setValue(30);
        numberPickerMins.setWrapSelectorWheel(false);

        NumberPicker durationInput = dialogView.findViewById(R.id.numberPickerDuration);
        durationInput.setMinValue(0);
        durationInput.setMaxValue(30);
        durationInput.setValue(10);
        durationInput.setWrapSelectorWheel(false);


        Switch vibrateSwitch = dialogView.findViewById(R.id.switchVibrate);
        audioSelectionText = dialogView.findViewById(R.id.textViewAudioSelection_e);
        Button selectAudioButton = dialogView.findViewById(R.id.buttonSelectAudio);

        selectedRingtoneUri = Settings.System.DEFAULT_ALARM_ALERT_URI.toString();
        audioSelectionText.setText("Default Alarm");

        selectAudioButton.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_ALARM_ALERT_URI);
            ringtonePickerLauncher.launch(intent);
        });

        builder.setTitle("New Alarm (Time elapsed)")
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
            .setPositiveButton("Submit", (dialog, which) -> {
            Alarm ala = new Alarm(TYPE_ELAPSED
            , numberPickerHours.getValue()
            , numberPickerMins.getValue()
            , durationInput.getValue()
            , vibrateSwitch.isChecked()
            , selectedRingtoneUri
            , selectedRingtoneText
            , false);
            saveNewAlarm(ala);
            });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void saveNewAlarm(Alarm ala) {
        viewModel.addAlarm(ala);
        Toast.makeText(this, "Alarm saved!", Toast.LENGTH_SHORT).show();
    }

    private boolean isExactAlarmPermissionGranted() {
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S ||
                checkSelfPermission("android.permission.SCHEDULE_EXACT_ALARM") == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNeededPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            requestPermissions(new String[]
                {   "android.permission.SCHEDULE_EXACT_ALARM"
                }, 101);

        }

        if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]
                {   "android.permission.POST_NOTIFICATIONS"
                }, 102);
        }

    }


    private void toggleAlarm(Alarm alarm, boolean enabled) {
        alarm.setEnabled(enabled);
        viewModel.updateAlarm(alarm);
    }

    private void deleteAlarm(Alarm alarm) {
        viewModel.deleteAlarm(alarm.id);
    }

    @Override
    protected void onDestroy() {
        AppLogger.getInstance().log("MainActivity onDestroy");
        super.onDestroy();
        AppLogger.getInstance().clearLogTextView(false);
    }

    @Override
    protected void onResume(){
        super.onResume();
//brings the app to the foreground, and the user is now able to interact with it.
//called at startup
        AppLogger.getInstance().log("MainActivity onResume");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        AppLogger.getInstance().log("MainActivity onSaveInstanceState");
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        AppLogger.getInstance().log("MainActivity onRestoreInstanceState");
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppLogger.getInstance().log("MainActivity onPause");
    }

    @Override protected void onRestart() {
        super.onRestart();
        AppLogger.getInstance().log("MainActivity onRestart");
    }
//
//
    @Override protected void onStop() {
        super.onStop();
        AppLogger.getInstance().log("MainActivity onStop");
    }

    @Override protected void onStart() {
        super.onStart(); // makes the app visible on the screen,
        AppLogger.getInstance().log("MainActivity onStart");
//but the user is not yet able to interact with it.
    }

}

