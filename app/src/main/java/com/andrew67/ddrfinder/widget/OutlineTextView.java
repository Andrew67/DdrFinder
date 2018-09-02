package com.andrew67.ddrfinder.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.TextView;
import android.support.v7.widget.AppCompatTextView;

import com.andrew67.ddrfinder.R;

import java.lang.reflect.Field;

/** https://stackoverflow.com/questions/39106454/add-opaque-shadow-outline-to-android-textview */
public class OutlineTextView extends AppCompatTextView {
    private Field colorField;
    private int textColor;
    private int outlineColor;

    public OutlineTextView(Context context) {
        this(context, null);
    }

    public OutlineTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public OutlineTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        try {
            //noinspection JavaReflectionMemberAccess
            colorField = TextView.class.getDeclaredField("mCurTextColor");
            colorField.setAccessible(true);

            // If the reflection fails (which really shouldn't happen), we
            // won't need the rest of this stuff, so we keep it in the try-catch

            textColor = getTextColors().getDefaultColor();

            // These can be changed to hard-coded default
            // values if you don't need to use XML attributes

            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OutlineTextView);
            outlineColor = a.getColor(R.styleable.OutlineTextView_outlineColor, Color.TRANSPARENT);
            setOutlineStrokeWidth(a.getDimensionPixelSize(R.styleable.OutlineTextView_outlineWidth, 0));
            a.recycle();
        }
        catch (NoSuchFieldException e) {
            // Optionally catch Exception and remove print after testing
            e.printStackTrace();
            colorField = null;
        }
    }

    /**
     * Adds spaces to the desired text to show outlined, so that the outline does not get cut off.
     * Note: this adds a small amount of left/right padding, so keep that in mind.
     */
    @SuppressLint("SetTextI18n")
    public void setOutlinedText(String s) {
        super.setText(" " + s + " ");
    }

    @Override
    public void setTextColor(int color) {
        // We want to track this ourselves
        // The super call will invalidate()

        textColor = color;
        super.setTextColor(color);
    }

    public void setOutlineColor(int color) {
        outlineColor = color;
        invalidate();
    }

    public void setOutlineWidth(float width) {
        setOutlineStrokeWidth(width);
        invalidate();
    }

    private void setOutlineStrokeWidth(float width) {
        getPaint().setStrokeWidth(2 * width + 1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // If we couldn't get the Field, then we
        // need to skip this, and just draw as usual

        if (colorField != null) {
            // Outline
            setColorField(outlineColor);
            getPaint().setStyle(Paint.Style.STROKE);
            super.onDraw(canvas);

            // Reset for text
            setColorField(textColor);
            getPaint().setStyle(Paint.Style.FILL);
        }

        super.onDraw(canvas);
    }

    private void setColorField(int color) {
        // We did the null check in onDraw()
        try {
            colorField.setInt(this, color);
        }
        catch (IllegalAccessException | IllegalArgumentException e) {
            // Optionally catch Exception and remove print after testing
            e.printStackTrace();
        }
    }

    // Optional saved state stuff

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.textColor = textColor;
        ss.outlineColor = outlineColor;
        ss.outlineWidth = getPaint().getStrokeWidth();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        textColor = ss.textColor;
        outlineColor = ss.outlineColor;
        getPaint().setStrokeWidth(ss.outlineWidth);
    }

    private static class SavedState extends BaseSavedState {
        int textColor;
        int outlineColor;
        float outlineWidth;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            textColor = in.readInt();
            outlineColor = in.readInt();
            outlineWidth = in.readFloat();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(textColor);
            out.writeInt(outlineColor);
            out.writeFloat(outlineWidth);
        }

        public static final Parcelable.Creator<SavedState>
                CREATOR = new Parcelable.Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
