package com.example.demo;

import android.app.Activity;
import android.os.Bundle;


public class DemoVideo extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container_demo, DemoVideoFragment.newInstance())
                    .commit();
        }
    }

}