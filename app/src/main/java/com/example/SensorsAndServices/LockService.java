package com.example.SensorsAndServices;

import android.app.Service;
import android.app.admin.DevicePolicyManager;
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
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LockService extends Service {
    private static final String TAG = "LockService";

    public static final String NEW_THRESHOLD_VALUE = "NEW_THRESHOLD_VALUE";

    private SensorManager sensorManager;
    private Sensor gravitySensor;

    private volatile HandlerThread mHandlerThread;
    private ServiceHandler mServiceHandler;

    private ScreenStatusReceiver screenStatusReceiver;


    @Override
    public void onCreate() {
        Toast.makeText(this, "Lock Service Enabled", Toast.LENGTH_SHORT).show();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } else {
            Log.d(TAG, "onCreate: SENSOR MANAGER IS NULL");
        }

//        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

//        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        DevicePolicyManager devicePolicyManager = MainActivity.getDevicePolicyManager();

        mHandlerThread = new HandlerThread("LockServiceHandlerThread");
        mHandlerThread.start();

        mServiceHandler = new ServiceHandler(mHandlerThread.getLooper(), sensorManager, gravitySensor, devicePolicyManager);

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenStatusReceiver = new ScreenStatusReceiver(mServiceHandler);
        registerReceiver(screenStatusReceiver, intentFilter);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.hasExtra(NEW_THRESHOLD_VALUE)) {
                double threshold_angle = intent.getDoubleExtra(NEW_THRESHOLD_VALUE, 70);
                Message msg = Message.obtain();
                msg.what = ServiceHandler.NEW_ANGLE_THRESHOLD;
                msg.obj = threshold_angle;
                mServiceHandler.sendMessage(msg);

            Log.d(TAG, "onStartCommand: " + threshold_angle);
        }


        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(screenStatusReceiver);
        mHandlerThread.quit();
    }


    private static final class ServiceHandler extends Handler implements SensorEventListener {
        static final int THRESHOLD_ACHIEVED = 1;
        static final int DEVICE_SCREEN_IS_OFF = 2;
        static final int DEVICE_SCREEN_IS_ON = 3;
        static final int NEW_ANGLE_THRESHOLD = 4;

        private double threshold_angle = 70;
        SensorManager sensorManager;
        Sensor sensor;
        DevicePolicyManager devicePolicyManager;

        ServiceHandler(Looper looper, SensorManager sensorManager, Sensor sensor, DevicePolicyManager devicePolicyManager) {
            super(looper);
            this.sensorManager = sensorManager;
            this.sensor = sensor;
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            this.devicePolicyManager = devicePolicyManager;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case DEVICE_SCREEN_IS_OFF: {
                    Log.d(TAG, "handleMessage: SCREEN_OFF");
                    sensorManager.unregisterListener(this);
                    this.removeMessages(THRESHOLD_ACHIEVED);
                    break;
                }
                case DEVICE_SCREEN_IS_ON: {
                    Log.d(TAG, "handleMessage: SCREEN_ON");
                    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
                    break;
                }
                case THRESHOLD_ACHIEVED: {
                    Log.d(TAG, "handleMessage: THRESHOLD ACHIEVED");
//                    if (devicePolicyManager.isAdminActive(MainActivity.getComponent()))
                        devicePolicyManager.lockNow();

//                    this.sendEmptyMessage(DEVICE_SCREEN_IS_OFF);
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Intent turnOfActivity = new Intent(LockServiceWithThread.this, TurnOffActivity.class);
//                            turnOfActivity.putExtra("receiver", screenStatusReceiver);
//                            startActivity(turnOfActivity);
//                        }
//                    }).start();

//                    final PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
//                            LockServiceWithThread.TAG + ": wake_lock");
//                    try{
//                        wakeLock.release();
//                    }catch (Exception e){
//
//                    }
//
//                    wakeLock.acquire(1000);
//                    this.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            wakeLock.release();
//                        }
//                    }, 2000);
//                    wakeLock.release();

                    break;
                }
                case NEW_ANGLE_THRESHOLD: {
                    Log.d(TAG, "handleMessage: NEW_THRESHOLD");
                    threshold_angle = (double) msg.obj;
                    break;
                }
            }
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            final double alpha = 3;
            double phone_angle = Math.abs(Math.toDegrees(Math.asin(event.values[2] / 9.81)));

            if (phone_angle + alpha >= threshold_angle || Double.isNaN(phone_angle)) {
                Log.d(TAG, "onSensorChanged: " + phone_angle);
                Message msg = Message.obtain();
                msg.what = THRESHOLD_ACHIEVED;
                this.sendMessage(msg);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

}
