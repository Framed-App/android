package dev.truewinter.framed.fonts;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

// https://stackoverflow.com/a/37959881
public class FontAwesomeSolid extends AppCompatTextView {
    public FontAwesomeSolid(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public FontAwesomeSolid(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FontAwesomeSolid(Context context) {
        super(context);
        init();
    }

    private void init() {
        //Font name should not contain "/".
        Typeface tf = Typeface.createFromAsset(getContext().getAssets(),
                "fa-solid-900.ttf");
        setTypeface(tf);
    }
}
