package com.example.bookelectrone;

import static com.example.bookelectrone.HelpClass.StyleTemplates.createAlphaAnimation;
import static com.example.bookelectrone.HelpClass.StyleTemplates.setPointColorInImageView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bookelectrone.HelpClass.HeaderView;
import com.example.bookelectrone.HelpClass.StyleTemplates;
import com.example.bookelectrone.HelpClass.UniversalAdapter;
import com.example.bookelectrone.db.BookDBManager.Bookmark;
import com.example.bookelectrone.HelpClass.PermissionsHelp;
import com.example.bookelectrone.db.BookDBManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;


public class BookmarksFragment extends Fragment implements MainActivity.FragmentCommunicator {

    private LinearLayout linearLayout;
    private Context context;
    private List<Bookmark> bookmarks;

    private View view;

    @Override public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_bookmarks, container, false);

        linearLayout = view.findViewById(R.id.layout_bookmarks);
        context = getContext();

        StyleTemplates.applyFontToAllTextViews((ViewGroup) view, getContext());
        return view;
    }

    private <T extends UniversalAdapter.DisplayableItem> void setHeader(View view) {
        HeaderView<T> headerView = view.findViewById(R.id.header_view);
        headerView.init((List<T>) bookmarks, context,
            (objects) -> showBookmarksOnDB((List<Bookmark>) objects),
            () -> {
                BookDBManager db = new BookDBManager(context);
                db.openDB();
                App.showBottomSheetMenuSelectItemOnList(db.getBooks(-1), context, (selectedBook) -> {
                    EditText searchInput = view.findViewById(R.id.search_input);
                    searchInput.setText(selectedBook.getTitle());
                    showBookmarksOnDB(App.performSearch(bookmarks, selectedBook.getTitle()));
                    searchInput.requestFocus();
                    searchInput.setSelection(searchInput.getText().length());
                }, getString(R.string.select_a_book_label));
                db.closeDB();
            }
        );
        headerView.setTitle(getString(R.string.bookmarks_label));
        headerView.startAnimation(createAlphaAnimation(0f, 1f, null, 300));
    }


    @Override public void onResume() {
        super.onResume();
        bookmarks = ((MainActivity) context).getBookmarkList();
        showBookmarksOnDB(bookmarks);
        setHeader(view);
    }
    @Override public void updateData() { onResume(); }
    private void updateList() {
        ((MainActivity) getActivity()).setBookmarkList();
        onResume();
    }



    public void showBookmarksOnDB(List<Bookmark> BOOKMARKS){
        linearLayout.removeAllViews();

        if (BOOKMARKS.isEmpty()){
            TextView textView = new TextView(getContext(), null, 0, R.style.LABEL_EMPTY);
            textView.setText(getString(R.string.list_is_empty_label));
            linearLayout.addView(textView);
        } else for (Bookmark b : BOOKMARKS)
            App.addCard(b, context, linearLayout,
                () -> {
                    Intent intent = new Intent(context, BookOpenActivity.class);
                    intent.putExtra("EXTRA_TITLE", b.BOOK.TITLE);
                    intent.putExtra("EXTRA_PATH", b.BOOK.PATH);
                    intent.putExtra("EXTRA_ID", String.valueOf(b.BOOK._ID));
                    intent.putExtra("EXTRA_ACTIVITY", String.valueOf(context));
                    intent.putExtra("EXTRA_FLAG", "Закладки");
                    intent.putExtra("EXTRA_PAGE", b.PAGE);

                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                },
                () -> showBottomSheetMenu(b, context));

        StyleTemplates.applyFontToAllTextViews((ViewGroup) linearLayout, getContext());
    }
    
    public void showBottomSheetMenu(Bookmark bookmark, Context activity) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
        View bottomSheetView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_menu_book, linearLayout, false);

        bottomSheetDialog.setContentView(bottomSheetView);
        StyleTemplates.applyFontToAllTextViews((ViewGroup) bottomSheetView, getContext());

        bottomSheetView.findViewById(R.id.action_card_image).setVisibility(View.GONE);
        bottomSheetView.findViewById(R.id.action_card_add_to_collection).setVisibility(View.GONE);

        ((TextView) bottomSheetView.findViewById(R.id.header)).setText(bookmark.TITLE);

        ImageView i = bottomSheetView.findViewById(R.id.image_header);
        i.setImageBitmap(null);
        i.setLayoutParams(new LinearLayout.LayoutParams(64, 64));
        setPointColorInImageView(i, bookmark.COLOR);

        bottomSheetView.findViewById(R.id.action_card_rename).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (PermissionsHelp.checkPermissions(activity)) {
                    App.showEditTextDialogInBookmark(bookmark, activity);
                    bottomSheetDialog.dismiss();
                } else {
                    PermissionsHelp.requestPermissions((Activity) activity, activity);
                }
            }
        });
        bottomSheetView.findViewById(R.id.action_card_delete).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (PermissionsHelp.checkPermissions(activity)) {
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.confirm_action_label)
                            .setMessage(R.string.you_confident_label)
                            .setPositiveButton(getString(R.string.ok_label), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    BookDBManager DBManager = new BookDBManager(activity);
                                    DBManager.openDB();
                                    DBManager.deleteBookmark(bookmark.ID);
                                    DBManager.closeDB();

                                    Toast.makeText(activity, R.string.successfully_label, Toast.LENGTH_SHORT).show();
                                    updateList();
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel_label), null)
                            .show();
                    bottomSheetDialog.dismiss();
                } else {
                    PermissionsHelp.requestPermissions((Activity) activity, activity);
                }
            }
        });

        bottomSheetDialog.show();
    }




}