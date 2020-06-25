package com.zwp.mobilefacenet.AIZOOTechfacemask;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import com.zwp.mobilefacenet.MyUtil;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;

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
 * <p>
 * python pytorch_infer.py  --img-path /path/to/your/img
 * python tensorflow_infer.py  --img-path /path/to/your/img
 * <p>
 * import numpy as np
 * import tensorflow as tf
 * <p>
 * # Load TFLite model and allocate tensors.
 * interpreter = tf.lite.Interpreter(model_path="converted_model.tflite")
 * interpreter.allocate_tensors()
 * <p>
 * # Get input and output tensors.
 * input_details = interpreter.get_input_details()
 * output_details = interpreter.get_output_details()
 * <p>
 * # Test model on random input data.
 * input_shape = input_details[0]['shape']
 * input_data = np.array(np.random.random_sample(input_shape), dtype=np.float32)
 * interpreter.set_tensor(input_details[0]['index'], input_data)
 * <p>
 * interpreter.invoke()
 * <p>
 * # The function `get_tensor()` returns a copy of the tensor data.
 * # Use `tensor()` in order to get a pointer to the tensor.
 * output_data = interpreter.get_tensor(output_details[0]['index'])
 * print(output_data
 */

/**
 * import numpy as np
 * import tensorflow as tf
 * import cv2
 * <p>
 * # import calass and function from TF object detection API
 * from object_detector_detection_api import ObjectDetectorDetectionAPI, \
 * PATH_TO_LABELS, NUM_CLASSES
 * <p>
 * <p>
 * class ObjectDetectorLite(ObjectDetectorDetectionAPI):
 * def __init__(self, model_path='detect.tflite'):
 * """
 * Builds Tensorflow graph, load model and labels
 * """
 * <p>
 * # Load lebel_map
 * self._load_label(PATH_TO_LABELS, NUM_CLASSES, use_disp_name=True)
 * <p>
 * # Define lite graph and Load Tensorflow Lite model into memory
 * self.interpreter = tf.contrib.lite.Interpreter(
 * model_path=model_path)
 * self.interpreter.allocate_tensors()
 * self.input_details = self.interpreter.get_input_details()
 * self.output_details = self.interpreter.get_output_details()
 * <p>
 * def detect(self, image, threshold=0.1):
 * """
 * Predicts person in frame with threshold level of confidence
 * Returns list with top-left, bottom-right coordinates and list with labels, confidence in %
 * """
 * <p>
 * # Resize and normalize image for network input
 * frame = cv2.resize(image, (300, 300))
 * frame = np.expand_dims(frame, axis=0)
 * frame = (2.0 / 255.0) * frame - 1.0
 * frame = frame.astype('float32')
 * <p>
 * # run model
 * self.interpreter.set_tensor(self.input_details[0]['index'], frame)
 * self.interpreter.invoke()
 * <p>
 * # get results
 * boxes = self.interpreter.get_tensor(
 * self.output_details[0]['index'])
 * classes = self.interpreter.get_tensor(
 * self.output_details[1]['index'])
 * scores = self.interpreter.get_tensor(
 * self.output_details[2]['index'])
 * num = self.interpreter.get_tensor(
 * self.output_details[3]['index'])
 * <p>
 * # Find detected boxes coordinates
 * return self._boxes_coordinates(image,
 * np.squeeze(boxes[0]),
 * np.squeeze(classes[0]+1).astype(np.int32),
 * np.squeeze(scores[0]),
 * min_score_thresh=threshold)
 */
/*
example for java

https://github.com/tensorflow/examples/blob/master/lite/examples/image_classification/android/EXPLORE_THE_CODE.md
 */


/*
code for this model
https://github.com/Tarnak-public/FaceMaskDetection
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

        for (int i = 0; i < interpreter.getInputTensorCount(); i++) {
            Tensor inputTensor = interpreter.getInputTensor(i);

        }
        for (int i = 0; i < interpreter.getOutputTensorCount(); i++) {
            Tensor outputTensor = interpreter.getOutputTensor(i);

        }

    }

}
