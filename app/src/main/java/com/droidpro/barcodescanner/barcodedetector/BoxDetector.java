package com.droidpro.barcodescanner.barcodedetector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.SparseArray;

import com.droidpro.barcodescanner.ui.IViewFinder;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;

import java.io.ByteArrayOutputStream;


public class BoxDetector extends Detector {
    private Detector mDelegate;
    private int mBoxWidth, mBoxHeight;
    private IViewFinder mRectView;
    private Rect mRect;

    public BoxDetector(Detector delegate, int boxWidth, int boxHeight, IViewFinder rectView, Rect rect) {
        mDelegate = delegate;
        mBoxWidth = boxWidth;
        mBoxHeight = boxHeight;
        mRectView = rectView;
        mRect = rect;
    }
    @Override
    public SparseArray detect(Frame frame) {
        int width = frame.getMetadata().getWidth();
        int height = frame.getMetadata().getHeight();

       /* int right = (width / 2) + (mBoxHeight / 2);
        int left = (width / 2) - (mBoxHeight / 2);
        int bottom = (height / 2) + (mBoxWidth / 2);
        int top = (height / 2) - (mBoxWidth / 2);*/
        int right = mRectView.getFramingRect().right  + 110;
        int left = mRectView.getFramingRect().left -110;
        int bottom = mRectView.getFramingRect().bottom -70;
        int top = mRectView.getFramingRect().top +70;


        YuvImage yuvImage = new YuvImage(frame.getGrayscaleImageData().array(), ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
       // yuvImage.compressToJpeg(new Rect(120 ,800, 950, 1350), 100, byteArrayOutputStream);
        yuvImage.compressToJpeg(new Rect(0 , 0, width, height), 100, byteArrayOutputStream);

        byte[] jpegArray = byteArrayOutputStream.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.length);

        Frame croppedFrame =
                new Frame.Builder()
                        .setBitmap(bitmap)
                        .setRotation(frame.getMetadata().getRotation())
                        .build();

        return mDelegate.detect(croppedFrame);
    }
}
