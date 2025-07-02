package com.example.bookelectrone;

import static com.example.bookelectrone.HelpClass.StyleTemplates.animateVisibility;
import static com.example.bookelectrone.HelpClass.StyleTemplates.createAlphaAnimation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.example.bookelectrone.HelpClass.FilesManipulated;
import com.example.bookelectrone.HelpClass.HeaderView;
import com.example.bookelectrone.HelpClass.LocaleHelper;
import com.example.bookelectrone.HelpClass.StyleTemplates;
import com.example.bookelectrone.HelpClass.UniversalAdapter;
import com.example.bookelectrone.db.BookDBManager;

import java.io.File;
import java.util.List;

public class CollectionActivity extends AppCompatActivity {

    public BookDBManager dbManager;
    public LinearLayout linearLayout;

    private Context context;
    List<BookDBManager.Book> books;

    @Override protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
        String selectedLan = prefs.getString("selected_lan", "English");
        Context context = LocaleHelper.updateLocale(newBase, selectedLan);
        super.attachBaseContext(context);
    }

    @SuppressLint("MissingInflatedId")
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_collection);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbManager = new BookDBManager(this);
        linearLayout = findViewById(R.id.layout_content_collection);
        context = this;

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), new OnApplyWindowInsetsListener() {
            @Override public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                params.bottomMargin = insets.getSystemWindowInsetBottom();
                v.setLayoutParams(params);
                return insets.consumeSystemWindowInsets();
            }
        });

        StyleTemplates.appColorTheme(this);
        StyleTemplates.applyFontToAllTextViews(findViewById(R.id.main), this);
    }

    public void showBooksOnDB(List<BookDBManager.Book> BOOKS) {
        linearLayout.removeAllViews();

        if(BOOKS.isEmpty()){
            TextView textView = new TextView(this, null, 0, R.style.LABEL_EMPTY);
            textView.setText(R.string.list_is_empty_label);

            linearLayout.addView(textView);
        } else {
            for (BookDBManager.Book b : BOOKS) {
                App.addCard(b, this, linearLayout,
                        () -> {
                            Intent intent = new Intent(this, BookOpenActivity.class);
                            intent.putExtra("EXTRA_TITLE", b.TITLE);
                            intent.putExtra("EXTRA_PATH", b.PATH);
                            intent.putExtra("EXTRA_ID", String.valueOf(b._ID));
                            intent.putExtra("EXTRA_ID_COLLECTION", String.valueOf(b.COLLECTION_ID));
                            intent.putExtra("EXTRA_ACTIVITY", String.valueOf(this));
                            intent.putExtra("EXTRA_FLAG", R.string.home_label);

                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        },
                        () -> App.showBottomSheetMenu(b, this, linearLayout,
                                (newTitle) -> {
                                    BookDBManager DBManager = new BookDBManager(this);
                                    DBManager.openDB();
                                    DBManager.updateBook(b.getID(), newTitle, null, -1, (s) -> {
                                            DBManager.closeDB();
                                            onResume();
                                    });
                                },
                                () -> {
                                    boolean d = FilesManipulated.deleteFileByPath(b.PATH, "books", context);
                                    if (d) {
                                        BookDBManager DBManager = new BookDBManager(this);
                                        DBManager.openDB();
                                        DBManager.deleteBook(b.getID());
                                        DBManager.closeDB();

                                        if (b.getImage() != null && !b.getImage().isEmpty())
                                            FilesManipulated.deleteFileByPath(b.getImage(), "images", context);
                                        onResume();

                                        Toast.makeText(this, R.string.successfully_label, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(this, R.string.error_label, Toast.LENGTH_SHORT).show();
                                    }
                                }
                        )
                );
            }
        }
        StyleTemplates.applyFontToAllTextViews((ViewGroup) linearLayout, this);
    }


    private <T extends UniversalAdapter.DisplayableItem> void setHeader(View view, String title) {
        HeaderView<T> headerView = view.findViewById(R.id.header_view);
        headerView.init((List<T>) books, context,
                (objects) -> showBooksOnDB((List<BookDBManager.Book>) objects),
                null
        );
        headerView.setTitle(title);

        findViewById(R.id.back_button).setVisibility(View.VISIBLE);
        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(CollectionActivity.this, MainActivity.class);
                intent.putExtra("EXTRA_BTN", "Collections");
                startActivity(intent);
            }
        });

        headerView.startAnimation(createAlphaAnimation(0f, 1f, null, 300));
    }


    @Override protected void onResume() {
        super.onResume();
        dbManager.openDB();

        Intent intent = getIntent();
        String TITLE = intent.getStringExtra("EXTRA_TITLE");

        String ID = intent.getStringExtra("EXTRA_ID_COLLECTION");
        books = dbManager.getBooks(Integer.parseInt(ID));
        showBooksOnDB(books);
        setHeader(findViewById(R.id.main), TITLE);
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        dbManager.closeDB();
    }
    @Override public void recreate() {
        onResume();
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FilesManipulated.PICK_IMAGE_BOOK_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            String imagePath = FilesManipulated.getPathFromUri(data.getData(), this);
            String savedImagePath = FilesManipulated.copyFileToAppDirectory(imagePath, this, "images");

            if (FilesManipulated.PATH != null && FilesManipulated.PATH.isEmpty())
                FilesManipulated.deleteFileByPath(FilesManipulated.PATH, "images", context);

            dbManager.updateBook(FilesManipulated.ID, null,
                    savedImagePath, -1, (success) -> onResume());
        }
        if (requestCode == FilesManipulated.PICK_IMAGE_COLLECTION_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            String imagePath = FilesManipulated.getPathFromUri(data.getData(), this);
            String savedImagePath = FilesManipulated.copyFileToAppDirectory(imagePath, this, "images");

            dbManager.updateCollection(FilesManipulated.ID, null,
                    savedImagePath, (success) -> onResume());
        }
    }


}