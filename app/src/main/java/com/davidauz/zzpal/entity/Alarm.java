package com.davidauz.zzpal.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "alarms")
public class Alarm {
    public static final int TYPE_FIXED=1
    ,   TYPE_ELAPSED=2
    ,   TYPE_RECURRING=3
    ;

    @PrimaryKey(autoGenerate = true) public long id;
    public int type;
    public int hours;
    public int minutes;
    public int durationSeconds;
    public boolean vibrate;
    public String audioUri;
    public String audioText;
    public boolean enabled;

    public Alarm(int type, int hours, int minutes, int durationSeconds, boolean vibrate, String audioUri, String audioText, boolean enabled) {
        this.type=type;
        this.hours = hours;
        this.minutes = minutes;
        this.durationSeconds = durationSeconds;
        this.vibrate = vibrate;
        this.audioUri = audioUri;
        this.audioText = audioText;
        this.enabled = enabled;
    }

    public Object getId() {
        return id;
    }

    public void setEnabled(boolean en) {
        enabled=en;
    }

    public boolean sameas(Alarm newItem) {
        return this.hours == newItem.hours
        && this.minutes == newItem.minutes
        && this.durationSeconds == newItem.durationSeconds
        && this.vibrate == newItem.vibrate
        && this.audioUri.equals(newItem.audioUri)
        && this.enabled == newItem.enabled
        && this.type == newItem.type;
    }

    public String getTypeDescr() {
        switch(this.type) {
            case TYPE_FIXED -> {return "Fixed";}
            case TYPE_ELAPSED -> {return "Interval";}
            case TYPE_RECURRING -> {return "Recurring";}
        }
        return this.type+": unknown";
    }

}
