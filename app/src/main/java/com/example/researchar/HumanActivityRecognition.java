package com.example.researchar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.researchar.helper.ActivityInference;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HumanActivityRecognition extends AppCompatActivity implements SensorEventListener{

    private final int N_SAMPLES = 90;
    private static List<Float> x;
    private static List<Float> y;
    private static List<Float> z;
    private static List<Float> input_signal;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ActivityInference activityInference;

    private TextView runningTextView;
    private TextView sittingTextView;
    private TextView walkingTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_human_recognition);
        x = new ArrayList<>();
        y = new ArrayList<>();
        z = new ArrayList<>();
        input_signal = new ArrayList<>();

        runningTextView = findViewById(R.id.running_prob);
        sittingTextView = findViewById(R.id.sitting_prob);
        walkingTextView = findViewById(R.id.walking_prob);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer , SensorManager.SENSOR_DELAY_FASTEST);
        activityInference = new ActivityInference(getApplicationContext());
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        activityPrediction();
        x.add(event.values[0]);
        y.add(event.values[1]);
        z.add(event.values[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @SuppressLint("SetTextI18n")
    private void activityPrediction()
    {
        if(x.size() == N_SAMPLES && y.size() == N_SAMPLES && z.size() == N_SAMPLES) {
            // Mean normalize the signal
            normalize();

            // Copy all x, y and z values to input_signal
            int i = 0;
            while (i < N_SAMPLES) {
                input_signal.add(x.get(i));
                input_signal.add(y.get(i));
                input_signal.add(z.get(i));
                i++;
            }

            // Perform inference using Tensorflow
            float[] results = activityInference.getActivityProb(toFloatArray(input_signal));

            runningTextView.setText(Float.toString(round(results[1],2)));
            sittingTextView.setText(Float.toString(round(Collections.max(Arrays.asList(results[2], results[3])), 2)));
            walkingTextView.setText(Float.toString(round(Collections.max(Arrays.asList(results[0], results[4], results[5])), 2)));



            // Clear all the values
            x.clear(); y.clear(); z.clear(); input_signal.clear();
        }
    }

    private float[] toFloatArray(List<Float> list)
    {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    private void normalize()
    {
        float x_m = 0.662868f; float y_m = 7.255639f; float z_m = 0.411062f;
        float x_s = 6.849058f; float y_s = 6.746204f; float z_s = 4.754109f;

        for(int i = 0; i < N_SAMPLES; i++)
        {
            x.set(i,((x.get(i) - x_m)/x_s));
            y.set(i,((y.get(i) - y_m)/y_s));
            z.set(i,((z.get(i) - z_m)/z_s));
        }
    }

    public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, RoundingMode.HALF_UP);
        return bd.floatValue();
    }
}