package com.example.bookelectrone.db;

public class BookConstans
{

    public static final String DB_NAME = "book_electrone.db";
    public static final int DB_VERSION = 30;


    public static final class BOOK {
        public static final String TABLE_NAME = "books";
        public static final String _ID = "_id";

        public static final String TITLE = "title";
        public static final String PATH = "path";
        public static final String IMAGE = "image";
        public static final String COLLECTION_ID = "collection_id";

        public static final String TABLE_STRUCTURE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                        " (" + _ID + " INTEGER PRIMARY KEY, " +
                        TITLE + " TEXT, " + PATH + " TEXT, " +
                        IMAGE + " TEXT, " + COLLECTION_ID + " INTEGER, " +
                        "FOREIGN KEY (" + COLLECTION_ID + ") REFERENCES " +
                        COLLECTIONS.TABLE_NAME + "(" + COLLECTIONS._ID + ")"
                        + ")";

        public static final String DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static final class COLLECTIONS {
        public static final String TABLE_NAME = "collections";
        public static final String _ID = "_id";

        public static final String TITLE = "title";
        public static final String IMAGE = "image";

        public static final String TABLE_STRUCTURE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                        " (" + _ID + " INTEGER PRIMARY KEY, " +
                        TITLE + " TEXT, " + IMAGE + " TEXT"
                        + ")";

        public static final String DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static final class BOOKMARKS {
        public static final String TABLE_NAME = "bookmarks";
        public static final String _ID = "_id";

        public static final String TITLE = "title";
        public static final String DESCRIPTION = "description";
        public static final String COLOR = "color";

        public static final String BOOK_ID = "book_id";
        public static final String PAGE = "page";
        public static final String POSITION = "position";

        public static final String TABLE_STRUCTURE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                        " (" + _ID + " INTEGER PRIMARY KEY, " +
                        TITLE + " TEXT, " + DESCRIPTION + " TEXT, " +
                        COLOR + " INTEGER, " + BOOK_ID + " INTEGER, " +
                        PAGE + " INTEGER, " + POSITION + " REAL, " +
                        "FOREIGN KEY (" + BOOK_ID + ") REFERENCES " +
                        BOOK.TABLE_NAME + "(" + BOOK._ID + ")"
                        + ")";

        public static final String DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static final class TESTS {
        public static final String TABLE_NAME = "tests";
        public static final String _ID = "_id";

        public static final String TITLE = "title";
        public static final String STATE = "state";
        public static final String GRADE = "grade";
        public static final String PATH = "path";

        public static final String QUESTIONS_ANSWERS = "questions_answers";
        public static final String BOOK_ID = "book_id";
        public static final String USER_LOGIN = "user_login";

        public static final String TABLE_STRUCTURE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                        " (" + _ID + " INTEGER PRIMARY KEY, " +
                        TITLE + " TEXT, " +
                        STATE + " INTEGER, " + BOOK_ID + " INTEGER, " +
                        GRADE + " INTEGER, " + PATH + " TEXT, " +
                        USER_LOGIN + " TEXT, " +
                        QUESTIONS_ANSWERS + " TEXT, " +
                        "FOREIGN KEY (" + BOOK_ID + ") REFERENCES " +
                        BOOK.TABLE_NAME + "(" + BOOK._ID + "), " +
                        "FOREIGN KEY (" + USER_LOGIN + ") REFERENCES " +
                        USER.TABLE_NAME + "(" + USER.LOGIN + ") "
                        + ")";

        public static final String DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static final class USER {
        public static final String TABLE_NAME = "user";
        public static final String LOGIN = "login";

        public static final String PASSWORD = "password";
        public static final String NAME = "name";
        public static final String EMAIL = "email";
        public static final String IMAGE = "image";

        public static final String TABLE_STRUCTURE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                        " (" + LOGIN + " TEXT PRIMARY KEY, " +
                        PASSWORD + " TEXT, " + NAME + " TEXT, " +
                        EMAIL + " TEXT, " + IMAGE + " TEXT, " +
                        "UNIQUE (" + EMAIL + ")"
                        + ")";

        public static final String DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

}
