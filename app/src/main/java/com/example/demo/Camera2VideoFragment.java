/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.DngCreator;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OisSample;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static android.os.SystemClock.elapsedRealtimeNanos;

public class Camera2VideoFragment extends Fragment
        implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback,
        OnChartValueSelectedListener, SensorEventListener {

    private List<OISResult> oisSamplesExport;



    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    private static final String TAG = "Camera2VideoFragment";
    private static final int REQUEST_VIDEO_PERMISSIONS = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
    };

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * Button to record video
     */
    private Button mButtonVideo;

    private Button mButtonMusic;

    private Button mOISSwitch;

    private boolean oisFlag = true;

    private ImageReader mImageReader;

    private byte[] mYuvBytes;
    private boolean mIsShutter;

    private LineChart lineChartX;
    private LineChart lineChartY;
    private LineChart lineChartZ;
    private SensorManager sensorManager;
    private List<float[]> lineData = new ArrayList<>();
    boolean flag = true;
    List<Entry> xData = new ArrayList<>();
    List<Entry> yData = new ArrayList<>();
    List<Entry> zData = new ArrayList<>();


    /**
     * A lock protecting camera state.
     */
    private final Object mCameraStateLock = new Object();

    private CameraCharacteristics mCharacteristics;

    private RefCountedAutoCloseable<ImageReader> mRawImageReader;

    private String mCameraId;

    /**
     * Request ID to {@link ImageSaver.ImageSaverBuilder} mapping for in-progress RAW captures.
     */
    private final TreeMap<Integer, ImageSaver.ImageSaverBuilder> mRawResultQueue = new TreeMap<>();

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * RAW image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnRawImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            dequeueAndSaveImage(mRawResultQueue, mRawImageReader);
        }

    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            if (image == null) {
                return;
            }

            int width = mPreviewSize.getWidth();
            int height = mPreviewSize.getHeight();
            if (mYuvBytes == null) {
                // YUV420 大小总是 width * height * 3 / 2
                mYuvBytes = new byte[width * height * 3 / 2];
            }

            // YUV_420_888
            Image.Plane[] planes = image.getPlanes();

            // Y通道，对应planes[0]
            // Y size = width * height
            // yBuffer.remaining() = width * height;
            // pixelStride = 1
            ByteBuffer yBuffer = planes[0].getBuffer();
            int yLen = width * height;
            yBuffer.get(mYuvBytes, 0, yLen);
            // U通道，对应planes[1]
            // U size = width * height / 4;
            // uBuffer.remaining() = width * height / 2;
            // pixelStride = 2
            ByteBuffer uBuffer = planes[1].getBuffer();
            int pixelStride = planes[1].getPixelStride(); // pixelStride = 2
            for (int i = 0; i < uBuffer.remaining(); i+=pixelStride) {
                mYuvBytes[yLen++] = uBuffer.get(i);
            }
            // V通道，对应planes[2]
            // V size = width * height / 4;
            // vBuffer.remaining() = width * height / 2;
            // pixelStride = 2
            ByteBuffer vBuffer = planes[2].getBuffer();
            pixelStride = planes[2].getPixelStride(); // pixelStride = 2
            for (int i = 0; i < vBuffer.remaining(); i+=pixelStride) {
                mYuvBytes[yLen++] = vBuffer.get(i);
            }

            if (mIsShutter) {
                mIsShutter = false;

                // save yuv data
//                String yuvPath = FileUtil.SAVE_DIR + System.currentTimeMillis() + ".yuv";
//                FileUtil.saveBytes(mYuvBytes, yuvPath);
//
//                // save bitmap data
//                String jpgPath = yuvPath.replace(".yuv", ".jpg");
//                Bitmap bitmap = ColorConvertUtil.yuv420pToBitmap(mYuvBytes, width, height);
//                FileUtil.saveBitmap(bitmap, jpgPath);
            }

            // 一定不能忘记close
            image.close();
        }
    };

    // music
    private AssetManager mAssetManager;
    private MediaPlayer mPlayer;

    private boolean musicFlag = false;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * A reference to the current {@link CameraCaptureSession} for
     * preview.
     */
    private CameraCaptureSession mPreviewSession;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @RequiresApi(api = Build.VERSION_CODES.P)
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                              int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

    };

    /**
     * The {@link Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * The {@link Size} of video recording.
     */
    private Size mVideoSize;

    /**
     * MediaRecorder
     */
    private MediaRecorder mMediaRecorder;

    /**
     * Whether the app is recording video now
     */
    private boolean mIsRecordingVideo = false;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its status.
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
            mCameraOpenCloseLock.release();
            if (null != mTextureView) {
                configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };
    private Integer mSensorOrientation;
    private String mNextVideoAbsolutePath;
    private CaptureRequest.Builder mPreviewBuilder;

    public static Camera2VideoFragment newInstance() {
        return new Camera2VideoFragment();
    }

    /**
     * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getHeight() <= 1080) {
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera2_video, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {


        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        mButtonVideo = (Button) view.findViewById(R.id.video);
        mButtonVideo.setOnClickListener(this);

//        mButtonMusic = (Button) view.findViewById(R.id.music);
//        mButtonMusic.setOnClickListener(this);

        mOISSwitch = view.findViewById(R.id.ois);
        mOISSwitch.setOnClickListener(this);

        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        lineChartX = view.findViewById(R.id.lineChartX);
        lineChartY = view.findViewById(R.id.lineChartY);
        // lineChartZ = view.findViewById(R.id.lineChartZ);

        initLineChart();
    }

    private void initLineChart() {
        lineChartX.setOnChartValueSelectedListener(this);
        lineChartX.getDescription().setEnabled(false);
        lineChartX.setBackgroundColor(Color.WHITE);
        lineChartX.setRotation(90);


        //自定义适配器，适配于X轴
        XAxis xAxis = lineChartX.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        YAxis leftAxis = lineChartX.getAxisLeft();
        leftAxis.setLabelCount(4, false);
//        leftAxis.setAxisMinimum(-12);
//        leftAxis.setAxisMaximum(12);
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        //leftAxis.setSpaceTop(15f);

        lineChartX.getAxisRight().setEnabled(false);


        lineChartY.setOnChartValueSelectedListener(this);
        lineChartY.getDescription().setEnabled(false);
        lineChartY.setBackgroundColor(Color.WHITE);
        lineChartY.setRotation(90);

        //自定义适配器，适配于X轴
        XAxis xAxisY = lineChartY.getXAxis();
        xAxisY.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisY.setGranularity(1f);

        YAxis leftAxisY = lineChartY.getAxisLeft();
        leftAxisY.setLabelCount(4, false);
//        leftAxisY.setAxisMinimum(-12);
//        leftAxisY.setAxisMaximum(12);
        leftAxisY.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        //leftAxisY.setSpaceTop(15f);

        lineChartY.getAxisRight().setEnabled(false);


//        lineChartZ.setOnChartValueSelectedListener(this);
//        lineChartZ.getDescription().setEnabled(false);
//        lineChartZ.setBackgroundColor(Color.WHITE);
//        lineChartZ.setRotation(90);
//
//        //自定义适配器，适配于X轴
//        XAxis xAxisZ = lineChartZ.getXAxis();
//        xAxisZ.setPosition(XAxis.XAxisPosition.BOTTOM);
//        xAxisZ.setGranularity(1f);
//
//        YAxis leftAxisZ = lineChartZ.getAxisLeft();
//        leftAxisZ.setLabelCount(4, false);
////        leftAxisZ.setAxisMinimum(-12);
////        leftAxisZ.setAxisMaximum(12);
//        leftAxisZ.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
//        //leftAxisZ.setSpaceTop(15f);
//
//        lineChartZ.getAxisRight().setEnabled(false);

        setLineChartData();
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }


    private void setLineChartData() {
        flag = false;
        //填充数据，在这里换成自己的数据源

        for (int i = yData.size(); i < lineData.size(); i++) {
            //xData.add(new Entry(i, lineData.get(i)[0]));
            yData.add(new Entry(i, lineData.get(i)[1]));
            zData.add(new Entry(i, lineData.get(i)[2]));
            //Log.d("chart", String.valueOf(lineData.get(i)[0]));
        }
        //Log.d("chart", String.valueOf(xData));
        //Log.d("chart", String.valueOf(lineData));

        //这里，每重新new一个LineDataSet，相当于重新画一组折线
        //每一个LineDataSet相当于一组折线。比如:这里有两个LineDataSet：setComp1，setComp2。

        LineDataSet setComp1 = new LineDataSet(zData, "X Data");
        setComp1.setAxisDependency(YAxis.AxisDependency.LEFT);
        setComp1.setColor(getResources().getColor(R.color.colorAccent));
        setComp1.setDrawCircles(false);
        setComp1.setMode(LineDataSet.Mode.LINEAR);

        LineDataSet setComp2 = new LineDataSet(yData, "Y Data");
        setComp2.setAxisDependency(YAxis.AxisDependency.LEFT);
        setComp2.setColor(getResources().getColor(R.color.red));
        setComp2.setDrawCircles(false);
        setComp2.setMode(LineDataSet.Mode.LINEAR);

//        LineDataSet setComp3 = new LineDataSet(zData, "Z Data");
//        setComp3.setAxisDependency(YAxis.AxisDependency.LEFT);
//        setComp3.setColor(getResources().getColor(R.color.green));
//        setComp3.setDrawCircles(false);
//        setComp3.setMode(LineDataSet.Mode.LINEAR);


        List<ILineDataSet> dataSetsX = new ArrayList<>();
        List<ILineDataSet> dataSetsY = new ArrayList<>();
        //List<ILineDataSet> dataSetsZ = new ArrayList<>();
        dataSetsX.add(setComp1);
        dataSetsY.add(setComp2);
        //dataSetsZ.add(setComp3);

        LineData lineDataX = new LineData(dataSetsX);
        lineChartX.setData(lineDataX);
        //lineChartX.setVisibleXRangeMaximum(100);
        //lineChartX.moveViewToX(100);
        lineChartX.invalidate();

        LineData lineDataY = new LineData(dataSetsY);
        lineChartY.setData(lineDataY);
        lineChartY.invalidate();

//        LineData lineDataZ = new LineData(dataSetsZ);
//        lineChartZ.setData(lineDataZ);
//        lineChartZ.invalidate();


        flag = true;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE && mIsRecordingVideo) {
//            lineData.add(new float[]{sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]});
//            if(flag) {
//                setLineChartData();
//            }
            OISResult oisResult = new OISResult();
            oisResult.x = sensorEvent.values[0];
            oisResult.y = sensorEvent.values[1];
            oisResult.z = sensorEvent.values[2];
            oisResult.timestamp = sensorEvent.timestamp;
            oisSamplesExport.add(oisResult);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);//获取数据速度
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        sensorManager.unregisterListener(this);//解除监听注册
        super.onPause();
    }

    @SuppressLint("NonConstantResourceId")
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.video: {
                if (mIsRecordingVideo) {
                    stopRecordingVideo();
                } else {
                    startRecordingVideo();
                }
                break;
            }
//            case R.id.music: {
//                if (!musicFlag) {
//                    mPlayer = new MediaPlayer();
//                    try {
//                        File file = getActivity().getExternalFilesDir(null);
//                        mPlayer.setDataSource(file.getPath() + "/input.wav");
//                        // mPlayer.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getStartOffset());
//                        mPlayer.prepare();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    mPlayer.start();
//                    musicFlag = true;
//                    mButtonMusic.setText(R.string.stop);
//                } else {
//                    mPlayer.stop();
//                    musicFlag = false;
//                    mButtonMusic.setText(R.string.play_music);
//                }
//                break;
//            }
            case R.id.ois: {
                oisFlag = !oisFlag;
                if (oisFlag) {
                    mOISSwitch.setText(R.string.ois_on);
                } else {
                    mOISSwitch.setText(R.string.ois_off);
                }
                if (mIsRecordingVideo) {
                    stopRecordingVideo();
                }
                break;
            }
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        if (requestCode == REQUEST_VIDEO_PERMISSIONS) {
            if (grantResults.length == VIDEO_PERMISSIONS.length) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        ErrorDialog.newInstance(getString(R.string.permission_request))
                                .show(getChildFragmentManager(), FRAGMENT_DIALOG);
                        break;
                    }
                }
            } else {
                ErrorDialog.newInstance(getString(R.string.permission_request))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasPermissionsGranted() {
        for (String permission : Camera2VideoFragment.VIDEO_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(getActivity(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    @SuppressWarnings("MissingPermission")
    private void openCamera(int width, int height) {
        if (!hasPermissionsGranted()) {
            return;
        }
        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            Log.d(TAG, "tryAcquire");
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            String cameraId = manager.getCameraIdList()[0];

            // Choose the sizes for camera preview and video recording
            mCharacteristics = manager.getCameraCharacteristics(cameraId);


            //Log.d("Char OIS_DATA_MODES: ", Arrays.toString(mCharacteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_OIS_DATA_MODES)));
            Log.d("Char ROTATION:  ", Arrays.toString(mCharacteristics.get(CameraCharacteristics.LENS_POSE_ROTATION)));
            Log.d("Char TRANSLATION:  ", Arrays.toString(mCharacteristics.get(CameraCharacteristics.LENS_POSE_TRANSLATION)));
            Log.d("Char TRANSLATION:  ", Arrays.toString(mCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)));

            StreamConfigurationMap map = mCharacteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//
//            // For still image captures, we use the largest available size.
//            Size largestRaw = Collections.max(
//                    Arrays.asList(map.getOutputSizes(ImageFormat.RAW_SENSOR)),
//                    new CompareSizesByArea());

            synchronized (mCameraStateLock) {
                // Set up ImageReaders for JPEG and RAW outputs.  Place these in a reference
                // counted wrapper to ensure they are only closed when all background tasks
                // using them are finished.
//                if (mRawImageReader == null || mRawImageReader.getAndRetain() == null) {
//                    mRawImageReader = new RefCountedAutoCloseable<>(
//                            ImageReader.newInstance(largestRaw.getWidth(),
//                                    largestRaw.getHeight(), ImageFormat.RAW_SENSOR, /*maxImages*/ 5));
//                }
//                mRawImageReader.get().setOnImageAvailableListener(
//                        mOnRawImageAvailableListener, mBackgroundHandler);

                mCameraId = cameraId;
            }


            mSensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (map == null) {
                throw new RuntimeException("Cannot get available preview/video sizes");
            }
            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    width, height, mVideoSize);

            mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                    ImageFormat.YUV_420_888, 2);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);

            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
            configureTransform(width, height);
            mMediaRecorder = new MediaRecorder();
            manager.openCamera(cameraId, mStateCallback, null);
        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            closePreviewSession();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Start the camera preview.
     */
    private void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            closePreviewSession();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            Surface previewSurface = new Surface(texture);
            mPreviewBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mPreviewSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Activity activity = getActivity();
                            if (null != activity) {
                                Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private void setUpMediaRecorder() throws IOException {
        final Activity activity = getActivity();
        if (null == activity) {
            return;
        }
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

//        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
//        Log.d(TAG, "setUpMediaRecorder: " + profile.videoFrameRate + " " + profile.videoCodec);

        if (mNextVideoAbsolutePath == null || mNextVideoAbsolutePath.isEmpty()) {
            mNextVideoAbsolutePath = getVideoFilePath(getActivity());
        }
        mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
        mMediaRecorder.setVideoEncodingBitRate(20000000);
        mMediaRecorder.setCaptureRate(60);
        mMediaRecorder.setVideoFrameRate(60);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (mSensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }
        mMediaRecorder.prepare();
    }

    private String getVideoFilePath(Context context) {
        final File dir = context.getExternalFilesDir(null);
        return (dir == null ? "" : (dir.getAbsolutePath() + "/"))
                + elapsedRealtimeNanos() + ".mp4";
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void startRecordingVideo() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);


            oisSamplesExport = new ArrayList<>();



            //mPreviewBuilder.set(CaptureRequest.STATISTICS_OIS_DATA_MODE, CaptureRequest.STATISTICS_OIS_DATA_MODE_ON);

            if (oisFlag) {
                // 开启 OIS
                mPreviewBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
                mPreviewBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
            } else {
                // 关闭 OIS
                mPreviewBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF);
                mPreviewBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
            }

            List<Surface> surfaces = new ArrayList<>();

            // Set up Surface for th
            // e camera preview
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            final int[] frame = {0};

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {



                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();

                    try {
                        CaptureRequest captureRequest = mPreviewBuilder.build();
                        mPreviewSession.setRepeatingRequest(captureRequest, new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                                // Log.d("Camera: AF", String.valueOf(result.get(CaptureResult.CONTROL_AF_MODE)));
//                                Log.d("Camera: TIME", String.valueOf(result.get(CaptureResult.SENSOR_TIMESTAMP)));
//                                Log.d("Camera: TRANSLATION", Arrays.toString(result.get(CaptureResult.LENS_INTRINSIC_CALIBRATION)));
                                // Log.d("Camera: ROTATION", Arrays.toString(result.get(CaptureResult.LENS_POSE_ROTATION)));

//                                OisSample[] oisSamples = result.get(CaptureResult.STATISTICS_OIS_SAMPLES);
//                                if (oisSamples != null && oisSamples.length > 0) {
//                                    for (int i = 0; i < oisSamples.length; i++) {
//                                        Log.d("Camera: OIS ", i + " " +  oisSamples[i].toString());
//                                        OISResult oisResult = new OISResult();
//                                        oisResult.oisSample = oisSamples[i];
//                                        oisResult.internalIndex = i;
//                                        oisResult.frameIndex = frame[0];
//                                        oisSamplesExport.add(oisResult);
//                                    }
//                                    frame[0]++;
//                                }

                                super.onCaptureCompleted(session, request, result);
                            }

                            @Override
                            public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
                                super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
                            }

                            @Override
                            public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                                super.onCaptureProgressed(session, request, partialResult);
                            }
                        }, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // UI
                            mButtonVideo.setText(R.string.stop);
                            mIsRecordingVideo = true;

                            OISResult oisResult = new OISResult();
                            oisResult.x = -1;
                            oisResult.y = -1;
                            oisResult.z = -1;
                            oisResult.timestamp = elapsedRealtimeNanos();
                            oisSamplesExport.add(oisResult);

                            // Start recording
                            mMediaRecorder.start();
                        }
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Activity activity = getActivity();
                    if (null != activity) {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }

    }

    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void stopRecordingVideo() {
        // UI
        mIsRecordingVideo = false;
        mButtonVideo.setText(R.string.record);
        // Stop recording
        mMediaRecorder.stop();
        mMediaRecorder.reset();

        OISResult oisResult = new OISResult();
        oisResult.x = -1;
        oisResult.y = -1;
        oisResult.z = -1;
        oisResult.timestamp = elapsedRealtimeNanos();
        oisSamplesExport.add(oisResult);

        Activity activity = getActivity();
        if (null != activity) {
            Toast.makeText(activity, "Video saved: " + mNextVideoAbsolutePath,
                    Toast.LENGTH_LONG).show();
            Log.d(TAG, "Video saved: " + mNextVideoAbsolutePath);
        }

        // 输出 csv
        StringBuilder buffer = new StringBuilder();
        buffer.append("X,Y,Z,timestamp\r\n");
        for (OISResult result : oisSamplesExport) {
//            buffer.append(oisResult.frameIndex).append(",").append(oisResult.internalIndex).append(",")
//                    .append(oisResult.oisSample.getTimestamp()).append(",").append(oisResult.oisSample.getXshift())
//                    .append(",").append(oisResult.oisSample.getYshift()).append("\r\n");
            buffer.append(result.x).append(",").append(result.y).append(",").append(result.z)
                    .append(",").append(result.timestamp).append("\r\n");
        }
        try {
            String data = buffer.toString();

            String filename = mNextVideoAbsolutePath.substring(0, mNextVideoAbsolutePath.length() - 3) + "txt";

            // String path = Environment.getExternalStorageDirectory()+"/Users";
            File file = new File(filename);
            OutputStream out=new FileOutputStream(file);


            out.write(data.getBytes());
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


        mNextVideoAbsolutePath = null;
        startPreview();
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }


    /**
     * Retrieve the next {@link Image} from a reference counted {@link ImageReader}, retaining
     * that {@link ImageReader} until that {@link Image} is no longer in use, and set this
     * {@link Image} as the result for the next request in the queue of pending requests.  If
     * all necessary information is available, begin saving the image to a file in a background
     * thread.
     *
     * @param pendingQueue the currently active requests.
     * @param reader       a reference counted wrapper containing an {@link ImageReader} from which
     *                     to acquire an image.
     */
    private void dequeueAndSaveImage(TreeMap<Integer, ImageSaver.ImageSaverBuilder> pendingQueue,
                                     RefCountedAutoCloseable<ImageReader> reader) {
        synchronized (mCameraStateLock) {
            Map.Entry<Integer, ImageSaver.ImageSaverBuilder> entry =
                    pendingQueue.firstEntry();
            ImageSaver.ImageSaverBuilder builder = entry.getValue();

            // Increment reference count to prevent ImageReader from being closed while we
            // are saving its Images in a background thread (otherwise their resources may
            // be freed while we are writing to a file).
            if (reader == null || reader.getAndRetain() == null) {
                Log.e(TAG, "Paused the activity before we could save the image," +
                        " ImageReader already closed.");
                pendingQueue.remove(entry.getKey());
                return;
            }

            Image image;
            try {
                image = reader.get().acquireNextImage();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Too many images queued for saving, dropping image for request: " +
                        entry.getKey());
                pendingQueue.remove(entry.getKey());
                return;
            }

            builder.setRefCountedReader(reader).setImage(image);

            handleCompletionLocked(entry.getKey(), builder, pendingQueue);
        }
    }

    /**
     * If the given request has been completed, remove it from the queue of active requests and
     * send an {@link ImageSaver} with the results from this request to a background thread to
     * save a file.
     * <p/>
     * Call this only with {@link #mCameraStateLock} held.
     *
     * @param requestId the ID of the {@link CaptureRequest} to handle.
     * @param builder   the {@link ImageSaver.ImageSaverBuilder} for this request.
     * @param queue     the queue to remove this request from, if completed.
     */
    private void handleCompletionLocked(int requestId, ImageSaver.ImageSaverBuilder builder,
                                        TreeMap<Integer, ImageSaver.ImageSaverBuilder> queue) {
        if (builder == null) return;
        ImageSaver saver = builder.buildIfComplete();
        if (saver != null) {
            queue.remove(requestId);
            AsyncTask.THREAD_POOL_EXECUTOR.execute(saver);
        }
    }

    /**
     * Runnable that saves an {@link Image} into the specified {@link File}, and updates
     * {@link android.provider.MediaStore} to include the resulting file.
     * <p/>
     * This can be constructed through an {@link ImageSaverBuilder} as the necessary image and
     * result information becomes available.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The image to save.
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        /**
         * The CaptureResult for this image capture.
         */
        private final CaptureResult mCaptureResult;

        /**
         * The CameraCharacteristics for this camera device.
         */
        private final CameraCharacteristics mCharacteristics;

        /**
         * The Context to use when updating MediaStore with the saved images.
         */
        private final Context mContext;

        /**
         * A reference counted wrapper for the ImageReader that owns the given image.
         */
        private final RefCountedAutoCloseable<ImageReader> mReader;

        private ImageSaver(Image image, File file, CaptureResult result,
                           CameraCharacteristics characteristics, Context context,
                           RefCountedAutoCloseable<ImageReader> reader) {
            mImage = image;
            mFile = file;
            mCaptureResult = result;
            mCharacteristics = characteristics;
            mContext = context;
            mReader = reader;
        }

        @Override
        public void run() {
            boolean success = false;
            int format = mImage.getFormat();
            switch (format) {
                case ImageFormat.JPEG: {
                    ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    FileOutputStream output = null;
                    try {
                        output = new FileOutputStream(mFile);
                        output.write(bytes);
                        success = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        mImage.close();
                        closeOutput(output);
                    }
                    break;
                }
                case ImageFormat.RAW_SENSOR: {
                    DngCreator dngCreator = new DngCreator(mCharacteristics, mCaptureResult);
                    FileOutputStream output = null;
                    try {
                        output = new FileOutputStream(mFile);
                        dngCreator.writeImage(output, mImage);
                        success = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        mImage.close();
                        closeOutput(output);
                    }
                    break;
                }
                default: {
                    Log.e(TAG, "Cannot save image, unexpected image format:" + format);
                    break;
                }
            }

            // Decrement reference count to allow ImageReader to be closed to free up resources.
            mReader.close();
        }

        /**
         * Builder class for constructing {@link ImageSaver}s.
         * <p/>
         * This class is thread safe.
         */
        public static class ImageSaverBuilder {
            private Image mImage;
            private File mFile;
            private CaptureResult mCaptureResult;
            private CameraCharacteristics mCharacteristics;
            private Context mContext;
            private RefCountedAutoCloseable<ImageReader> mReader;

            /**
             * Construct a new ImageSaverBuilder using the given {@link Context}.
             *
             * @param context a {@link Context} to for accessing the
             *                {@link android.provider.MediaStore}.
             */
            public ImageSaverBuilder(final Context context) {
                mContext = context;
            }

            public synchronized ImageSaverBuilder setRefCountedReader(
                    RefCountedAutoCloseable<ImageReader> reader) {
                if (reader == null) throw new NullPointerException();

                mReader = reader;
                return this;
            }

            public synchronized ImageSaverBuilder setImage(final Image image) {
                if (image == null) throw new NullPointerException();
                mImage = image;
                return this;
            }

            public synchronized ImageSaverBuilder setFile(final File file) {
                if (file == null) throw new NullPointerException();
                mFile = file;
                return this;
            }

            public synchronized ImageSaverBuilder setResult(final CaptureResult result) {
                if (result == null) throw new NullPointerException();
                mCaptureResult = result;
                return this;
            }

            public synchronized ImageSaverBuilder setCharacteristics(
                    final CameraCharacteristics characteristics) {
                if (characteristics == null) throw new NullPointerException();
                mCharacteristics = characteristics;
                return this;
            }

            public synchronized ImageSaver buildIfComplete() {
                if (!isComplete()) {
                    return null;
                }
                return new ImageSaver(mImage, mFile, mCaptureResult, mCharacteristics, mContext,
                        mReader);
            }

            public synchronized String getSaveLocation() {
                return (mFile == null) ? "Unknown" : mFile.toString();
            }

            private boolean isComplete() {
                return mImage != null && mFile != null && mCaptureResult != null
                        && mCharacteristics != null;
            }
        }
    }


    private static void closeOutput(OutputStream outputStream) {
        if (null != outputStream) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static class RefCountedAutoCloseable<T extends AutoCloseable> implements AutoCloseable {
        private T mObject;
        private long mRefCount = 0;

        /**
         * Wrap the given object.
         *
         * @param object an object to wrap.
         */
        public RefCountedAutoCloseable(T object) {
            if (object == null) throw new NullPointerException();
            mObject = object;
        }

        /**
         * Increment the reference count and return the wrapped object.
         *
         * @return the wrapped object, or null if the object has been released.
         */
        public synchronized T getAndRetain() {
            if (mRefCount < 0) {
                return null;
            }
            mRefCount++;
            return mObject;
        }

        /**
         * Return the wrapped object.
         *
         * @return the wrapped object, or null if the object has been released.
         */
        public synchronized T get() {
            return mObject;
        }

        /**
         * Decrement the reference count and release the wrapped object if there are no other
         * users retaining this object.
         */
        @Override
        public synchronized void close() {
            if (mRefCount >= 0) {
                mRefCount--;
                if (mRefCount < 0) {
                    try {
                        mObject.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        mObject = null;
                    }
                }
            }
        }
    }
}
