package org.spacebison.multimic;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

/**
 * Created by cmb on 29.10.15.
 */
public class LogView extends TextView {
    private static ForegroundColorSpan redText = new ForegroundColorSpan(Color.RED);
    private static ForegroundColorSpan blueText = new ForegroundColorSpan(Color.BLUE);

    public LogView(Context context) {
        super(context);
    }

    public LogView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LogView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void d(String tag, String msg) {
        Log.d(tag, msg);
        setText(getText().toString() + '\n' + msg);
    }

    public void w(String tag, String msg) {
        Log.w(tag, msg);
        CharSequence prevText = getText();
        SpannableStringBuilder ssb = new SpannableStringBuilder(prevText);
        ssb.append(msg);
        ssb.setSpan(blueText, prevText.length(), prevText.length() + msg.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        setText(ssb);
    }

    public void e(String tag, String msg) {
        Log.w(tag, msg);
        CharSequence prevText = getText();
        SpannableStringBuilder ssb = new SpannableStringBuilder(prevText);
        ssb.append(msg);
        ssb.setSpan(redText, prevText.length(), prevText.length() + msg.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        setText(ssb);
    }
}
