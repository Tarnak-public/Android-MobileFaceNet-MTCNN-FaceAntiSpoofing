package softkom.com.classes;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.TextureView;

import com.inex.mobilefacenet.utils.MyUtil;
import com.inex.mobilefacenet.utils.PermissionHelper;

import java.io.IOException;

public class CameraSurfaceListener implements TextureView.SurfaceTextureListener {
    public static final int IMAGE_FORMAT = ImageFormat.NV21;
    //    public static final int CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_FRONT;//Camera.CameraInfo.CAMERA_FACING_FRONT;
    private final String TAG = "LiveCameraTViewActivity";
    private final TextureView camTextureView;
    private final ImageFaceAndMaskDetection imageFaceAndMaskDetection;
    public Camera mCamera;
    Activity activity;
    int camId;
    private SurfaceTexture mSurfaceTexture;
    private Bitmap bitmap;
    private long start = System.currentTimeMillis();
    private long end = System.currentTimeMillis();
    private long frameSkipperInMS = 300;
    private long frameSkipperLastMS = 0;
    private int displayDegree;
    private Camera.Size mSize;
    private byte[] mData;

    public CameraSurfaceListener(Activity activity, ImageFaceAndMaskDetection imageFaceAndMaskDetection, int camId, int displayDegree, TextureView camTextureView) {
        this.activity = activity;
        this.imageFaceAndMaskDetection = imageFaceAndMaskDetection;
        this.camId = camId;
        this.displayDegree = displayDegree;
//        this.mSurfaceTexture = mSurfaceTexture;
        this.camTextureView = camTextureView;
    }

    private int setCameraDisplayOrientation(Camera camera, int displayDegree) {
        camera.setDisplayOrientation(displayDegree);
        return displayDegree;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mSurfaceTexture = surface;
        if (!PermissionHelper.hasCameraPermission(activity)) {
            PermissionHelper.requestCameraPermission(activity, false);
        } else {
            startPreviewNewSingle(camId);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed() ");
        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mCamera.release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        start = System.currentTimeMillis();
        bitmap = camTextureView.getBitmap();
        end = System.currentTimeMillis();
//        Log.v(TAG, "onSurfaceTextureUpdated() actual ms difference " + (start - frameSkipperLastMS) + " , frame skip threshold:" + frameSkipperInMS);
//        Log.v(TAG, "onSurfaceTextureUpdated() START FACE AND MASK DETECTION ");
        if (imageFaceAndMaskDetection.isProcessingFinished()) {
//            Log.v(TAG, "onSurfaceTextureUpdated() RUNNING FACE AND MASK ");
/*
            //*********** temporary ********
            bitmapTemp1 = MyUtil.readFromAssets(this,"NO_MASK_99_170814.jpg");
            ((ImageView)(findViewById(R.id.FaceImageView))).setImageBitmap(bitmap);
            //*********** temporary ********
*/
            imageFaceAndMaskDetection.DetectFromImage(bitmap);
        } else {
//            Log.v(TAG, "onSurfaceTextureUpdated() STILL BUSY PROCESSING PREVIOUS DETECTION ");
        }
    }

    private void startPreviewNewSingle(int camId) {
        Log.d(TAG, "startPreviewNewSingle() ");
        mCamera = Camera.open(camId);
        Camera.Parameters parameters = mCamera.getParameters();
        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

        }
        setCameraDisplayOrientation(mCamera, displayDegree);

        // Get the right resolution
        mSize = MyUtil.getOptimalSize(parameters.getSupportedPreviewSizes(), camTextureView.getWidth(), camTextureView.getHeight());
//        parameters.setPreviewSize(640, 480);
        //800 600
        parameters.setPreviewSize(mSize.width, mSize.height);
        Log.v(TAG, "startPreviewNewSingle() camid=" + camId + " , " + mSize.width + " x " + mSize.height);

        parameters.setPreviewFormat(IMAGE_FORMAT);
        mCamera.setParameters(parameters);

        // Camera callback for every frame
        mCamera.setPreviewCallback((data, camera) -> {
            mData = data;
            camera.addCallbackBuffer(data);
        });
        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException ioe) {
            // Something bad happened
            Log.e(TAG, "Exception starting preview", ioe);
        }
        mCamera.startPreview();
    }

    /*
   Fix for back camera treated as front and do Mirroring
*/
    public void applyMirroringOnCamera() {
        int width = Resources.getSystem().getDisplayMetrics().widthPixels;
        Matrix matrix = new Matrix();
        matrix.setScale(-1, 1);
        matrix.postTranslate(width, 0);
        camTextureView.setTransform(matrix);
    }
}
