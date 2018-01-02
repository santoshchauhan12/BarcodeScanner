package com.droidpro.barcodescanner.barcodedetector;

import android.content.Context;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

// creates Tracker for Barcodes

public class BarcodeTrackerFactory implements MultiProcessor.Factory<Barcode> {
    private Context mContext;

    public BarcodeTrackerFactory(Context context){
        this.mContext=context;
    }
    @Override
    public Tracker<Barcode> create(Barcode barcode) {
        return new BarcodeTracker(mContext);
    }
}
