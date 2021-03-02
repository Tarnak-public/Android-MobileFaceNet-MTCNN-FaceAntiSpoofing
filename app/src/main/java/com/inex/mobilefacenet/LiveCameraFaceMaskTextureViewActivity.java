
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
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.inex.mobilefacenet.mtcnn.Box;
import com.inex.mobilefacenet.mtcnn.Utils;

import org.tensorflow.lite.examples.detection.tflite.Classifier;

import softkom.com.classes.AutoFitTextureView;
import softkom.com.classes.CameraSurfaceListener;
import softkom.com.classes.FaceClassifier;
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

    public static void drawFaceBox(Bitmap bitmap, Box box, int thick) {
        Utils.drawRect(bitmap, box.transform2Rect(), thick);
        Utils.drawPoints(bitmap, box.landmark, thick);
    }

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
        ImageView overlay0FaceImageView = findViewById(R.id.overlay_cam0_ImageView);

        AutoFitTextureView cam1TextureView = findViewById(R.id.live_cam1_TextureViewActivity);
        TextView cam1StatusTextView = findViewById(R.id.faceMask_cam1_status_textView);
        ImageView cam1FaceImageView = findViewById(R.id.face_cam1_ImageView);
        ImageView overlay1FaceImageView = findViewById(R.id.overlay_cam1_ImageView);

        ImageFaceAndMaskDetection cam0Detector = camFaceDetector(cam0FaceImageView, cam0StatusTextView, cam0TextureView, overlay0FaceImageView);
        ImageFaceAndMaskDetection cam1Detector = camFaceDetector(cam1FaceImageView, cam1StatusTextView, cam1TextureView, overlay1FaceImageView);

        camera0SurfaceListener = new CameraSurfaceListener(this, cam0Detector, Camera.CameraInfo.CAMERA_FACING_BACK, 270, cam0TextureView);
        camera1SurfaceListener = new CameraSurfaceListener(this, cam1Detector, Camera.CameraInfo.CAMERA_FACING_FRONT, 270, cam1TextureView);

        cam0TextureView.setAspectRatio(10, 13); // 2/3
        cam1TextureView.setAspectRatio(10, 13); // 2/3

        cam0TextureView.setSurfaceTextureListener(camera0SurfaceListener);
        cam1TextureView.setSurfaceTextureListener(camera1SurfaceListener);
    }

    private ImageFaceAndMaskDetection camFaceDetector(ImageView camFaceImageView, TextView camStatusTextView, AutoFitTextureView camTextureView, ImageView overlayFaceImageView) {
        ImageFaceAndMaskDetection faceAndMaskDetection = new ImageFaceAndMaskDetection(this,false);
        //        regularListenerBitmap(camFaceImageView, camStatusTextView, faceAndMaskDetection);
        classifierListenerBitmap(camFaceImageView, camStatusTextView, faceAndMaskDetection, camTextureView, overlayFaceImageView);

        return faceAndMaskDetection;
    }

    private void regularListenerBitmap(ImageView camFaceImageView, TextView camStatusTextView, ImageFaceAndMaskDetection faceAndMaskDetection, AutoFitTextureView camTextureView) {
        faceAndMaskDetection.setDetectionListener(new ImageFaceAndMaskDetection.DetectionListener() {
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
    }

    private void classifierListenerBitmap(ImageView camFaceImageView, TextView camStatusTextView, ImageFaceAndMaskDetection faceAndMaskDetection, AutoFitTextureView camTextureView, ImageView overlayFaceImageView) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10f);

        faceAndMaskDetection.setDetectionListenerFaceClassifier(new ImageFaceAndMaskDetection.DetectionListenerFaceClassifier() {
            Bitmap overlayBitmap;

            @Override
            public void onFaceDetected(FaceClassifier faceClassifier) {
//                Log.d("DetectFaceWithClassifier", "camTextureView dimensions:" + camTextureView.getWidth() + " x " + camTextureView.getHeight() +
//                        " , overlay1FaceImageView dimensions:" + overlayFaceImageView.getWidth() + " x " + overlayFaceImageView.getHeight()
//                );

                if (overlayBitmap == null) {
                    overlayBitmap = Bitmap.createBitmap(overlayFaceImageView.getWidth(), overlayFaceImageView.getHeight(), Bitmap.Config.ARGB_8888);
                } else {
                    overlayBitmap.eraseColor(Color.TRANSPARENT);
                }
//                Bitmap finalOverlay = Bitmap.createBitmap(overlayFaceImageView.getWidth(), overlayFaceImageView.getHeight(), Bitmap.Config.ARGB_8888);

                drawFaceBox(overlayBitmap, faceClassifier.boxes1.get(0), 3);
                runOnUiThread(() -> runOnUiThread(() -> {
                            overlayFaceImageView.setImageBitmap(overlayBitmap);
                            camFaceImageView.setImageBitmap(faceClassifier.faceBitmap);
                        }
                ));
            }

            @Override
            public void onNoFaceDetected(FaceClassifier faceClassifier) {
                runOnUiThread(() -> runOnUiThread(() -> {
                    overlayFaceImageView.setImageBitmap(null);
                    camFaceImageView.setImageResource(R.drawable.no_face);
                }));
            }

            @Override
            public void onResultOfMaskDetection(FaceClassifier faceClassifier) {
                runOnUiThread(() -> {
                    camStatusTextView.setTextColor(faceClassifier.classifierRecognition.getColor());
                    camStatusTextView.setText(faceClassifier.classifierRecognition.toString());
                });
            }
        });
    }
}