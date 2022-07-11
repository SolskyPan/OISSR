package com.example.demo;

import android.app.Activity;
import android.os.Bundle;

/**
 * Activity displaying a fragment that implements RAW photo captures.
 */
public class MainCamera extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.avtivity_main_camera);
        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container2, MainCameraFragment.newInstance())
                    .commit();
        }
    }

}