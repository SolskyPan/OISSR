package com.example.demo;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FrontCamera extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_front_camera);


        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, FrontCameraFragment.newInstance())
                    .commit();
        }
    }



}