package com.example.researchar;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {
    static {
        if(OpenCVLoader.initDebug()){
            Log.d("Main Activity", "Open CV is Loaded");
        }
        else {
            Log.d("Main Activity", "Open CV is not Loaded");

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        Button face_detect = findViewById(R.id.face_detect);
        face_detect.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, FaceDetection.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        Button emotion_detect = findViewById(R.id.emotion_detect);
        emotion_detect.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, EmotionDetection.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        Button object_detect = findViewById(R.id.object_detect);
        object_detect.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ObjectDetection.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        Button view_emoji = findViewById(R.id.emoji_view);
        view_emoji.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, EmojiViewer.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        Button save_object = findViewById(R.id.save_object);
        save_object.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ObjectSaver.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        Button sensor = findViewById(R.id.sensor);
        sensor.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, HumanActivityRecognition.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        Button facemask = findViewById(R.id.facemask);
        facemask.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, FaceAugmentation.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)));
    }
}