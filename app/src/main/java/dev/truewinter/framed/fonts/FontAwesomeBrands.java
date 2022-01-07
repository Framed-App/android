package dev.truewinter.framed.fonts;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

// https://stackoverflow.com/a/37959881
public class FontAwesomeBrands extends AppCompatTextView {
    public FontAwesomeBrands(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public FontAwesomeBrands(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FontAwesomeBrands(Context context) {
        super(context);
        init();
    }

    private void init() {
        //Font name should not contain "/".
        Typeface tf = Typeface.createFromAsset(getContext().getAssets(),
                "fa-brands-400.ttf");
        setTypeface(tf);
    }

}
