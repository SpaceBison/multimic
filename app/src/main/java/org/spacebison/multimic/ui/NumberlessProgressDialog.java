package org.spacebison.multimic.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

/**
 * Created by cmb on 22.12.15.
 */
public class NumberlessProgressDialog extends ProgressDialog {
    public NumberlessProgressDialog(Context context) {
        super(context);
    }

    public NumberlessProgressDialog(Context context, int theme) {
        super(context, theme);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
}
