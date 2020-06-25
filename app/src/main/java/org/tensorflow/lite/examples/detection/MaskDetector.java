package org.tensorflow.lite.examples.detection;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.media.FaceDetector;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.Classifier;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


/* usage:

new MaskDetector();
InitMaskDetector(Context Mcontext, AssetManager assetsManager, final Size size, final int rotation, int getSensorOrientation, int CameraFacing)
processImage(); // here we process it and do  face recognition


*/
public class MaskDetector {
    private static final Logger LOGGER = new Logger();


    // Face Mask
    private static final int TF_OD_API_INPUT_SIZE = 224;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "mask_detector.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/mask_labelmap.txt";

    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = false;

    //actual level of threshold
    private static float CONFIDENCE_LEVEL_TRESHOLD = 0.6f;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(800, 600);
    //private static final int CROP_SIZE = 320;
    //private static final Size CROP_SIZE = new Size(320, 320);


    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    OverlayView trackingOverlay;
    private Integer sensorOrientation;

    private Classifier detector;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    private BorderedText borderedText;
    private int CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_BACK;//Camera.CameraInfo.CAMERA_FACING_FRONT;

    int cameraFacing; //provided camera facing

    // Face detector
    //private FaceDetector faceDetector;

    // here the preview image is drawn in portrait way
    private Bitmap portraitBmp = null;
    // here the face is cropped and drawn
    private Bitmap faceBmp = null;

    Context context;
    AssetManager assetManager;
    private int previewWidth, previewHeight;

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }



    /*
    normalnie demo na telefonie takie cos generuje

    2020-06-25 21:56:51.090 10812-10812/? W/tensorflow: TFLiteObjectDetectionAPIModel: mask
2020-06-25 21:56:51.091 10812-10812/? W/tensorflow: TFLiteObjectDetectionAPIModel: no-mask
2020-06-25 21:56:51.094 10812-10812/? I/tensorflow: DetectorActivity: Camera orientation relative to screen canvas: 90
2020-06-25 21:56:51.094 10812-10812/? I/tensorflow: DetectorActivity: Initializing at size 880x720
2020-06-25 21:56:51.095 10812-10812/? I/tensorflow: DetectorActivity: Preparing image 1 for detection in bg thread.

     */
    /*
        mcontext            use this or GetContgext()
        assetmanager        use getAssets()
        size                Size object
        rotation
        getSensorOrientation use getScreenOrientation()
        CameraFacing         use camera id face or front (used to rotate final stages)
     */
    public boolean InitMaskDetector(Context Mcontext, AssetManager assetsManager, final Size size, final int rotation, int getSensorOrientation, int CameraFacing) {
        context = Mcontext;
        assetsManager = assetsManager;
        cameraFacing =  CameraFacing;

        tracker = new MultiBoxTracker(context);

        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            assetsManager,
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            //cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            //Toast toast = Toast.makeText(context.getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
//            /toast.show();
            //finish();
            return false;
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();
        sensorOrientation = rotation - getSensorOrientation;
        LOGGER.d("Camera orientation relative to screen canvas:  sensorOrientation(%d) = rotation(%d) - getSensorOrientation(%d)", sensorOrientation,rotation,getSensorOrientation);

        LOGGER.d("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);

        int targetW, targetH;
        if (sensorOrientation == 90 || sensorOrientation == 270) {
            targetH = previewWidth;
            targetW = previewHeight;
            LOGGER.d("Changed  image parameters *** Width = Height and Height = Width ***", previewWidth, previewHeight);
        } else {
            targetW = previewWidth;
            targetH = previewHeight;
        }
        int cropW = (int) (targetW / 2.0);
        int cropH = (int) (targetH / 2.0);

        croppedBitmap = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888);

        portraitBmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        faceBmp = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropW, cropH,
                        sensorOrientation, MAINTAIN_ASPECT);

//    frameToCropTransform =
//            ImageUtils.getTransformationMatrix(
//                    previewWidth, previewHeight,
//                    previewWidth, previewHeight,
//                    sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);


        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
        return true;

    }


    public void processImage(Bitmap croppedFaceWithBitmap) {
        ++timestamp;
        final long currTimestamp = timestamp;

        computingDetection = true;
        LOGGER.d("Preparing image " + currTimestamp + " for detection in bg thread.");

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        //detect face here from bitmap if it's not processed yet

        //here we got cropped image

        onFacesDetected(currTimestamp, croppedFaceWithBitmap);

    }

    protected void setUseNNAPI(final boolean isChecked) {
        detector.setUseNNAPI(isChecked);
    }


    protected void setNumThreads(final int numThreads) {
        detector.setNumThreads(numThreads);
    }

    void onFacesDetectedClear(long currTimestamp) {
        // if (faces.size() == 0) {
        //     updateResults(currTimestamp, new LinkedList<>());
        return;
        //}
    }

    // Face Mask Processing
    private Matrix createTransform(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation) {

        Matrix matrix = new Matrix();
        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
                LOGGER.d("Rotation of %d % 90 != 0", applyRotation);
            }

            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

//        // Account for the already applied rotation, if any, and then determine how
//        // much scaling is needed for each axis.
//        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;
//
//        final int inWidth = transpose ? srcHeight : srcWidth;
//        final int inHeight = transpose ? srcWidth : srcHeight;

        if (applyRotation != 0) {

            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;

    }

    private void updateResults(long currTimestamp, final List<Classifier.Recognition> mappedRecognitions) {

        tracker.trackResults(mappedRecognitions, currTimestamp);
        //trackingOverlay.postInvalidate();
        computingDetection = false;


    }

    //got detected faces
    void onFacesDetected(long currTimestamp, Bitmap croppedFaceWithBitmap) {
        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
        final Canvas canvas = new Canvas(cropCopyBitmap);
        final Paint paint = new Paint();

        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);

        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
        switch (MODE) {
            case TF_OD_API:
                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                break;
        }

        final List<Classifier.Recognition> mappedRecognitions =
                new LinkedList<Classifier.Recognition>();
        //final List<Classifier.Recognition> results = new ArrayList<>();

        // Note this can be done only once
        int sourceW = rgbFrameBitmap.getWidth();
        int sourceH = rgbFrameBitmap.getHeight();
        int targetW = portraitBmp.getWidth();
        int targetH = portraitBmp.getHeight();
        Matrix transform = createTransform(
                sourceW,
                sourceH,
                targetW,
                targetH,
                sensorOrientation);
        final Canvas cv = new Canvas(portraitBmp);

        // draws the original image in portrait mode.
        cv.drawBitmap(rgbFrameBitmap, transform, null);

        final Canvas cvFace = new Canvas(faceBmp);

        boolean saved = false;


        /*
         here we should iterate through faces
             for (Face face : faces) {
         */
        {
            LOGGER.d("Running detection on face " + currTimestamp);


            //results = detector.recognizeImage(croppedBitmap);
            final RectF boundingBox = new RectF(0, 0, croppedFaceWithBitmap.getWidth(), croppedFaceWithBitmap.getHeight());
            //croppedFaceWithBitmap
            final boolean goodConfidence = true;
            if (boundingBox != null && goodConfidence) {

                // maps crop coordinates to original
                cropToFrameTransform.mapRect(boundingBox);

                // maps original coordinates to portrait coordinates
                RectF faceBB = new RectF(boundingBox);
                transform.mapRect(faceBB);

                // translates portrait to origin and scales to fit input inference size
                //cv.drawRect(faceBB, paint);
                float sx = ((float) TF_OD_API_INPUT_SIZE) / faceBB.width();
                float sy = ((float) TF_OD_API_INPUT_SIZE) / faceBB.height();
                Matrix matrix = new Matrix();
                matrix.postTranslate(-faceBB.left, -faceBB.top);
                matrix.postScale(sx, sy);
                cvFace.drawBitmap(portraitBmp, matrix, null);

                String label = "";
                float confidence = -1f;
                Integer color = Color.BLUE;

                final long startTime = SystemClock.uptimeMillis();
                final List<Classifier.Recognition> resultsAux = detector.recognizeImage(faceBmp);
                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                if (resultsAux.size() > 0) {

                    Classifier.Recognition result = resultsAux.get(0);

                    float conf = result.getConfidence();
                    if (conf >= CONFIDENCE_LEVEL_TRESHOLD ) {

                        confidence = conf;
                        label = result.getTitle();
                        if (result.getId().equals("0")) {
                            color = Color.GREEN;
                        } else {
                            color = Color.RED;
                        }
                    }

                    Log.d("onFacesDetected()", "conf level:[" + conf +"] .Result is " + label );
                }
                if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {

                    Log.d("onFacesDetected()", "Flipping image because is Frontal camera" );

                    // camera is frontal so the image is flipped horizontally
                    // flips horizontally
                    Matrix flip = new Matrix();
                    if (sensorOrientation == 90 || sensorOrientation == 270) {
                        flip.postScale(1, -1, previewWidth / 2.0f, previewHeight / 2.0f);
                    } else {
                        flip.postScale(-1, 1, previewWidth / 2.0f, previewHeight / 2.0f);
                    }
                    //flip.postScale(1, -1, targetW / 2.0f, targetH / 2.0f);
                    flip.mapRect(boundingBox);

                }

                final Classifier.Recognition result = new Classifier.Recognition(
                        "0", label, confidence, boundingBox);

                Log.d("onFacesDetected()", "classifier recognition():" + result.toString() );

                result.setColor(color);
                result.setLocation(boundingBox);
                mappedRecognitions.add(result);
            }


        }//endfor ;)

        if (saved) {
            //lastSaved = System.currentTimeMillis();
        }
        updateResults(currTimestamp, mappedRecognitions);

    }



    //got detected faces
    void onFacesDetected_org(long currTimestamp, Bitmap croppedFaceWithBitmap) {
        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
        final Canvas canvas = new Canvas(cropCopyBitmap);
        final Paint paint = new Paint();

        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);

        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
        switch (MODE) {
            case TF_OD_API:
                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                break;
        }

        final List<Classifier.Recognition> mappedRecognitions =
                new LinkedList<Classifier.Recognition>();
        //final List<Classifier.Recognition> results = new ArrayList<>();

        // Note this can be done only once
        int sourceW = rgbFrameBitmap.getWidth();
        int sourceH = rgbFrameBitmap.getHeight();
        int targetW = portraitBmp.getWidth();
        int targetH = portraitBmp.getHeight();
        Matrix transform = createTransform(
                sourceW,
                sourceH,
                targetW,
                targetH,
                sensorOrientation);
        final Canvas cv = new Canvas(portraitBmp);

        // draws the original image in portrait mode.
        cv.drawBitmap(rgbFrameBitmap, transform, null);

        final Canvas cvFace = new Canvas(faceBmp);

        boolean saved = false;

        /*
         here we should iterate through faces
             for (Face face : faces) {
         */
        {
            LOGGER.d("Running detection on face " + currTimestamp);


            //results = detector.recognizeImage(croppedBitmap);
            final RectF boundingBox = new RectF(0, 0, croppedFaceWithBitmap.getWidth(), croppedFaceWithBitmap.getHeight());
            //croppedFaceWithBitmap
            final boolean goodConfidence = true;
            if (boundingBox != null && goodConfidence) {

                // maps crop coordinates to original
                cropToFrameTransform.mapRect(boundingBox);

                // maps original coordinates to portrait coordinates
                RectF faceBB = new RectF(boundingBox);
                transform.mapRect(faceBB);

                // translates portrait to origin and scales to fit input inference size
                //cv.drawRect(faceBB, paint);
                float sx = ((float) TF_OD_API_INPUT_SIZE) / faceBB.width();
                float sy = ((float) TF_OD_API_INPUT_SIZE) / faceBB.height();
                Matrix matrix = new Matrix();
                matrix.postTranslate(-faceBB.left, -faceBB.top);
                matrix.postScale(sx, sy);
                cvFace.drawBitmap(portraitBmp, matrix, null);

                String label = "";
                float confidence = -1f;
                Integer color = Color.BLUE;

                final long startTime = SystemClock.uptimeMillis();
                final List<Classifier.Recognition> resultsAux = detector.recognizeImage(faceBmp);
                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                if (resultsAux.size() > 0) {

                    Classifier.Recognition result = resultsAux.get(0);

                    float conf = result.getConfidence();
                    if (conf >= CONFIDENCE_LEVEL_TRESHOLD ) {

                        confidence = conf;
                        label = result.getTitle();
                        if (result.getId().equals("0")) {
                            color = Color.GREEN;
                        } else {
                            color = Color.RED;
                        }
                    }

                    Log.d("onFacesDetected()", "conf level:[" + conf +"] .Result is " + label );
                }
                if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {

                    Log.d("onFacesDetected()", "Flipping image because is Frontal camera" );
                    // camera is frontal so the image is flipped horizontally
                    // flips horizontally
                    Matrix flip = new Matrix();
                    if (sensorOrientation == 90 || sensorOrientation == 270) {
                        flip.postScale(1, -1, previewWidth / 2.0f, previewHeight / 2.0f);
                    } else {
                        flip.postScale(-1, 1, previewWidth / 2.0f, previewHeight / 2.0f);
                    }
                    //flip.postScale(1, -1, targetW / 2.0f, targetH / 2.0f);
                    flip.mapRect(boundingBox);

                }

                final Classifier.Recognition result = new Classifier.Recognition(
                        "0", label, confidence, boundingBox);

                Log.d("onFacesDetected()", "classifier recognition():" + result.toString() );
                result.setColor(color);
                result.setLocation(boundingBox);
                mappedRecognitions.add(result);
            }


        }//endfor ;)

        if (saved) {
            //lastSaved = System.currentTimeMillis();
        }
        updateResults(currTimestamp, mappedRecognitions);

    }


}
