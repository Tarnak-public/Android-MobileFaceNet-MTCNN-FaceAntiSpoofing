package softkom.com.classes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

import com.inex.mobilefacenet.mtcnn.Box;
import com.inex.mobilefacenet.mtcnn.MTCNN;
import com.inex.mobilefacenet.utils.MyUtil;

import java.io.IOException;
import java.util.Vector;

public class FaceDetection {

    private final Context appContext;
    private final boolean cropDetectedFace;
    protected MTCNN mtcnn; // Face Detection

    public FaceDetection(Context applicationContext, boolean cropDetectedFace) {
        appContext = applicationContext;
        this.cropDetectedFace = cropDetectedFace;

        try {
            mtcnn = new com.inex.mobilefacenet.mtcnn.MTCNN(appContext.getAssets());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bitmap DetectFace(Bitmap snapshotFromCameraBitmap) {
        Vector<Box> boxes1;
        Bitmap bitmapCroppedToFace;

        if (snapshotFromCameraBitmap == null)
            return null;

        //        Bitmap bitmapForFaceDetection = snapshotFromCameraBitmap.copy(snapshotFromCameraBitmap.getConfig(), true);
        boxes1 = mtcnn.detectFaces(snapshotFromCameraBitmap, snapshotFromCameraBitmap.getWidth() / 5); // Only this code detects the face, the following is based on the Box to cut out the face in the picture
        if (boxes1.size() == 0) {  //no face
            return null;
        }
        Box box1 = boxes1.get(0);
        box1.toSquareShape();
        box1.limitSquare(snapshotFromCameraBitmap.getWidth(), snapshotFromCameraBitmap.getHeight());
        Rect rect1 = box1.transform2Rect();

        //Cut face
        bitmapCroppedToFace = MyUtil.crop(snapshotFromCameraBitmap, rect1);
        return bitmapCroppedToFace;
    }

    public FaceDetectionClassifier DetectFaceWithClassifier(Bitmap snapshotFromCameraBitmap, boolean cropAllFaces) {
        FaceDetectionClassifier faceDetectionClassifier = new FaceDetectionClassifier();
        faceDetectionClassifier.sourceBitmap = snapshotFromCameraBitmap;
        faceDetectionClassifier.faceBitmap = null;

        if (snapshotFromCameraBitmap == null)
            return faceDetectionClassifier;

        faceDetectionClassifier.boxes1 = mtcnn.detectFaces(snapshotFromCameraBitmap, snapshotFromCameraBitmap.getWidth() / 5); // Only this code detects the face, the following is based on the Box to cut out the face in the picture
        if (faceDetectionClassifier.boxes1.size() == 0)   //no face
            return faceDetectionClassifier;

        if (cropAllFaces) {
            //faceClassifier.boxes1 - contains faces
        }

        Box box1 = faceDetectionClassifier.boxes1.get(0);
        box1.toSquareShape();
        box1.limitSquare(snapshotFromCameraBitmap.getWidth(), snapshotFromCameraBitmap.getHeight());
        faceDetectionClassifier.faceRect = box1.transform2Rect();
        faceDetectionClassifier.faceDetected = true;

        if (cropDetectedFace)
            faceDetectionClassifier.faceBitmap = MyUtil.crop(snapshotFromCameraBitmap, faceDetectionClassifier.faceRect);

//        Log.d("DetectFaceWithClassifier", "DetectFaceWithClassifier source bitmap: " + snapshotFromCameraBitmap.getWidth() +  " x " + snapshotFromCameraBitmap.getHeight() +
//                " , face bitmap " + faceClassifier.faceBitmap.getWidth() + " x " + faceClassifier.faceBitmap.getHeight() + " , " +
//                "face: left eye: " + box1.landmark[0].x + " : " + box1.landmark[0].y
//        );

        return faceDetectionClassifier;
    }
}
