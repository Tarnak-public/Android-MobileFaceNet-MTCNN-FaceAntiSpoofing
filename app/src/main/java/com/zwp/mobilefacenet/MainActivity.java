/*
Z domu:

F:\AndroidSDK\platform-tools\adb.exe connect 192.168.1.11:5555
 */

package com.zwp.mobilefacenet;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.zwp.mobilefacenet.AIZOOTechfacemask.AIZOOTechFaceMask;
import com.zwp.mobilefacenet.faceantispoofing.FaceAntiSpoofing;
import com.zwp.mobilefacenet.mobilefacenet.MobileFaceNet;
import com.zwp.mobilefacenet.mtcnn.Box;
import com.zwp.mobilefacenet.mtcnn.MTCNN;
import com.zwp.mobilefacenet.mtcnn.Utils;
import com.zwp.mobilefacenet.utils.MyUtil;

import softkom.com.classes.MaskDetector;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import static com.zwp.mobilefacenet.utils.PermissionHelper.requestWriteStoragePermission;

import org.tensorflow.lite.examples.detection.tflite.Classifier;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;
import  org.tensorflow.lite.examples.detection.tflite.TestModelFromKerasFMask;

public class MainActivity extends AppCompatActivity {

    // Face Mask
    private static final int TF_OD_API_INPUT_SIZE = 224;

    //https://pytorch.org/docs/stable/quantization.html
    //https://towardsdatascience.com/a-tale-of-model-quantization-in-tf-lite-aebe09f255ca
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "converted_model_piotr.tflite";
    private static final String TF_OD_API_LABELS_FILE = "mask_labelmap.txt";

   // private static final MaskDetector.DetectorMode MODE = MaskDetector.DetectorMode.TF_OD_API;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = false;

    //actual level of threshold
    private static float CONFIDENCE_LEVEL_TRESHOLD = 0.6f;
    //----------------------------------------------
    public static final int IMAGE_FORMAT = ImageFormat.NV21;
    public static final int CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_BACK;//Camera.CameraInfo.CAMERA_FACING_FRONT;
    public static Camera mCamera;

    public static final String TAG = "";

    public static MTCNN mtcnn; // Face Detection
    public FaceAntiSpoofing fas; // Biopsy
    public MobileFaceNet mfn; // Face comparison
    public AIZOOTechFaceMask aFaceMask; // zoo model

    public static Bitmap bitmap1;
    public static Bitmap bitmap2;
    private Bitmap bitmapCrop1;
    private Bitmap bitmapCrop1ForFaceMask;
    private Bitmap bitmapCrop2;

    private ImageButton imageButton1;
    private ImageButton imageButton2;
    private ImageView imageViewCrop1;
    private ImageView imageViewCrop2;
    private TextView resultTextView;
    private TextView resultTextView2;

    public static Context appContext;

//    private ImageButton TakePictureButton1;
//    private ImageButton TakePictureButton2;

    public static List<String> modelsWithCameraIssue = Arrays.asList("GC116C");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appContext = getApplicationContext();

        imageButton1 = findViewById(R.id.image_button1);
        imageButton2 = findViewById(R.id.image_button2);
        imageViewCrop1 = findViewById(R.id.imageview_crop1);
        imageViewCrop2 = findViewById(R.id.imageview_crop2);
        Button cropBtn = findViewById(R.id.crop_btn);
        Button deSpoofingBtn = findViewById(R.id.de_spoofing_btn);
        Button compareBtn = findViewById(R.id.compare_btn);
        Button liveviewBtn = findViewById(R.id.liveview_btn);
        Button facemask_btn = findViewById(R.id.facemask_btn);
        resultTextView = findViewById(R.id.result_text_view);
        resultTextView2 = findViewById(R.id.result_text_view2);


        requestWriteStoragePermission(this);


        try {
            mtcnn = new com.zwp.mobilefacenet.mtcnn.MTCNN(getAssets());
            fas = new FaceAntiSpoofing(getAssets());
            mfn = new MobileFaceNet(getAssets());
            aFaceMask = new AIZOOTechFaceMask(getAssets());


        } catch (IOException e) {
            e.printStackTrace();
        }

        initCamera();
        cropBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                faceCrop();
            }
        });
        deSpoofingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //antiSpoofing();

                Bitmap faceCroppedBmp = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888);
                final Canvas cvFace = new Canvas(faceCroppedBmp);
                final RectF boundingBox = new RectF(0, 0, bitmapCrop1ForFaceMask.getWidth(), bitmapCrop1ForFaceMask.getHeight());
                RectF faceBB = new RectF(boundingBox);

                float sx = ((float) TF_OD_API_INPUT_SIZE) / faceBB.width();
                float sy = ((float) TF_OD_API_INPUT_SIZE) / faceBB.height();
                Matrix matrix = new Matrix();
                matrix.postTranslate(-faceBB.left, -faceBB.top);
                matrix.postScale(sx, sy);
                cvFace.drawBitmap(bitmapCrop1ForFaceMask, matrix, null);


/*
                Classifier detector = null;

                try {
                    detector =
                            TestModelFromKerasFMask.create(
                                    getAssets(),
                                    TF_OD_API_MODEL_FILE,
                                    TF_OD_API_LABELS_FILE,
                                    TF_OD_API_INPUT_SIZE,
                                    TF_OD_API_IS_QUANTIZED);

                    final List<Classifier.Recognition> resultsAux = detector.recognizeImage(bitmapCrop1ForFaceMask);

                } catch (IOException e) {
                    e.printStackTrace();
                }
*/

                try {
                    tflite_digit_recognize.ClassifierMaskDetect classifier = new tflite_digit_recognize.ClassifierMaskDetect(MainActivity.this);

                    //classifier.interpreter.
                    int[] count = classifier.interpreter.getInputTensor(0).shape(); //1,224,224,3
                    int[] count2 = classifier.interpreter.getOutputTensor(0).shape(); //0,1
                    int returned = classifier.classify(faceCroppedBmp); //0 = no mask, 1 = mask

                    Toast.makeText(MainActivity.appContext, "Classifier:[" + returned + "] "+ (returned == 0 ? "No mask" : "mask") + "this.", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
        compareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                faceCompare();
            }
        });
        liveviewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, LiveCameraFaceMaskTextureViewActivity.class));
           }
        });
        facemask_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String errorString = "";
                boolean gotError = false;
                MaskDetector maskDetector;
                Size size;
                if (bitmapCrop1 != null) {
                    //try actions on this bitmap
                    if ((maskDetector = new MaskDetector()) != null) {
                        //original is this:
                        //bitmapCrop1ForFaceMask = bitmapCrop1;

                        //tricked resized image for width and height
                        ((ImageView)findViewById(R.id.imageview_crop2)).setImageBitmap(bitmapCrop1ForFaceMask);

                        size = new Size(bitmapCrop1ForFaceMask.getWidth(), bitmapCrop1ForFaceMask.getHeight());

                        //for portrait mode:
                        // I/tensorflow: DetectorActivity: Camera orientation relative to screen canvas: 90
                        if ((maskDetector.InitMaskDetector(MainActivity.appContext, getAssets(), size, 0, 180, 0)) == true) {
                            maskDetector.processImage(bitmapCrop1ForFaceMask);
                        } else
                            errorString = "InitMaskDetector failed";

                    } else
                        errorString = "init MaskDetector failed somehow";
                } else
                    errorString = "No cropped bitmap in slot 1";


                if (gotError) {
                    Toast.makeText(MainActivity.appContext, errorString, Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    /**
     * Face detection and reduction
     */
    private void faceCrop() {
        if (bitmap1 == null || bitmap2 == null) {
            Toast.makeText(this, "Please take two photos first", Toast.LENGTH_LONG).show();
            return;
        }

        Bitmap bitmapTemp1 = bitmap1.copy(bitmap1.getConfig(), true);
        Bitmap bitmapTemp2 = bitmap2.copy(bitmap1.getConfig(), true);

        //https://towardsdatascience.com/mtcnn-face-detection-cdcb20448ce0
        //landmarks
        // Face data detect
        //https://towardsdatascience.com/face-detection-using-mtcnn-a-guide-for-face-extraction-with-a-focus-on-speed-c6d59f82d49
        //https://towardsdatascience.com/how-to-detect-mouth-open-for-face-login-84ca834dff3b

        //b) Detection of keypoints (left eye, right eye, nose, mouth_left, mouth_right)
        Vector<Box> boxes1 = new Vector<>(), boxes2 = new Vector<>();
        String errorString = "";
        boolean error = false;

        try {
            long start = System.currentTimeMillis();
            boxes1 = mtcnn.detectFaces(bitmapTemp1, bitmapTemp1.getWidth() / 5); // Only this code detects the face, the following is based on the Box to cut out the face in the picture
            long end = System.currentTimeMillis();
            resultTextView.setText("Face detection time-consuming forward propagation:" + (end - start));
            resultTextView2.setText("");
            boxes2 = mtcnn.detectFaces(bitmapTemp2, bitmapTemp2.getWidth() / 5); // Only this code detects faces, the following are all cut out from the picture according to Box

/*
            if(boxes1.isEmpty()  == true) {
                errorString += "No face detected on Image 1 . ";
                error = true;
            }
            if(boxes2.isEmpty()  == true) {
                errorString += "No face detected on Image 2 . ";
                error = true;
            }

            if(error == true)
                Toast.makeText(MainActivity.this, errorString, Toast.LENGTH_LONG).show();
*/

        } catch (IllegalArgumentException e) {
            //e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

        if (boxes1.size() == 0 || boxes2.size() == 0) {
            Toast.makeText(MainActivity.this, "No face detected", Toast.LENGTH_LONG).show();
            return;
        }

        // Because there is only one face in each photo used here, the first value is used to crop the face
        Box box1 = boxes1.get(0);
        Box box2 = boxes2.get(0);
        box1.toSquareShape();
        box2.toSquareShape();
        box1.limitSquare(bitmapTemp1.getWidth(), bitmapTemp1.getHeight());
        box2.limitSquare(bitmapTemp2.getWidth(), bitmapTemp2.getHeight());
        Rect rect1 = box1.transform2Rect();
        Rect rect2 = box2.transform2Rect();
        Rect rect3 = box1.transform2Rect();

        //Cut face
        bitmapCrop1 = MyUtil.crop(bitmapTemp1, rect1);
        bitmapCrop2 = MyUtil.crop(bitmapTemp2, rect2);

        //for better recognition, I've resized box so see more ear and  bottom face part
        //rect3.right = rect3.right + 10;
        //rect3.bottom = rect3.bottom + 10;
        bitmapCrop1ForFaceMask = MyUtil.crop(bitmapTemp1, rect3);

        // Draw face frame and five points
        Utils.drawBox(bitmapTemp1, box1, 10);
        Utils.drawBox(bitmapTemp2, box2, 10);

//        bitmapCrop1 = MyUtil.readFromAssets(this, "42.png");
//        bitmapCrop2 = MyUtil.readFromAssets(this, "52.png");
        imageViewCrop1.setImageBitmap(bitmapTemp1); //bitmapCrop1

        //mageViewCrop1.setVisibility(View.INVISIBLE);
        //((ImageView)findViewById(R.id.ImagCrop1Mask_imageView)).setImageBitmap(bitmapTemp1);
        imageViewCrop2.setImageBitmap(bitmapTemp2); //bitmapCrop2
        //imageViewCrop1.refreshDrawableState();
//        imageViewCrop2.invalidate();
    }

    /**
     * 活体检测
     */
    private void antiSpoofing() {
        if (bitmapCrop1 == null || bitmapCrop2 == null) {
            Toast.makeText(this, "Please detect the face first", Toast.LENGTH_LONG).show();
            return;
        }

        // 活体检测前先判断图片清晰度
        int laplace1 = fas.laplacian(bitmapCrop1);

        String text = "Clarity test results left：" + laplace1;
        if (laplace1 < FaceAntiSpoofing.LAPLACIAN_THRESHOLD) {
            text = text + "，" + "False";
            resultTextView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        } else {
            long start = System.currentTimeMillis();

            // 活体检测
            float score1 = fas.antiSpoofing(bitmapCrop1);

            long end = System.currentTimeMillis();

            text = "Biopsy results left：" + score1;
            if (score1 < FaceAntiSpoofing.THRESHOLD) {
                text = text + "，" + "True";
                resultTextView.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            } else {
                text = text + "，" + "False";
                resultTextView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            }
            text = text + ". time consuming" + (end - start);
        }
        resultTextView.setText(text);

        // 第二张图片活体检测前先判断图片清晰度
        int laplace2 = fas.laplacian(bitmapCrop2);

        String text2 = "Clarity test results left：" + laplace2;
        if (laplace2 < FaceAntiSpoofing.LAPLACIAN_THRESHOLD) {
            text2 = text2 + "，" + "False";
            resultTextView2.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        } else {
            // 活体检测
            float score2 = fas.antiSpoofing(bitmapCrop2);
            text2 = "Biopsy results right：" + score2;
            if (score2 < FaceAntiSpoofing.THRESHOLD) {
                text2 = text2 + "，" + "True";
                resultTextView2.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            } else {
                text2 = text2 + "，" + "False";
                resultTextView2.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            }
        }
        resultTextView2.setText(text2);
    }

    /**
     * 人脸比对
     */
    private void faceCompare() {
        if (bitmapCrop1 == null || bitmapCrop2 == null) {
            Toast.makeText(this, "face compare - no images (USE CROP first)", Toast.LENGTH_LONG).show();
            return;
        }

        compareMTCNN();
    }
//https://medium.com/analytics-vidhya/facenet-on-modile-part-3-cc6f6d5752d6

    void compareMTCNN() {
        long start = System.currentTimeMillis();
        float same = mfn.compare(bitmapCrop1, bitmapCrop2); //
        long end = System.currentTimeMillis();

        String shortedFloat = String.format("%.03f", same);
        int percentageSame = (int) ((same) * 100);

        Log.d("faceCompare()", "Face comparison result: [" + shortedFloat + "] [" + percentageSame + "%]");
        String text = "Face comparison result is:[" + percentageSame + "%] for threshold:[" + MobileFaceNet.THRESHOLD + "] is matched:";
        if (same > MobileFaceNet.THRESHOLD) {
            text = text + "[True]";
            resultTextView.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            text = text + "[False]";
            resultTextView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        }
        text = text + "，time elapsed" + (end - start);
        resultTextView.setText(text);
        resultTextView2.setText("");
    }

    /*********************************** 以下是相机部分 ***********************************/
    public static ImageButton currentBtn;

    private void initCamera() {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentBtn = (ImageButton) v;
                //startActivity(new Intent(MainActivity.this, CameraActivity.class));
                startActivity(new Intent(MainActivity.this, LiveCameraTextureViewActivity.class));
            }
        };

        imageButton1.setOnClickListener(listener);
        imageButton2.setOnClickListener(listener);

    }
}
