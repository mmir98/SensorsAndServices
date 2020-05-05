package com.example.SensorsAndServices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;


public class ScreenStatusReceiver extends BroadcastReceiver{
    private static final String TAG = "ScreenStatusReceiver";

    public static final int DEVICE_SCREEN_ON = 3;
    public static final int DEVICE_SCREEN_OFF = 2;

    private Handler handler;

    ScreenStatusReceiver(Handler handler){
        this.handler = handler;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)){
                Log.d(TAG, "onReceive: SCREEN ON");
                handler.sendEmptyMessage(DEVICE_SCREEN_ON);
            }
            else {
                Log.d(TAG, "onReceive: SCREEN OFF");
                handler.sendEmptyMessage(DEVICE_SCREEN_OFF);
            }
    }
}
