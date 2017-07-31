package ru.itsps.smrecord.smrecord;

/**
 * Created by penart on 04.10.2016.
 */


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
                Intent serviceIntent = new Intent(context, SmRecordService.class);
                context.startService(serviceIntent);
            }
        }
}


