package com.droidpro.barcodescanner;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.droidpro.barcodescanner.barcodedetector.BarcodeTracker;
import com.droidpro.barcodescanner.barcodedetector.BarcodeTrackerFactory;
import com.droidpro.barcodescanner.barcodedetector.BoxDetector;
import com.droidpro.barcodescanner.camera.CameraSource;
import com.droidpro.barcodescanner.camera.CameraSourcePreview;
import com.droidpro.barcodescanner.ui.IViewFinder;
import com.droidpro.barcodescanner.ui.IViewListener;
import com.droidpro.barcodescanner.ui.ViewFinderView;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

public class BarcodeScannerActivity extends AppCompatActivity implements BarcodeTracker.BarcodeUpdateListner, IViewListener {
    private static final String TAG = BarcodeScannerActivity.class.getCanonicalName();
    private CameraSourcePreview mPreview;
    private ImageView mBarcodeFrame;
    private TextView mText;
    private CameraSource mCameraSource;
    private static Rect mRectBounds;

    @Override
    public void onViewRectCreated(Rect rect) {
        mRectBounds = rect;
    }

    private static final int CAMERA_REQUEST_CODE = 101;
    private int height=130,width=600;
    private IViewFinder mViewFinderView;
    private Rect mFramingRectInPreview;
    private Boolean mFlashState;
    private boolean mAutofocusState = true;
    private boolean mShouldScaleToFill = true;

    private boolean mIsLaserEnabled = true;
    @ColorInt
    private int mLaserColor;
    @ColorInt private int mBorderColor;
    private int mMaskColor;
    private int mBorderWidth ;
    private int mBorderLength;
    private boolean mRoundedCorner = false;
    private int mCornerRadius = 0;
    private boolean mSquaredFinder = false;
    private float mBorderAlpha = 1.0f;
    private int mViewFinderOffset = 0;
    private float mAspectTolerance = 0.1f;
    private CheckBox mAutoFocus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scanner);
        mLaserColor = this.getResources().getColor(R.color.viewfinder_laser);
        mBorderColor = this.getResources().getColor(R.color.viewfinder_border);
        mMaskColor = this.getResources().getColor(R.color.viewfinder_mask);
        mBorderWidth = this.getResources().getInteger(R.integer.viewfinder_border_width);
        mBorderLength = this.getResources().getInteger(R.integer.viewfinder_border_length);
        mPreview = findViewById(R.id.preview);
        mText = findViewById(R.id.barcodevalue_text);
        //mBarcodeFrame = findViewById(R.id.imgview_barcode_frame);
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermissions();
        }

    }

    private void requestCameraPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                CAMERA_REQUEST_CODE);
    }

    private void createCameraSource() {
        /*
        A barcode detector is created to track barcodes.
        //An associated multi-processor instance is set to receive the barcode detection results, track the barcodes.
        */
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this).build();
        BarcodeTrackerFactory barcodeTrackerFactory = new BarcodeTrackerFactory(this);
        ViewFinderView view = new CustomViewFinderView(this, this);
        view.setViewRectListener(this);
        mPreview.addView(view);
        BoxDetector boxDetector = new BoxDetector(barcodeDetector, width, height, view, mRectBounds);
        boxDetector.setProcessor(new MultiProcessor.Builder<>(barcodeTrackerFactory).build());
        if (!boxDetector.isOperational()) {
            Log.w(TAG, "Dependencies are not available yet");

            /*
            If the device is low on Storage
            */
            IntentFilter storageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, storageFilter) != null;
            if (hasLowStorage) {
                Log.w(TAG, "Low storage");
                Toast.makeText(this, "Low Storage on device", Toast.LENGTH_SHORT).show();
            }

        }
        /*
        Creates and starts the camera
        */
        CameraSource.Builder builder = new CameraSource.Builder(getApplicationContext(), boxDetector).
                setRequestedFps(15.0f).setFacing(CameraSource.CAMERA_FACING_BACK).
                setRequestedPreviewSize(768, 1600);


        // make sure that auto focus is an available option

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder = builder.setFocusMode(
                    true ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);
        }

        mCameraSource = builder
                .setFlashMode(true ? Camera.Parameters.FLASH_MODE_TORCH : null)
                .build();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != CAMERA_REQUEST_CODE) {
            Log.w(TAG, "Request code for camera Permission doesnt match");
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createCameraSource();
                return;
            }
        }
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        };
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(R.string.alertDialog_title).setMessage(R.string.no_camera_permission).
                setPositiveButton("Ok", listener).show();

    }


    @Override
    public void onBarCodeDetected(Barcode barcode) {
        mText.setText(barcode.rawValue);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }



    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {
        if (mCameraSource != null) {
            mPreview.start(mCameraSource);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    protected ViewFinderView createViewFinderView(Context context) {
        ViewFinderView viewFinderView = new ViewFinderView(context);
        viewFinderView.setBorderColor(mBorderColor);
        viewFinderView.setLaserColor(mLaserColor);
        viewFinderView.setLaserEnabled(mIsLaserEnabled);
        viewFinderView.setBorderStrokeWidth(mBorderWidth);
        viewFinderView.setBorderLineLength(mBorderLength);
        viewFinderView.setMaskColor(mMaskColor);

        viewFinderView.setBorderCornerRounded(mRoundedCorner);
        viewFinderView.setBorderCornerRadius(mCornerRadius);
        viewFinderView.setSquareViewFinder(mSquaredFinder);
        viewFinderView.setViewFinderOffset(mViewFinderOffset);
        return viewFinderView;
    }

    private static class CustomViewFinderView extends ViewFinderView {
        public static final String TRADE_MARK_TEXT = "Scan your bar code";
        public static final int TRADE_MARK_TEXT_SIZE_SP = 40;
        public final Paint PAINT = new Paint();

        public CustomViewFinderView(Context context) {
            super(context);
            init();
        }
        public CustomViewFinderView(Context context, IViewListener viewListener) {
            super(context, viewListener);
            init();
        }

        public CustomViewFinderView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        private void init() {
            PAINT.setColor(Color.WHITE);
            PAINT.setAntiAlias(true);
            float textPixelSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                    TRADE_MARK_TEXT_SIZE_SP, getResources().getDisplayMetrics());
            PAINT.setTextSize(textPixelSize);
            setSquareViewFinder(true);
            setLaserEnabled(true);
            //setViewFinderOffset(100);
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            drawTradeMark(canvas);
        }

        private void drawTradeMark(Canvas canvas) {
            Rect framingRect = getFramingRect();
            float tradeMarkTop;
            float tradeMarkLeft;
            if (framingRect != null) {
                tradeMarkTop = framingRect.bottom + PAINT.getTextSize() + 10;
                tradeMarkLeft = framingRect.left;
            } else {
                tradeMarkTop = 10;
                tradeMarkLeft = canvas.getHeight() - PAINT.getTextSize() - 10;
            }
            canvas.drawText(TRADE_MARK_TEXT, tradeMarkLeft, tradeMarkTop, PAINT);
        }
    }
}
