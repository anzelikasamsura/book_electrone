package com.example.bookelectrone;

import static com.example.bookelectrone.HelpClass.StyleTemplates.createAlphaAnimation;
import static com.example.bookelectrone.HelpClass.StyleTemplates.setPointColorInImageView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.example.bookelectrone.HelpClass.FilesManipulated;
import com.example.bookelectrone.HelpClass.HeaderView;
import com.example.bookelectrone.HelpClass.LocaleHelper;
import com.example.bookelectrone.HelpClass.PermissionsHelp;
import com.example.bookelectrone.HelpClass.StyleTemplates;
import com.example.bookelectrone.HelpClass.UniversalAdapter;
import com.example.bookelectrone.db.BookDBManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.example.bookelectrone.db.BookDBManager.Bookmark;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class App extends Application {


    @Override protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
        String selectedLan = prefs.getString("selected_lan", "English");
        Context context = LocaleHelper.updateLocale(newBase, selectedLan);
        super.attachBaseContext(context);
    }



    /**
     * Данные методы отвечают за добавление карточки в любой активности и в любой LinearLayout.
     *
     * @param activity
     * @param linearLayout
     */
    public static <T extends UniversalAdapter.DisplayableItem> void addCard(
            T object, Context activity, LinearLayout linearLayout,
            OnCardSelectedListener cardListener, OnPointButtonSelectedListener pointListener)
    {
        LayoutInflater inflater = LayoutInflater.from(activity);
        View cardView = inflater.inflate(R.layout.card_book, linearLayout, false);

        ((TextView) cardView.findViewById(R.id.NameBook)).setText(object.getTitle());
        ((TextView) cardView.findViewById(R.id.DescriptionBook)).setText(object.getDescription());

        ImageView imageView = cardView.findViewById(R.id.ImageBook);
        if (object.getImage() == null) {
            if (object.getType() == BookDBManager.IS_BOOK)
                imageView.setImageResource(R.drawable.book);
            else if (object.getType() == BookDBManager.IS_COLLECTION)
                imageView.setImageResource(R.drawable.collection);
            else if (object.getType() == BookDBManager.IS_BOOKMARK) {
                imageView.setImageBitmap(null);
                setPointColorInImageView(imageView, object.getColor());

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(150, 150);
                params.setMargins(36,16,16,16);
                imageView.setLayoutParams(params);
            }
        }
        else imageView.setImageBitmap(
                new BitmapDrawable(String.valueOf(
                new File(
                new File(activity.getFilesDir(), "images"), object.getImage()))).getBitmap());


        if (object.getType() == BookDBManager.IS_BOOKMARK) {
            cardView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    String desc = ((Bookmark) object).DESCRIPTION != null
                            ? ((Bookmark) object).DESCRIPTION
                            : activity.getString(R.string.empty_label);
                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                    builder.setTitle(R.string.description_label)
                            .setMessage(desc)
                            .setPositiveButton(R.string.close_label, null)
                            .show();
                    return true;
                }
            });
        }

        int cardID = object.getID();
        cardView.findViewById(R.id.Card_0).setId(cardID);

        if (pointListener != null)
            cardView.findViewById(R.id.point_button).setOnClickListener(v -> pointListener.onPointButtonSelected());
        if (cardListener != null)
            cardView.findViewById(cardID).setOnClickListener(v -> cardListener.onCardSelected());

        cardView.startAnimation(createAlphaAnimation(0f, 1f, null, 300));
        linearLayout.addView(cardView);
    }
    public interface OnCardSelectedListener {
        void onCardSelected();
    }
    public interface OnPointButtonSelectedListener {
        void onPointButtonSelected();
    }


    /**
     * Нижние меню изменения карточки.
     *
     * @param activity
     * @param linearLayout
     */
    public static <T extends UniversalAdapter.DisplayableItem> void showBottomSheetMenu(
            T object, Context activity, LinearLayout linearLayout,
            OnOkListener renameListener, OnDeleteListener deleteListener)
    {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
        View bottomSheetView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_menu_book, linearLayout, false);

        bottomSheetDialog.setContentView(bottomSheetView);
        StyleTemplates.applyFontToAllTextViews((ViewGroup) bottomSheetView, activity);
        ((TextView) bottomSheetView.findViewById(R.id.header)).setText(object.getTitle());


        ImageView imageView = bottomSheetView.findViewById(R.id.image_header);
        if (object.getImage() == null) {
            if (object.getType() == BookDBManager.IS_BOOK)
                imageView.setImageResource(R.drawable.book);
            else if (object.getType() == BookDBManager.IS_COLLECTION)
                imageView.setImageResource(R.drawable.collection);
        }
        else imageView.setImageBitmap(
                new BitmapDrawable(String.valueOf(
                new File(
                new File(activity.getFilesDir(), "images"), object.getImage()))).getBitmap());

        bottomSheetView.findViewById(R.id.action_card_rename).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (PermissionsHelp.checkPermissions(activity)) {
                    showTextDialog(activity, renameListener, activity.getString(R.string.enter_name_label), object.getTitle(), InputType.TYPE_CLASS_TEXT);
                    bottomSheetDialog.dismiss();
                } else PermissionsHelp.requestPermissions((Activity) activity, activity);
            }
        });
        bottomSheetView.findViewById(R.id.action_card_image).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (PermissionsHelp.checkPermissions(activity)) {
                    int flag = object.getType() == BookDBManager.IS_BOOK ? BookDBManager.IS_BOOK : BookDBManager.IS_COLLECTION;
                    FilesManipulated.openFileManagerImage((Activity) activity, object.getID(), object.getImage(), flag);
                    bottomSheetDialog.dismiss();
                } else PermissionsHelp.requestPermissions((Activity) activity, activity);
            }
        });
        if (object.getType() == BookDBManager.IS_BOOK) {
            bottomSheetView.findViewById(R.id.action_card_add_to_collection).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (PermissionsHelp.checkPermissions(activity)) {
                        BookDBManager db = new BookDBManager(activity);
                        db.openDB();
                        showBottomSheetMenuSelectItemOnList(db.getCollections(), activity, (selectedObject) -> {
                            BookDBManager dbManager = new BookDBManager(activity);
                            dbManager.openDB();
                            dbManager.updateBook(object.getID(), null, null, selectedObject.getID(), (success) -> {
                                dbManager.closeDB();
                                if (activity instanceof MainActivity)
                                    ((MainActivity) activity).onResumeData();
                                else
                                    ((Activity) activity).recreate();
                            });
                        }, activity.getString(R.string.select_collection_label));
                        db.closeDB();
                        bottomSheetDialog.dismiss();
                    } else {
                        PermissionsHelp.requestPermissions((Activity) activity, activity);
                    }
                }
            });
        }
        else {
            bottomSheetView.findViewById(R.id.action_card_add_to_collection).setVisibility(View.GONE);
        }
        bottomSheetView.findViewById(R.id.action_card_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionsHelp.checkPermissions(activity)) {
                    new AlertDialog.Builder(activity)
                        .setTitle(R.string.confirm_action_label)
                        .setMessage(R.string.you_confident_label)
                        .setPositiveButton(R.string.ok_label, (dialog, which) -> deleteListener.onDeleteSelected())
                        .setNegativeButton(R.string.cancel_label, null)
                        .show();

                    bottomSheetDialog.dismiss();
                } else {
                    PermissionsHelp.requestPermissions((Activity) activity, activity);
                }
            }
        });

        bottomSheetDialog.show();
    }
    public interface OnDeleteListener {
        void onDeleteSelected();
    }
    public interface OnOkListener {
        void onOkSelected(String res);
    }


    /**
     * Вызов нижнего меню с выбором одного объекта из листа.
     *
     * @param activity
     * @param listener Слушатель, выполняеться код по нажатию на коллекцию.
     */
    public static <T extends UniversalAdapter.DisplayableItem> void showBottomSheetMenuSelectItemOnList(
            List<T> objects, Context activity, OnItemSelectedListener<T> listener, String header)
    {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
        View bottomSheetView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_select_collection, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        ListView listView = bottomSheetView.findViewById(R.id.list_collections);
        ((TextView) bottomSheetView.findViewById(R.id.header)).setText(header);

        if(objects.isEmpty()) {
            List<String> emptyListMessage = new ArrayList<>();
            emptyListMessage.add(activity.getString(R.string.list_is_empty_label));
            ArrayAdapter<String> emptyAdapter = new ArrayAdapter(activity, android.R.layout.simple_list_item_1, emptyListMessage) {
                @Override public boolean isEnabled(int position) { return false; }
            };
            listView.setAdapter(emptyAdapter);
        }
        else {
            UniversalAdapter adapter = new UniversalAdapter(activity, objects);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    T object = objects.get(position);
                    listener.onItemSelected(object);
                    bottomSheetDialog.dismiss();
                }
            });
        }
        StyleTemplates.applyFontToAllTextViews((ViewGroup) bottomSheetView, activity);
        bottomSheetDialog.show();
    }
    public interface OnItemSelectedListener<T extends UniversalAdapter.DisplayableItem> {
        void onItemSelected(T obj);
    }


    /**
     * Диалоговое окно для переименования чего-либо.
     * Содержит текстовое поле и выбор: ОК или отмена.
     */
    public static void showTextDialog(Context context, OnOkListener listener, String header, String text, int type)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(header);

        final EditText input = new EditText(context);
        input.setInputType(type);
        input.setText(text);
        input.setLayoutParams(StyleTemplates.getTextEditOnDialogStyle());

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(input);

        builder.setView(layout);
        builder.setPositiveButton(R.string.ok_label, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                String res = input.getText().toString().trim();
                if (!res.isEmpty()) listener.onOkSelected(res);
                else Toast.makeText(context, R.string.error_empty_field_label, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(context.getString(R.string.cancel_label), new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) { dialog.cancel(); }
        });
        builder.show();
    }


    /**
     * Диалоговое окно для изменения закладки.
     *
     * @param bookmark Объект закладки.
     * @param context Context, из которого вызываеться диалог.
     */
    public static void showEditTextDialogInBookmark(@NonNull Bookmark bookmark, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.update_bookmark_label);

        final EditText input = new EditText(context);
        input.setHint(R.string.name_label);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(bookmark.TITLE);
        input.setLayoutParams(StyleTemplates.getTextEditOnDialogStyle());

        final EditText input2 = new EditText(context);
        input2.setHint(R.string.description_label);
        input2.setInputType(InputType.TYPE_CLASS_TEXT);
        input2.setText(bookmark.DESCRIPTION);
        input2.setLayoutParams(StyleTemplates.getTextEditOnDialogStyle());

        final int[] selectedColor = {bookmark.COLOR};
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(input);
        layout.addView(input2);
        layout.addView(StyleTemplates.getColorsOnDialog(context, bookmark.COLOR, (c) -> selectedColor[0] = c ));

        builder.setView(layout);
        builder.setPositiveButton(R.string.change_label, (dialog, which) -> {
            String newTitle = input.getText().toString().trim();
            String newDesc = input2.getText().toString().trim();
            if (!newTitle.isEmpty()) {
                if (newDesc.isEmpty() || newDesc.equals("") || newDesc == null) newDesc = null;

                BookDBManager DBManager = new BookDBManager(context);
                DBManager.openDB();
                DBManager.updateBookmark(bookmark.ID, newTitle, newDesc, selectedColor[0], -1, -1,
                    (success) -> {
                    DBManager.closeDB();
                    if (context instanceof MainActivity) ((MainActivity) context).onResumeData();
                    else ((Activity) context).recreate();
                });
            }
            else Toast.makeText(context, R.string.error_empty_field_label, Toast.LENGTH_SHORT).show();
        });
        if (context instanceof BookOpenActivity) {
            builder.setNeutralButton(R.string.delete_label, (dialog, which) -> {
                BookDBManager DBManager = new BookDBManager(context);
                DBManager.openDB();
                DBManager.deleteBookmark(bookmark.ID);
                DBManager.closeDB();
                Toast.makeText(context, R.string.successfully_label, Toast.LENGTH_SHORT).show();
            });
        }
        builder.setNegativeButton(R.string.cancel_label, (dialog, which) -> dialog.cancel());
        builder.show();
    }




    public static <T extends UniversalAdapter.DisplayableItem> void showFilterDialog(
            List<T> objects, Context context, OnShowListListener<T> listener, OnSearchOtherButtonListener listener2)
    {
        String[] param = new String[]{
                context.getString(R.string.a_z_label),
                context.getString(R.string.z_a_label)};
        if (listener2 != null) param = new String[]{
                context.getString(R.string.a_z_label),
                context.getString(R.string.z_a_label),
                context.getString(R.string.other_label)};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.filter_label)
            .setItems(param , (DialogInterface dialog, int which) -> {
                switch (which) {
                    case 0:
                        listener.onShowSelected(filterBookmarksAlphabetically(objects, true));
                        break;
                    case 1:
                        listener.onShowSelected(filterBookmarksAlphabetically(objects, false));
                        break;
                    case 2:
                        listener2.onSearchOtherButtonSelected();
                        break;
                }
            }).show();
    }
    public interface OnShowListListener<T extends UniversalAdapter.DisplayableItem> {
        void onShowSelected(List<T> objects);
    }
    public interface OnSearchOtherButtonListener {
        void onSearchOtherButtonSelected();
    }


    public static <T extends UniversalAdapter.DisplayableItem> List<T> performSearch(List<T> objects, String query) {
        List<T> obs = new ArrayList<>();
        for (T o : objects) {
            String title = o.getTitle() != null ? o.getTitle() : "";
            String description = o.getDescription() != null ? o.getDescription() : "";

            if (title.toLowerCase().contains(query.toLowerCase()) ||
                    description.toLowerCase().contains(query.toLowerCase())) {
                obs.add(o);
            }
        }
        return obs;
    }

    private static <T extends UniversalAdapter.DisplayableItem> List<T> filterBookmarksAlphabetically(
            List<T> objects, final boolean ascending) {
        if (objects == null) return new ArrayList<T>();
        if (!objects.isEmpty()) {
            objects.sort((b1, b2) -> ascending
                    ? b1.getTitle().compareToIgnoreCase(b2.getTitle())
                    : b2.getTitle().compareToIgnoreCase(b1.getTitle()));
        }
        return objects;
    }



    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            Toast.makeText(context, "Нет подключения к сети!", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


}
