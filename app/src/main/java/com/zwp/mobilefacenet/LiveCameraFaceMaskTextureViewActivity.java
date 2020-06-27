
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
package com.zwp.mobilefacenet;


import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.zwp.mobilefacenet.mtcnn.Box;
import com.zwp.mobilefacenet.utils.MyUtil;
import com.zwp.mobilefacenet.utils.PermissionHelper;

import softkom.com.classes.ImageFaceAndMaskDetection;
import softkom.com.classes.MaskDetector;
import org.tensorflow.lite.examples.detection.tflite.Classifier;

import java.io.IOException;
import java.util.Vector;

//import android.support.annotation.NonNull;

/**
 * More or less straight out of TextureView's doc.
 * <p>
 * TODO: add options for different display sizes, frame rates, camera selection, etc.
 */
public class LiveCameraFaceMaskTextureViewActivity extends Activity implements TextureView.SurfaceTextureListener {
    private final String TAG = "LiveCameraTViewActivity";


    private SurfaceTexture mSurfaceTexture;
    private TextView statusTextView;
    TextureView textureView;
    ImageView faceImageView;

    private int displayDegree;
    private byte[] mData;
    private Camera.Size mSize;
    Bitmap bitmap;
    Bitmap bitmapCroppedFace;
    long start = System.currentTimeMillis();
    long end = System.currentTimeMillis();

    MaskDetector maskDetector = null;

    ImageFaceAndMaskDetection imageFaceAndMaskDetection;

//    Log.d(TAG, "textureView.getBitmap() time elapsed " + (end - start) + "ms");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_live_camera_textureview_facemask);
        textureView = ((TextureView) findViewById(R.id.LiveCameraFacemaskTextureViewActivity));
        statusTextView = ((TextView) findViewById(R.id.FaceMaskStatus_textView));
        faceImageView = ((ImageView) findViewById(R.id.FaceImageView));
        textureView.setSurfaceTextureListener(this);
        applyMirroringOnCamera(textureView);

        imageFaceAndMaskDetection = new ImageFaceAndMaskDetection();

/*
        Button button = findViewById(R.id.TakePictureButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Because the photo is too large, it cannot be passed back using Intent. For simplicity, static variables are used.// It is recommended to save it to a file in your own project.

                Log.d(TAG, "onListener() TakePictureButton()");
                //MyUtil.convertBitmap() time elapsed 566ms
                start = System.currentTimeMillis();
                //bitmap = MyUtil.convertBitmap(mData, mCamera, displayDegree);
                end = System.currentTimeMillis();

                Log.d(TAG, "MyUtil.convertBitmap() time elapsed " + (end - start) + "ms");

                //here this is working but need to swap mirroring
                //textureView.getBitmap() time elapsed 47
                start = System.currentTimeMillis();
                //bitmap = textureView.getBitmap();
                end = System.currentTimeMillis();

                Log.d(TAG, "textureView.getBitmap() time elapsed " + (end - start) + "ms");

                MainActivity.currentBtn.setImageBitmap(bitmap);
                if (MainActivity.currentBtn.getId() == R.id.image_button1) {
                    MainActivity.bitmap1 = bitmap;
                } else {
                    MainActivity.bitmap2 = bitmap;
                }
                finish();
            }
        });
        */

    }


    /*
       Fix for back camera treated as front and do Mirroring
    */
    private void applyMirroringOnCamera(TextureView textureView) {

        // MainActivity.appContext.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
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

    //    static int frameSkipperEnd  = 2;
//    static int frameSkipperCounter = frameSkipperEnd + 1;
    private long frameSkipperInMS = 300;
    private long frameSkipperLastMS = 0;

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invoked every time there's a new Camera preview frame
/*
		TextureView.getBitmap(bitmap);

		final Canvas c = SurfaceHolder.lockCanvas();
		if ( c != null) {
			//canvas.drawText("getBmp= "  + time1, 10, 40, paint1);
			c.drawBitmap(bmp, 0, 0, null);
            SurfaceHolder.unlockCanvasAndPost(c);
		}
		long total = System.currentTimeMillis() - time0;
		long time2 = total -time1;
		Log.i("onSurfaceTextureUpdated", "timing: getBmp= "  + time1 + " draw= " + time2 + " total= " + total);
*/


        // Invoked every time there's a new Camera preview frame
        start = System.currentTimeMillis();
        bitmap = textureView.getBitmap();
        end = System.currentTimeMillis();
        //Log.d(TAG, "onSurfaceTextureUpdated() " + surface.getTimestamp());
        //       Log.d(TAG, "onSurfaceTextureUpdated()->textureView.getBitmap() time elapsed " + (end - start) + "ms");


        Log.v(TAG, "onSurfaceTextureUpdated() occured time:" + start + " bitmap:" + bitmap);
        Log.v(TAG, "onSurfaceTextureUpdated() actual ms difference " + (start - frameSkipperLastMS) + " , frame skip threshold:" + frameSkipperInMS);

        if (start - frameSkipperLastMS >= frameSkipperInMS) {
            //RunLiveFaceDetect();

            //runLiveFaceDetectInBackground();
            imageFaceAndMaskDetection.DetectFromImage(bitmap);

            if((bitmapCroppedFace = imageFaceAndMaskDetection.getBitmapOfDetectedFace())  != null) {

                faceImageView.setImageBitmap(bitmapCroppedFace);
                //statusTextView.setText( "Title: " + imageFaceAndMaskDetection.getClassifierRecognition().getTitle() + " , " + imageFaceAndMaskDetection.getClassifierRecognition().getConfidence());
                statusTextView.setText( imageFaceAndMaskDetection.getClassifierRecognition().toString());

                //imageFaceAndMaskDetection.FinishDetectingImage();
            } else{
                faceImageView.setImageDrawable(null);
            }
            frameSkipperLastMS = start;
            Log.v(TAG, "onSurfaceTextureUpdated() run face detect ");
        }


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

    private void startPreview() {

        startPreview_newway();
    }


    public Classifier.Recognition RunLiveFaceDetect() {
        String errorString = "";
        boolean gotError = false;
        Size size;
        Classifier.Recognition classifierRecognition = null;

        if (bitmap != null) {
            //try actions on this bitmap

//            if (maskDetector == null)
                maskDetector = new MaskDetector();

            if (maskDetector != null) {

                //find face
                if ((bitmapCroppedFace = findFace(bitmap)) != null) {

                    //tricked resized image for width and height
                    //                   ((ImageView) findViewById(R.id.FaceImageView)).setImageBitmap(bitmapCroppedFace);

                    size = new Size(bitmapCroppedFace.getWidth(), bitmapCroppedFace.getHeight());

                    //for portrait mode:
                    // I/tensorflow: DetectorActivity: Camera orientation relative to screen canvas: 90
                    if ((maskDetector.InitMaskDetector(MainActivity.appContext, getAssets(), size, 0, 180, 0)) == true) {
                        classifierRecognition = maskDetector.processImage(bitmapCroppedFace);
                    } else
                        errorString = "InitMaskDetector failed";
                } else {
                    errorString = "No face found.";
                }

            } else
            errorString = "Init MaskDetector failed somehow";
        } else
            errorString = "No bitmap face provided";


        if (gotError) {
            Toast.makeText(MainActivity.appContext, errorString, Toast.LENGTH_LONG).show();
        }

    return classifierRecognition;
    }


    private Bitmap findFace(Bitmap snapshotFromCameraBitmap) {
        Vector<Box> boxes1 = new Vector<>();
        Bitmap bitmapCroppedToFace;

        //Bitmap bitmapForFaceDetection = snapshotFromCameraBitmap;
        Bitmap bitmapForFaceDetection = snapshotFromCameraBitmap.copy(snapshotFromCameraBitmap.getConfig(), true);

        boxes1 = MainActivity.mtcnn.detectFaces(bitmapForFaceDetection, bitmapForFaceDetection.getWidth() / 5); // Only this code detects the face, the following is based on the Box to cut out the face in the picture

        if (boxes1.size() == 0) {  //got face?
            return null;
        }
        Box box1 = boxes1.get(0);
        box1.toSquareShape();
        box1.limitSquare(bitmapForFaceDetection.getWidth(), bitmapForFaceDetection.getHeight());
        Rect rect1 = box1.transform2Rect();

        //Cut face
        bitmapCroppedToFace = MyUtil.crop(bitmapForFaceDetection, rect1);

        return bitmapCroppedToFace;
    }





    /*
     *
     *
     *
     * */


    private void startPreview_newway() {
//---------------testing this-----------
        Log.d(TAG, "startPreview_newway() ");
        MainActivity.mCamera = Camera.open(MainActivity.CAMERA_ID);
        Camera.Parameters parameters = MainActivity.mCamera.getParameters();

        displayDegree = MyUtil.setCameraDisplayOrientation(0, MainActivity.mCamera, getWindowManager());
        // Get the right resolution
        mSize = MyUtil.getOptimalSize(parameters.getSupportedPreviewSizes(), textureView.getWidth(), textureView.getHeight());
        parameters.setPreviewSize(mSize.width, mSize.height);

        parameters.setPreviewFormat(MainActivity.IMAGE_FORMAT);
        MainActivity.mCamera.setParameters(parameters);

        // try {
        //    mCamera.setPreviewDisplay(holder);
        //} catch (IOException ioe) {
        //   ioe.printStackTrace();
        //}

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

    //---------------this working somehow-----------
    private void startPreview_oldway() {
        MainActivity.mCamera = Camera.open();
        if (MainActivity.mCamera == null) {
            // Seeing this on Nexus 7 2012 -- I guess it wants a rear-facing camera, but
            // there isn't one.  TODO: fix
            throw new RuntimeException("Default camera not available");
        }

        try {
            MainActivity.mCamera.setPreviewTexture(mSurfaceTexture);
            Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

            if (display.getRotation() == Surface.ROTATION_0) {
                MainActivity.mCamera.setDisplayOrientation(90);
            }
            if (display.getRotation() == Surface.ROTATION_270) {
                MainActivity.mCamera.setDisplayOrientation(180);

            }

            MainActivity.mCamera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
            Log.e(TAG, "Exception starting preview", ioe);
        }

    }

    //https://android-developers.googleblog.com/2009/05/painless-threading.html
    private void runLiveFaceDetectInBackground() {
    new Thread(new Runnable() {
        @Override
        public void run() {
            //do something
            Classifier.Recognition classifierRecognition;

            ImageView imageView = (ImageView) findViewById(R.id.FaceImageView);

            classifierRecognition = RunLiveFaceDetect();
            // Update the progress bar and display the
            //current value in the text view
            if (bitmapCroppedFace != null) {

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        // Stuff that updates the UI
                        imageView.setImageBitmap(bitmapCroppedFace);
                    }
                });
            }
        } // run()
    }).start();

}
}