package com.example.bookelectrone.HelpClass;

import static com.example.bookelectrone.HelpClass.StyleTemplates.setPointColorInImageView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.bookelectrone.R;
import com.example.bookelectrone.db.BookDBManager;

import java.io.File;
import java.util.List;

public class UniversalAdapter<T extends UniversalAdapter.DisplayableItem> extends ArrayAdapter<T> {

    public interface DisplayableItem {
        int getID();
        String getTitle();
        String getImage();
        int getColor();
        String getDescription();
        int getType();
    }

    private final Context context;
    private List<T> items;

    public UniversalAdapter(Context context, List<T> items) {
        super(context, 0, items);
        this.context = context;
        this.items = items;
    }

    @NonNull @SuppressLint("SetTextI18n")
    @Override public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_menu, parent, false);

        ImageView imageView = convertView.findViewById(R.id.collection_icon);
        TextView textView = convertView.findViewById(R.id.collection_title);

        T item = getItem(position);

        if (item.getImage() == null) {
            if (item instanceof BookDBManager.Book)
                imageView.setImageResource(R.drawable.book);
            else if (item instanceof BookDBManager.Bookmark)
                setPointColorInImageView(imageView, item.getColor());
            else if (item instanceof BookDBManager.Collection)
                imageView.setImageResource(R.drawable.collection);
        } else imageView.setImageBitmap(
                new BitmapDrawable(String.valueOf(
                new File(
                new File(context.getFilesDir(), "images"), item.getImage()))).getBitmap());

        if (item instanceof BookDBManager.Bookmark) textView.setText(item.getTitle() +
                " (" + context.getString(R.string.page_label) + ". " + ((BookDBManager.Bookmark) item).PAGE + ")");
        else textView.setText(item.getTitle());

        StyleTemplates.applyFontToAllTextViews((ViewGroup) convertView, context);
        return convertView;
    }
}
