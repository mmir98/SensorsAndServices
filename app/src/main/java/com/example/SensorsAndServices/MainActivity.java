package com.example.SensorsAndServices;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    public static final int SHAKE_SERVICE_LOW_SENSITIVITY = 0;
    public static final int SHAKE_SERVICE_NORMAL_SENSITIVITY = 1;
    public static final int SHAKE_SERVICE_HIGH_SENSITIVITY = 2;

    public static final int REQUEST_ADMIN_ENABLE = 0;

    LinearLayout lockServiceSetting;
    LinearLayout alarmServiceSetting;
    LinearLayout shakeServiceSetting;

    SeekBar lockServiceSeekBar;
    SeekBar shakeServiceSeekBar;

    TextView lockServiceValueIndicator;
    TextView shakeServiceSensitivityIndicator;

    Switch lockServiceSwitch;
    Switch alarmServiceSwitch;
    Switch shakeServiceSwitch;

    TimePicker timePicker;

    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;
    private Intent myIntent;


    private static String userInputForLockValue = "userInputForLockValue";
    private static String userInputForShakeSensitivity = "userInputForShakeSensitivity";
    private static String lockServiceSwitchStatus = "lockServiceSwitchStatus";
    private static String shakeServiceSwitchStatus = "shakeServiceSwitchStatus";
    private static String alarmServiceSwitchStatus = "alarmServiceSwitchStatus";

    private int userInput_lockValue = 20;
    private int userInput_shakeSensitivity = SHAKE_SERVICE_NORMAL_SENSITIVITY;

    static DevicePolicyManager devicePolicyManager;
    static ComponentName componentName;

    public static DevicePolicyManager getDevicePolicyManager() {
        return devicePolicyManager;
    }

    Intent lockServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        readSettings();
        initializeSwitchListeners();


    }



    @Override
    protected void onResume() {
        super.onResume();
        alarmServiceSwitch.setChecked(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveSettings();
    }

    private void initializeViews() {
        lockServiceSetting = findViewById(R.id.lockServiceSettings);
        alarmServiceSetting = findViewById(R.id.alarmServiceSettings);
        shakeServiceSetting = findViewById(R.id.shakeServiceSettings);

        lockServiceSwitch = findViewById(R.id.lock_service_switch);
        alarmServiceSwitch = findViewById(R.id.alarm_switch);
        shakeServiceSwitch = findViewById(R.id.shake_switch);

        lockServiceSeekBar = findViewById(R.id.gravity_seekbar);
        shakeServiceSeekBar = findViewById(R.id.shakeServiceSeekBar);

        lockServiceValueIndicator = findViewById(R.id.lock_service_threshold_value);
        shakeServiceSensitivityIndicator = findViewById(R.id.shake_service_sensitivity);

        timePicker = findViewById(R.id.time_picker);
        timePicker.setIs24HourView(true);

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        myIntent = new Intent(MainActivity.this, AlarmReceiver.class);
    }

    private void initializeSwitchListeners() {
        lockServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                componentName = new ComponentName(MainActivity.this, DeviceAdminRec.class);
                if (isChecked) {
                    if (!devicePolicyManager.isAdminActive(componentName)) {
                        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
                        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Permission would be needed to perform lock.");
                        startActivityForResult(intent, REQUEST_ADMIN_ENABLE);

//                        lockServiceSetting.setVisibility(View.VISIBLE);//todo remove this line
                    } else {
                        lockServiceIntent = new Intent(MainActivity.this, LockService.class);
                        startService(lockServiceIntent);
                        lockServiceSetting.setVisibility(View.VISIBLE);
                    }
                } else {
//                    devicePolicyManager.removeActiveAdmin(componentName);
                    lockServiceSetting.setVisibility(View.GONE);
                    try {
                        stopService(lockServiceIntent);
                    } catch (Exception e) {

                    }
                }
            }
        });

        alarmServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    initializeAlarmServiceSetting();
                }
                else{
                    alarmServiceSetting.setVisibility(View.GONE);

                    alarmManager.cancel(pendingIntent);
                    myIntent.putExtra("extra", "alarm off");
                    sendBroadcast(myIntent);
                }
            }
        });

        shakeServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent intent = new Intent(MainActivity.this, ShakeService.class);
                if (isChecked) {
                    startService(intent);
                    initializeShakeServiceSetting();
                } else {
                    try {
                        stopService(intent);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                    shakeServiceSetting.setVisibility(View.GONE);
                }
            }
        });

    }

    private void initializeLockServiceSetting() {
        lockServiceSeekBar.setProgress(userInput_lockValue);
        lockServiceSetting.setVisibility(View.VISIBLE);
        lockServiceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "onProgressChanged: " + progress);
                lockServiceValueIndicator.setText(progress);
                userInput_lockValue = progress;
                Intent intent = new Intent(MainActivity.this, LockService.class);
                intent.putExtra(LockService.NEW_THRESHOLD_VALUE, (double) (90 - progress));
                startService(intent);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initializeAlarmServiceSetting(){
        alarmServiceSetting.setVisibility(View.VISIBLE);

        final Calendar calendar = Calendar.getInstance();

        Log.i("soundPlayer", "in main activity");
        calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
        calendar.set(Calendar.MINUTE, timePicker.getMinute());
        myIntent.putExtra("extra", "alarm on");

        pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    private void initializeShakeServiceSetting() {
        shakeServiceSeekBar.setProgress(userInput_shakeSensitivity);
        shakeServiceSetting.setVisibility(View.VISIBLE);
        shakeServiceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "onProgressChanged: SENSITIVITY CHANGED");
                Intent intent = new Intent(MainActivity.this, ShakeService.class);
                switch (progress) {
                    case SHAKE_SERVICE_LOW_SENSITIVITY: { //low sensitivity
                        shakeServiceSensitivityIndicator.setText(R.string.sensitivity_low);
                        userInput_shakeSensitivity = SHAKE_SERVICE_LOW_SENSITIVITY;
                        intent.putExtra(ShakeService.CHANGE_SENSITIVITY, ShakeService.LOW_SENSITIVITY);
                        break;
                    }
                    case SHAKE_SERVICE_NORMAL_SENSITIVITY: { // normal sensitivity
                        shakeServiceSensitivityIndicator.setText(R.string.sensitivity_normal);
                        userInput_shakeSensitivity = SHAKE_SERVICE_NORMAL_SENSITIVITY;
                        intent.putExtra(ShakeService.CHANGE_SENSITIVITY, ShakeService.NORMAL_SENSITIVITY);
                        break;
                    }
                    case SHAKE_SERVICE_HIGH_SENSITIVITY: { //high sensitivity
                        shakeServiceSensitivityIndicator.setText(R.string.sensitivity_high);
                        userInput_shakeSensitivity = SHAKE_SERVICE_HIGH_SENSITIVITY;
                        intent.putExtra(ShakeService.CHANGE_SENSITIVITY, ShakeService.HIGH_SENSITIVITY);
                        break;
                    }
                }
                startService(intent);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_ADMIN_ENABLE){
            if (resultCode == RESULT_OK){
                Log.d(TAG, "onActivityResult: GOT PERMISSION");
                Toast.makeText(this, "LOCK FEATURE ACTIVATED", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LockService.class);
                startService(intent);
                initializeLockServiceSetting();
            }else{
                Log.d(TAG, "onActivityResult: PERMISSION DENIED");
                Toast.makeText(this, "PERMISSION WOULD BE NEEDED", Toast.LENGTH_SHORT).show();
                lockServiceSwitch.setChecked(false);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void saveSettings(){
        final String fileDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Integer.toString(R.string.app_name);
        final String fileName = "Settings.json";
        final JSONObject setting = new JSONObject();
        try {
            setting.put(lockServiceSwitchStatus, lockServiceSwitch.isChecked());
            setting.put(alarmServiceSwitchStatus, alarmServiceSwitch.isChecked());
            setting.put(shakeServiceSwitchStatus, shakeServiceSwitch.isChecked());

            setting.put(userInputForLockValue, userInput_lockValue);
            setting.put(userInputForShakeSensitivity, userInput_shakeSensitivity);

            Log.d(TAG, "saveSettings: SAVE SUCCESSFUL");
        }
        catch (JSONException e){
            Log.d(TAG, "saveSettings: SAVE UNSUCCESSFUL");
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                StorageManager.writeOnMemory(fileDirectory, fileName, setting);
            }
        }).start();
    }

    private void readSettings(){
        final String fileDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Integer.toString(R.string.app_name);
        final String fileName = "Settings.json";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String settings = StorageManager.readFromMemory(fileDirectory, fileName);
                    final JSONObject settingsJSON = new JSONObject(settings);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                lockServiceSwitch.setChecked(settingsJSON.getBoolean(lockServiceSwitchStatus));
                                alarmServiceSwitch.setChecked(settingsJSON.getBoolean(alarmServiceSwitchStatus));
                                shakeServiceSwitch.setChecked(settingsJSON.getBoolean(shakeServiceSwitchStatus));

                                userInput_shakeSensitivity = settingsJSON.getInt(userInputForShakeSensitivity);
                                Log.d(TAG, "run: READ SUCCESSFUL");

                                lockServiceSeekBar.setProgress(settingsJSON.getInt(userInputForLockValue));
                                shakeServiceSeekBar.setProgress(settingsJSON.getInt(userInputForShakeSensitivity));
                            } catch (JSONException e) {
                                Log.d(TAG, "run: READ UNSUCCESSFUL");
                                e.printStackTrace();
                            }
                        }
                    });
                }
                catch (IOException e){
                    Log.d(TAG, "run: FILE NOT FOUND");
                    e.printStackTrace();
                }
                catch (JSONException e){
                    Log.d(TAG, "run: JSON EXCEPTION");
                    e.printStackTrace();
                }
            }
        }).start();
    }



}
