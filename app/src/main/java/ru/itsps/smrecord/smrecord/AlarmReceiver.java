package ru.itsps.smrecord.smrecord;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, SmRecordService.class);
        Log.d(this.getClass().getName(), "onReceiver!");
        if(!SmRecordService.isServiceStarted()) {
            Log.d(this.getClass().getName(), "onReceiver: start service!");
            context.startService(serviceIntent);
        }
    }
}
