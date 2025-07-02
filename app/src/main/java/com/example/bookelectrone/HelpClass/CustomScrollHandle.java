package com.example.bookelectrone.HelpClass;

import static com.example.bookelectrone.HelpClass.StyleTemplates.createAlphaAnimation;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import androidx.core.content.ContextCompat;

import com.example.bookelectrone.R;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;

public class CustomScrollHandle extends DefaultScrollHandle {

    public CustomScrollHandle(Context context) {
        super(context);
    }

    @Override public void setupLayout(PDFView pdfView) {
        textView.setTextColor(Color.BLACK);
        textView.setTextSize(14);

        super.setupLayout(pdfView);

        setBackground(ContextCompat.getDrawable(context, R.drawable.custom_scroll_handle));
        StyleTemplates.applyFontToAllTextViews((ViewGroup) this, context);
    }

    @Override public void show() {
        this.startAnimation(createAlphaAnimation(0f, 1f,
                () -> setVisibility(VISIBLE), 100));
    }

    @Override public void hide() {
        this.startAnimation(createAlphaAnimation(1f, 0f,
                () -> setVisibility(VISIBLE), 300));
    }

}
