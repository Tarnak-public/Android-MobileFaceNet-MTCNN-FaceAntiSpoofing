package softkom.com.classes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.icu.util.Output;
import android.os.AsyncTask;
import android.os.Handler;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.zwp.mobilefacenet.MainActivity;
import com.zwp.mobilefacenet.R;
import com.zwp.mobilefacenet.mtcnn.Box;
import com.zwp.mobilefacenet.mtcnn.MTCNN;
import com.zwp.mobilefacenet.utils.MyUtil;

import org.tensorflow.lite.examples.detection.tflite.Classifier;

import java.io.IOException;
import java.util.Vector;

public class FaceDetection {
    private String errorString = "";
    private boolean gotError = false;
    private Size size;
    private Classifier.Recognition classifierRecognition = null;
    private MaskDetector maskDetector = null;
    protected MTCNN mtcnn; // Face Detection

    private Bitmap bitmapCroppedFace;
    private Context appContext;


    void FaceDetection(Context applicationContext) {
        //can be used later from thread?
        appContext = applicationContext;


        //if (maskDetector == null)
        maskDetector = new MaskDetector();

        try {
            mtcnn = new MTCNN(appContext.getAssets());
        } catch (IOException e) {
            e.printStackTrace();
            EndFaceDetect();
        }

    }

    //clean up stuff here
    public void EndFaceDetect() {
        maskDetector = null;
        mtcnn = null;
        bitmapCroppedFace = null;

    }


    public Classifier.Recognition RunFaceDetect(Bitmap snapshotFromCameraBitmap) {


        if (snapshotFromCameraBitmap != null) {
            //try actions on this bitmap



            if (maskDetector != null) {

                //find face
                if ((bitmapCroppedFace = findFace(snapshotFromCameraBitmap)) != null) {

                     size = new Size(bitmapCroppedFace.getWidth(), bitmapCroppedFace.getHeight());

                    //for portrait mode:
                    // I/tensorflow: DetectorActivity: Camera orientation relative to screen canvas: 90
                    if ((maskDetector.InitMaskDetector(appContext, appContext.getAssets(), size, 0, 180, 0)) == true) {
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
            //Toast.makeText(MainActivity.appContext, errorString, Toast.LENGTH_LONG).show();
            Log.v("RunFaceDetect()","ERROR: " + errorString);
        }

        return classifierRecognition;
    }


    private Bitmap findFace(Bitmap snapshotFromCameraBitmap) {
        Vector<Box> boxes1 = new Vector<>();
        Bitmap bitmapCroppedToFace;

        //Bitmap bitmapForFaceDetection = snapshotFromCameraBitmap;
        Bitmap bitmapForFaceDetection = snapshotFromCameraBitmap.copy(snapshotFromCameraBitmap.getConfig(), true);

        boxes1 = mtcnn.detectFaces(bitmapForFaceDetection, bitmapForFaceDetection.getWidth() / 5); // Only this code detects the face, the following is based on the Box to cut out the face in the picture

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

}
