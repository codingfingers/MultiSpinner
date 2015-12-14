/*
 * Copyright (c) 2015. Thomas Haertel
 *
 * Licensed under MIT License (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The software is provided "AS IS", Without warranty of any kind, express or
 * implied, including but not limited to the warranties of merchantability,
 * fitness for a particular purpose and noninfringement, in no event shall the
 * authors or copyright holders be liable for any claim, damages or other
 * liability, whether in an action of contract, tort or otherwise, arising from,
 * out of or in connection with the software or the use or other dealings in
 * the software.
 */
package com.thomashaertel.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class MultiSpinner extends TextView implements OnMultiChoiceClickListener {
    private static final int SPINNER_SINGLE_MODE = 1;
    private static final int SPINNER_MULTI_MODE = 2;

    private SpinnerAdapter mAdapter;
    private boolean[] mOldSelection;
    private boolean[] mSelected;
    private String mTitle;
    private boolean mAllSelected;
    private MultiSpinnerListener mListener;
    private int mTheme;
    private int mMode;
    private Paint mPaint;
    private float mUnderlineSize;

    public MultiSpinner(Context context) {
        super(context);
    }

    public MultiSpinner(Context context, AttributeSet attr) {
        super(context, attr);
        getValues(context, attr);
    }

    public MultiSpinner(Context context, AttributeSet attr, int defStyle) {
        super(context, attr, defStyle);
        getValues(context, attr);
    }

    private void getValues(Context context, AttributeSet attrs) {
        TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MultiSpinner, 0, 0);
        try {
            mTitle = array.getString(R.styleable.MultiSpinner_msDialogPrompt);
            mTheme = array.getResourceId(R.styleable.MultiSpinner_msDialogTheme, -1);
            mMode = array.getInteger(R.styleable.MultiSpinner_msSpinnerMode, 1);
            int color = 0;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                color = array.getColor(R.styleable.MultiSpinner_msUnderlineColor, getResources().getColor(android.R.color.black));
            } else {
                color = array.getColor(R.styleable.MultiSpinner_msUnderlineColor, getResources().getColor(android.R.color.black, null));
            }
            if (color != 0) {
                mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                mPaint.setColor(color);
            }
            mUnderlineSize = array.getDimension(R.styleable.MultiSpinner_msUnderlineSize, 0);
            if(mPaint != null) {
                mPaint.setStrokeWidth(mUnderlineSize);
            }
        } finally {
            array.recycle();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mPaint != null) {
            canvas.drawLine(0, getMeasuredHeight(), getMeasuredWidth(), getMeasuredHeight(), mPaint);
        }
        super.onDraw(canvas);
    }

    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        mSelected[which] = isChecked;
    }

    private OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = null;
            if (mTheme != -1) {
                builder = new AlertDialog.Builder(getContext(), mTheme);
            } else {
                builder = new AlertDialog.Builder(getContext());
            }
            if (mTitle != null) {
                builder.setTitle(mTitle);
            }

            String choices[] = new String[mAdapter.getCount()];

            for (int i = 0; i < choices.length; i++) {
                choices[i] = mAdapter.getItem(i).toString();
            }

            for (int i = 0; i < mSelected.length; i++) {
                mOldSelection[i] = mSelected[i];
            }

            if (mMode == SPINNER_MULTI_MODE) {
                builder.setMultiChoiceItems(choices, mSelected, MultiSpinner.this);

                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        for (int i = 0; i < mSelected.length; i++) {
                            mSelected[i] = mOldSelection[i];
                        }

                        dialog.dismiss();
                    }
                });

                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        refreshSpinner();
                        mListener.onItemsSelected(mSelected);
                        dialog.dismiss();
                    }
                });
            }
            if (mMode == SPINNER_SINGLE_MODE) {
                builder.setItems(choices, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSelected = new boolean[mSelected.length];
                        mSelected[which] = true;
                        if (mListener != null) {
                            mListener.onItemsSelected(mSelected);
                        }
                        refreshSpinner();
                    }
                });
            }

            builder.show();
        }
    };

    public SpinnerAdapter getAdapter() {
        return this.mAdapter;
    }

    public int getCount() {
        if (this.mAdapter != null) {
            return this.mAdapter.getCount();
        } else {
            return 0;
        }
    }

    DataSetObserver dataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            // all selected by default
            mSelected = new boolean[mAdapter.getCount()];
            for (int i = 0; i < mSelected.length; i++) {
                if (mOldSelection == null) {
                    mOldSelection = new boolean[mAdapter.getCount()];
                }
                if (mOldSelection.length > i) {
                    mSelected[i] = mOldSelection[i];
                } else {
                    mSelected[i] = mAllSelected;
                }
            }
            mOldSelection = new boolean[mAdapter.getCount()];
        }
    };


    public void setAdapter(SpinnerAdapter adapter, boolean allSelected, MultiSpinnerListener listener) {
        SpinnerAdapter oldAdapter = this.mAdapter;

        setOnClickListener(null);

        this.mAdapter = adapter;
        this.mListener = listener;
        this.mAllSelected = allSelected;

        if (oldAdapter != null) {
            oldAdapter.unregisterDataSetObserver(dataSetObserver);
        }

        if (mAdapter != null) {
            mAdapter.registerDataSetObserver(dataSetObserver);

            // all selected by default
            mOldSelection = new boolean[mAdapter.getCount()];
            mSelected = new boolean[mAdapter.getCount()];
            for (int i = 0; i < mSelected.length; i++) {
                mOldSelection[i] = false;
                mSelected[i] = allSelected;
            }

            setOnClickListener(onClickListener);
        }

    }

    public void setOnItemsSelectedListener(MultiSpinnerListener listener) {
        this.mListener = listener;
    }

    public interface MultiSpinnerListener {
        public void onItemsSelected(boolean[] selected);
    }

    public boolean[] getSelected() {
        return this.mSelected;
    }

    public void setSelected(boolean[] selected) {
        if (this.mSelected.length != selected.length)
            return;

        this.mSelected = selected;

        refreshSpinner();
    }

    private void refreshSpinner() {
        // refresh text on spinner
        StringBuffer spinnerBuffer = new StringBuffer();

        for (int i = 0; i < mAdapter.getCount(); i++) {
            if (mSelected[i]) {
                spinnerBuffer.append(mAdapter.getItem(i).toString());
                spinnerBuffer.append(", ");
            }
        }

        if (spinnerBuffer.length() > 0) {
            if (spinnerBuffer.charAt(spinnerBuffer.length() - 1) == ' ' && spinnerBuffer.charAt(spinnerBuffer.length() - 2) == ',') {
                spinnerBuffer = spinnerBuffer.deleteCharAt(spinnerBuffer.length() - 1);
                spinnerBuffer = spinnerBuffer.deleteCharAt(spinnerBuffer.length() - 1);
            }
            setText(spinnerBuffer);
        } else {
            setText("");
        }
    }

    public String getPrompt() {
        return mTitle;
    }

    public void setPrompt(String mTitle) {
        this.mTitle = mTitle;
    }
}
