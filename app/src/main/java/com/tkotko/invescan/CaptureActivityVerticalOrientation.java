package com.tkotko.invescan;

import android.os.Bundle;

import com.journeyapps.barcodescanner.CaptureActivity;

public class CaptureActivityVerticalOrientation extends CaptureActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_capture_activity_any_orientation);
    }
}
