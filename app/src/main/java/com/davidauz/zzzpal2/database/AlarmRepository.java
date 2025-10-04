package com.davidauz.zzzpal2.database;

import androidx.lifecycle.LiveData;

import com.davidauz.zzzpal2.entity.Alarm;
import com.davidauz.zzzpal2.entity.AlarmDao;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

public class AlarmRepository {
    private AlarmDao alarmDao;
    private Executor executor;

    public AlarmRepository(AlarmDatabase database, Executor executor) {
        this.alarmDao = database.alarmDao();
        this.executor=executor;
    }

    public LiveData<List<Alarm>> getAllAlarms() {
        return alarmDao.getAllAlarms();
    }

    public void insert(Alarm alarm) {
        executor.execute(() -> {
            alarmDao.insert(alarm);
        });
    }

    public void update(Alarm alarm) {
        alarmDao.update(alarm);
    }

    public void delete(Alarm alarm) {
        executor.execute(() -> {
            alarmDao.delete(alarm);
        });
    }

    public Alarm getAlarm(long id) {
        return alarmDao.getAlarm(id);
    }

}
