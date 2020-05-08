package com.example.SensorsAndServices;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.Nullable;

public class RingtonePlayingService extends Service {

    MediaPlayer media_song;
    int startId;
    boolean isRunning;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("soundPlayer", "in start command");
        String state = intent.getExtras().getString("extra");

        // Fetch sound_choice integer values
        assert state != null;
        switch (state) {
            case "alarm on":
                startId = 1;
                break;
            case "alarm off":
                startId = 0;
                Log.e("Start ID is ", state);
                break;
            default:
                startId = 0;
                break;
        }

        if(!this.isRunning && startId == 1) {
            this.isRunning = true;
            this.startId = 0;
            media_song = MediaPlayer.create(this, R.raw.tasbihat);
            media_song.start();

            startActivity(new Intent(this, AlarmActivity.class));

            Thread vibration = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            //deprecated in API 26
                            v.vibrate(500);
                            Log.i("VibrationLog", "Vibrated");

                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            vibration.start();

            Log.i("soundPlayer", "in if (near mediaplayer)");
        } else if(this.isRunning && startId == 0) {
            media_song.stop();
            media_song.reset();

            this.isRunning = false;
            this.startId = 0;

        } else if(!this.isRunning && startId == 0) {
            this.isRunning = false;
            startId = 0;

        } else if(this.isRunning && startId == 1) {
            this.isRunning = true;
            this.startId = 1;

        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        // Tell user we stopped
        // Log.e("onDestroy called", "ta da");

        super.onDestroy();
        this.isRunning = false;
    }



}

