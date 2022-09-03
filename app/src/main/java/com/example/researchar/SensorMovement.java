package com.example.researchar;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.kircherelectronics.fsensor.observer.SensorSubject;
import com.kircherelectronics.fsensor.sensor.FSensor;
import com.kircherelectronics.fsensor.sensor.acceleration.LowPassLinearAccelerationSensor;

public class SensorMovement extends AppCompatActivity {
    TextView acceleration = null;
    private FSensor fSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_movement);
        acceleration = findViewById(R.id.acceleration);
    }

    private SensorSubject.SensorObserver sensorObserver = values -> acceleration.setText("x: " + values[0] + "\ny: " + values[1] + "\nz: " + values[2]);

    @Override
    public void onResume() {
        super.onResume();
        fSensor = new LowPassLinearAccelerationSensor(this);
        fSensor.register(sensorObserver);
        fSensor.start();
    }

    @Override
    public void onPause() {
        fSensor.unregister(sensorObserver);
        fSensor.stop();

        super.onPause();
    }
}