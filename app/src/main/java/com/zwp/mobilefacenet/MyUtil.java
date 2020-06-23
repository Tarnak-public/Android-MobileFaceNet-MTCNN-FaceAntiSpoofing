package com.zwp.mobilefacenet;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.zwp.mobilefacenet.mtcnn.Box;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class MyUtil {

    long start = System.currentTimeMillis();
    long end = System.currentTimeMillis();
//    Log.d(TAG, "textureView.getBitmap() time elapsed " + (end - start) + "ms");


    /**
     * 从assets中读取图片
     * @param context
     * @param filename
     * @return
     */
    public static Bitmap readFromAssets(Context context, String filename){
        Bitmap bitmap;
        AssetManager asm = context.getAssets();
        try {
            InputStream is = asm.open(filename);
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    /**
     * 给rect增加margin
     * @param bitmap
     * @param rect
     * @param marginX
     * @param marginY
     */
    public static void rectExtend(Bitmap bitmap, Rect rect, int marginX, int marginY) {
        rect.left = max(0, rect.left - marginX / 2);
        rect.right = min(bitmap.getWidth() - 1, rect.right + marginX / 2);
        rect.top = max(0, rect.top - marginY / 2);
        rect.bottom = min(bitmap.getHeight() - 1, rect.bottom + marginY / 2);
    }

    /**
     * 给rect增加margin
     * 使用长度不变，宽度增加到和长度一样
     * @param bitmap
     * @param rect
     */
    public static void rectExtend(Bitmap bitmap, Rect rect) {
        int width = rect.right - rect.left;
        int height = rect.bottom - rect.top;
        int margin = (height - width) / 2;
        rect.left = max(0, rect.left - margin);
        rect.right = min(bitmap.getWidth() - 1, rect.right + margin);
    }

    /**
     * 加载模型文件
     * @param assetManager
     * @param modelPath
     * @return
     * @throws IOException
     */
    public static MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * 归一化图片到[-1, 1]
     * @param bitmap
     * @return
     */
    public static float[][][] normalizeImage(Bitmap bitmap) {
        int h = bitmap.getHeight();
        int w = bitmap.getWidth();
        float[][][] floatValues = new float[h][w][3];

        float imageMean = 127.5f;
        float imageStd = 128;

        int[] pixels = new int[h * w];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, w, h);
        for (int i = 0; i < h; i++) { // 注意是先高后宽
            for (int j = 0; j < w; j++) {
                final int val = pixels[i * w + j];
                float r = (((val >> 16) & 0xFF) - imageMean) / imageStd;
                float g = (((val >> 8) & 0xFF) - imageMean) / imageStd;
                float b = ((val & 0xFF) - imageMean) / imageStd;
                float[] arr = {r, g, b};
                floatValues[i][j] = arr;
            }
        }
        return floatValues;
    }

    /**
     * 缩放图片
     * @param bitmap
     * @param scale
     * @return
     */
    public static Bitmap bitmapResize(Bitmap bitmap, float scale) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        return Bitmap.createBitmap(
                bitmap, 0, 0, width, height, matrix, true);
    }

    /**
     * 图片矩阵宽高转置
     * @param in
     * @return
     */
    public static float[][][] transposeImage(float[][][] in) {
        int h = in.length;
        int w = in[0].length;
        int channel = in[0][0].length;
        float[][][] out = new float[w][h][channel];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                out[j][i] = in[i][j] ;
            }
        }
        return out;
    }

    /**
     * 4维图片batch矩阵宽高转置
     * @param in
     * @return
     */
    public static float[][][][] transposeBatch(float[][][][] in) {
        int batch = in.length;
        int h = in[0].length;
        int w = in[0][0].length;
        int channel = in[0][0][0].length;
        float[][][][] out = new float[batch][w][h][channel];
        for (int i = 0; i < batch; i++) {
            for (int j = 0; j < h; j++) {
                for (int k = 0; k < w; k++) {
                    out[i][k][j] = in[i][j][k] ;
                }
            }
        }
        return out;
    }

    /**
     * 截取box中指定的矩形框(越界要处理)，并resize到size*size大小，返回数据存放到data中。
     * @param bitmap
     * @param box
     * @param size
     * return
     */
    public static float[][][] cropAndResize(Bitmap bitmap, Box box, int size) {
        // crop and resize
        Matrix matrix = new Matrix();
        float scaleW = 1.0f * size / box.width();
        float scaleH = 1.0f * size / box.height();
        matrix.postScale(scaleW, scaleH);
        Rect rect = box.transform2Rect();
        Bitmap croped = Bitmap.createBitmap(
                bitmap, rect.left, rect.top, box.width(), box.height(), matrix, true);

        return normalizeImage(croped);
    }

    /**
     * 按照rect的大小裁剪出人脸
     * @param bitmap
     * @param rect
     * @return
     */
    public static Bitmap crop(Bitmap bitmap, Rect rect) {
        Bitmap cropped = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
        return cropped;
    }

    /**
     * l2范数归一化
     * @param embeddings
     * @param epsilon 惩罚项
     * @return
     */
    public static void l2Normalize(float[][] embeddings, double epsilon) {
        for (int i = 0; i < embeddings.length; i++) {
            float squareSum = 0;
            for (int j = 0; j < embeddings[i].length; j++) {
                squareSum += Math.pow(embeddings[i][j], 2);
            }
            float xInvNorm = (float) Math.sqrt(Math.max(squareSum, epsilon));
            for (int j = 0; j < embeddings[i].length; j++) {
                embeddings[i][j] = embeddings[i][j] / xInvNorm;
            }
        }
    }

    /**
     * 图片转为灰度图
     * @param bitmap
     * @return 灰度图数据
     */
    public static int[][] convertGreyImg(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pixels = new int[h * w];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);

        int[][] result = new int[h][w];
        int alpha = 0xFF << 24;
        for(int i = 0; i < h; i++)	{
            for(int j = 0; j < w; j++) {
                int val = pixels[w * i + j];

                int red = ((val >> 16) & 0xFF);
                int green = ((val >> 8) & 0xFF);
                int blue = (val & 0xFF);

                int grey = (int)((float) red * 0.3 + (float)green * 0.59 + (float)blue * 0.11);
                grey = alpha | (grey << 16) | (grey << 8) | grey;
                result[i][j] = grey;
            }
        }
        return result;
    }

    public static Bitmap convertBitmap(byte[] data, Camera camera, int displayDegree) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        YuvImage yuvimage = new YuvImage(
                data,
                camera.getParameters().getPreviewFormat(),
                previewSize.width,
                previewSize.height,
                null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, baos);
        byte[] rawImage = baos.toByteArray();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);
        Matrix m = new Matrix();
        // 这里我的手机需要旋转一下图像方向才正确，如果在你们的手机上不正确，自己调节，
        // 正式项目中不能这么写，需要计算方向，计算YuvImage方向太麻烦，我这里没有做。
        //m.setRotate(-displayDegree);
        Log.d("CameraActivity()", "->convertBitmap() displayDegree changed as workaround");
        m.setRotate(displayDegree);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    /**
     * 设置相机显示方向
     * @param cameraId 前或者后摄像头
     * @param camera 相机
     * @return
     */
    public static int setCameraDisplayOrientation(int cameraId, Camera camera, WindowManager windowManager) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        int degrees = 0;

        //rotation = 90;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
//hacks for gfx operations
//https://github.com/google/grafika

        Log.d("android-model: ", Build.MODEL);
        if( MainActivity.modelsWithCameraIssue.contains(Build.MODEL) == true) {
            degrees = 270;
            Log.d("onResume()","For this model workaround for camera rotation was needed.");
        }

        int displayDegree;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            displayDegree = (info.orientation + degrees) % 360;
            displayDegree = (360 - displayDegree) % 360;  // compensate the mirror
        } else {
            displayDegree = (info.orientation - degrees + 360) % 360; //original way
            //displayDegree = (info.orientation + degrees) % 180;
            //displayDegree = (360 - displayDegree) % 360;  // compensate the mirror

        }
        camera.setDisplayOrientation(displayDegree);

        /*
        Matrix matrix = new Matrix();
        matrix.setScale(-1, 1);
        matrix.postTranslate(mSurfaceView.getWidth(), 0);
        */
        //mTextureView.setTransform(matrix);
        //(mSurfaceView).setTransform(matrix);
        //mSurfaceView.setScale(-(float) newWidth / viewWidth, (float) newHeight / viewHeight, viewWidth / 2.f , 0);

        //mSurfaceView.setScaleX(-(float) mSurfaceView.getWidth() / mSurfaceView.getWidth());
        //mSurfaceView.setScaleY(-(float) mSurfaceView.getHeight() / mSurfaceView.getHeight());
        return displayDegree;
    }


    /**
     * 获取合适的分辨率
     * @param sizes Camera SupportedPreviewSizes
     * @param w 显示界面宽
     * @param h 显示界面高
     * @return
     */
    public  static Camera.Size getOptimalSize(@NonNull List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }

}
