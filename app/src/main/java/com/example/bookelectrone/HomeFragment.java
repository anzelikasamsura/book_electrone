package com.example.bookelectrone;

import static com.example.bookelectrone.HelpClass.StyleTemplates.createAlphaAnimation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bookelectrone.HelpClass.FilesManipulated;
import com.example.bookelectrone.HelpClass.HeaderView;
import com.example.bookelectrone.HelpClass.PermissionsHelp;
import com.example.bookelectrone.HelpClass.StyleTemplates;
import com.example.bookelectrone.HelpClass.UniversalAdapter;
import com.example.bookelectrone.db.BookDBManager;

import java.util.List;

public class HomeFragment extends Fragment implements MainActivity.FragmentCommunicator {

    private LinearLayout linearLayout;
    private Context context;
    List<BookDBManager.Book> books;

    private View view;


    @Override public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }

    @SuppressLint("MissingInflatedId")
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_home, container, false);

        linearLayout = view.findViewById(R.id.layout_cards_book);
        context = getContext();

        view.findViewById(R.id.fab_add_book).setOnClickListener((v) -> {
            if (PermissionsHelp.checkPermissions(getContext()))
                FilesManipulated.openFileManagerBook(getActivity(), -1);
            else
                PermissionsHelp.requestPermissions(getActivity(), getContext());
        });

        StyleTemplates.applyFontToAllTextViews((ViewGroup) view, getContext());
        return view;
    }

    private <T extends UniversalAdapter.DisplayableItem> void setHeader(View view) {
        HeaderView<T> headerView = view.findViewById(R.id.header_view);
        headerView.init((List<T>) books, context,
            (objects) -> showBooksOnDB((List<BookDBManager.Book>) objects),
            () -> {
                BookDBManager db = new BookDBManager(context);
                db.openDB();
                App.showBottomSheetMenuSelectItemOnList(db.getCollections(), context, (selectedBook) -> {
                    EditText searchInput = view.findViewById(R.id.search_input);
                    searchInput.setText(selectedBook.getTitle());
                    showBooksOnDB(App.performSearch(books, selectedBook.getTitle()));
                    searchInput.requestFocus();
                    searchInput.setSelection(searchInput.getText().length());
                }, getString(R.string.select_collection_label));
                db.closeDB();
            }
        );
        headerView.setTitle(getString(R.string.home_label));
        headerView.startAnimation(createAlphaAnimation(0f, 1f, null, 300));
    }


    @Override public void onResume() {
        super.onResume();
        books = ((MainActivity) getActivity()).getBookList();
        showBooksOnDB(books);
        setHeader(view);
    }
    @Override public void updateData() {
        onResume();
    }
    private void updateList() {
        ((MainActivity) getActivity()).setInfo();
        onResume();
    }


    public void showBooksOnDB(List<BookDBManager.Book> BOOKS){
        linearLayout.removeAllViews();

        if(BOOKS.isEmpty()){
            TextView textView = new TextView(getContext(), null, 0, R.style.LABEL_EMPTY);
            textView.setText(R.string.list_is_empty_label);
            linearLayout.addView(textView);
        } else {
            for (BookDBManager.Book b : BOOKS) {
                App.addCard(b, getContext(), linearLayout,
                    () -> {
                        Intent intent = new Intent(getActivity(), BookOpenActivity.class);
                        intent.putExtra("EXTRA_TITLE", b.TITLE);
                        intent.putExtra("EXTRA_PATH", b.PATH);
                        intent.putExtra("EXTRA_ID", String.valueOf(b._ID));
                        intent.putExtra("EXTRA_ID_COLLECTION", String.valueOf(b.COLLECTION_ID));
                        intent.putExtra("EXTRA_ACTIVITY", String.valueOf(getActivity()));
                        intent.putExtra("EXTRA_FLAG", "Главная");

                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    },
                    () -> {
                        App.showBottomSheetMenu(b, getActivity(), linearLayout,
                            (newTitle) -> {
                                BookDBManager DBManager = new BookDBManager(context);
                                DBManager.openDB();
                                DBManager.updateBook(b.getID(), newTitle, null, -1,  (s) -> {
                                    DBManager.closeDB();
                                    updateList();
                                });
                            },
                            () -> {
                                boolean d = FilesManipulated.deleteFileByPath(b.PATH, "books", context);
                                if (d) {
                                    if (b.getImage() != null && !b.getImage().isEmpty())
                                        FilesManipulated.deleteFileByPath(b.getImage(), "images", context);

                                    BookDBManager DBManager = new BookDBManager(getActivity());
                                    DBManager.openDB();
                                    DBManager.deleteBook(b.getID());
                                    DBManager.closeDB();

                                    updateList();
                                    Toast.makeText(getContext(), R.string.successfully_label, Toast.LENGTH_SHORT).show();
                                } else Toast.makeText(getContext(), R.string.error_label, Toast.LENGTH_SHORT).show();
                            }
                        );
                    });
            }
        }
        StyleTemplates.applyFontToAllTextViews((ViewGroup) linearLayout, getContext());
    }

}