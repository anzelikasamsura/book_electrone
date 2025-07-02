package com.example.bookelectrone;

import static com.example.bookelectrone.HelpClass.StyleTemplates.createAlphaAnimation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bookelectrone.HelpClass.FilesManipulated;
import com.example.bookelectrone.HelpClass.HeaderView;
import com.example.bookelectrone.HelpClass.StyleTemplates;
import com.example.bookelectrone.HelpClass.UniversalAdapter;
import com.example.bookelectrone.db.BookDBManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;


public class CollectionsFragment extends Fragment implements MainActivity.FragmentCommunicator {

    private LinearLayout linearLayout;
    private Context context;
    private BookDBManager DBManager;
    List<BookDBManager.Collection> collections;

    private View view;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_collections, container, false);

        linearLayout = view.findViewById(R.id.layout_collection_book);
        context = getContext();

        FloatingActionButton fabAddBook = view.findViewById(R.id.fab_add_collection);
        fabAddBook.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                App.showTextDialog(context,
                    (name) -> {
                        ((MainActivity) getActivity()).addCollectionToDB(name, null);
                        onResume();
                    },
                    getString(R.string.enter_name_label), getString(R.string.name_label), InputType.TYPE_CLASS_TEXT);
            }
        });

        StyleTemplates.applyFontToAllTextViews((ViewGroup) view, getActivity());
        return view;
    }

    private <T extends UniversalAdapter.DisplayableItem> void setHeader(View view) {
        HeaderView<T> headerView = view.findViewById(R.id.header_view);
        headerView.init((List<T>) collections, context,
                (objects) -> showCollectionsOnDB((List<BookDBManager.Collection>) objects),
                null);
        headerView.setTitle(getString(R.string.collections_label));
        headerView.startAnimation(createAlphaAnimation(0f, 1f, null, 300));
    }


    @Override public void onResume() {
        super.onResume();
        collections = ((MainActivity) getActivity()).getCollectionList();
        showCollectionsOnDB(((MainActivity) getActivity()).getCollectionList());
        setHeader(view);
    }
    @Override public void updateData() {
        onResume();
    }
    private void updateList() {
        ((MainActivity) getActivity()).setInfo();
        onResume();
    }



    public void showCollectionsOnDB(List<BookDBManager.Collection> COLLECTIONS) {
        linearLayout.removeAllViews();

        if(COLLECTIONS.isEmpty()){
            TextView textView = new TextView(getActivity(), null, 0, R.style.LABEL_EMPTY);
            textView.setText(R.string.list_is_empty_label);
            linearLayout.addView(textView);
        } else
            for (BookDBManager.Collection c : COLLECTIONS)
                App.addCard(c, getContext(), linearLayout,
                    () -> {
                        Intent intent = new Intent(getActivity(), CollectionActivity.class);
                        intent.putExtra("EXTRA_TITLE", c.TITLE);
                        intent.putExtra("EXTRA_ID_COLLECTION", String.valueOf(c._ID));

                        startActivity(intent);
                    },
                    () -> {
                        App.showBottomSheetMenu(c, getActivity(), linearLayout,
                            (newTitle) -> {
                                DBManager = new BookDBManager(getContext());
                                DBManager.openDB();
                                DBManager.updateCollection(c.getID(), newTitle, null, (s) -> {
                                        DBManager.closeDB();
                                        updateList();
                                });
                            },
                            () -> {
                                DBManager = new BookDBManager(getContext());
                                DBManager.openDB();
                                DBManager.deleteCollection(c.getID());
                                DBManager.closeDB();

                                if (c.getImage() != null && c.getImage().isEmpty())
                                    FilesManipulated.deleteFileByPath(c.getImage(), "images", context);
                                updateList();
                            }
                        );
                    });

        StyleTemplates.applyFontToAllTextViews((ViewGroup) linearLayout, getContext());
    }

}