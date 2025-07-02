package com.example.bookelectrone;

import static com.example.bookelectrone.HelpClass.StyleTemplates.setPointColorInImageView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.example.bookelectrone.HelpClass.LocaleHelper;
import com.example.bookelectrone.db.BookDBManager.Bookmark;
import com.example.bookelectrone.HelpClass.FilesManipulated;
import com.example.bookelectrone.HelpClass.StyleTemplates;
import com.example.bookelectrone.db.BookDBManager;
import com.github.barteksc.pdfviewer.PDFView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class ReportBookActivity extends AppCompatActivity {

    Context context = this;
    int BOOK_ID;
    public BookDBManager dbManager;

    LinearLayout linearLayoutBookmarks;
    ScrollView scrollView;


    @Override protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
        String selectedLan = prefs.getString("selected_lan", "English");
        Context context = LocaleHelper.updateLocale(newBase, selectedLan);
        super.attachBaseContext(context);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report_book);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbManager = new BookDBManager(this);
        scrollView = findViewById(R.id.settings_items);
        linearLayoutBookmarks = findViewById(R.id.bookmarks_layout);


        // delete margin
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), new OnApplyWindowInsetsListener() {
            @NonNull
            @Override public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                params.bottomMargin = insets.getSystemWindowInsetBottom();
                v.setLayoutParams(params);
                return insets.consumeSystemWindowInsets();
            }
        });


        findViewById(R.id.backButton).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(ReportBookActivity.this, MainActivity.class);
                intent.putExtra("EXTRA_BTN", "Settings");
                startActivity(intent);
            }
        });

        findViewById(R.id.button_save).setOnClickListener((View v) -> {
            findViewById(R.id.backButton).callOnClick();
        });


        // intent
        Intent intent = getIntent();
        BOOK_ID = Integer.parseInt(intent.getStringExtra("EXTRA_BOOK"));


        // theme font and color
        StyleTemplates.appColorTheme(this);
        StyleTemplates.applyFontToAllTextViews((ViewGroup) findViewById(R.id.main), this);
    }


    @Override protected void onResume() {
        super.onResume();
        dbManager.openDB();

        try { loadInfo(); }
        catch (IOException e) { nullLabel(); }

        StyleTemplates.applyFontToAllTextViews((ViewGroup) scrollView, this);
        scrollView.startAnimation(StyleTemplates.createAlphaAnimation(0f, 1, null, 500));
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        dbManager.closeDB();
    }


    private void loadInfo() throws IOException {
        BookDBManager.Book book = dbManager.getBook(BOOK_ID);
        BookDBManager.Collection collection = dbManager.getCollection(book.COLLECTION_ID);


        ((TextView) findViewById(R.id.book_name)).setText(book.TITLE);
        ((TextView) findViewById(R.id.path)).setHint(book.PATH);
        if (book.IMAGE == null)
            ((ImageView) findViewById(R.id.image_user)).setImageResource(R.drawable.book);
        else ((ImageView) findViewById(R.id.image_user)).setImageBitmap(
            new BitmapDrawable(String.valueOf(
            new File(
            new File(getFilesDir(), "images"), book.getImage()))).getBitmap());


        if (collection.TITLE != null) {
            ((TextView) findViewById(R.id.collection_title)).setHint(collection.getTitle());
            if (collection.IMAGE == null)
                ((ImageView) findViewById(R.id.image_collection)).setImageResource(R.drawable.collection);
            else
                ((ImageView) findViewById(R.id.image_collection)).setImageURI(Uri.parse(collection.IMAGE));
        } else {
            ((ImageView) findViewById(R.id.image_collection)).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.collection_title)).setHint(R.string.empty_label);
        }


        linearLayoutBookmarks.removeAllViews();
        List<Bookmark> bookmarksList = dbManager.getBookmarks(BOOK_ID);
        if (!bookmarksList.isEmpty()) {
            findViewById(R.id.bookmarks_label).setVisibility(View.GONE);
            for (Bookmark b : bookmarksList) addCard(b, this);
        } else findViewById(R.id.bookmarks_label).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.bookmarks_count)).setHint(String.valueOf(bookmarksList.stream().count()));


        File file = new File(new File(getFilesDir(), "books"), book.PATH);
        ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        PdfRenderer pdfRenderer = new PdfRenderer(fd);
        int pageCount = pdfRenderer.getPageCount();
        PdfRenderer.Page page = pdfRenderer.openPage(0);
        int pageWidth = page.getWidth();
        int pageHeight = page.getHeight();
        page.close(); pdfRenderer.close(); fd.close();
        ((TextView) findViewById(R.id.count_page)).setHint(String.valueOf(pageCount));
        ((TextView) findViewById(R.id.format_page)).setHint(pageWidth + " × " + pageHeight);


        String fileName = (new File(book.PATH)).getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1) fileName = fileName.substring(dotIndex, fileName.length());
        ((TextView) findViewById(R.id.format_document)).setHint(fileName);


    }


    private void nullLabel() {
        TextView textView = new TextView(this, null, 0, R.style.LABEL_EMPTY);
        textView.setText(R.string.failed_to_generate_report_label);

        scrollView.removeAllViews();
        scrollView.addView(textView);
    }


    public void addCard(Bookmark BOOKMARK, Context activity) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        View cardView = inflater.inflate(R.layout.card_book, linearLayoutBookmarks, false);

        ((TextView) cardView.findViewById(R.id.NameBook)).setText(BOOKMARK.TITLE);
        ((TextView) cardView.findViewById(R.id.DescriptionBook)).setText(BOOKMARK.DESCRIPTION);

        ImageView imageView = cardView.findViewById(R.id.ImageBook);
        imageView.setImageBitmap(null);
        setPointColorInImageView(imageView, BOOKMARK.COLOR);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(150, 150);
        params.setMargins(36,16,16,16);
        imageView.setLayoutParams(params);

        cardView.findViewById(R.id.Card_0).setId(BOOKMARK.ID);

        (cardView.findViewById(R.id.point_button)).setVisibility(View.GONE);
        (cardView.findViewById(R.id.point_button)).setEnabled(false);
        (cardView.findViewById(R.id.point_button)).setClickable(false);

        linearLayoutBookmarks.addView(cardView);
    }


}