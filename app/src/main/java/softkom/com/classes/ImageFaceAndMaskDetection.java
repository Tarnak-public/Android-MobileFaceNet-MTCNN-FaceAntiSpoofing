package softkom.com.classes;
//https://towardsdatascience.com/how-to-detect-mouth-open-for-face-login-84ca834dff3b
//https://medium.com/@luca_anzalone/setting-up-dlib-and-opencv-for-android-3efdbfcf9e7f

import android.graphics.Bitmap;
import android.os.AsyncTask;

public class ImageFaceAndMaskDetection {

    // private static MTCNN mtcnn;

    MaskDetector maskDetector = null;
    FaceDetection faceDetection = null;

    public ImageFaceAndMaskDetection() {

        new
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
    protected AsyncContext doInBackground(AsyncContext...args) {
        AsyncContext asyncContext = null;
        for(Object o : args) {
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
