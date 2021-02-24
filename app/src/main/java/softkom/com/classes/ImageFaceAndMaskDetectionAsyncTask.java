package softkom.com.classes;
//https://towardsdatascience.com/how-to-detect-mouth-open-for-face-login-84ca834dff3b
//https://medium.com/@luca_anzalone/setting-up-dlib-and-opencv-for-android-3efdbfcf9e7f

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Size;

import com.inex.mobilefacenet.MainActivity;

import org.tensorflow.lite.examples.detection.tflite.Classifier;

public class ImageFaceAndMaskDetectionAsyncTask extends AsyncTask<Bitmap, Bitmap, Bitmap> {

    protected boolean StillProcessing;
    MaskDetector maskDetector = null;
    FaceDetection faceDetection = null;
    Size size;
    Runnable myRunner = new Runnable() {
        public void run() {
            new ImageFaceAndMaskDetection();
        }
    };
    private Bitmap bitmapWithCroppedFace = null;
    private Bitmap bitmapForFaceFind = null;
    private Classifier.Recognition classifierRecognition = null;

    public ImageFaceAndMaskDetectionAsyncTask() {
        faceDetection = new FaceDetection(MainActivity.appContext);
        maskDetector = new MaskDetector();
    }

    public Bitmap getBitmapOfDetectedFace() {
        return bitmapWithCroppedFace;
    }

    public Classifier.Recognition getClassifierRecognition() {
        return classifierRecognition;
    }

    //http://www.android4devs.pl/2011/08/asynctask-asynchroniczne-wykonywanie-czasochlonnych-zadan/
    public boolean DetectFromImage(Bitmap imageToDetect) {
        //return DetectFromImageInternal(imageToDetect);
        return DetectFromImageAsync(imageToDetect);
    }

    private boolean DetectFromImageAsync(Bitmap imageToDetect) {
        this.execute(imageToDetect);
        return true;
    }

    private boolean DetectFromImageInternal(Bitmap imageToDetect) {
        boolean result = false;
        //copy image to be safe that will be not changed
        bitmapForFaceFind = Bitmap.createBitmap(imageToDetect);
        //use as is, will be changed meantime?
        //bitmapForFaceFind = imageToDetect;
        bitmapWithCroppedFace = null;
        if ((bitmapWithCroppedFace = faceDetection.RunFaceDetect(bitmapForFaceFind)) != null) {
            size = new Size(bitmapWithCroppedFace.getWidth(), bitmapWithCroppedFace.getHeight());
            //for portrait mode: I/tensorflow: DetectorActivity: Camera orientation relative to screen canvas: 90
            if ((maskDetector.InitMaskDetector(MainActivity.appContext, MainActivity.appContext.getAssets(), size, 0, 180, 0)) == true) {
                classifierRecognition = maskDetector.processImage(bitmapWithCroppedFace);
                result = true;
            }
        }
        return result;
    }

    public void FinishDetectingImage() {
        maskDetector.endMaskDetector();
    }

    public boolean isStillProcessing() {
        return StillProcessing;
    }

    @Override
    protected Bitmap doInBackground(Bitmap... args) {
        boolean result = false;
        Bitmap bitmapToProcess = args[0];

/*
        asyncContext = (AsyncContext) args[0];
        asyncContext = (DetectFromImageInternal()) args[1];
        //doSomething();
*/
        result = DetectFromImageInternal(bitmapToProcess);

        //publishProgress("Sleeping..."); // Calls onProgressUpdate()
        return bitmapToProcess;
    }

    @Override
    protected void onPreExecute() { //call from UI thread
        //progressDialog.show();
        StillProcessing = true;
    }

    @Override
    protected void onPostExecute(Bitmap imageToDetect) {
        // execution of result of Long time consuming operation            . progressDialog.dismiss();
        //updateUIWithResult();
        StillProcessing = false;
    }

    protected void onProgressUpdate(Bitmap imageToDetect) {
        //updateProgressUI();
    }
}
