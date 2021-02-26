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
    private DetectionListener detectionListener;
    private DetectionListenerFaceClassifier detectionListenerFaceClassifier;

    private Bitmap bitmapWithCroppedFace = null;
    private Bitmap bitmapForFaceFind = null;
    private Classifier.Recognition classifierRecognition = null;

    public ImageFaceAndMaskDetection(boolean enableMaskDetector) {
        faceDetection = new FaceDetection(MainActivity.appContext, true);
        if (enableMaskDetector)
            maskDetector = new MaskDetector();
        else
            maskDetector = null;

        this.detectionListener = null;
    }

    public ImageFaceAndMaskDetection() {
        this(true);
    }

    public Bitmap getBitmapOfDetectedFace() {
        return bitmapWithCroppedFace;
    }

    public Classifier.Recognition getClassifierRecognition() {
        return classifierRecognition;
    }

    // Assign the listener implementing events interface that will receive the events
    public void setDetectionListener(DetectionListener listener) {
        this.detectionListener = listener;
    }

    public void setDetectionListenerFaceClassifier(DetectionListenerFaceClassifier listener) {
        this.detectionListenerFaceClassifier = listener;
    }

    //http://www.android4devs.pl/2011/08/asynctask-asynchroniczne-wykonywanie-czasochlonnych-zadan/
    public boolean DetectFromImage(Bitmap imageToDetect) {
        bitmapForFaceFind = imageToDetect;
        return DetectFromImageAsync(imageToDetect);
    }

    private boolean DetectFromImageAsync(Bitmap imageToDetect) {
        new Thread(() -> {
            long startMeasurement = System.currentTimeMillis(), endMeasurement = 0;
            StillProcessing = true;

            if (detectionListener == null)
                detectFromImageFaceClassifier(bitmapForFaceFind);
            else
                detectFromImageInternal(bitmapForFaceFind);

            StillProcessing = false;
            endMeasurement = System.currentTimeMillis();
            Log.v("ImageFaceAndMaskDetection()", "DetectFromImageAsync() Processing image took: " + (endMeasurement - startMeasurement) + "ms");
        }).start();
        return true;
    }

    private void detectFromImageInternal(Bitmap imageToDetect) {
        //copy image to be safe that will be not changed
//        bitmapForFaceFind = Bitmap.createBitmap(imageToDetect);

        if (detectionListener == null)
            return;

        bitmapForFaceFind = imageToDetect;
        bitmapWithCroppedFace = faceDetection.DetectFace(bitmapForFaceFind);
        if (bitmapWithCroppedFace != null) {
            detectionListener.onFaceDetected(bitmapWithCroppedFace);
            if (maskDetector != null) {
                classifierRecognition = detectMaskInBitmap();
                detectionListener.onResultOfMaskDetection(classifierRecognition);
            }
        } else {
            detectionListener.onNoFaceDetected(imageToDetect);
        }
    }

    private void detectFromImageFaceClassifier(Bitmap imageToDetect) {
        //copy image to be safe that will be not changed
//        bitmapForFaceFind = Bitmap.createBitmap(imageToDetect);
        if (detectionListenerFaceClassifier == null)
            return;

        bitmapForFaceFind = imageToDetect;
        FaceClassifier faceClassifier = faceDetection.DetectFaceWithClassifier(bitmapForFaceFind,false);

        if (faceClassifier.faceDetected) {
            detectionListenerFaceClassifier.onFaceDetected(faceClassifier);
            if (maskDetector != null) {
                classifierRecognition = detectMaskInBitmap();
                faceClassifier.classifierRecognition = classifierRecognition;
                detectionListenerFaceClassifier.onResultOfMaskDetection(faceClassifier);
            }
        } else {
            detectionListenerFaceClassifier.onNoFaceDetected(faceClassifier);
        }
    }

    private Classifier.Recognition detectMaskInBitmap() {
        size = new Size(bitmapWithCroppedFace.getWidth(), bitmapWithCroppedFace.getHeight());
        //for portrait mode: I/tensorflow: DetectorActivity: Camera orientation relative to screen canvas: 90
        if ((maskDetector.InitMaskDetector(MainActivity.appContext, MainActivity.appContext.getAssets(), size, 0, 180, 0))) {
            return maskDetector.processImage(bitmapWithCroppedFace);
        }
        return null;
    }

    public void FinishDetectingImage() {
        maskDetector.endMaskDetector();
    }

    public boolean isProcessingFinished() {
        return !StillProcessing;
    }

    // Step 1 - This interface defines the type of messages to communicate to my owner
    public interface DetectionListener {
        void onFaceDetected(Bitmap bitmapOfDetectedFace);

        void onNoFaceDetected(Bitmap bitmapOfUndetectedFace);

        // result of mask detection
        void onResultOfMaskDetection(Classifier.Recognition classRecognition);
    }

    public interface DetectionListenerFaceClassifier {
        void onFaceDetected(FaceClassifier faceClassifier);

        void onNoFaceDetected(FaceClassifier faceClassifier);

        // result of mask detection
        void onResultOfMaskDetection(FaceClassifier faceClassifier);
    }
}

