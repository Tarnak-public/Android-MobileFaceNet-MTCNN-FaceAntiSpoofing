package tflite_digit_recognize;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import androidx.appcompat.widget.ThemedSpinnerAdapter;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

/**
 * This image classifier classifies each drawing as one of the 10 digits - 2 digits!!
 *
 * https://medium.com/@margaretmz/e2e-tfkeras-tflite-android-273acde6588
 */
public class ClassifierMaskDetect {

    private static final String LOG_TAG = ClassifierMaskDetect.class.getSimpleName();

    // Name of the model file (under assets folder)
    private static final String MODEL_PATH = "converted_model_piotr.tflite";

    // TensorFlow Lite interpreter for running inference with the tflite model
    public final Interpreter interpreter;

    /* Input */
    // A ByteBuffer to hold image data for input to model
    private final ByteBuffer inputImage;
    // Input size
    private static final int DIM_BATCH_SIZE = 1;    // batch size
    public static final int DIM_IMG_SIZE_X = 224;   // height
    public static final int DIM_IMG_SIZE_Y = 224;   // width
    private static final int DIM_PIXEL_SIZE = 3;    // 1 for gray scale & 3 for color images

    private final int[] imagePixels = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];



    /* Output*/
    // Output size is 10 (number of digits)
    private static final int DIGITS = 2; //org was 10

    // Output array [batch_size, number of digits]
    // 10 floats, each corresponds to the probability of each digit
    private float[][] outputArray = new float[DIM_BATCH_SIZE][DIGITS];

    public ClassifierMaskDetect(Activity activity) throws IOException {
        interpreter = new Interpreter(loadModelFile(activity));
        inputImage =
                ByteBuffer.allocateDirect(4
                        * DIM_BATCH_SIZE
                        * DIM_IMG_SIZE_X
                        * DIM_IMG_SIZE_Y
                        * DIM_PIXEL_SIZE);
        inputImage.order(ByteOrder.nativeOrder());
    }

    // Memory-map the model file in Assets
    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    /**
     * To classify an image, follow these steps:
     * 1. pre-process the input image
     * 2. run inference with the model
     * 3. post-process the output result for display in UI
     *
     * @param bitmap
     * @return the digit with the highest probability
     */
    public int classify(Bitmap bitmap) {
        preprocess(bitmap);
        runInference();
        return postprocess();
    }

    /**
     * Preprocess the bitmap by converting it to ByteBuffer & grayscale
     *
     * @param bitmap
     */
    private void preprocess(Bitmap bitmap) {
        convertBitmapToByteBuffer(bitmap);
        //BmpToGrayscaleByteBuffer(bitmap);
        //BmpToByteBuffer(bitmap);
    }

    private Bitmap BmpToByteBuffer(Bitmap bmpOriginal) {
        bmpOriginal.copyPixelsToBuffer(inputImage);
        return bmpOriginal;
    }
/*
    private float[][][][] bitmapToInputArray(Bitmap bitmap) {
        // [START mlkit_bitmap_input]
        //Bitmap bitmap= getYourInputImage();
        int batchNum = 0;
        float[][][][] input = new float[1][112][112][3];
        for (int x = 0; x < 112; x++) {
            for (int y = 0; y < 112; y++) {
                int pixel = bitmap.getPixel(x, y);
                // Normalize channel values to [-1.0, 1.0]. This requirement varies by
                // model. For example, some models might require values to be normalized
                // to the range [0.0, 1.0] instead.
                input[batchNum][x][y][0] = (Color.red(pixel))/ 255.0f;
                input[batchNum][x][y][1] = (Color.green(pixel)) / 255.0f;
                input[batchNum][x][y][2] = (Color.blue(pixel))/ 255.0f;
                //Log.i("Input","input"+input[batchNum][x][y][0]);
                //Log.i("input","input"+input[batchNum][x][y][1]);

            }
        }
        // [END mlkit_bitmap_input]

        return input;
    }


    public byte[] convertBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = null;
        try {
            stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);

            return stream.toByteArray();
        }finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    Log.e(ThemedSpinnerAdapter.Helper.class.getSimpleName(), "ByteArrayOutputStream was not closed");
                }
            }
        }
    }

    private Bitmap getYourInputImage(Bitmap bitmap) {
        // This method is just for show
        //BitmapDrawable drawable = (BitmapDrawable) image2.getDrawable();
        //Bitmap bitmap = drawable.getBitmap();
        Bitmap bitmapp=Bitmap.createScaledBitmap(bitmap,112,112,true);
        Bitmap bitmap2= bitmapp.copy(Bitmap.Config.ARGB_8888, true);
        return bitmap2;
    }

*/
 /*
    byte[] bytes=convertBitmapToByteArray(bitmap1);
    Log.i("byte",""+ Arrays.toString(bytes));
    float[][][][] inp = new float[1][112][112][3];
    inp=bitmapToInputArray();
    Log.i("byte2",""+Arrays.toString(inp[0]));
*/

    private Bitmap BmpToGrayscaleByteBuffer(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);

        //return bmpGrayscale;
        inputImage.rewind();
        bmpGrayscale.copyPixelsToBuffer(inputImage);
/*
        convertBitmapToByteBuffer(bmpOriginal);
        inputImage.rewind();
        bmpGrayscale.copyPixelsFromBuffer(inputImage);
*/
        return bmpGrayscale;
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (inputImage == null) {
            return;
        }
        inputImage.rewind();

        bitmap.getPixels(imagePixels, 0, bitmap.getWidth(), 0, 0,
                bitmap.getWidth(), bitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = imagePixels[pixel++];
                inputImage.putFloat(convertToGreyScale(val));
            }
        }
    }

    private float convertToGreyScale(int color) {
        float r = ((color >> 16) & 0xFF);
        float g = ((color >> 8) & 0xFF);
        float b = ((color) & 0xFF);

        int grayscaleValue = (int) (0.299f * r + 0.587f * g + 0.114f * b);
        float preprocessedValue = grayscaleValue / 255.0f; // normalize the value by dividing by 255.0f
        return preprocessedValue;
    }

    /**
     * Run inference with the classifier model
     * Input is image
     * Output is an array of probabilities
     */
    private void runInference() {
        interpreter.run(inputImage, outputArray);
    }

    /**
     * Figure out the prediction of digit by finding the index with the highest probability
     *
     * @return
     */
    private int postprocess() {
        // Index with highest probability
        int maxIndex = -1;
        float maxProb = 0.0f;
        for (int i = 0; i < outputArray[0].length; i++) {
            if (outputArray[0][i] > maxProb) {
                maxProb = outputArray[0][i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

}
