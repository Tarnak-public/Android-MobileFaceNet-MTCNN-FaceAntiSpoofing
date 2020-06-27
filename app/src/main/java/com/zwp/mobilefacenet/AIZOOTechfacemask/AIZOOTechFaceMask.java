package com.zwp.mobilefacenet.AIZOOTechfacemask;

import android.content.res.AssetManager;

import com.zwp.mobilefacenet.utils.MyUtil;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;

/**
 * https://stackoverflow.com/questions/51742805/ssd-anchors-in-tensorflow-detection-api
 * SSD anchor configurtion is show bellow:
 * multibox layers 	feature map size 	anchor size 	aspect ratio）
 * First 	33x33 	0.04,0.056 	1,0.62,0.42
 * Second 	17x17 	0.08,0.11
 * Third 	9x9 	0.16,0.22 	1,0.62,0.42
 * Forth 	5x5 	0.32,0.45 	1,0.62,0.42
 * Fifth 	3x3 	0.64,0.72 	1,0.62,0.42
 *
 * python pytorch_infer.py  --img-path /path/to/your/img
 * python tensorflow_infer.py  --img-path /path/to/your/img

 import numpy as np
 import tensorflow as tf

 # Load TFLite model and allocate tensors.
 interpreter = tf.lite.Interpreter(model_path="converted_model.tflite")
 interpreter.allocate_tensors()

 # Get input and output tensors.
 input_details = interpreter.get_input_details()
 output_details = interpreter.get_output_details()

 # Test model on random input data.
 input_shape = input_details[0]['shape']
 input_data = np.array(np.random.random_sample(input_shape), dtype=np.float32)
 interpreter.set_tensor(input_details[0]['index'], input_data)

 interpreter.invoke()

 # The function `get_tensor()` returns a copy of the tensor data.
 # Use `tensor()` in order to get a pointer to the tensor.
 output_data = interpreter.get_tensor(output_details[0]['index'])
 print(output_data

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

        interpreter.allocateTensors();
    }


}
