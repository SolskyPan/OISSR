package com.example.demo;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;

import java.util.List;

public class SensorList extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_list);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        recyclerView.addItemDecoration(new TestDecoration(this, OrientationHelper.HORIZONTAL));

        // use a linear layout manager
        LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);


        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        final List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
        String[] sensorNameList = new String[sensorList.size()];
        for (int i = 0; i < sensorList.size(); i++) {
            sensorNameList[i] = sensorList.get(i).getName();
        }

        // specify an adapter
        RecyclerView.Adapter mAdapter = new MyAdapter(sensorNameList);
        recyclerView.setAdapter(mAdapter);

    }
}
