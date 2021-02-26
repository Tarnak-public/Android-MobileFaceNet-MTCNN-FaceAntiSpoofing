package softkom.com.classes;


import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class AutoFitTextureView extends TextureView {
    // implements TextureView.SurfaceTextureListener

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    private int measuredWidth = 0;
    private int measuredHeight = 0;

    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public int getRatioWidth() {
        return mRatioWidth;
    }

    public int getRatioHeight() {
        return mRatioHeight;
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            measuredWidth = width;
            measuredHeight = height;
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                measuredWidth = width;
                measuredHeight = width * mRatioHeight / mRatioWidth;
            } else {
                measuredWidth = height * mRatioWidth / mRatioHeight;
                measuredHeight = height;
            }
            setMeasuredDimension(measuredWidth, measuredHeight);
        }
//        Log.d("DetectFaceWithClassifier", "onMeasure():" + measuredWidth + " x " + measuredHeight);
    }

}
