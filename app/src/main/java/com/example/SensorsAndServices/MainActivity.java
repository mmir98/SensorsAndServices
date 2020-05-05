package com.example.SensorsAndServices;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    public static final int REQUEST_ADMIN_ENABLE = 0;

    LinearLayout lockServiceSetting;
    LinearLayout alarmServiceSetting;
    LinearLayout shakeServiceSetting;

    SeekBar lockServiceSeekBar;
    SeekBar shakeServiceSeekBar;

    Switch lockServiceSwitch;
    Switch alarmServiceSwitch;
    Switch shakeServiceSwitch;

    static DevicePolicyManager devicePolicyManager;
    static ComponentName componentName;

    public static DevicePolicyManager getDevicePolicyManager(){
        return devicePolicyManager;
    }

    public static ComponentName getComponent(){
        return componentName;
    }

    Intent lockServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeSwitchListeners();


    }

    private void initializeViews(){
        lockServiceSetting = findViewById(R.id.lockServiceSettings);
        alarmServiceSetting = findViewById(R.id.alarmServiceSettings);
        shakeServiceSetting = findViewById(R.id.shakeServiceSettings);

        lockServiceSwitch = findViewById(R.id.lock_service_switch);
        alarmServiceSwitch = findViewById(R.id.alarm_switch);
        shakeServiceSwitch = findViewById(R.id.shake_switch);

        lockServiceSeekBar = findViewById(R.id.gravity_seekbar);
        shakeServiceSeekBar = findViewById(R.id.shakeServiceSeekBar);

    }

    private void initializeSwitchListeners(){
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
                    try{
                        stopService(lockServiceIntent);
                    }
                    catch (Exception e){

                    }
                }
            }
        });

        alarmServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    initializeAlarmServiceSetting();
                    //todo start alarm service
                }
                else{
                    alarmServiceSetting.setVisibility(View.GONE);
                }
            }
        });

        shakeServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent intent = new Intent(MainActivity.this, ShakeService.class);
                if (isChecked){
                    startService(intent);
                    initializeShakeServiceSetting();
                }
                else{
                    stopService(intent);
                    shakeServiceSetting.setVisibility(View.GONE);
                }
            }
        });

    }

    private void initializeLockServiceSetting() {
        lockServiceSetting.setVisibility(View.VISIBLE);
        lockServiceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "onProgressChanged: " + progress);
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

    private void initializeAlarmServiceSetting(){
        alarmServiceSetting.setVisibility(View.VISIBLE);
        //todo initialize time picker and tone picker
    }

    private void initializeShakeServiceSetting(){
        shakeServiceSetting.setVisibility(View.VISIBLE);
        shakeServiceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "onProgressChanged: SENSITIVITY CHANGED");
                Intent intent = new Intent(MainActivity.this, ShakeService.class);
                switch (progress){
                    case 0:{ //low sensitivity
                        intent.putExtra(ShakeService.CHANGE_SENSITIVITY, ShakeService.LOW_SENSITIVITY);
                        break;
                    }
                    case 1:{ // normal sensitivity
                        intent.putExtra(ShakeService.CHANGE_SENSITIVITY, ShakeService.NORMAL_SENSITIVITY);
                        break;
                    }
                    case 2:{ //high sensitivity
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


}
