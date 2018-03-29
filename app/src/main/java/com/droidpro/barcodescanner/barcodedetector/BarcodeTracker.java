package com.droidpro.barcodescanner.barcodedetector;


import android.content.Context;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

/**
 * Created by lloyddcosta on 27/12/17.
 */

public class BarcodeTracker extends Tracker<Barcode> {
    BarcodeUpdateListner barcodeUpdateListner;
    private Context mContext;

    public BarcodeTracker(Context context) {
        this.mContext = context;
/////check for hockey build
        if (context instanceof BarcodeUpdateListner) {
            this.barcodeUpdateListner = (BarcodeUpdateListner) context;
        } else {
            throw new RuntimeException("Hosting activity must implement BarcodeUpdateListener");
        }
    }

    //gets a callback whenever new barcode is detected.
    @Override
    public void onNewItem(int i, Barcode barcode) {
        barcodeUpdateListner.onBarCodeDetected(barcode);
        super.onNewItem(i, barcode);
    }

    public interface BarcodeUpdateListner {
        void onBarCodeDetected(Barcode barcode);
    }
}
