package softkom.com.classes;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.inex.mobilefacenet.mtcnn.Box;

import org.tensorflow.lite.examples.detection.tflite.Classifier;

import java.util.Vector;

public class FaceDetectionClassifier {
    public Bitmap sourceBitmap;
    public Bitmap faceBitmap;
    public Rect faceRect;
    public boolean faceDetected = false;
    public Vector<Box> boxes1;
    public Classifier.Recognition classifierRecognition;
}
