package com.droidpro.barcodescanner.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;

import com.droidpro.barcodescanner.R;
import com.droidpro.barcodescanner.utils.DisplayUtils;

public class ViewFinderView extends AppCompatImageView implements IViewFinder {
    private static final String TAG = "ViewFinderView";

    private Rect mFramingRect;

    private static final float PORTRAIT_WIDTH_RATIO = 7f/8;
    private static final float PORTRAIT_WIDTH_HEIGHT_RATIO = 0.99f;

    private static final float LANDSCAPE_HEIGHT_RATIO = 5f/8;
    private static final float LANDSCAPE_WIDTH_HEIGHT_RATIO = 1.4f;
    private static final int MIN_DIMENSION_DIFF = 400;

    private static final float DEFAULT_SQUARE_DIMENSION_RATIO = 5f / 8;

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private int scannerAlpha;
    private static final int POINT_SIZE = 100;
    private static final long ANIMATION_DELAY = 80l;

    private final int mDefaultLaserColor = getResources().getColor(R.color.viewfinder_laser);
    private final int mDefaultMaskColor = getResources().getColor(R.color.viewfinder_mask);
    private final int mDefaultBorderColor = getResources().getColor(R.color.viewfinder_border);
    private final int mDefaultBorderStrokeWidth = getResources().getInteger(R.integer.viewfinder_border_width);
    private final int mDefaultBorderLineLength = getResources().getInteger(R.integer.viewfinder_border_length);

    protected Paint mLaserPaint;
    protected Paint mFinderMaskPaint;
    protected Paint mBorderPaint;
    protected int mBorderLineLength;
    protected boolean mSquareViewFinder;
    private boolean mIsLaserEnabled;
    private float mBordersAlpha;
    private int mViewFinderOffset = 0;
    private Rect mRectBounds;
    private Context mContext;
    private IViewListener mViewRectListener;

    public ViewFinderView(Context context) {
        super(context);
        mContext  = context;
        init();
    }

    public ViewFinderView(Context context, IViewListener viewListener) {
        super(context);
        mContext  = context;
        mViewRectListener = viewListener;
        init();
    }

    public ViewFinderView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mContext  = context;
        init();
    }

    private void init() {
        //set up laser paint
        mLaserPaint = new Paint();
        mLaserPaint.setColor(mDefaultLaserColor);
        mLaserPaint.setStyle(Paint.Style.FILL);

        //finder mask paint
        mFinderMaskPaint = new Paint();
        mFinderMaskPaint.setColor(mDefaultMaskColor);

        //border paint
        mBorderPaint = new Paint();
        mBorderPaint.setColor(mDefaultBorderColor);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(mDefaultBorderStrokeWidth);
        mBorderPaint.setAntiAlias(true);

        mBorderLineLength = mDefaultBorderLineLength;
    }

    @Override
    public void setLaserColor(int laserColor) {
        mLaserPaint.setColor(laserColor);
    }

    @Override
    public void setMaskColor(int maskColor) {
        mFinderMaskPaint.setColor(maskColor);
    }

    @Override
    public void setBorderColor(int borderColor) {
        mBorderPaint.setColor(borderColor);
    }

    @Override
    public void setBorderStrokeWidth(int borderStrokeWidth) {
        mBorderPaint.setStrokeWidth(borderStrokeWidth);
    }

    @Override
    public void setBorderLineLength(int borderLineLength) {
        mBorderLineLength = borderLineLength;
    }

    @Override
    public void setLaserEnabled(boolean isLaserEnabled) { mIsLaserEnabled = isLaserEnabled; }

    @Override
    public void setBorderCornerRounded(boolean isBorderCornersRounded) {
        if (isBorderCornersRounded) {
            mBorderPaint.setStrokeJoin(Paint.Join.ROUND);
        } else {
            mBorderPaint.setStrokeJoin(Paint.Join.BEVEL);
        }
    }

    @Override
    public void setBorderAlpha(float alpha) {
        int colorAlpha = (int) (255 * alpha);
        mBordersAlpha = alpha;
        mBorderPaint.setAlpha(colorAlpha);
    }

    @Override
    public void setBorderCornerRadius(int borderCornersRadius) {
        mBorderPaint.setPathEffect(new CornerPathEffect(borderCornersRadius));
    }

    @Override
    public void setViewFinderOffset(int offset) {
        mViewFinderOffset = offset;
    }

    // TODO: Need a better way to configure this. Revisit when working on 2.0
    @Override
    public void setSquareViewFinder(boolean set) {
        mSquareViewFinder = set;
    }

    public void setupViewFinder() {
        updateFramingRect();
        invalidate();
    }

    public Rect getFramingRect() {
        return mFramingRect;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if(getFramingRect() == null) {
            return;
        }

        drawViewFinderMask(canvas);
        drawViewFinderBorder(canvas);

        if (mIsLaserEnabled) {
            drawLaser(canvas);
        }
    }

    public Rect getScannerRect() {
        return mRectBounds;
    }

    public void drawViewFinderMask(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        Rect framingRect = getFramingRect();
        int leftMargin = 100;
        int rightMargin = 100;
        int topMargin = 50;
        int bottomMargin = 50;

        if(!isPortraitMode()) {
            leftMargin = 300;
            rightMargin = 300;
            topMargin = 50;
            bottomMargin = 50;
        }
        canvas.drawRect(0, 0, width, framingRect.top +topMargin, mFinderMaskPaint);
        canvas.drawRect(0, framingRect.top +topMargin, framingRect.left - leftMargin, framingRect.bottom -bottomMargin, mFinderMaskPaint);
        canvas.drawRect(framingRect.right + rightMargin, framingRect.top +topMargin, width, framingRect.bottom - bottomMargin, mFinderMaskPaint);
        canvas.drawRect(0, framingRect.bottom - bottomMargin, width, height, mFinderMaskPaint);
        setRecBounds(canvas.getClipBounds());
        mViewRectListener.onViewRectCreated(canvas.getClipBounds());
    }

    public void setViewRectListener(IViewListener viewListener) {
        if(null != viewListener) {
            mViewRectListener = viewListener;
        }
    }

    private boolean isPortraitMode() {
        int orientation = mContext.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return false;
        }
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true;
        }

        Log.d(TAG, "isPortraitMode returning false by default");
        return false;
    }

    public void drawViewFinderBorder(Canvas canvas) {
        Rect framingRect = getFramingRect();
        int leftMargin = 102;
        int rightMargin = 102;
        int topMargin = 48;
        int bottomMargin = 48;
        if(!isPortraitMode()) {
            leftMargin = 302;
            rightMargin = 302;
            topMargin = 48;
            bottomMargin = 48;
        }
        // Top-left corner
        Path path = new Path();
        path.moveTo(framingRect.left - leftMargin, framingRect.top +topMargin + mBorderLineLength);
        path.lineTo(framingRect.left - leftMargin, framingRect.top +topMargin);
        path.lineTo(framingRect.left - leftMargin + mBorderLineLength, framingRect.top + topMargin);
        canvas.drawPath(path, mBorderPaint);

        // Top-right corner
        path.moveTo(framingRect.right + rightMargin, framingRect.top + topMargin + mBorderLineLength);
        path.lineTo(framingRect.right +rightMargin, framingRect.top+topMargin);
        path.lineTo(framingRect.right + rightMargin - mBorderLineLength, framingRect.top + topMargin);
        canvas.drawPath(path, mBorderPaint);

        // Bottom-right corner
        path.moveTo(framingRect.right + rightMargin, framingRect.bottom - bottomMargin - mBorderLineLength);
        path.lineTo(framingRect.right + rightMargin, framingRect.bottom - bottomMargin);
        path.lineTo(framingRect.right + rightMargin - mBorderLineLength, framingRect.bottom - bottomMargin);
        canvas.drawPath(path, mBorderPaint);

        // Bottom-left corner
        path.moveTo(framingRect.left - leftMargin, framingRect.bottom - bottomMargin - mBorderLineLength);
        path.lineTo(framingRect.left - leftMargin, framingRect.bottom - bottomMargin);
        path.lineTo(framingRect.left - leftMargin + mBorderLineLength, framingRect.bottom -bottomMargin );
        canvas.drawPath(path, mBorderPaint);
    }

    public void drawLaser(Canvas canvas) {
        Rect framingRect = getFramingRect();
        int leftMargin = 98;
        int rightMargin = 98;
        if(!isPortraitMode()) {
            leftMargin = 298;
            rightMargin = 298;
        }
        // Draw a red "laser scanner" line through the middle to show decoding is active
        mLaserPaint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
        scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
        int middle = framingRect.height() / 2 + framingRect.top;
        canvas.drawRect(framingRect.left - leftMargin, middle - 1, framingRect.right + rightMargin, middle + 2, mLaserPaint);

        postInvalidateDelayed(ANIMATION_DELAY,
                framingRect.left - POINT_SIZE,
                framingRect.top - POINT_SIZE,
                framingRect.right + POINT_SIZE,
                framingRect.bottom + POINT_SIZE);
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        updateFramingRect();
    }

    public synchronized void updateFramingRect() {
        Point viewResolution = new Point(getWidth(), getHeight());
        int width;
        int height;
        int orientation = DisplayUtils.getScreenOrientation(getContext());

        if(mSquareViewFinder) {
            if(orientation != Configuration.ORIENTATION_PORTRAIT) {
                height = (int) (getHeight() * DEFAULT_SQUARE_DIMENSION_RATIO);
                width = height;
            } else {
                width = (int) (getWidth() * DEFAULT_SQUARE_DIMENSION_RATIO);
                height = width;
            }
        } else {
            if(orientation != Configuration.ORIENTATION_PORTRAIT) {
                height = (int) (getHeight() * LANDSCAPE_HEIGHT_RATIO);
                width = (int) (LANDSCAPE_WIDTH_HEIGHT_RATIO * height);
            } else {
                width = (int) (getWidth() * PORTRAIT_WIDTH_RATIO);
                height = (int) (PORTRAIT_WIDTH_HEIGHT_RATIO * width);
            }
        }

        if(width > getWidth()) {
            width = getWidth() - MIN_DIMENSION_DIFF;
        }

        if(height > getHeight()) {
            height = getHeight() - MIN_DIMENSION_DIFF;
        }

        int leftOffset = (viewResolution.x - width) / 2;
        int topOffset = (viewResolution.y - height) / 2;
        mFramingRect = new Rect(leftOffset + mViewFinderOffset, topOffset + mViewFinderOffset, leftOffset + width - mViewFinderOffset, topOffset + height - mViewFinderOffset);
    }

    public void setRecBounds(Rect recBounds) {
        mRectBounds = recBounds;
    }
}


