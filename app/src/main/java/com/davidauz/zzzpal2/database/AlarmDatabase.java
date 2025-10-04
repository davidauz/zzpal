package com.davidauz.zzzpal2.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.davidauz.zzzpal2.entity.AlarmDao;
import com.davidauz.zzzpal2.entity.Alarm;

@Database(entities = {Alarm.class}, version = 3, exportSchema = false)
public abstract class AlarmDatabase extends RoomDatabase {
    public abstract AlarmDao alarmDao();

    private static volatile AlarmDatabase INSTANCE;

    public static AlarmDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AlarmDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AlarmDatabase.class,
                            "alarm_database"
                    ).fallbackToDestructiveMigration() // when increasing the version number
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
