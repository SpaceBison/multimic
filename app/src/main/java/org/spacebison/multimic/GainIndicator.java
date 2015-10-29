package org.spacebison.multimic;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by cmb on 27.10.15.
 */
public class GainIndicator extends View {
    Bitmap mBitmap;
    float mCurrentSweep = 0;
    float mStepSweep = 1;
    float mClipThreshold = 0.9f;
    float mMinGainSize = 0f;
    float mMaxGainSize = 0.66f;
    float mRadius = 0.66f;
    int mGainColor = Color.RED;

    public GainIndicator(Context context) {
        super(context);
    }

    public GainIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.GainIndicator, 0, 0);
        try {
            mStepSweep = a.getFloat(R.styleable.GainIndicator_stepSweep, 1);
            mClipThreshold = a.getFloat(R.styleable.GainIndicator_clipThreshold, 0.9f);
            mGainColor = a.getColor(R.styleable.GainIndicator_clipColor, Color.RED);
            mMinGainSize = a.getFloat(R.styleable.GainIndicator_minGainSize, 0f);
            mMaxGainSize = a.getFloat(R.styleable.GainIndicator_maxGainSize, 0.66f);
            mRadius = a.getFloat(R.styleable.GainIndicator_radius, 0.66f);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int size = Math.min(canvas.getWidth(), canvas.getHeight());
        super.onDraw(canvas);
    }
}
