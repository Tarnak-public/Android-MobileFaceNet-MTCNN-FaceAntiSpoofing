package com.zwp.mobilefacenet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 自定义相机，不要使用任何光线调节功能，否则活体检测无法通过。
 * 在你的实际项目中，可以手动拍照进行人脸识别，也可以自动取每一帧的图像进行人脸识别。
 */
public class CameraActivity_org extends AppCompatActivity {
    private static final int IMAGE_FORMAT = ImageFormat.NV21;
    private static final int CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_BACK ;//Camera.CameraInfo.CAMERA_FACING_FRONT;

    //private SurfaceView mSurfaceView;
    private SurfaceView mSurfaceView;
    private Camera mCamera;
    private Camera.Size mSize;
    private int displayDegree;

    private byte[] mData;
    private boolean isPreviewRunning = false;

    List<String> modelsWithCameraIssue = Arrays.asList("GC116C");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);

        Button button = findViewById(R.id.take_picture);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 由于照片太大，使用Intent传不回去，为了简单就使用静态变量了，在自己项目里建议先存到文件中
                Bitmap bitmap = convertBitmap(mData, mCamera);
                MainActivity.currentBtn.setImageBitmap(bitmap);
                if (MainActivity.currentBtn.getId() == R.id.image_button1) {
                    MainActivity.bitmap1 = bitmap;
                } else {
                    MainActivity.bitmap2 = bitmap;
                }
                finish();
            }
        });

        // 打开相机并放入surfaceView中
        mSurfaceView = findViewById(R.id.surface);
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                openCamera(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
/*
                if (isPreviewRunning)
                {
                    mCamera.stopPreview();
                }

                Camera.Parameters parameters = mCamera.getParameters();
                Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

                if(display.getRotation() == Surface.ROTATION_0)
                {
                    parameters.setPreviewSize(height, width);
                    mCamera.setDisplayOrientation(90);
                }

                if(display.getRotation() == Surface.ROTATION_90)
                {
                    parameters.setPreviewSize(width, height);
                }

                if(display.getRotation() == Surface.ROTATION_180)
                {
                    parameters.setPreviewSize(height, width);
                }

                if(display.getRotation() == Surface.ROTATION_270)
                {
                    parameters.setPreviewSize(width, height);
                    mCamera.setDisplayOrientation(180);
                }

                mCamera.setParameters(parameters);
                mCamera.startPreview();
                isPreviewRunning = true;
*/
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                releaseCamera();
            }


        });
    }

    /**
     * 打开相机
     * @param holder SurfaceHolder
     */
    private void openCamera(SurfaceHolder holder) {
        releaseCamera();
        mCamera = Camera.open(CAMERA_ID);//CAMERA_ID
        Camera.Parameters parameters = mCamera.getParameters();
        displayDegree = setCameraDisplayOrientation(CAMERA_ID, mCamera);

        // 获取合适的分辨率
        mSize = getOptimalSize(parameters.getSupportedPreviewSizes(), mSurfaceView.getWidth(), mSurfaceView.getHeight());
        parameters.setPreviewSize(mSize.width, mSize.height);

        parameters.setPreviewFormat(IMAGE_FORMAT);
        mCamera.setParameters(parameters);

        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // 相机每一帧图像回调
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                mData = data;
                camera.addCallbackBuffer(data);
            }
        });
        isPreviewRunning = true;
        mCamera.startPreview();
    }

    private synchronized void releaseCamera() {
        if (mCamera != null) {
            try {
                mCamera.setPreviewCallback(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                mCamera.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mCamera = null;
        }
    }

    /**
     * 设置相机显示方向
     * @param cameraId 前或者后摄像头
     * @param camera 相机
     * @return
     */
    private int setCameraDisplayOrientation(int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
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
        if( modelsWithCameraIssue.contains(Build.MODEL) == true) {
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
        mSurfaceView.setScaleY(-(float) mSurfaceView.getHeight() / mSurfaceView.getHeight());
        return displayDegree;
    }

    private static final String LOG_TAG = "CameraPreviewSample";
    private static final String CAMERA_PARAM_ORIENTATION = "orientation";
    private static final String CAMERA_PARAM_LANDSCAPE = "landscape";
    private static final String CAMERA_PARAM_PORTRAIT = "portrait";



    /**
     * 获取合适的分辨率
     * @param sizes Camera SupportedPreviewSizes
     * @param w 显示界面宽
     * @param h 显示界面高
     * @return
     */
    private static Camera.Size getOptimalSize(@NonNull List<Camera.Size> sizes, int w, int h) {
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

    private Bitmap convertBitmap(byte[] data, Camera camera) {
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
}
