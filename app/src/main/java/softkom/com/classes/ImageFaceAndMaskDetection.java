package softkom.com.classes;
//https://towardsdatascience.com/how-to-detect-mouth-open-for-face-login-84ca834dff3b
//https://medium.com/@luca_anzalone/setting-up-dlib-and-opencv-for-android-3efdbfcf9e7f


//callbacks
//https://stackoverflow.com/questions/18054720/what-is-callback-in-android
//https://guides.codepath.com/android/Creating-Custom-Listeners

import android.graphics.Bitmap;
import android.util.Log;
import android.util.Size;

import com.inex.mobilefacenet.MainActivity;

import org.tensorflow.lite.examples.detection.tflite.Classifier;

public class ImageFaceAndMaskDetection {
    protected boolean StillProcessing;
    MaskDetector maskDetector = null;
    FaceDetection faceDetection = null;
    Size size;
    private DetectionPhaseListener listener;
    private Bitmap bitmapWithCroppedFace = null;
    private Bitmap bitmapForFaceFind = null;
    private Classifier.Recognition classifierRecognition = null;


    public ImageFaceAndMaskDetection() {
        faceDetection = new FaceDetection(MainActivity.appContext);
        maskDetector = new MaskDetector();
        // set null or default listener or accept as argument to constructor
        this.listener = null;
    }

    public Bitmap getBitmapOfDetectedFace() {
        return bitmapWithCroppedFace;
    }

    public Classifier.Recognition getClassifierRecognition() {
        return classifierRecognition;
    }

    // Assign the listener implementing events interface that will receive the events
    public void setCustomObjectListener(DetectionPhaseListener listener) {
        this.listener = listener;
    }

    //http://www.android4devs.pl/2011/08/asynctask-asynchroniczne-wykonywanie-czasochlonnych-zadan/
    public boolean DetectFromImage(Bitmap imageToDetect) {
        bitmapForFaceFind = imageToDetect;
        return DetectFromImageAsync(imageToDetect);
    }

    private boolean DetectFromImageAsync(Bitmap imageToDetect) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long startMeasurement = System.currentTimeMillis(), endMeasurement = 0;
                StillProcessing = true;
                detectFromImageInternal(bitmapForFaceFind);
                StillProcessing = false;
                endMeasurement = System.currentTimeMillis();
                Log.v("ImageFaceAndMaskDetection()", "DetectFromImageAsync() Processing image took: " + (endMeasurement - startMeasurement) + "ms");
            }
        }).start();
        return true;
    }

    private void detectFromImageInternal(Bitmap imageToDetect) {
        //copy image to be safe that will be not changed
        bitmapForFaceFind = Bitmap.createBitmap(imageToDetect);
        //use as is, will be changed meantime?
        //bitmapForFaceFind = imageToDetect;
        bitmapWithCroppedFace = null;
        if ((bitmapWithCroppedFace = faceDetection.RunFaceDetect(bitmapForFaceFind)) != null) {
            if (listener != null)
                listener.onFaceDetected(bitmapWithCroppedFace);

            size = new Size(bitmapWithCroppedFace.getWidth(), bitmapWithCroppedFace.getHeight());
            //for portrait mode: I/tensorflow: DetectorActivity: Camera orientation relative to screen canvas: 90
            if ((maskDetector.InitMaskDetector(MainActivity.appContext, MainActivity.appContext.getAssets(), size, 0, 180, 0))) {
                classifierRecognition = maskDetector.processImage(bitmapWithCroppedFace);
            }
        } else {
            if (listener != null)
                listener.onNoFaceDetected(imageToDetect);
        }
        if (listener != null)
            listener.onResultOfDetection(classifierRecognition);
    }

    public void FinishDetectingImage() {
        maskDetector.endMaskDetector();
    }

    public boolean isStillProcessing() {
        return StillProcessing;
    }

    // Step 1 - This interface defines the type of messages to communicate to my owner
    public interface DetectionPhaseListener {
        // fire when face was detected
        public void onFaceDetected(Bitmap bitmapOfDetectedFace);

        public void onNoFaceDetected(Bitmap bitmapOfUndetectedFace);

        // result of detection
        public void onResultOfDetection(Classifier.Recognition classRecognition);
    }
}

