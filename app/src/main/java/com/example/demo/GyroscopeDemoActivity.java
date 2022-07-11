package com.example.demo;

import android.app.NotificationChannel;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static android.os.SystemClock.elapsedRealtimeNanos;


public class GyroscopeDemoActivity extends AppCompatActivity {
    TextView textViewX, textViewY, textViewZ, textViewTime;
    Button button;
    boolean isRecording = false;
    Intent intent;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gyroscope_demo);
        textViewX = findViewById(R.id.textView);
        textViewY = findViewById(R.id.textView2);
        textViewZ = findViewById(R.id.textView3);
        textViewTime = findViewById(R.id.textView4);
        button = findViewById(R.id.button9);
        intent = new Intent(this, GyroscopeService.class);

//        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//        try {
//            for (String cameraId : manager.getCameraIdList()) {
//                Log.d("Camera ID: ", cameraId);
//                CameraCharacteristics characteristics
//                        = manager.getCameraCharacteristics(cameraId);
//                Log.d("Character: ", Arrays.toString(characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)));
//                Log.d("Lens Facing: ", String.valueOf(characteristics.get(CameraCharacteristics.LENS_FACING)));
//                Log.d("Lens Facing: ", Arrays.toString(characteristics.getPhysicalCameraIds().toArray()));
//
//                StreamConfigurationMap map = characteristics.get(
//                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//
//                // For still image captures, we use the largest available size.
////                Size largestJpeg = Collections.max(
////                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
////                        new Camera2RawFragment.CompareSizesByArea());
////                Log.d("Size: ", largestJpeg.toString());
////                Log.d("---------------", "");
//            }
//
//            for (int i = 2; i < 5; i++) {
//                String cameraId = String.valueOf(i);
//                Log.d("Camera ID: ", cameraId);
//                CameraCharacteristics characteristics
//                        = manager.getCameraCharacteristics(cameraId);
//                Log.d("Character: ", Arrays.toString(characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)));
//                Log.d("Lens Facing: ", String.valueOf(characteristics.get(CameraCharacteristics.LENS_FACING)));
//
//
//                // For still image captures, we use the largest available size.
////                Size largestJpeg = Collections.max(
////                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
////                        new Camera2RawFragment.CompareSizesByArea());
////                Log.d("Size: ", largestJpeg.toString());
////                Log.d("---------------", "");
//            }
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }

//        getDualCamera(getApplicationContext());
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public static void getDualCamera(Context context){
//        DualCamera dualCamera = new DualCamera();
        //获取管理类
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        assert manager != null;
        try {
            //获取所有逻辑ID
            String[] cameraIdList = manager.getCameraIdList();

            //获取逻辑摄像头下拥有多个物理摄像头的类 作为双镜类
            for (String id : cameraIdList) {
                try {
                    CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(id);
                    Set<String> physicalCameraIds = cameraCharacteristics.getPhysicalCameraIds();
                    Log.d("TAG", "逻辑ID：" + id + " 下的物理ID: " + Arrays.toString(physicalCameraIds.toArray()));
                    if (physicalCameraIds.size() >= 2) {
//                        dualCamera.setLogicCameraId(id);
                        Object[] objects = physicalCameraIds.toArray();
                        //获取前两个物理摄像头作为双镜头
//                        dualCamera.setPhysicsCameraId1(String.valueOf(objects[0]));
//                        dualCamera.setPhysicsCameraId2(String.valueOf(objects[1]));
                        return;
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }





    public void onButtonClick(View view) {
        isRecording = !isRecording;
        if (isRecording) {
            startService(intent);
            button.setText(R.string.stop);
        } else {
            stopService(intent);
            button.setText(R.string.record);
        }
    }

}
