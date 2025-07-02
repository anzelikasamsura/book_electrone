package com.example.bookelectrone.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.bookelectrone.R;

import java.io.File;

public class BookDBHelper extends SQLiteOpenHelper
{

    /**
     * Создает объект BookDBHelper в {@link BookDBManager}.
     *
     * @param context
     */
    public BookDBHelper(@Nullable Context context) {
        super(context, BookConstans.DB_NAME, null, BookConstans.DB_VERSION);

        CreateDirs(context);
    }

    private void CreateDirs(Context context) {
        File booksDir = new File(context.getFilesDir(), "books");
        if (!booksDir.exists()) { booksDir.mkdirs(); }

        File imagesDir = new File(context.getFilesDir(), "images");
        if (!imagesDir.exists()) { imagesDir.mkdirs(); }

        File testsDir = new File(context.getFilesDir(), "tests");
        if (!testsDir.exists()) { testsDir.mkdirs(); }
    }

    /**
     * Метод, создающий базу данных и ее объекты при их отсутствии.
     *
     * @param db The database.
     */
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL(BookConstans.BOOK.TABLE_STRUCTURE);
        db.execSQL(BookConstans.COLLECTIONS.TABLE_STRUCTURE);
        db.execSQL(BookConstans.BOOKMARKS.TABLE_STRUCTURE);
        db.execSQL(BookConstans.USER.TABLE_STRUCTURE);
        db.execSQL(BookConstans.TESTS.TABLE_STRUCTURE);
    }

    /**
     * Удаляет базу данных, если ее версия обновилась. В конце создает ее заново методом {@link BookDBHelper#onCreate(SQLiteDatabase) onCreate()}.
     *
     * @param db The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(BookConstans.BOOK.DROP_TABLE);
        db.execSQL(BookConstans.COLLECTIONS.DROP_TABLE);
        db.execSQL(BookConstans.BOOKMARKS.DROP_TABLE);
        db.execSQL(BookConstans.USER.DROP_TABLE);
        db.execSQL(BookConstans.TESTS.DROP_TABLE);
        onCreate(db);
    }
}
