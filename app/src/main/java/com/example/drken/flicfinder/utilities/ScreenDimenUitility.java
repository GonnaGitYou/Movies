package com.example.drken.flicfinder.utilities;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Display;

/**
 * Utility class for determining screen dimensions.  This class will be used to determine device dimensions at
 * runtime so that we can design the application to be responsive.
 * @author Dr. Ken 3/17/2016.
 */
public class ScreenDimenUitility {
    private Activity mActivity;
    private float mHeightDP;
    private float mWidthDP;


    private float mOrientation;

    public ScreenDimenUitility(Activity _activity){
        mActivity = _activity;

        Display display = _activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);

        float screenDensity = _activity.getResources().getDisplayMetrics().density;
        mHeightDP = displayMetrics.heightPixels / screenDensity;
        mWidthDP = displayMetrics.widthPixels / screenDensity;
        mOrientation = display.getRotation();
    }

    public float getmHeightDP() {
        return mHeightDP;
    }

    public float getmWidthDP() {
        return mWidthDP;
    }

    public float getmOrientation() {
        return mOrientation;
    }


}
