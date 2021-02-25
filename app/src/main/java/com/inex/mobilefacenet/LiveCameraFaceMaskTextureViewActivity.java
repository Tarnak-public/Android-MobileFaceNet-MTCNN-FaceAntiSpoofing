
/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * package com.android.grafika;
 */
package com.inex.mobilefacenet;


import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.inex.mobilefacenet.utils.MyUtil;
import com.inex.mobilefacenet.utils.PermissionHelper;

import org.tensorflow.lite.examples.detection.tflite.Classifier;

import java.io.IOException;

import softkom.com.classes.ImageFaceAndMaskDetection;

//import android.support.annotation.NonNull;

/**
 * More or less straight out of TextureView's doc.
 * <p>
 * TODO: add options for different display sizes, frame rates, camera selection, etc.
 */
public class LiveCameraFaceMaskTextureViewActivity extends Activity implements TextureView.SurfaceTextureListener {
    private final String TAG = "LiveCameraTViewActivity";
    private TextureView textureView;
    private ImageView faceImageView;
    private Bitmap bitmap;
    private long start = System.currentTimeMillis();
    private long end = System.currentTimeMillis();
    private ImageFaceAndMaskDetection imageFaceAndMaskDetection;
    private SurfaceTexture mSurfaceTexture;
    private TextView statusTextView;
    private int displayDegree;
    private byte[] mData;
    private Camera.Size mSize;
    private long frameSkipperInMS = 300;
    private long frameSkipperLastMS = 0;
    private boolean wasPreviousDetectionWithFace = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_live_camera_textureview_facemask);
        textureView = findViewById(R.id.LiveCameraFacemaskTextureViewActivity);
        statusTextView = findViewById(R.id.FaceMaskStatus_textView);
        faceImageView = findViewById(R.id.FaceImageView);
        textureView.setSurfaceTextureListener(this);

//        applyMirroringOnCamera(textureView);
        imageFaceAndMaskDetection = new ImageFaceAndMaskDetection(false);
        imageFaceAndMaskDetection.setCustomObjectListener(new ImageFaceAndMaskDetection.DetectionPhaseListener() {
            @Override
            public void onFaceDetected(Bitmap bitmapOfDetectedFace) {
                runOnUiThread(() -> runOnUiThread(() -> faceImageView.setImageBitmap(bitmapOfDetectedFace)));
                wasPreviousDetectionWithFace = true;
            }

            @Override
            public void onNoFaceDetected(Bitmap bitmapOfDetectedFace) {
                if (wasPreviousDetectionWithFace)
                    runOnUiThread(() -> runOnUiThread(() -> faceImageView.setImageResource(R.drawable.no_face)));
                wasPreviousDetectionWithFace = false;
            }

            @Override
            public void onResultOfDetection(Classifier.Recognition classRecognition) {
                if (classRecognition != null) {
                    runOnUiThread(() -> {
                        statusTextView.setTextColor(classRecognition.getColor());
                        statusTextView.setText(classRecognition.toString());
                    });
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!PermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                    "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            PermissionHelper.launchPermissionSettings(this);
            finish();
        } else {
            startPreview();
        }
    }

    /*
       Fix for back camera treated as front and do Mirroring
    */
    private void applyMirroringOnCamera(TextureView textureView) {

        int width = Resources.getSystem().getDisplayMetrics().widthPixels;
        Matrix matrix = new Matrix();
        matrix.setScale(-1, 1);
        matrix.postTranslate(width, 0);
        textureView.setTransform(matrix);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        mSurfaceTexture = surface;
        if (!PermissionHelper.hasCameraPermission(this)) {
            PermissionHelper.requestCameraPermission(this, false);
        } else {
            startPreview();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed() ");
        MainActivity.mCamera.stopPreview();
        MainActivity.mCamera.setPreviewCallback(null);
        MainActivity.mCamera.release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invoked every time there's a new Camera preview frame
        start = System.currentTimeMillis();
        bitmap = textureView.getBitmap();
        end = System.currentTimeMillis();
//        Log.v(TAG, "onSurfaceTextureUpdated() occured time:" + start + " bitmap:" + bitmap);
        Log.v(TAG, "onSurfaceTextureUpdated() actual ms difference " + (start - frameSkipperLastMS) + " , frame skip threshold:" + frameSkipperInMS);
        Log.v(TAG, "onSurfaceTextureUpdated() START FACE AND MASK DETECTION ");
        if (!imageFaceAndMaskDetection.isStillProcessing()) {
            Log.v(TAG, "onSurfaceTextureUpdated() RUNNING FACE AND MASK ");
/*
            //*********** temporary ********
            bitmapTemp1 = MyUtil.readFromAssets(this,"NO_MASK_99_170814.jpg");
            ((ImageView)(findViewById(R.id.FaceImageView))).setImageBitmap(bitmap);
            //*********** temporary ********
*/
            imageFaceAndMaskDetection.DetectFromImage(bitmap);
        } else {
            Log.v(TAG, "onSurfaceTextureUpdated() STILL BUSY PROCESSING PREVIOUS DETECTION ");
        }
    }

    private void startPreview() {
        startPreviewNewSingle();
    }

    private void startPreviewNewSingle() {
//---------------testing this-----------
        Log.d(TAG, "startPreviewNewSingle() ");
        MainActivity.mCamera = Camera.open(MainActivity.CAMERA_ID);
        Camera.Parameters parameters = MainActivity.mCamera.getParameters();
        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        //was camera 0, but if MainActivity.CAMERA_ID it goesmessed
        displayDegree = MyUtil.setCameraDisplayOrientation(MainActivity.CAMERA_ID, MainActivity.mCamera, getWindowManager());

        // Get the right resolution
        mSize = MyUtil.getOptimalSize(parameters.getSupportedPreviewSizes(), textureView.getWidth(), textureView.getHeight());
        parameters.setPreviewSize(mSize.width, mSize.height);

        parameters.setPreviewFormat(MainActivity.IMAGE_FORMAT);
        MainActivity.mCamera.setParameters(parameters);

        // Camera callback for every frame
        MainActivity.mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                //Log.d(TAG, "onPreviewFrame() callback");
                mData = data;
                camera.addCallbackBuffer(data);
            }
        });
        try {
            MainActivity.mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException ioe) {
            // Something bad happened
            Log.e(TAG, "Exception starting preview", ioe);
        }
        MainActivity.mCamera.startPreview();
    }
    //https://android-developers.googleblog.com/2009/05/painless-threading.html
}