package softkom.com.classes;
//https://towardsdatascience.com/how-to-detect-mouth-open-for-face-login-84ca834dff3b
//https://medium.com/@luca_anzalone/setting-up-dlib-and-opencv-for-android-3efdbfcf9e7f

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Size;

import softkom.com.classes.FaceDetection;
import softkom.com.classes.MaskDetector;

import com.zwp.mobilefacenet.MainActivity;

import org.tensorflow.lite.examples.detection.tflite.Classifier;

public class ImageFaceAndMaskDetection {

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

    public ImageFaceAndMaskDetection() {

        faceDetection = new FaceDetection(MainActivity.appContext);
        maskDetector = new MaskDetector();
    }


    public boolean DetectFromImage(Bitmap imageToDetect) {
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
}


class AsyncContext {
    int value;
    Bitmap ImageToProcess;
}

class AsyncTaskRunner extends AsyncTask<AsyncContext, AsyncContext, AsyncContext> {
    @Override
    protected void onPreExecute() { //call from UI thread
        //progressDialog.show();
    }

    @Override
    protected AsyncContext doInBackground(AsyncContext... args) {
        AsyncContext asyncContext = null;
        for (Object o : args) {
            asyncContext = (AsyncContext) o;
        }
        //doSomething();


        //publishProgress("Sleeping..."); // Calls onProgressUpdate()
        return asyncContext;
    }

    @Override
    protected void onPostExecute(AsyncContext asyncContext) {
        // execution of result of Long time consuming operation            . progressDialog.dismiss();
        //updateUIWithResult();
    }

    protected void onProgressUpdate(AsyncContext asyncContext) {
        //updateProgressUI();
    }
}


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
