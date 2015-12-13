package org.spacebison.multimic.ui.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import org.spacebison.multimic.R;
import org.spacebison.multimic.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cmb on 11.12.15.
 */
public class TrackView extends View {
    private List<Float> mSamples = new ArrayList<>();
    private float mVerticalZoom = 1.0f;
    private float mHorizontalZoom  = 0.01f;
    private int mPlotColor;
    private Paint mPlotPaint = new Paint();

    public TrackView(Context context) {
        super(context);
        mPlotColor = Util.getThemeColor(context, R.attr.colorAccent);
        mPlotPaint.setColor(mPlotColor);
        mPlotPaint.setStrokeWidth(1);
    }

    public TrackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPlotColor = attrs.getAttributeUnsignedIntValue(R.attr.colorAccent, Util.getThemeColor(context, R.attr.colorAccent));
        mPlotPaint.setColor(mPlotColor);
        mPlotPaint.setStrokeWidth(1);
    }

    public TrackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPlotColor = attrs.getAttributeUnsignedIntValue(R.attr.colorAccent, Util.getThemeColor(context, R.attr.colorAccent));
        mPlotPaint.setColor(mPlotColor);
        mPlotPaint.setStrokeWidth(1);
    }

    public void setSamples(List<Float> samples) {
        mSamples = samples;
    }

    public void setVerticalZoom(float verticalZoom) {
        mVerticalZoom = verticalZoom;
    }

    public void setHorizontalZoom(float horizontalZoom) {
        mHorizontalZoom = horizontalZoom;
    }

    public void setPlotColor(int plotColor) {
        mPlotColor = plotColor;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int sampleSkip = (int) (1 / mHorizontalZoom);
        int width = getMeasuredWidth();
        int wavePixels = Math.min(mSamples.size() / sampleSkip, width);
        int height = getMeasuredHeight();
        int middle = height / 2;

        for (int i = 0; i < wavePixels; i++) {
            float val = Math.min(mSamples.get(i * sampleSkip) * mVerticalZoom, height);
            canvas.drawLine(i, middle - val / 2, i, middle + val / 2, mPlotPaint);
        }

        if (wavePixels < width) {
            canvas.drawLine(wavePixels, middle, width, middle, mPlotPaint);
        }
    }
}
