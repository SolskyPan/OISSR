package com.example.demo;

import android.app.Activity;
import android.os.Bundle;

/**
 * Activity displaying a fragment that implements RAW photo captures.
 */
public class CameraActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2RawFragment.newInstance())
                    .commit();
        }
    }

}
