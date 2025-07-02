package com.example.bookelectrone.HelpClass;

import static com.example.bookelectrone.HelpClass.StyleTemplates.animateVisibility;
import static com.example.bookelectrone.HelpClass.StyleTemplates.createAlphaAnimation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.bookelectrone.App;
import com.example.bookelectrone.CollectionActivity;
import com.example.bookelectrone.R;

import java.util.List;

public class HeaderView <T extends UniversalAdapter.DisplayableItem> extends LinearLayout
{
    private TextView headerTitle;
    private EditText searchInput;
    private ImageButton searchButton, cancelButton, filterButton, backButton;

    private List<T> OBJECTS;


    private App.OnShowListListener<T> showListener;
    private App.OnSearchOtherButtonListener otherListener;


    public HeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(List<T> objects, Context context,
                     App.OnShowListListener<T> show, App.OnSearchOtherButtonListener other) {
        inflate(context, R.layout.header_view, this);

        OBJECTS = objects;
        showListener = show;
        otherListener = other;

        headerTitle = findViewById(R.id.title);
        searchInput = findViewById(R.id.search_input);
        searchButton = findViewById(R.id.search_button);
        cancelButton = findViewById(R.id.cross_button);
        filterButton = findViewById(R.id.filter_button);
        backButton = findViewById(R.id.back_button);

        searchButton.setOnClickListener(v -> {
            if (searchInput.getVisibility() == View.GONE) {
                animateVisibility(headerTitle, View.GONE);
                animateVisibility(searchInput, View.VISIBLE);
                animateVisibility(cancelButton, View.VISIBLE);
                animateVisibility(filterButton, View.VISIBLE);
                searchInput.requestFocus();
                searchInput.setEnabled(true);
            } else {
                showListener.onShowSelected(App.performSearch(OBJECTS, searchInput.getText().toString()));
            }
        });

        cancelButton.setOnClickListener(v -> {
            showListener.onShowSelected(OBJECTS);
            searchInput.setText("");
            animateVisibility(headerTitle, View.VISIBLE);
            animateVisibility(searchInput, View.GONE);
            animateVisibility(cancelButton, View.GONE);
            animateVisibility(filterButton, View.GONE);
        });

        filterButton.setOnClickListener(v ->
            App.showFilterDialog(OBJECTS, context, showListener, otherListener)
        );

        StyleTemplates.applyFontToAllTextViews((ViewGroup) this, context);
    }

    public void setTitle(String title) {
        headerTitle.setText(title);
    }

}

