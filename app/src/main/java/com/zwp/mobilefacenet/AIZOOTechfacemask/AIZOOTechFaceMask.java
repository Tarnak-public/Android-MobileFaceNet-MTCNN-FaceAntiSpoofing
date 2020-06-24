package com.zwp.mobilefacenet.AIZOOTechfacemask;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import com.zwp.mobilefacenet.MyUtil;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;

/**
 * 人脸比对
 */
public class AIZOOTechFaceMask {
    private static final String MODEL_FILE = "face_mask_detection.tflite";

    public static final int INPUT_IMAGE_SIZE = 112; // The width and height of the placeholder image that needs feed data
    public static final float THRESHOLD = 0.8f; // Set a threshold, greater than this value is considered the same person

    private Interpreter interpreter;

    public AIZOOTechFaceMask(AssetManager assetManager) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        interpreter = new Interpreter(MyUtil.loadModelFile(assetManager, MODEL_FILE), options);
    }


}
