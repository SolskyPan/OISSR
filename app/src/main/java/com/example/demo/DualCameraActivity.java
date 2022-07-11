package com.example.demo;

import android.app.Activity;
import android.os.Bundle;

public class DualCameraActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dual);
        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container_dual, DualCameraFragment.newInstance())
                    .commit();
        }
    }
}
