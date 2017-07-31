package ru.itsps.smrecord.smrecord;

/**
 * Created by penart on 04.10.2016.
 */

public class RecordRow {
    private int id;
    private String call_start;
    private String call_stop;
    private int call_duration;
    private int call_unanswered;
    private int call_direction;
    private String call_remote_phone;
    private String call_record_file;

    public String getCall_start() {
        return call_start;
    }

    public void setCall_start(String call_start) {
        this.call_start = call_start;
    }

    public String getCall_stop() {
        return call_stop;
    }

    public void setCall_stop(String call_stop) {
        this.call_stop = call_stop;
    }

    public int getCall_duration() {
        return call_duration;
    }

    public void setCall_duration(int call_duration) {
        this.call_duration = call_duration;
    }

    public int getCall_unanswered() {
        return call_unanswered;
    }

    public void setCall_unanswered(int call_unanswered) {
        this.call_unanswered = call_unanswered;
    }

    public int getCall_direction() {
        return call_direction;
    }

    public void setCall_direction(int call_direction) {
        this.call_direction = call_direction;
    }

    public String getCall_remote_phone() {
        return call_remote_phone;
    }

    public void setCall_remote_phone(String call_remote_phone) {
        this.call_remote_phone = call_remote_phone;
    }

    public String getCall_record_file() {
        return call_record_file;
    }

    public void setCall_record_file(String call_record_file) {
        this.call_record_file = call_record_file;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
