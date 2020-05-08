package com.example.SensorsAndServices;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;

public class AlarmActivity extends AppCompatActivity implements SensorEventListener {
    Button cancelButton;
    PowerManager.WakeLock wakeLock;
    SeekBar shakeServiceSeekBar;

    public static final int SHAKE_SERVICE_LOW_SENSITIVITY = 0;
    public static final int SHAKE_SERVICE_NORMAL_SENSITIVITY = 1;
    public static final int SHAKE_SERVICE_HIGH_SENSITIVITY = 2;
    private int userInput_shakeSensitivity = SHAKE_SERVICE_NORMAL_SENSITIVITY;

    private static final String TAG = "AlarmShakeService";

    private float last_z = 0;
    private long last_time = 0;

    SensorManager sensorManager;
    Sensor sensor;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        unlockScreen();

        cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                System.exit(0);
            }
        });

        Thread cancelTime = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10 * 60 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                finish();
                System.exit(0);
            }
        });
        cancelTime.start();

    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, sensor);
    }

    private void unlockScreen() {
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d(TAG, "onSensorChanged: CHANGE DETECTED");
        float[] values = {0, 0, 0};

        long current_time = System.currentTimeMillis();
        if (current_time - last_time > 200) {
            long difference_time = current_time - last_time;
            last_time = current_time;

            values[2] = event.values[2];

            float speed = Math.abs(values[2] - last_z) / difference_time * 10000;

            if (speed >= 100) {
                Log.d(TAG, "onSensorChanged: SHAKE DETECTED" + speed);
                finish();
                System.exit(0);
            }
            last_z = values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
