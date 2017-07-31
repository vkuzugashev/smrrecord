package ru.itsps.smrecord.smrecord;

/**
 * Created by penart on 04.10.2016.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;

public class CallReceiver extends BroadcastReceiver {

    private static final String TAG = "CallReceiver";
    private final static String simSlotName[] = {
            "extra_asus_dial_use_dualsim",
            "com.android.phone.extra.slot",
            "slot",
            "simslot",
            "sim_slot",
            "subscription",
            "Subscription",
            "phone",
            "com.android.phone.DialingMode",
            "simSlot",
            "slot_id",
            "simId",
            "simnum",
            "phone_type",
            "slotId",
            "slotIdx"
    };

    @Override
    public void onReceive(Context context, Intent intent) {
            String originalNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            /*
            int whichSIM = 0; // this for security fallback to SIM 1
            if (intent.getExtras().containsKey("subscription")) {
                whichSIM = intent.getExtras().getInt("subscription");
                Log.d(TAG, "intent.getExtras().containsKey(\"subscription\"), whichSIM="+whichSIM);
            }
            else
                Log.d(TAG, "whichSIM="+whichSIM);
            //int slot = intent.getIntExtra("com.android.phone.extra.slot", -1);
            int slot = intent.getIntExtra("simId", -1);
            for (String sim : simSlotName) {
                Log.d(TAG, sim+"="+intent.getIntExtra(sim, -1));
            }
            Log.v(TAG, "SIM: " + slot);*/
            //Брать только первый слот
            if (originalNumber != null /*&& slot == 0*/) {
                originalNumber = originalNumber.replace(" ", "");
                originalNumber = originalNumber.replace("-", "");
                SmRecordService.setOutcoming_nr(originalNumber);
                //SmRecordService.setSlot(slot);
                Log.v(TAG, "originalNumber: " + originalNumber);
            }
            /*else if (slot == 1) {
                SmRecordService.setSlot(slot);
            }*/
    }
}
