
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
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import org.tensorflow.lite.examples.detection.tflite.Classifier;

import softkom.com.classes.AutoFitTextureView;
import softkom.com.classes.CameraSurfaceListener;
import softkom.com.classes.ImageFaceAndMaskDetection;

//import android.support.annotation.NonNull;

/**
 * More or less straight out of TextureView's doc.
 * <p>
 * TODO: add options for different display sizes, frame rates, camera selection, etc.
 */
public class LiveCameraFaceMaskTextureViewActivity extends Activity {
    private final String TAG = "LiveCameraTViewActivity";
    CameraSurfaceListener camera0SurfaceListener;
    CameraSurfaceListener camera1SurfaceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_live_camera_textureview_facemask);

        AutoFitTextureView cam0TextureView = findViewById(R.id.live_cam0_TextureViewActivity);
        TextView cam0StatusTextView = findViewById(R.id.faceMask_cam0_status_textView);
        ImageView cam0FaceImageView = findViewById(R.id.face_cam0_ImageView);

        AutoFitTextureView cam1TextureView = findViewById(R.id.live_cam1_TextureViewActivity);
        TextView cam1StatusTextView = findViewById(R.id.faceMask_cam1_status_textView);
        ImageView cam1FaceImageView = findViewById(R.id.face_cam1_ImageView);

        ImageFaceAndMaskDetection cam0Detector = camFaceDetector(cam0FaceImageView, cam0StatusTextView);
        ImageFaceAndMaskDetection cam1Detector = camFaceDetector(cam1FaceImageView, cam1StatusTextView);

        camera0SurfaceListener = new CameraSurfaceListener(this, cam0Detector, Camera.CameraInfo.CAMERA_FACING_BACK, 270, cam0TextureView);
        camera1SurfaceListener = new CameraSurfaceListener(this, cam1Detector, Camera.CameraInfo.CAMERA_FACING_FRONT, 270, cam1TextureView);

        cam0TextureView.setAspectRatio(10,13); // 2/3
        cam1TextureView.setAspectRatio(10,13); // 2/3

        cam0TextureView.setSurfaceTextureListener(camera0SurfaceListener);
        cam1TextureView.setSurfaceTextureListener(camera1SurfaceListener);
    }

    private ImageFaceAndMaskDetection camFaceDetector(ImageView camFaceImageView, TextView camStatusTextView) {
        ImageFaceAndMaskDetection faceAndMaskDetection = new ImageFaceAndMaskDetection(false);
        faceAndMaskDetection.setCustomObjectListener(new ImageFaceAndMaskDetection.DetectionPhaseListener() {
            @Override
            public void onFaceDetected(Bitmap bitmapOfDetectedFace) {
                runOnUiThread(() -> runOnUiThread(() -> camFaceImageView.setImageBitmap(bitmapOfDetectedFace)));
            }

            @Override
            public void onNoFaceDetected(Bitmap bitmapOfDetectedFace) {
                runOnUiThread(() -> runOnUiThread(() -> camFaceImageView.setImageResource(R.drawable.no_face)));
            }

            @Override
            public void onResultOfMaskDetection(Classifier.Recognition classRecognition) {
                if (classRecognition != null) {
                    runOnUiThread(() -> {
                        camStatusTextView.setTextColor(classRecognition.getColor());
                        camStatusTextView.setText(classRecognition.toString());
                    });
                }
            }
        });
        return faceAndMaskDetection;
    }
}