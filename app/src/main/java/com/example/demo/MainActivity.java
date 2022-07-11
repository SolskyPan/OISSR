package com.example.demo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        String[] permissions = new String[]{Manifest.permission.ACTIVITY_RECOGNITION,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WAKE_LOCK};
        List<String> needGranted = new ArrayList<>();
        for (String permission : permissions) {
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                needGranted.add(permission);
            }
        }
        if(!needGranted.isEmpty()) {
            ActivityCompat.requestPermissions(this, needGranted.toArray(new String[0]), 1);
        }
    }

    public void showSensorList(View view) {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensorList) {
            Log.d("sensor", "onResume: "+sensor.getName());
        }
        Intent intent = new Intent(this, SensorList.class);
        startActivity(intent);
    }

    public void gyroscopeDemo(View view) {
        Intent intent = new Intent(this, GyroscopeDemoActivity.class);
        startActivity(intent);
    }

    public void cameraDemo(View view) {
        Intent intent = new Intent(this, CameraDemo.class);
        startActivity(intent);
    }

    public void FrontCameraDemo(View view) {
        Intent intent = new Intent(this, MainCamera.class);
        startActivity(intent);
    }

    public void PictureDemo(View view) {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    public void ChartDemo(View view) {
        Intent intent = new Intent(this, ChartActivity.class);
        startActivity(intent);
    }

    public void DepthDemo(View view) {
        Intent intent = new Intent(this, DepthActivity.class);
        startActivity(intent);
    }

    public void DemoDemo(View view) {
        Intent intent = new Intent(this, DemoVideo.class);
        startActivity(intent);
    }

    public void DualCamera(View view) {
        Intent intent = new Intent(this, DualCameraActivity.class);
        startActivity(intent);
    }
}