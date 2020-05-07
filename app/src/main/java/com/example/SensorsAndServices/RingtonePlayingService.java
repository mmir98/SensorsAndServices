package com.example.SensorsAndServices;


import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
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
        int sound_id = intent.getExtras().getInt("sound_choice");
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

