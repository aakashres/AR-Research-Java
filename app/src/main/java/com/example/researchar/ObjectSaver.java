package com.example.researchar;

import android.graphics.ImageFormat;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import com.example.researchar.helper.ImageUtils;
import com.example.researchar.helper.ObjectDetector;
import com.example.researchar.helper.ObjectIdentifier;
import com.example.researchar.helper.PersistObject;
import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class ObjectSaver extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnTapArPlaneListener,
        BaseArFragment.OnSessionConfigurationListener,
        ArFragment.OnViewCreatedListener,
        View.OnClickListener{

    private final String TAG = "Emoji Viewer";
    private ArFragment arFragment;
    private Renderable model;
    private ViewRenderable viewRenderable;
    private Boolean save_object = false;
    private ImageUtils imageUtils;
    private ObjectDetector objectDetector;
    private PersistObject persistObject;


    private Anchor objectAnchor;
    private TransformableNode objectModel;

    final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface
                    .SUCCESS) {
                Log.i(TAG, "OpenCv is loaded");
            }
            super.onManagerConnected(status);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_object_saver);
        getSupportFragmentManager().addFragmentOnAttachListener(this);

        Button find_object = findViewById(R.id.find_object);
        find_object.setOnClickListener(this);

        if (OpenCVLoader.initDebug()){
            Log.d(TAG, "Opencv initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else {
            Log.d(TAG, "Opencv is not loaded. Try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        }

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFragment.class, null)
                        .commit();
            }
        }

        loadModels();

        try{
            // input size of model is  300x300;
            objectDetector = new ObjectDetector(getAssets(), "ssd_mobilenet_v1_1_metadata_1.tflite", "objectlabelmap.txt", 300);
            Log.d(TAG, "Model is loaded successfully");
        } catch (IOException e){
            Log.d(TAG, "Getting error while loading model");
            e.printStackTrace();
        }

        imageUtils = new ImageUtils();
        persistObject = new PersistObject();
    }

    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragment) {
            arFragment = (ArFragment) fragment;
            arFragment.setOnSessionConfigurationListener(this);
            arFragment.setOnViewCreatedListener(this);
            arFragment.setOnTapArPlaneListener(this);

        }
    }

    @Override
    public void onSessionConfiguration(Session session, Config config) {
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.setDepthMode(Config.DepthMode.AUTOMATIC);
        }
        config.setFocusMode(Config.FocusMode.AUTO);
        config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);
    }

    @Override
    public void onViewCreated(ArSceneView arSceneView) {
        arFragment.setOnViewCreatedListener(null);

        arSceneView.getScene().addOnUpdateListener(this::onUpdate);
        // Fine adjust the maximum frame rate
        arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL);
        arSceneView.getPlaneRenderer().setVisible(false);
    }

    public void loadModels() {
        WeakReference<ObjectSaver> weakActivity = new WeakReference<>(this);
        ModelRenderable.builder()
                .setSource(this, R.raw.cube)
                .setIsFilamentGltf(true)
                .setAsyncLoadEnabled(true)
                .build()
                .thenAccept(model -> {
                    ObjectSaver activity = weakActivity.get();
                    if (activity != null) {
                        activity.model = model;
                    }
                })
                .exceptionally(throwable -> {
                    Toast.makeText(
                            this, "Unable to load model", Toast.LENGTH_LONG).show();
                    return null;
                });
        ViewRenderable.builder()
                .setView(this, R.layout.layout_model_object)
                .build()
                .thenAccept(viewRenderable -> {
                    ObjectSaver activity = weakActivity.get();
                    if (activity != null) {
                        activity.viewRenderable = viewRenderable;
                    }
                })
                .exceptionally(throwable -> {
                    Toast.makeText(this, "Unable to load model", Toast.LENGTH_LONG).show();
                    return null;
                });
    }

    @Override
    public void onTapPlane(HitResult hitResult, Plane plane, MotionEvent motionEvent) {
        if (model == null || viewRenderable == null) {
            Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT).show();
            return;
        }

        if(objectModel != null){
            return;
        }

        // Create the Anchor.
        objectAnchor = hitResult.createAnchor();
        AnchorNode anchorNode = new AnchorNode(objectAnchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        // Create the transformable model and add it to the anchor.
        objectModel = new TransformableNode(arFragment.getTransformationSystem());
        objectModel.setParent(anchorNode);
        objectModel.setRenderable(this.model)
                .animate(true).start();
        objectModel.setLocalScale(new Vector3(0.001f, 0.001f, 0.001f));
        objectModel.setLocalPosition(new Vector3(0.0f, 0.0f, -5.0f));
        objectModel.select();

        Log.d(TAG, "" + arFragment.getArSceneView().getScene().getCamera().worldToScreenPoint(objectModel.getWorldPosition()));
    }

    private void onUpdate(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();
        // If there is no frame or ARCore is not tracking yet, just return.
        if (frame == null || frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
            return;
        }
        if(save_object){
            try (Image image = frame.acquireCameraImage()) {
                if (image.getFormat() != ImageFormat.YUV_420_888) {
                    throw new IllegalArgumentException(
                            "Expected image in YUV_420_888 format, got format " + image.getFormat());
                }
                Mat mRgb = imageUtils.getRgbaMat(image);
                ArrayList<ObjectIdentifier> objects =  objectDetector.getObjectIdentifiers(mRgb);
                for(ObjectIdentifier obj: objects){
                    Log.d(TAG, "" + obj.getbBox());
                    Log.d(TAG, "" + arFragment.getArSceneView().getScene().getCamera().worldToScreenPoint(objectModel.getWorldPosition()));
                    persistObject.saveObject(obj.getObjectType(), obj.getObjectKeypoints(), obj.getObjectDescriptors());
                    save_object = false;
                    Toast.makeText(this, "Object Saved: "+ objects.get(0).getObjectType(), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception copying image", e);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if(save_object == false){
            save_object = true;
            Toast.makeText(this, "Saving Object", Toast.LENGTH_SHORT).show();
        }
    }
}

