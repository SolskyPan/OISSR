package com.example.demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChartActivity extends AppCompatActivity implements
        OnChartValueSelectedListener, SensorEventListener {
    private LineChart lineChart;
    private SensorManager sensorManager;
    private List<float[]> lineData = new ArrayList<>();
    boolean flag = true;
    boolean stop = false;
    Button button;
    List<Entry> xData = new ArrayList<>();
    List<Entry> yData = new ArrayList<>();
    List<Entry> zData = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_demo);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        mTfRegular = Typeface.createFromAsset(getAssets(), "OpenSans-Regular.ttf");
//        mTfLight = Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf");

        lineChart = findViewById(R.id.lineChart);
        button = findViewById(R.id.button6);

        initLineChart();
    }


    /**
     * 初始化折线图控件属性
     */
    private void initLineChart() {
        lineChart.setOnChartValueSelectedListener(this);
        lineChart.getDescription().setEnabled(false);
        lineChart.setBackgroundColor(Color.WHITE);

        //自定义适配器，适配于X轴
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        //xAxis.setTypeface(mTfLight);
        xAxis.setGranularity(1f);
        //xAxis.setValueFormatter(xAxisFormatter);

        //自定义适配器，适配于Y轴
        //IAxisValueFormatter custom = new MyAxisValueFormatter();

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setLabelCount(8, false);
//        leftAxis.setAxisMinimum(-12);
//        leftAxis.setAxisMaximum(12);
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setSpaceTop(15f);

        lineChart.getAxisRight().setEnabled(false);

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


        for (int i = zData.size(); i < lineData.size(); i++) {
            xData.add(new Entry(i, lineData.get(i)[0]));
            yData.add(new Entry(i, lineData.get(i)[1]));
            zData.add(new Entry(i, lineData.get(i)[2]));
            //Log.d("chart", String.valueOf(lineData.get(i)[0]));
        }
        //Log.d("chart", String.valueOf(xData));
        //Log.d("chart", String.valueOf(lineData));

        //这里，每重新new一个LineDataSet，相当于重新画一组折线
        //每一个LineDataSet相当于一组折线。比如:这里有两个LineDataSet：setComp1，setComp2。
        LineDataSet setComp1 = new LineDataSet(xData, "X Data");
        setComp1.setAxisDependency(YAxis.AxisDependency.LEFT);
        setComp1.setColor(getResources().getColor(R.color.colorAccent));
        setComp1.setDrawCircles(false);
        setComp1.setMode(LineDataSet.Mode.LINEAR);

        LineDataSet setComp2 = new LineDataSet(yData, "Y Data");
        setComp2.setAxisDependency(YAxis.AxisDependency.LEFT);
        setComp2.setColor(getResources().getColor(R.color.red));
        setComp2.setDrawCircles(false);
        setComp2.setMode(LineDataSet.Mode.LINEAR);

        LineDataSet setComp3 = new LineDataSet(zData, "Z Data");
        setComp3.setAxisDependency(YAxis.AxisDependency.LEFT);
        setComp3.setColor(getResources().getColor(R.color.green));
        setComp3.setDrawCircles(false);
        setComp3.setMode(LineDataSet.Mode.LINEAR);


        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(setComp1);
        dataSets.add(setComp2);
        dataSets.add(setComp3);

        LineData lineData = new LineData(dataSets);

        lineChart.setData(lineData);
        lineChart.invalidate();
        flag = true;
    }

    public static void startActivity(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, ChartActivity.class);
        context.startActivity(intent);
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            if(!stop) {
                lineData.add(new float[]{sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]});
                if(flag) {
                    setLineChartData();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),//加速度传感器
                100000);//获取数据速度

    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);//解除监听注册
    }

    public void ChangeState(View view) {
        stop = !stop;
        if(stop) {
            button.setText(R.string.start);
        } else {
            button.setText(R.string.stop);
        }
    }

}
