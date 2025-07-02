package com.example.bookelectrone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.example.bookelectrone.HelpClass.LocaleHelper;
import com.example.bookelectrone.db.BookDBManager.Bookmark;
import com.example.bookelectrone.HelpClass.PermissionsHelp;
import com.example.bookelectrone.HelpClass.FilesManipulated;
import com.example.bookelectrone.HelpClass.StyleTemplates;
import com.example.bookelectrone.db.BookDBManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity
{
    private List<BookDBManager.Book> BOOKS = new ArrayList<>();
    public void setBookList() { BOOKS = dbManager.getBooks(-1); }
    public List<BookDBManager.Book> getBookList() { return BOOKS; }

    private List<BookDBManager.Collection> COLLECTION = new ArrayList<>();
    public void setCollectionList() { COLLECTION = dbManager.getCollections(); }
    public List<BookDBManager.Collection> getCollectionList() { return COLLECTION; }

    private List<Bookmark> BOOKMARKS = new ArrayList<>();
    public void setBookmarkList() { BOOKMARKS = dbManager.getBookmarks(-1); }
    public List<Bookmark> getBookmarkList() { return BOOKMARKS; }

    public BookDBManager.User USER;
    public void setUSER() { USER = dbManager.getUser(); }
    public void deleteUSER(String login) { dbManager.deleteUser(login); setUSER(); }

    private List<BookDBManager.Test> TESTS;
    public void setTests() { TESTS = dbManager.getTests(); }
    public List<BookDBManager.Test> getTestsList() { return TESTS; }

    public void setInfo() {
        setBookList();
        setCollectionList();
        setBookmarkList();
        setUSER();
        setTests();
    }

    private BottomNavigationView btnNavView;
    private FragmentCommunicator fragmentCommunicator;

    public BookDBManager dbManager;

    Fragment currentFragment;


    @Override protected void onCreate(Bundle savedInstanceState) {
        dbManager = new BookDBManager(this);

        // activity
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // navigation menu
        btnNavView = findViewById(R.id.bottomNavigationView2);
        NavController controller = Navigation.findNavController(this, R.id.fragmentContainerView2);
        NavigationUI.setupWithNavController(btnNavView, controller);
        SetFontInBottomMenu();

        // intent
        Intent intent = getIntent();
        String btnMenu = intent.getStringExtra("EXTRA_BTN");
        if (btnMenu != null) SetFragmentBottomMenu(btnMenu);

        // delete margin menu and set margin fragment
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                params.bottomMargin = insets.getSystemWindowInsetBottom();
                v.setLayoutParams(params);
                return insets.consumeSystemWindowInsets();
            }
        });

        LocaleHelper.getContextToLanguage(this);
        StyleTemplates.appColorTheme(this);
    }


    @Override protected void onResume() {
        super.onResume();
        dbManager.openDB();
        setInfo();
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        dbManager.closeDB();
    }
    public void onResumeData() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView2);
        if (navHostFragment != null) {
            currentFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
            if (currentFragment instanceof FragmentCommunicator) {
                fragmentCommunicator = (FragmentCommunicator) currentFragment;
            }
        }
        if (fragmentCommunicator != null) {
            setInfo();
            fragmentCommunicator.updateData();
        }
    }



    // add to db

    private void addBookToDB(String filePath) {
        String path = FilesManipulated.copyFileToAppDirectory(filePath, this, "books");

        if (path != null) {
            dbManager.insertBook(FilesManipulated.getFileNameInPath(path), path,  null, null);
            setBookList();
        } else Toast.makeText(this, R.string.error_label, Toast.LENGTH_SHORT).show();
    }

    public void addCollectionToDB(String title, String imageResId) {
        dbManager.insertCollection(title, imageResId);
        setCollectionList();
    }



    @Override protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
        String selectedLan = prefs.getString("selected_lan", "English");
        Context context = LocaleHelper.updateLocale(newBase, selectedLan);
        super.attachBaseContext(context);
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FilesManipulated.PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            addBookToDB(FilesManipulated.getPathFromUri(data.getData(), this));
        }

        if (requestCode == FilesManipulated.PICK_IMAGE_BOOK_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            String imagePath = FilesManipulated.getPathFromUri(data.getData(), this);
            String savedImagePath = FilesManipulated.copyFileToAppDirectory(imagePath, this, "images");

            if (FilesManipulated.PATH != null && FilesManipulated.PATH.isEmpty())
                FilesManipulated.deleteFileByPath(FilesManipulated.PATH, "images", this);

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

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!PermissionsHelp.handlePermissionsResult(requestCode, grantResults)) {
            Toast.makeText(this, R.string.permission_not_granted_label, Toast.LENGTH_SHORT).show();
        }
    }



    // others

    public void SetFontInBottomMenu() {
        StyleTemplates.applyFontToAllTextViews(findViewById(R.id.bottomNavigationView2), this);
    }
    public void SetFragmentBottomMenu(String num) {
        switch (num) {
            case "Collections": btnNavView.setSelectedItemId(R.id.collectionsFragment); break;
            case "Bookmarks": btnNavView.setSelectedItemId(R.id.bookmarksFragment); break;
            case "Home": btnNavView.setSelectedItemId(R.id.homeFragment); break;
            case "Tests": btnNavView.setSelectedItemId(R.id.testsFragment); break;
            case "Settings": btnNavView.setSelectedItemId(R.id.settingsFragment); break;
            default: break;
        }
    }


    public interface FragmentCommunicator { void updateData(); }

}