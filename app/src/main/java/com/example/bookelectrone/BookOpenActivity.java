package com.example.bookelectrone;

import static com.example.bookelectrone.HelpClass.StyleTemplates.createAlphaAnimation;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.example.bookelectrone.HelpClass.LocaleHelper;
import com.example.bookelectrone.HelpClass.UniversalAdapter;
import com.example.bookelectrone.db.BookDBManager.Bookmark;
import com.example.bookelectrone.HelpClass.CustomScrollHandle;
import com.example.bookelectrone.HelpClass.StyleTemplates;
import com.example.bookelectrone.db.BookDBManager;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.barteksc.pdfviewer.PDFView;


public class BookOpenActivity extends AppCompatActivity {

    private BookDBManager.Book BOOK;
    private File FILE_BOOK;

    private List<Bookmark> bookmarks;

    private FrameLayout bookmarkPanel;
    private FrameLayout linearLayout;

    private BookDBManager dbManager;


    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book_open);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbManager = new BookDBManager(this);
        linearLayout = findViewById(R.id.layout_content_book);
        bookmarkPanel = findViewById(R.id.bookmarkPanel);


        // delete margin
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), new OnApplyWindowInsetsListener() {
            @NonNull @Override public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                params.bottomMargin = insets.getSystemWindowInsetBottom();
                v.setLayoutParams(params);
                return insets.consumeSystemWindowInsets();
            }
        });


        Intent intent = getIntent();
        String collectionId = intent.getStringExtra("EXTRA_ID_COLLECTION");
        String act = intent.getStringExtra("EXTRA_ACTIVITY");
        String flag = intent.getStringExtra("EXTRA_FLAG");
        String filePath = intent.getStringExtra("EXTRA_PATH");
        int page = intent.getIntExtra("EXTRA_PAGE", 0);

        findViewById(R.id.backButton).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent;
                if (act.contains("CollectionActivity")){
                    intent = new Intent(BookOpenActivity.this, CollectionActivity.class);
                    intent.putExtra("EXTRA_ID_COLLECTION", collectionId);
                    intent.putExtra("EXTRA_TITLE", flag);
                } else {
                    intent = new Intent(BookOpenActivity.this, MainActivity.class);
                    if (flag.contains(getString(R.string.home_label)))
                        intent.putExtra("EXTRA_BTN", "Home");
                    else
                        intent.putExtra("EXTRA_BTN", "Bookmarks");
                }
                startActivity(intent);
            }
        });

        LoadBook(filePath, page);

        StyleTemplates.appColorTheme(this);
        StyleTemplates.applyFontToAllTextViews((ViewGroup) findViewById(R.id.main), this);
    }


    @Override protected void onResume() {
        super.onResume();
        dbManager.openDB();

        setButtons();
        redrawBookmarks();
        ((TextView) findViewById(R.id.labelNameBook)).setText(BOOK.TITLE);
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        dbManager.closeDB();
    }
    @Override public void recreate() {
        redrawBookmarks();
    }



    // open book

    private void LoadBook(String path, int page) {
        File Dir = new File(this.getFilesDir(), "books");
        FILE_BOOK = new File(Dir, path);

        if (FILE_BOOK.exists()) displayPdfPages(page);
        else showMessageInErrorBook();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setButtons() {
        setButtonsEnabled(true);

        Intent intent = getIntent();
        BOOK = dbManager.getBook(Integer.parseInt(intent.getStringExtra("EXTRA_ID")));

        FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) bookmarkPanel.getLayoutParams();
        param.width = 200; bookmarkPanel.setLayoutParams(param);

        findViewById(R.id.favorite_button).setOnClickListener((View v) -> {
            ValueAnimator animator;
            if (bookmarkPanel.getWidth() == 200)
                animator = ValueAnimator.ofInt(200, 20);
            else
                animator = ValueAnimator.ofInt(20, 200);
            animator.setDuration(300);
            animator.addUpdateListener(animation -> {
                int animatedWidth = (int) animation.getAnimatedValue();
                FrameLayout.LayoutParams updatedParams = (FrameLayout.LayoutParams) bookmarkPanel.getLayoutParams();
                updatedParams.width = animatedWidth;
                bookmarkPanel.setLayoutParams(updatedParams);
            });
            animator.start();
        });

        bookmarkPanel.setOnTouchListener((View v, MotionEvent event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                showAddBookmarkDialog(pdfView.getCurrentPage(), event.getY(), v.getContext());
                redrawBookmarks();
            }
            return true;
        });

        findViewById(R.id.other_button).setOnClickListener((view) -> {
            final String[] options = new String[]{
                    getString(R.string.rename_book_label),
                    getString(R.string.go_to_page_label),
                    getString(R.string.go_to_bookmark_label)};

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.actions_label))
                .setItems(options, (DialogInterface dialog, int which) -> {
                    switch (which) {
                        case 0:
                            App.showTextDialog(this, (newTitle) -> {
                                dbManager.updateBook(BOOK.getID(), newTitle, null, -1, (success) -> onResume());
                            }, getString(R.string.enter_name_label), BOOK.TITLE, InputType.TYPE_CLASS_TEXT);
                            break;
                        case 1:
                            App.showTextDialog(this, (res) -> {
                                int p = Integer.parseInt(res);
                                if (p > 0 && pdfView.getPageCount() + 1 > p)
                                    pdfView.jumpTo(p + 1, true);
                                else
                                    Toast.makeText(this, R.string.enter_correct_page_label, Toast.LENGTH_SHORT).show();
                            },
                            getString(R.string.enter_number_page_label), String.valueOf(pdfView.getCurrentPage() + 1), InputType.TYPE_CLASS_NUMBER);
                            break;
                        case 2: {
                            if (bookmarks == null || bookmarks.isEmpty()) {
                                Toast.makeText(this, R.string.list_is_empty_label, Toast.LENGTH_SHORT).show();
                                break;
                            }

                            UniversalAdapter<Bookmark> bookmarkAdapter = new UniversalAdapter<>(this, bookmarks);
                            AlertDialog.Builder bookmarksDialog = new AlertDialog.Builder(this);
                            bookmarksDialog.setTitle(getString(R.string.enter_name_bookmark_label))
                                .setAdapter(bookmarkAdapter, (dialog1, which1) -> {
                                    Bookmark selectedBookmark = bookmarkAdapter.getItem(which1);
                                    if (selectedBookmark != null)
                                        pdfView.jumpTo(selectedBookmark.PAGE, true);
                                });
                            bookmarksDialog.show();
                            break;
                        }
                    }
                }).show();
        });

    }


    // bookmarks methods

    private Set<Bookmark.BookmarkView> visibleBookmarks = new HashSet<>();

    private long lastUpdateTime = 0;
    private static final int UPDATE_THRESHOLD = 200;

    private void updateBookmarkMarkers(int currentPage) {
        bookmarkPanel.removeAllViews();
        if (bookmarks.isEmpty()) return;
        Set<Bookmark.BookmarkView> newVisibleBookmarks = new HashSet<>();

        for (Bookmark bookmark : bookmarks) {
            if (currentPage == bookmark.PAGE || bookmark.PAGE + 1 == currentPage || bookmark.PAGE - 1 == currentPage){
                Bookmark.BookmarkView bookmarkView = bookmark.bookmarkView;
                boolean isCurrent = (bookmark.PAGE == currentPage);

                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) bookmark.bookmarkView.getLayoutParams();
                params.topMargin = (int) (bookmark.POSITION);
                bookmarkView.setLayoutParams(params);

                if (isCurrent) {
                    bookmarkView.startAnimation(createAlphaAnimation(0f, 1f, null, UPDATE_THRESHOLD / 2));
                    StyleTemplates.applyFontToAllTextViews((ViewGroup) bookmarkView, this);
                    if (!visibleBookmarks.contains(bookmarkView))
                        bookmarkPanel.addView(bookmarkView);
                    newVisibleBookmarks.add(bookmarkView);
                }
                else if (visibleBookmarks.contains(bookmark.bookmarkView) && !isCurrent) {
                    bookmarkView.startAnimation(createAlphaAnimation(1f, 0f,
                            () -> bookmarkPanel.post(() -> bookmarkPanel.removeView(bookmarkView)),
                            UPDATE_THRESHOLD / 2));
                }
            } else bookmarkPanel.removeView(bookmark.bookmarkView);
        }

        for (Bookmark.BookmarkView bookmarkView : visibleBookmarks)
            if (!newVisibleBookmarks.contains(bookmarkView))
                bookmarkPanel.removeView(bookmarkView);

        visibleBookmarks = newVisibleBookmarks;
    }

    private void redrawBookmarks() {
        bookmarks = dbManager.getBookmarks(BOOK._ID);
        bookmarkPanel.removeAllViews();
        Context c = this;

        for (Bookmark b: bookmarks) {
            b.bookmarkView.touchOverlay.setOnLongClickListener((v) -> {
                App.showEditTextDialogInBookmark(b, c);
                return true;
            });
            bookmarkPanel.addView(b.bookmarkView);
        }
        updateBookmarkMarkers(pdfView.getCurrentPage());
    }


    private void showAddBookmarkDialog(int page, float position, Context c) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_bookmark_label);

        final EditText titleInput = new EditText(this);
        titleInput.setHint(R.string.name_label);
        titleInput.setLayoutParams(StyleTemplates.getTextEditOnDialogStyle());

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(titleInput);

        builder.setView(layout);

        builder.setPositiveButton(R.string.save_label,(dialog, w) -> {
            String title = titleInput.getText().toString();
            Bookmark bookmark = new Bookmark(page, position, c);
            bookmark.setBookmarkAttributes(-1, title, null, Bookmark.BASE_COLOR, BOOK);
            dbManager.insertBookmark(bookmark);
            redrawBookmarks();
            dialog.cancel();
            Toast.makeText(getApplicationContext(), R.string.successfully_label, Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(R.string.cancel_label, (d, w) -> d.cancel());
        builder.show();
    }




    // PDF CONTENT

    private PDFView pdfView;

    private void displayPdfPages(int pageBook) {
        pdfView = new PDFView(this, null);

        pdfView.fromFile(FILE_BOOK)
            .enableAnnotationRendering(true)
            .spacing(20)
            .enableSwipe(true).swipeHorizontal(false)
                .scrollHandle(new CustomScrollHandle(this))
            .defaultPage(pageBook)
                .onPageChange((page, pageCount) -> {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastUpdateTime < UPDATE_THRESHOLD) return;
                    lastUpdateTime = currentTime;

                    updateBookmarkMarkers(page);
                })
            .load();

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        );
        pdfView.setLayoutParams(layoutParams);
        pdfView.useBestQuality(true);

        StyleTemplates.applyFontToAllTextViews((ViewGroup) pdfView, this);
        linearLayout.addView(pdfView, 0);
    }



    // other

    private void showMessageInErrorBook(){
        TextView textView = new TextView(this, null, 0, R.style.LABEL_EMPTY);
        textView.setText(R.string.failed_to_load_book_label);

        setButtonsEnabled(false);

        bookmarkPanel.setEnabled(false);
        bookmarkPanel.setVisibility(View.GONE);

        linearLayout.addView(textView);
    }

    private void setButtonsEnabled(boolean enabled) {
        findViewById(R.id.favorite_button).setEnabled(enabled);
        findViewById(R.id.other_button).setEnabled(enabled);
    }

    @Override protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
        String selectedLan = prefs.getString("selected_lan", "English");
        Context context = LocaleHelper.updateLocale(newBase, selectedLan);
        super.attachBaseContext(context);
    }


}