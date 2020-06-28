package softkom.com.classes;
//https://towardsdatascience.com/how-to-detect-mouth-open-for-face-login-84ca834dff3b
//https://medium.com/@luca_anzalone/setting-up-dlib-and-opencv-for-android-3efdbfcf9e7f

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Size;

import com.zwp.mobilefacenet.MainActivity;

import org.tensorflow.lite.examples.detection.tflite.Classifier;

//public class ImageFaceAndMaskDetection extends AsyncTask<AsyncContext, AsyncContext, AsyncContext> {
public class ImageFaceAndMaskDetectionAsyncTask extends AsyncTask<Bitmap, Bitmap, Bitmap> {

    MaskDetector maskDetector = null;
    FaceDetection faceDetection = null;

    Size size;

    public Bitmap getBitmapOfDetectedFace() {
        return bitmapWithCroppedFace;
    }

    private Bitmap bitmapWithCroppedFace = null;

    private Bitmap bitmapForFaceFind = null;

    public Classifier.Recognition getClassifierRecognition() {
        return classifierRecognition;
    }

    private Classifier.Recognition classifierRecognition = null;

    public ImageFaceAndMaskDetectionAsyncTask() {

        faceDetection = new FaceDetection(MainActivity.appContext);
        maskDetector = new MaskDetector();
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

    Runnable myRunner = new Runnable(){
        public void run() {
            new ImageFaceAndMaskDetection();
        }};

    public void FinishDetectingImage() {
        maskDetector.endMaskDetector();
    }

    public boolean isStillProcessing() {
        return StillProcessing;
    }

    protected boolean StillProcessing;

    @Override
    protected void onPreExecute() { //call from UI thread
        //progressDialog.show();
        StillProcessing = true;
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
    protected void onPostExecute(Bitmap imageToDetect) {
        // execution of result of Long time consuming operation            . progressDialog.dismiss();
        //updateUIWithResult();
        StillProcessing = false;
    }

    protected void onProgressUpdate(Bitmap imageToDetect) {
        //updateProgressUI();


    }
}

/*
class AsyncContext {
    int value;
    Bitmap imageToProcess;
    Bitmap faceDetected;
    Classifier.Recognition classiRecognition;
}
*/
/*
Pierwszym z nich jest typ danych wejściowych, jakie możemy przekazać wątkowi,
 drugim typ danych przedstawiających postęp w działaniu,
  trzecim wynik zwracany przez zadanie
 */
/*
//class AsyncTaskRunner extends AsyncTask<AsyncContext, AsyncContext, AsyncContext> {
class AsyncTaskRunner extends AsyncTask<Bitmap, Bitmap, Bitmap> {
    @Override
    protected void onPreExecute() { //call from UI thread
        //progressDialog.show();
    }

    @Override
    protected Bitmap doInBackground(Bitmap... args) {
        //AsyncContext asyncContext = null;

//        asyncContext = (AsyncContext) args[0];
//        asyncContext = (DetectFromImageInternal()) args[1];


        //publishProgress("Sleeping..."); // Calls onProgressUpdate()
        return null;
    }

    @Override
    protected void onProgressUpdate(Bitmap imageToDetect) {
        //updateProgressUI();
    }

    @Override
    protected void onPostExecute(Bitmap imageToDetect) {
        // execution of result of Long time consuming operation            . progressDialog.dismiss();
        //updateUIWithResult();
    }

}
*/

//https://stackoverflow.com/questions/33041573/android-create-a-background-thread-that-runs-periodically-and-does-ui-tasks
//https://android-developers.googleblog.com/2009/05/painless-threading.html


//async are oboslet in api 30 :(
//https://medium.com/better-programming/threading-in-android-129b8688436a

/*
    //this should work always
    private class MyTask extends AsyncTask<Input, Void, Output> {
        protected Output doInBackground(Input... inputs) {
            // do something on the network
            return myOutput;// use this to transmit your result
        }

        protected void onPostExecute(Output result) {
            // do something on UI thread with the result
        }
    }

    Runnable myRunnable = new Runnable() {
        @Override
        public void run() {
            MyTask myTask = new MyTask();
            myTask.execute(myArg);
            handler.postDelayed(netRunnable, 60000); // schedule next call
        }
    };
  */
