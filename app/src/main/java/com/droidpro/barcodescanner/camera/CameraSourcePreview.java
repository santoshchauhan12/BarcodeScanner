package com.droidpro.barcodescanner.camera;


import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.google.android.gms.common.images.Size;

import java.io.IOException;


public class CameraSourcePreview extends ViewGroup {
    private CameraSource mCameraSource;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Context mContext;
    private boolean mStartRequested;
    private boolean mIsSurfaceAvailable;
    private static final String TAG = CameraSourcePreview.class.getName();


    public CameraSourcePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        mStartRequested = false;
        mIsSurfaceAvailable = false;
        mSurfaceView = new SurfaceView(context);
        mSurfaceView.setLayoutParams(new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(new surfaceCallback());
        addView(mSurfaceView);

    }


    public void start(CameraSource cameraSource) {
        if (cameraSource == null) {
            stop();
        }
        this.mCameraSource = cameraSource;
        if (mCameraSource != null) {
            mStartRequested = true;
            try {
                startIfReady();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void startIfReady() throws  IOException,SecurityException {
        if (mIsSurfaceAvailable && mStartRequested){
            mCameraSource.start(mSurfaceHolder);
            mStartRequested=false;

        }

    }

    private void stop() {

    }

    private class surfaceCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mIsSurfaceAvailable = true;
            try {
                startIfReady();
            } catch (SecurityException se) {
                Log.e(TAG, "no camera Permission", se);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = 320;
        int height = 240;
        if (mCameraSource != null) {
            Size size = mCameraSource.getPreviewSize();
            if (size != null) {
                width = size.getWidth();
                height = size.getHeight();
            }
        }

        // Swap width and height sizes when in portrait, since it will be rotated 90 degrees
        if (isPortraitMode()) {
            int tmp = width;
            //noinspection SuspiciousNameCombination
            width = height;
            height = tmp;
        }

        final int layoutWidth = right - left;
        final int layoutHeight = bottom - top;

        // Computes height and width for potentially doing fit width.
        int childWidth = layoutWidth;
        int childHeight = (int)(((float) layoutWidth / (float) width) * height);

        // If height is too tall using fit width, does fit height instead.
        if (childHeight > layoutHeight) {
            childHeight = layoutHeight;
            childWidth = (int)(((float) layoutHeight / (float) height) * width);
        }

        for (int i = 0; i < getChildCount(); ++i) {
            getChildAt(i).layout(0, 0, childWidth, layoutHeight);
        }

        try {
            startIfReady();
        } catch (SecurityException se) {
            Log.e(TAG,"Do not have permission to start the camera", se);
        } catch (IOException e) {
            Log.e(TAG, "Could not start camera source.", e);
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
    }

