package com.example.bookelectrone.HelpClass;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.preference.PreferenceManager;

import com.example.bookelectrone.R;
import com.example.bookelectrone.db.BookDBManager;
import com.example.bookelectrone.db.BookDBManager.Bookmark;

import java.lang.reflect.Field;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;


public class StyleTemplates {

    /**
     * Устанавливает стиль в диалоговом окне у {@code TextEdit}.
     * @return LinearLayout.LayoutParams (params)
     */
    public static LinearLayout.LayoutParams getTextEditOnDialogStyle() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(64, 32, 64, 32);

        return params;
    }





    @NonNull private static LinearLayout.LayoutParams getColorStyle() {
        return new LinearLayout.LayoutParams(204, 204);
    }
    @NonNull private static LinearLayout.LayoutParams getColorStyleInClick() {
        return new LinearLayout.LayoutParams(224, 224);
    }


    private static void resetSelection(@NonNull LinearLayout l) {
        for (int i = 0; i < l.getChildCount(); i++) {
            View child = l.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View colorView = row.getChildAt(j);
                    if (colorView instanceof ImageView)
                        colorView.setLayoutParams(getColorStyle());
                }
            }
        }
    }


    private static class ColorClickListener implements View.OnClickListener {
        private final int color;
        private final ImageView imageView;
        private final OnColorSelectedListener colorSelectedListener;

        ColorClickListener(int color, ImageView imageView, OnColorSelectedListener c) {
            this.color = color;
            this.imageView = imageView;
            colorSelectedListener = c;
        }

        @Override public void onClick(View v) {
            LinearLayout parent = (LinearLayout) v.getParent().getParent();
            TransitionManager.beginDelayedTransition(parent, new ChangeBounds().setDuration(200));
            resetSelection(parent);
            imageView.setLayoutParams(getColorStyleInClick());
            colorSelectedListener.onColorSelected(color);
        }
    }
    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }


    /**
     * Метод для добавления в диалог view с выбором цвета закладки.
     *
     * @param context Context.
     * @param currentColor Текущий цвет закладки.
     * @param listener Слушатель, который срабатывает при клике на цвет.
     * @return LinearLayout с цветами.
     */
    public static LinearLayout getColorsOnDialog(Context context, int currentColor, OnColorSelectedListener listener)
    {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,196);
        params.setMargins(64, 32, 64, 32);

        LinearLayout layoutColors1 = new LinearLayout(context);
        layoutColors1.setLayoutParams(params);
        layoutColors1.setOrientation(LinearLayout.HORIZONTAL);
        layoutColors1.setGravity(Gravity.TOP | Gravity.CENTER);
        layoutColors1.setClickable(false);
        layoutColors1.setFocusable(false);
        layoutColors1.addView(getImageView(context, Bookmark.RED_COLOR, currentColor, listener));
        layoutColors1.addView(getImageView(context, Bookmark.BASE_COLOR, currentColor, listener));
        layoutColors1.addView(getImageView(context, Bookmark.GREEN_COLOR, currentColor, listener));

        LinearLayout layoutColors2 = new LinearLayout(context);
        layoutColors1.setLayoutParams(params);
        layoutColors2.setOrientation(LinearLayout.HORIZONTAL);
        layoutColors2.setGravity(Gravity.BOTTOM | Gravity.CENTER);
        layoutColors2.setClickable(false);
        layoutColors2.setFocusable(false);
        layoutColors2.addView(getImageView(context, Bookmark.BLUE_COLOR, currentColor, listener));
        layoutColors2.addView(getImageView(context, Bookmark.PURPLE_COLOR, currentColor, listener));
        layoutColors2.addView(getImageView(context, Bookmark.PINK_COLOR, currentColor, listener));

        LinearLayout layout = new LinearLayout(context);
        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 450);
        params1.setMargins(0,0,0,0);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(params1);
        layout.setClickable(false);
        layout.setFocusable(false);
        layout.addView(layoutColors1);
        layout.addView(layoutColors2);

        return layout;
    }

    /**
     * Возвращает view для цветов в диалоге.
     *
     * @return ImageView color.
     */
    private static ImageView getImageView(Context context, int c, int curr, OnColorSelectedListener listener) {
        ImageView color = new ImageView(context);

        if (c == curr) color.setLayoutParams(getColorStyleInClick());
        else color.setLayoutParams(getColorStyle());

        color.setClickable(true);
        color.setFocusable(true);
        color.setOnClickListener(new ColorClickListener(c, color, listener));
        setPointColorInImageView(color, c);

        return color;
    }

    public static void setPointColorInImageView(ImageView i, int c) {
        i.setImageResource(R.drawable.point_icon_24);
        Drawable drawable = i.getDrawable();
        drawable = DrawableCompat.wrap(drawable).mutate();
        DrawableCompat.setTint(drawable, c);
        i.setImageDrawable(drawable);
    }




    /**
     * Создает анимацию перехода с одной прозрачности на другую.
     *
     * @param fromAlpha С какой прозрачности.
     * @param toAlpha В какую прозрачность.
     * @param listener Слушатель.
     * @return
     */
    public static AlphaAnimation createAlphaAnimation(float fromAlpha, float toAlpha, OnEndAnimationListener listener, int duration) {
        AlphaAnimation alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);
        alphaAnimation.setDuration(duration);
        alphaAnimation.setFillAfter(true);

        alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) { }
            @Override public void onAnimationRepeat(Animation animation) { }
            @Override public void onAnimationEnd(Animation animation) {
                if (listener != null) listener.onEnd();
            }
        });

        return alphaAnimation;
    }
    public interface OnEndAnimationListener {
        void onEnd();
    }


    public static void animateVisibility(View view, int visibility) {
        view.startAnimation(createAlphaAnimation(view.getVisibility() == View.GONE ? 0f : 1f, visibility == View.GONE ? 0f : 1f,
                () -> {
                    view.setVisibility(visibility);
                    view.setEnabled(visibility == View.VISIBLE);
                }, 500));
    }



    /**
     * Задает цветовую тему странице, из которой был вызван метод.
     * @param context Context страницы.
     */
    public static void appColorTheme(Context context) {
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
//        Boolean bool = getSharedPreferences("MODE", Context.MODE_PRIVATE);
//        boolean nightMODE = sharedPreferences.getBoolean("night", false);

//        if (nightMODE) {
//            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
//        } else {
//            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
//        }
    }




    /**
     * Изменение шрифта во всем приложении! Методы отвечают за проверку системного шрифта и его сброса.
     * return: {@code Typeface}
     */
    public static void applySavedFont(Context context) {
        Typeface typeface = getFontApp(context);
        if (typeface != null) {
            setDefaultFont("DEFAULT", typeface);
            setDefaultFont("MONOSPACE", typeface);
            setDefaultFont("SERIF", typeface);
            setDefaultFont("SANS_SERIF", typeface);
        }
    }
    private static void setDefaultFont(String staticTypefaceFieldName, Typeface newTypeface) {
        try {
            final Field staticField = Typeface.class.getDeclaredField(staticTypefaceFieldName);
            staticField.setAccessible(true);
            staticField.set(null, newTypeface);
        } catch (Exception e) {}
    }

    private static Typeface getFontApp(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String selectedFont = sharedPreferences.getString("selected_font", "Roboto");
        Typeface typeface = null;

        switch (selectedFont) {
            case "Roboto":
                typeface = ResourcesCompat.getFont(context, R.font.roboto_regular);
                break;
            case "Montserrat":
                typeface = ResourcesCompat.getFont(context, R.font.montserrat_regular);
                break;
            case "Arial":
                typeface = ResourcesCompat.getFont(context, R.font.arial_regular);
                break;
            case "Times New Roman":
                typeface = ResourcesCompat.getFont(context, R.font.times_new_roman_regular);
                break;
        }
        return typeface;
    }

    /**
     * Метод, вызываемый из страниц и их фрагментов для установки в них стиля шрифта.
     * Так же вызывается для отдельных {@code View} и их разновидностей.
     *
     * @param root Группа {@code view} для изменения.
     * @param context
     */
    public static void applyFontToAllTextViews(ViewGroup root, Context context) {
        Typeface typeface = getFontApp(context);
        if (typeface != null) {
            for (int i = 0; i < root.getChildCount(); i++) {
                View view = root.getChildAt(i);
                if (view instanceof ViewGroup) {
                    applyFontToAllTextViews((ViewGroup) view, context);
                } else if (view instanceof TextView) {
                    ((TextView) view).setTypeface(typeface);
                }
            }
        }
    }



}
