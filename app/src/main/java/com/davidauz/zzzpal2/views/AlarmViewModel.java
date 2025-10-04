package com.davidauz.zzzpal2.views;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.davidauz.zzzpal2.database.AlarmDatabase;
import com.davidauz.zzzpal2.database.AlarmRepository;
import com.davidauz.zzzpal2.entity.Alarm;
import com.davidauz.zzzpal2.service.AppLogger;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AlarmViewModel extends AndroidViewModel {
    private AlarmRepository repository;
    private LiveData<List<Alarm>> allAlarms;
    private Executor executor = Executors.newSingleThreadExecutor();

    public AlarmViewModel(@NonNull Application application) {
        super(application);
        AlarmDatabase db = AlarmDatabase.getDatabase(application);
        repository = new AlarmRepository(db, executor);
        allAlarms = repository.getAllAlarms();
    }

    public LiveData<List<Alarm>> getAllAlarms() {
        return allAlarms;
    }

    public void toggleAlarm(long id) {
        executor.execute(() -> {
            Alarm alarm = repository.getAlarm(id);
            if (alarm != null) {
                alarm.enabled = !alarm.enabled;
                repository.update(alarm);
            }
        });
    }

    public void addAlarm(Alarm ala) {
        repository.insert(ala);
    }

    public void deleteAlarm(long id) {
        AppLogger.getInstance().log("Deleting alarm #"+id);
        executor.execute(() -> {
            Alarm alarm = repository.getAlarm(id);
            if (alarm != null) {
                repository.delete(alarm);
            }
        });
    }

    public void updateAlarm(Alarm alarm) {
        executor.execute(() -> {
            repository.update(alarm);
        });
    }

    public Alarm getAlarmById(long id) {
        return repository.getAlarm(id);
    }
}
