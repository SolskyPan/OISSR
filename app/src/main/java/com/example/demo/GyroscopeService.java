package com.example.demo;

import static android.os.SystemClock.elapsedRealtimeNanos;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class GyroscopeService extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    boolean isRecording = true;
    private List<OISResult> gyroList = new ArrayList<>(), acceleList = new ArrayList<>(), magneticList = new ArrayList<>();
    long startTime;
    long unixTime;
    PowerManager.WakeLock wakeLock;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onCreate() {
        super.onCreate();


        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, GyroscopeService.class.getName());
        wakeLock.acquire();

//        NotificationChannel channel = createNotificationChannel();
//        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
//        notificationManagerCompat.createNotificationChannel(channel);
        int notificationId = 1;
        //使用兼容版本
//        NotificationCompat.Builder builder=new NotificationCompat.Builder(this);
//        //设置状态栏的通知图标
//        builder.setSmallIcon(R.mipmap.ic_launcher);
//        //设置通知栏横条的图标
////        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.screenflash_logo));
//        //禁止用户点击删除按钮删除
//        builder.setAutoCancel(false);
//        //禁止滑动删除
//        builder.setOngoing(true);
//        //右上角的时间显示
////        builder.setShowWhen(true);
//        //设置通知栏的标题内容
//        builder.setContentTitle("I am Foreground Service!!!");
//        //创建通知
//        Notification notification = builder.build();

        CharSequence name = "name";
        String description = "description";
        String channelID = "channel";
        NotificationChannel channel = new NotificationChannel(channelID, name, NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

//        Intent notificationIntent = new Intent(this, GyroscopeService.class);
//        PendingIntent pendingIntent =
//                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new Notification.Builder(this, channelID)
                        .setContentTitle("Demo")
                        .setContentText("I am Foreground Service!!!")
                        .setSmallIcon(R.mipmap.ic_launcher)
//                        .setContentIntent(pendingIntent)
                        .setTicker("I am Foreground Service!!!")
                        .build();

        startForeground(notificationId, notification);

        sensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);

        startTime = elapsedRealtimeNanos();
        unixTime = System.currentTimeMillis();

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),//加速度传感器
                SensorManager.SENSOR_DELAY_FASTEST);//获取数据速度
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),//加速度传感器
                SensorManager.SENSOR_DELAY_FASTEST);//获取数据速度
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),//加速度传感器
                SensorManager.SENSOR_DELAY_FASTEST);//获取数据速度

    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE && isRecording) {
            OISResult oisResult = new OISResult();
            oisResult.x = sensorEvent.values[0];
            oisResult.y = sensorEvent.values[1];
            oisResult.z = sensorEvent.values[2];
            oisResult.timestamp = sensorEvent.timestamp;
            gyroList.add(oisResult);
        }
        if(sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION && isRecording) {
            OISResult oisResult = new OISResult();
            oisResult.x = sensorEvent.values[0];
            oisResult.y = sensorEvent.values[1];
            oisResult.z = sensorEvent.values[2];
            oisResult.timestamp = sensorEvent.timestamp;
            acceleList.add(oisResult);
        }
        if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD && isRecording) {
            OISResult oisResult = new OISResult();
            oisResult.x = sensorEvent.values[0];
            oisResult.y = sensorEvent.values[1];
            oisResult.z = sensorEvent.values[2];
            oisResult.timestamp = sensorEvent.timestamp;
            magneticList.add(oisResult);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    @Override
    public void onDestroy() {
        saveTxt();

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
//        sensorManager.unregisterListener(this);//解除监听注册
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),//加速度传感器
                SensorManager.SENSOR_DELAY_FASTEST);//获取数据速度
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),//加速度传感器
                SensorManager.SENSOR_DELAY_FASTEST);//获取数据速度
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),//加速度传感器
                SensorManager.SENSOR_DELAY_FASTEST);//获取数据速度

        return Service.START_STICKY;
    }


    private void saveTxt() {
        final File dir = getExternalFilesDir(null);
        String filePrefix = (dir == null ? "" : (dir.getAbsolutePath() + "/"))
                + startTime + "_" + unixTime;
        // 输出 csv
        StringBuilder buffer = new StringBuilder();
        buffer.append("X,Y,Z,timestamp\r\n");
        for (OISResult result : gyroList) {
            buffer.append(result.x).append(",").append(result.y).append(",").append(result.z)
                    .append(",").append(result.timestamp).append("\r\n");
        }
        try {
            String data = buffer.toString();
            String filename = filePrefix + "_GYROSCOPE.txt";

            // String path = Environment.getExternalStorageDirectory()+"/Users";
            File file = new File(filename);
            OutputStream out=new FileOutputStream(file);
            out.write(data.getBytes());
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        StringBuilder buffer2 = new StringBuilder();
        buffer2.append("X,Y,Z,timestamp\r\n");
        for (OISResult result : acceleList) {
            buffer2.append(result.x).append(",").append(result.y).append(",").append(result.z)
                    .append(",").append(result.timestamp).append("\r\n");
        }
        try {
            String data = buffer2.toString();
            String filename = filePrefix + "_ACCELERATION.txt";

            // String path = Environment.getExternalStorageDirectory()+"/Users";
            File file = new File(filename);
            OutputStream out=new FileOutputStream(file);
            out.write(data.getBytes());
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        StringBuilder buffer3 = new StringBuilder();
        buffer3.append("X,Y,Z,timestamp\r\n");
        for (OISResult result : magneticList) {
            buffer3.append(result.x).append(",").append(result.y).append(",").append(result.z)
                    .append(",").append(result.timestamp).append("\r\n");
        }
        try {
            String data = buffer3.toString();
            String filename = filePrefix + "_MAGNETIC.txt";

            File file = new File(filename);
            OutputStream out=new FileOutputStream(file);
            out.write(data.getBytes());
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
