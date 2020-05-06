package com.example.SensorsAndServices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("soundPlayer", "in alarm receiver");
        String fetch_string = intent.getExtras().getString("extra");
        Intent serviceIntent = new Intent(context, RingtonePlayingService.class);
        serviceIntent.putExtra("extra", fetch_string);
        context.startService(serviceIntent);
    }
}

