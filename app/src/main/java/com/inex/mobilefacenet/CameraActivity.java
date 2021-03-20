package com.inex.mobilefacenet;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.inex.mobilefacenet.utils.MyUtil;

import java.io.IOException;

/**
 * 自定义相机，不要使用任何光线调节功能，否则活体检测无法通过。
 * 在你的实际项目中，可以手动拍照进行人脸识别，也可以自动取每一帧的图像进行人脸识别。
 */
public class CameraActivity extends AppCompatActivity {
    private static final int IMAGE_FORMAT = ImageFormat.NV21;
    private static final int CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_BACK;//Camera.CameraInfo.CAMERA_FACING_FRONT;
    private static final String LOG_TAG = "CameraPreviewSample";
    private static final String CAMERA_PARAM_ORIENTATION = "orientation";
    private static final String CAMERA_PARAM_LANDSCAPE = "landscape";
    private static final String CAMERA_PARAM_PORTRAIT = "portrait";
    //private SurfaceView mSurfaceView;
    private SurfaceView mSurfaceView;
    private Camera mCamera;
    private Camera.Size mSize;
    private int displayDegree;
    private byte[] mData;
    private boolean isPreviewRunning = false;

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
                // Because the photo is too large, it cannot be passed back using Intent. For simplicity, static variables are used.
                // It is recommended to save it to a file in your own project.
                Bitmap bitmap = MyUtil.convertBitmap(mData, mCamera, displayDegree);
                MainActivity.currentBtn.setImageBitmap(bitmap);
                if (MainActivity.currentBtn.getId() == R.id.image_button1) {
                    MainActivity.bitmap1 = bitmap;
                } else {
                    MainActivity.bitmap2 = bitmap;
                }
                finish();
            }
        });

        //Turn on the camera and put it in surfaceView
        mSurfaceView = findViewById(R.id.surface);
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                openCamera(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                releaseCamera();
            }


        });
    }

    /**
     * 打开相机
     *
     * @param holder SurfaceHolder
     */
    private void openCamera(SurfaceHolder holder) {
        releaseCamera();
        mCamera = Camera.open(CAMERA_ID);//CAMERA_ID
        Camera.Parameters parameters = mCamera.getParameters();
        displayDegree = MyUtil.setCameraDisplayOrientation(MainActivity.modelsWithCameraIssue, CAMERA_ID, mCamera, getWindowManager());

        // 获取合适的分辨率
        mSize = MyUtil.getOptimalSize(parameters.getSupportedPreviewSizes(), mSurfaceView.getWidth(), mSurfaceView.getHeight());
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


}
