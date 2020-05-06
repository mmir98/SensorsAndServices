package com.example.SensorsAndServices;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ShakeService extends Service {
    private static final String TAG = "ShakeService";

    public static final String CHANGE_SENSITIVITY = "SENSITIVITY_CHANGED";

    static final int LOW_SENSITIVITY = 1000;
    static final int NORMAL_SENSITIVITY = 500;
    static final int HIGH_SENSITIVITY = 200;

    private volatile HandlerThread handlerThread;
    private ShakeServiceHandler shakeServiceHandler;

    private SensorManager sensorManager;
    private Sensor sensor;
    PowerManager powerManager;

    ScreenStatusReceiver screenStatusReceiver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        handlerThread = new HandlerThread("ShakeServiceHandlerThread");
        handlerThread.start();
        shakeServiceHandler = new ShakeServiceHandler(handlerThread.getLooper(), sensorManager, sensor, powerManager);

        IntentFilter broadCastIntent = new IntentFilter(Intent.ACTION_SCREEN_ON);
        broadCastIntent.addAction(Intent.ACTION_SCREEN_OFF);
        screenStatusReceiver = new ScreenStatusReceiver(shakeServiceHandler);
        registerReceiver(screenStatusReceiver, broadCastIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.hasExtra(CHANGE_SENSITIVITY)) {
            int newSensitivity = intent.getIntExtra(CHANGE_SENSITIVITY, NORMAL_SENSITIVITY);
            Message msg = Message.obtain();
            msg.what = ShakeServiceHandler.SENSITIVITY_CHANGED;
            msg.obj = newSensitivity;
            shakeServiceHandler.sendMessage(msg);
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(screenStatusReceiver);
        handlerThread.quit();
    }


    private final static class ShakeServiceHandler extends Handler implements SensorEventListener {
        static final int SENSITIVITY_CHANGED = 1;
        static final int THRESHOLD_ACHIEVED = 4;
        static final int DEVICE_SCREEN_ON = 3;
        static final int DEVICE_SCREEN_OFF = 2;

        int shakeThreshold = NORMAL_SENSITIVITY;

        SensorManager sensorManager;
        Sensor sensor;
        PowerManager.WakeLock wakeLock;


        private float last_x = 0;
        private float last_y = 0;
        private float last_z = 0;

        private long last_time = 0;


        ShakeServiceHandler(Looper looper, SensorManager sensorManager, Sensor sensor, PowerManager powerManager) {
            super(looper);
            this.sensorManager = sensorManager;
            this.sensor = sensor;

            wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.ON_AFTER_RELEASE, "ShakeService : WakeLock");
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case SENSITIVITY_CHANGED: {
                    Log.d(TAG, "handleMessage: SENSITIVITY_CHANGED");
                    shakeThreshold = (int) msg.obj;
                    break;
                }
                case THRESHOLD_ACHIEVED: {
                    Log.d(TAG, "handleMessage: THRESHOLD_ACHIEVED");
                    wakeLock.setReferenceCounted(false);
                    wakeLock.acquire(1000);
                    break;
                }
                case DEVICE_SCREEN_ON: {
                    Log.d(TAG, "handleMessage: DEVICE_SCREEN_ON");
                    if (wakeLock.isHeld())
                        wakeLock.release();
                    sensorManager.unregisterListener(this);
                    this.removeMessages(THRESHOLD_ACHIEVED);
                    break;
                }
                case DEVICE_SCREEN_OFF: {
                    Log.d(TAG, "handleMessage: DEVICE_SCREEN_OFF");
                    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
                    break;
                }
            }
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] values = {0, 0, 0};

            long current_time = System.currentTimeMillis();
            if (current_time - last_time > 200) {
                long difference_time = current_time - last_time;
                last_time = current_time;

                values[0] = event.values[0];
                values[1] = event.values[1];
                values[2] = event.values[2];

                float speed = Math.abs(values[0] + values[1] + values[2] - last_x - last_y - last_z) / difference_time * 10000;

                if (speed >= shakeThreshold) {
                    Log.d(TAG, "onSensorChanged: SHAKE DETECTED" + speed);
                    this.sendEmptyMessage(THRESHOLD_ACHIEVED);
                }

                last_x = values[0];
                last_y = values[1];
                last_z = values[2];
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

}
