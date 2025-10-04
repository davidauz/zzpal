package com.davidauz.zzzpal2.entity;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hours, minutes")
    LiveData<List<Alarm>> getAllAlarms();

    @Query("SELECT * FROM alarms WHERE id = :id")
    Alarm getAlarm(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Alarm alarm);

    @Update
    void update(Alarm alarm);

    @Delete
    void delete(Alarm alarm);
}
