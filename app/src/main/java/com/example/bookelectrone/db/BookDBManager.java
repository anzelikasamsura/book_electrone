package com.example.bookelectrone.db;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.bookelectrone.HelpClass.FilesManipulated;
import com.example.bookelectrone.HelpClass.UniversalAdapter;
import com.example.bookelectrone.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class BookDBManager
{
    private Context context;
    private BookDBHelper bookDBHelper;
    private SQLiteDatabase db;


    public BookDBManager(Context c) {
        context = c;
        bookDBHelper = new BookDBHelper(context);
    }

    public interface EndCallback {
        void onEndCompleted(int success);
    }

    public void openDB(){
        if (db == null) db = bookDBHelper.getWritableDatabase();
    }
    public void closeDB(){
        bookDBHelper.close();
    }



    /**
     * Метод добавления книги в базу данных.
     *
     * @param title Заголовок книги.
     * @param path Путь до файла книги.
     * @param image Путь до изображения книги (Обложка).
     * @param collectionId id сборника. null если книга не включина в сборник.
     */
    public void insertBook(String title, String path, String image, Integer collectionId) {
        ContentValues cv = new ContentValues();

        cv.put(BookConstans.BOOK.TITLE, title);
        cv.put(BookConstans.BOOK.PATH, path);
        cv.put(BookConstans.BOOK.IMAGE, image);
        cv.put(BookConstans.BOOK.COLLECTION_ID, collectionId);

        db.insert(BookConstans.BOOK.TABLE_NAME, null, cv);
    }

    /**
     * Метод выдачи списка книг.
     *
     * @param collectionId id сборника. Если значение -1 - выдает все книги, иначе - книги определенного сборника.
     * @return {@code List<Book>}
     */
    @SuppressLint("Range")
    public List<Book> getBooks(int collectionId) {
        List<Book> booksList = new ArrayList<>();
        String selection = null;
        String[] selectionArgs = null;

        if (collectionId != -1) {
            selection = BookConstans.BOOK.COLLECTION_ID + "=?";
            selectionArgs = new String[]{String.valueOf(collectionId)};
        }

        Cursor cursor = db.query(BookConstans.BOOK.TABLE_NAME, null,
                selection, selectionArgs, null, null, null);

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex(BookConstans.BOOK._ID));
            String title = cursor.getString(cursor.getColumnIndex(BookConstans.BOOK.TITLE));
            String path = cursor.getString(cursor.getColumnIndex(BookConstans.BOOK.PATH));
            String image = cursor.getString(cursor.getColumnIndex(BookConstans.BOOK.IMAGE));
            int collectionIdFromDb = cursor.getInt(cursor.getColumnIndex(BookConstans.BOOK.COLLECTION_ID));

            booksList.add(new Book(id, title, path, image, collectionIdFromDb, getCollection(collectionIdFromDb)));
        }

        cursor.close();
        return booksList;
    }

    @SuppressLint("Range")
    public Book getBook(int id) {
        Book book = new Book(-1, null, null, null, -1, null);

        Cursor cursor = db.query(BookConstans.BOOK.TABLE_NAME, null,
                null, null, null, null, null);

        while (cursor.moveToNext()) {
            int i = cursor.getInt(cursor.getColumnIndex(BookConstans.BOOK._ID));
            if (i == id) {
                book._ID = i;
                book.TITLE = cursor.getString(cursor.getColumnIndex(BookConstans.BOOK.TITLE));
                book.PATH = cursor.getString(cursor.getColumnIndex(BookConstans.BOOK.PATH));
                book.IMAGE = cursor.getString(cursor.getColumnIndex(BookConstans.BOOK.IMAGE));
                book.COLLECTION_ID = cursor.getInt(cursor.getColumnIndex(BookConstans.BOOK.COLLECTION_ID));
                book.COLLECTION = getCollection(book.COLLECTION_ID);

                cursor.close();
                return book;
            }
        }
        cursor.close();
        return book;
    }

    @SuppressLint("Range")
    public int getBookIDInPath(String path) {
        Cursor cursor = db.query(BookConstans.BOOK.TABLE_NAME, null,
                null, null, null, null, null);

        while (cursor.moveToNext()) {
            String i = cursor.getString(cursor.getColumnIndex(BookConstans.BOOK.PATH));
            if (Objects.equals(i, path)) {
                int id = cursor.getInt(cursor.getColumnIndex(BookConstans.BOOK._ID));
                cursor.close();
                return id;
            }
        }
        cursor.close();
        return -1;
    }

    /**
     * Метод обновления книги по его id.
     * <p>
     * {@code Если значение, кроме id, = -1 или = null, то этот параметр не учитываеться!}
     *
     * @param id id книги.
     * @param title Заголовок.
     * @param image Путь до изображения.
     * @param collection id сборника.
     */
    public void updateBook(int id, String title, String image, int collection, EndCallback callback) {
        new Thread(new Runnable() {
            @Override public void run() {
                ContentValues cv = new ContentValues();

                if (title != null){
                    cv.put(BookConstans.BOOK.TITLE, title);
                }
                if (image != null){
                    cv.put(BookConstans.BOOK.IMAGE, image);
                }
                if (collection != -1){
                    cv.put(BookConstans.BOOK.COLLECTION_ID, collection);
                }
                int isUpdated = db.update(BookConstans.BOOK.TABLE_NAME, cv, BookConstans.BOOK._ID + "=?", new String[]{String.valueOf(id)});

                if (callback != null) ((Activity) context).runOnUiThread(() -> callback.onEndCompleted(isUpdated));
            }
        }).start();
    }

    public void deleteBook(int id) {
        db.delete(BookConstans.BOOK.TABLE_NAME, BookConstans.BOOK._ID + "=?", new String[]{String.valueOf(id)});
        db.delete(BookConstans.BOOKMARKS.TABLE_NAME, BookConstans.BOOKMARKS.BOOK_ID + "=?", new String[]{String.valueOf(id)});

        deleteFilesTests(getTests());
        db.delete(BookConstans.TESTS.TABLE_NAME, BookConstans.TESTS.BOOK_ID + "=?", new String[]{String.valueOf(id)});
    }




    /**
     * Метод добавления сборника в базу данных.
     *
     * @param title Заголовок сборника.
     * @param image Путь до изображения сборника (Обложка).
     */
    public void insertCollection(String title, String image) {
        ContentValues cv = new ContentValues();
        cv.put(BookConstans.COLLECTIONS.TITLE, title);
        cv.put(BookConstans.COLLECTIONS.IMAGE, image);

        db.insert(BookConstans.COLLECTIONS.TABLE_NAME, null, cv);
    }

    /**
     * Метод выдачи списка сборников.
     *
     * @return {@code List<Collection>}
     */
    @SuppressLint("Range")
    public List<Collection> getCollections() {
        List<Collection> CollectionsList = new ArrayList<>();

        Cursor cursor = db.query(BookConstans.COLLECTIONS.TABLE_NAME, null,
                null, null, null, null, null);
        while (cursor.moveToNext())
        {
            int id = cursor.getInt(cursor.getColumnIndex(BookConstans.COLLECTIONS._ID));
            String title = cursor.getString(cursor.getColumnIndex(BookConstans.COLLECTIONS.TITLE));
            String image = cursor.getString(cursor.getColumnIndex(BookConstans.COLLECTIONS.IMAGE));
            CollectionsList.add(new Collection(id, title, image));
        }
        cursor.close();

        return CollectionsList;
    }

    @SuppressLint("Range")
    public Collection getCollection(int id) {
        Collection collection = new Collection(-1, null, null);

        Cursor cursor = db.query(BookConstans.COLLECTIONS.TABLE_NAME, null,
                null, null, null, null, null);
        while (cursor.moveToNext())
        {
            int i = cursor.getInt(cursor.getColumnIndex(BookConstans.COLLECTIONS._ID));
            if (i == id) {
                collection._ID = i;
                collection.TITLE = cursor.getString(cursor.getColumnIndex(BookConstans.COLLECTIONS.TITLE));
                collection.IMAGE = cursor.getString(cursor.getColumnIndex(BookConstans.COLLECTIONS.IMAGE));
                cursor.close();
                return collection;
            }
        }
        cursor.close();
        return collection;
    }

    /**
     * Метод обновления сборника по его id.
     * <p>
     * {@code Если значение, кроме id, = -1 или = null, то этот параметр не учитываеться!}
     *
     * @param id id книги.
     * @param newTitle Заголовок.
     * @param newImage Путь до изображения.
     */
    public void updateCollection(int id, String newTitle, String newImage, EndCallback callback) {
        new Thread(new Runnable() {
            @Override public void run() {
                ContentValues cv = new ContentValues();

                if (newTitle != null){
                    cv.put(BookConstans.COLLECTIONS.TITLE, newTitle);
                }
                if (newImage != null){
                    cv.put(BookConstans.COLLECTIONS.IMAGE, newImage);
                }
                int isUpdated = db.update(BookConstans.COLLECTIONS.TABLE_NAME, cv, BookConstans.COLLECTIONS._ID + "=?", new String[]{String.valueOf(id)});

                if (callback != null) ((Activity) context).runOnUiThread(() -> callback.onEndCompleted(isUpdated));
            }
        }).start();
    }

    public void deleteCollection(int id) {
        ContentValues cv = new ContentValues();
        cv.put(BookConstans.BOOK.COLLECTION_ID, (Integer) null);

        db.update(BookConstans.BOOK.TABLE_NAME, cv, BookConstans.BOOK.COLLECTION_ID + "=?", new String[]{String.valueOf(id)});

        db.delete(BookConstans.COLLECTIONS.TABLE_NAME, BookConstans.COLLECTIONS._ID + "=?", new String[]{String.valueOf(id)});
    }




    /**
     * Метод добавления закладки в базу данных.
     *
     * @param bookmark Объект закладки из {@link Bookmark#Bookmark(int, float, Context) Bookmark}.
     */
    public void insertBookmark(Bookmark bookmark) {
        ContentValues cv = new ContentValues();
        cv.put(BookConstans.BOOKMARKS.TITLE, bookmark.TITLE);
        cv.put(BookConstans.BOOKMARKS.DESCRIPTION, bookmark.DESCRIPTION);
        cv.put(BookConstans.BOOKMARKS.COLOR, String.valueOf(bookmark.COLOR));
        cv.put(BookConstans.BOOKMARKS.BOOK_ID, bookmark.BOOK._ID);

        cv.put(BookConstans.BOOKMARKS.PAGE, bookmark.PAGE);
        cv.put(BookConstans.BOOKMARKS.POSITION, bookmark.POSITION);

        db.insert(BookConstans.BOOKMARKS.TABLE_NAME, null, cv);
    }

    /**
     * Метод выдачи списка закладок.
     *
     * @param bookId id книги. -1 для извлечения всех закладок из базы данных.
     *               В этом случае в объекте Bookmark создаеться подобъект book, олицетворяющий книгу, к которой привязана закладка.
     *               Нужна для ссылок.
     * @return {@code List<Bookmark>}
     */
    @SuppressLint("Range")
    public List<Bookmark> getBookmarks(int bookId) {
        List<Bookmark> bookmarksList = new ArrayList<>();

        Cursor cursor;

        if (bookId != -1) {
            String selection = BookConstans.BOOKMARKS.BOOK_ID + "=?";
            String[] selectionArgs = new String[]{String.valueOf(bookId)};

            cursor = db.query(BookConstans.BOOKMARKS.TABLE_NAME, null,
                    selection, selectionArgs, null, null, null);
        } else {
            cursor = db.query(BookConstans.BOOKMARKS.TABLE_NAME, null,
                    null, null, null, null, null);
        }

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex(BookConstans.BOOKMARKS._ID));
            String title = cursor.getString(cursor.getColumnIndex(BookConstans.BOOKMARKS.TITLE));
            String description = cursor.getString(cursor.getColumnIndex(BookConstans.BOOKMARKS.DESCRIPTION));
            int color = cursor.getInt(cursor.getColumnIndex(BookConstans.BOOKMARKS.COLOR));

            int pageNumber = cursor.getInt(cursor.getColumnIndex(BookConstans.BOOKMARKS.PAGE));
            int bookID = cursor.getInt(cursor.getColumnIndex(BookConstans.BOOKMARKS.BOOK_ID));
            float textSelection = cursor.getFloat(cursor.getColumnIndex(BookConstans.BOOKMARKS.POSITION));

            Bookmark bookmark = new Bookmark(pageNumber, textSelection, context);
            bookmark.setBookmarkAttributes(id, title, description, color, getBook(bookID));

            bookmarksList.add(bookmark);
        }

        cursor.close();
        return bookmarksList;
    }

    /**
     * Метод обновления сборника по его id.
     * <p>
     * {@code Если значение, кроме id, = -1 или = null, то этот параметр не учитываеться!}
     *
     * @param id id закладки.
     * @param newTitle Заголовок.
     * @param desc Описание.
     * @param newColor Цвет.
     * @param page Номер страницы книги.
     * @param position Позиция по оси Y относительно страницы.
     */
    public void updateBookmark(int id, String newTitle, String desc, int newColor, int page, float position, EndCallback callback) {
        new Thread(new Runnable() {
            @Override public void run() {
                ContentValues cv = new ContentValues();

                if (newTitle != null){
                    cv.put(BookConstans.BOOKMARKS.TITLE, newTitle);
                }
                if (desc != null){
                    cv.put(BookConstans.BOOKMARKS.DESCRIPTION, desc);
                }
                if (newColor != -1){
                    cv.put(BookConstans.BOOKMARKS.COLOR, newColor);
                }
                if (page != -1){
                    cv.put(BookConstans.BOOKMARKS.PAGE, page);
                }
                if (position != -1){
                    cv.put(BookConstans.BOOKMARKS.POSITION, position);
                }
                int isUpdated = db.update(BookConstans.BOOKMARKS.TABLE_NAME, cv, BookConstans.BOOKMARKS._ID + "=?", new String[]{String.valueOf(id)});

                if (callback != null) ((Activity) context).runOnUiThread(() -> callback.onEndCompleted(isUpdated));
            }
        }).start();
    }

    public void deleteBookmark(int id) {
        db.delete(BookConstans.BOOKMARKS.TABLE_NAME, BookConstans.BOOKMARKS._ID + "=?", new String[]{String.valueOf(id)});
    }




    public void insertUser(User user) {
        ContentValues cv = new ContentValues();
        cv.put(BookConstans.USER.LOGIN, user.LOGIN);
        cv.put(BookConstans.USER.PASSWORD, user.PASSWORD);
        cv.put(BookConstans.USER.NAME, user.NAME);
        cv.put(BookConstans.USER.EMAIL, user.EMAIL);
        cv.put(BookConstans.USER.IMAGE, user.IMAGE);

        db.insert(BookConstans.USER.TABLE_NAME, null, cv);
    }

    @SuppressLint("Range")
    public User getUser() {
        List<User> users = new ArrayList<>();

        Cursor cursor = db.query(BookConstans.USER.TABLE_NAME, null,
                null, null, null, null, null);
        while (cursor.moveToNext())
        {
            String login = cursor.getString(cursor.getColumnIndex(BookConstans.USER.LOGIN));
            String pass = cursor.getString(cursor.getColumnIndex(BookConstans.USER.PASSWORD));
            String name = cursor.getString(cursor.getColumnIndex(BookConstans.USER.NAME));
            String email = cursor.getString(cursor.getColumnIndex(BookConstans.USER.EMAIL));
            String image = cursor.getString(cursor.getColumnIndex(BookConstans.USER.IMAGE));

            users.add(new User(login, pass, name, email, image));
        }
        cursor.close();

        if (users.isEmpty()) return null;
        return users.get(0);
    }

    public void updateUser(String login, String newLogin, String pass, String name, String email, String image, EndCallback callback) {
        new Thread(new Runnable() {
            @Override public void run() {
                ContentValues cv = new ContentValues();

                if (login != null && newLogin == null){
                    cv.put(BookConstans.USER.LOGIN, login);
                } else if (login != null && newLogin != null) {
                    cv.put(BookConstans.USER.LOGIN, newLogin);
                }

                if (pass != null){
                    cv.put(BookConstans.USER.PASSWORD, pass);
                }
                if (name != null){
                    cv.put(BookConstans.USER.NAME, name);
                }
                if (email != null){
                    cv.put(BookConstans.USER.EMAIL, email);
                }
                if (image != null){
                    cv.put(BookConstans.USER.IMAGE, image);
                }

                int isUpdated = db.update(BookConstans.USER.TABLE_NAME, cv, BookConstans.USER.LOGIN + "=?", new String[]{String.valueOf(login)});

                if (callback != null) ((Activity) context).runOnUiThread(() -> callback.onEndCompleted(isUpdated));
            }
        }).start();
    }

    public void deleteUser(String login) {
        db.delete(BookConstans.USER.TABLE_NAME, BookConstans.USER.LOGIN + "=?", new String[]{String.valueOf(login)});

        deleteFilesTests(getTests());
        db.delete(BookConstans.TESTS.TABLE_NAME, BookConstans.TESTS.USER_LOGIN + "=?", new String[]{String.valueOf(login)});
    }




    public void insertTest(int idNetwork, String title, boolean state, int grade, int id_book, String path, String qa) {
        ContentValues cv = new ContentValues();

        cv.put(BookConstans.TESTS._ID, String.valueOf(idNetwork));
        cv.put(BookConstans.TESTS.TITLE, title);
        cv.put(BookConstans.TESTS.STATE, String.valueOf(state ? 1 : 0));
        cv.put(BookConstans.TESTS.GRADE, String.valueOf(grade));
        cv.put(BookConstans.TESTS.BOOK_ID, String.valueOf(id_book));
        cv.put(BookConstans.TESTS.PATH, path);
        cv.put(BookConstans.TESTS.QUESTIONS_ANSWERS, qa);

        User u = getUser();
        cv.put(BookConstans.TESTS.USER_LOGIN, u.LOGIN);

        db.insert(BookConstans.TESTS.TABLE_NAME, null, cv);
    }

    @SuppressLint("Range")
    public List<Test> getTests() {
        List<Test> TestsList = new ArrayList<>();

        Cursor cursor = db.query(BookConstans.TESTS.TABLE_NAME, null,
                null, null, null, null, null);
        while (cursor.moveToNext())
        {
            int id = cursor.getInt(cursor.getColumnIndex(BookConstans.TESTS._ID));
            String title = cursor.getString(cursor.getColumnIndex(BookConstans.TESTS.TITLE));
            int state = cursor.getInt(cursor.getColumnIndex(BookConstans.TESTS.STATE));
            int grade = cursor.getInt(cursor.getColumnIndex(BookConstans.TESTS.GRADE));
            int id_book = cursor.getInt(cursor.getColumnIndex(BookConstans.TESTS.BOOK_ID));
            String path = cursor.getString(cursor.getColumnIndex(BookConstans.TESTS.PATH));
            String que = cursor.getString(cursor.getColumnIndex(BookConstans.TESTS.QUESTIONS_ANSWERS));
            String login = cursor.getString(cursor.getColumnIndex(BookConstans.TESTS.USER_LOGIN));

            Test test = new Test(id, title, state, grade, id_book, path, que, login);
            if (id_book != 0) test.BOOK = getBook(id_book);
            TestsList.add(test);
        }
        cursor.close();

        return TestsList;
    }

    @SuppressLint("Range")
    public Test getTest(int id) {
        Test test = new Test(-1, null, 0, 0, -1, null, null, null);

        Cursor cursor = db.query(BookConstans.TESTS.TABLE_NAME, null,
                null, null, null, null, null);
        while (cursor.moveToNext()) {
            int i = cursor.getInt(cursor.getColumnIndex(BookConstans.TESTS._ID));
            if (i == id) {
                test._ID = id;
                test.TITLE = cursor.getString(cursor.getColumnIndex(BookConstans.TESTS.TITLE));
                test.STATE = cursor.getInt(cursor.getColumnIndex(BookConstans.TESTS.STATE)) == 1;
                test.GRADE = cursor.getInt(cursor.getColumnIndex(BookConstans.TESTS.GRADE));
                test.BOOK_ID = cursor.getInt(cursor.getColumnIndex(BookConstans.TESTS.BOOK_ID));
                test.PATH = cursor.getString(cursor.getColumnIndex(BookConstans.TESTS.PATH));
                test.QUESTION_ANSWERS = cursor.getString(cursor.getColumnIndex(BookConstans.TESTS.QUESTIONS_ANSWERS));
                test.USER_LOGIN = cursor.getString(cursor.getColumnIndex(BookConstans.TESTS.USER_LOGIN));
                if (test.BOOK_ID != 0) test.BOOK = getBook(test.BOOK_ID);

                cursor.close();
                return test;
            }
        }
        cursor.close();
        return test;
    }




    public void updateTest(int id, String newTitle, int newState, int newGrade, String newQue, EndCallback callback) {
        if (newState == 2) {
            Test test = getTest(id);
            updateTest(id, newTitle, test.STATE, newGrade, newQue, callback);
        }
        else updateTest(id, newTitle, newState == 1, newGrade, newQue, callback);
    }
    public void updateTest(int id, String newTitle, boolean newState, int newGrade, String newQue, EndCallback callback) {
        new Thread(() -> {
            ContentValues cv = new ContentValues();

            if (newTitle != null)
                cv.put(BookConstans.TESTS.TITLE, newTitle);
            if (newGrade != -1)
                cv.put(BookConstans.TESTS.GRADE, newGrade);
            if (newQue != null)
                cv.put(BookConstans.TESTS.QUESTIONS_ANSWERS, newQue);
            cv.put(BookConstans.TESTS.STATE, newState ? 1 : 0);

            int isUpdated = db.update(BookConstans.TESTS.TABLE_NAME, cv, BookConstans.TESTS._ID + "=?", new String[]{String.valueOf(id)});

            if (callback != null) ((Activity) context).runOnUiThread(() -> callback.onEndCompleted(isUpdated));
        }).start();
    }

    private void deleteFilesTests(List<Test> tests) {
        for (Test t : tests) {
            boolean b = FilesManipulated.deleteFileByPath(t.PATH, "tests", context);
        }
    }




    public static final int IS_BOOK = 137218;
    public static final int IS_COLLECTION = 137803;
    public static final int IS_BOOKMARK = 137082;
    public static final int IS_USER = 137289;
    public static final int IS_TEST = 139834;


    public static class Book implements UniversalAdapter.DisplayableItem {
        public int _ID;
        public String TITLE;
        public String PATH;
        public String IMAGE;
        public int COLLECTION_ID;
        public Collection COLLECTION;

        public Book(int id, String t, String p, String i, int coll, Collection c) {
            _ID = id; TITLE = t; PATH = p;
            IMAGE = i; COLLECTION_ID = coll;
            COLLECTION = c;
        }
        public Book(){}

        @Override public int getID() { return _ID; }
        @Override public String getTitle() { return TITLE; }
        @Override public String getImage() { return IMAGE; }
        @Override public int getColor() { return -1; }
        @Override public int getType() { return IS_BOOK; }
        @Override public String getDescription() { return COLLECTION.TITLE; }
    }

    public static class Collection implements UniversalAdapter.DisplayableItem {
        public int _ID;
        public String TITLE;
        public String IMAGE;

        public Collection(int id, String t, String i){
            _ID = id; TITLE = t; IMAGE = i;
        }
        public Collection(){}
        @Override public int getID() { return _ID; }
        @Override public String getTitle() { return TITLE; }
        @Override public String getImage() { return IMAGE; }
        @Override public int getColor() { return -1; }
        @Override public int getType() { return IS_COLLECTION; }
        @Override public String getDescription() { return null; }
    }

    public static class User {
        public String LOGIN;
        public String PASSWORD;
        public String NAME;
        public String EMAIL;
        public String IMAGE;

        public User(String l, String p, String n, String e, String i) {
            LOGIN = l; PASSWORD = p; NAME = n; EMAIL = e; IMAGE = i;
        }
        public User(){}
    }

    public static class Test implements UniversalAdapter.DisplayableItem {
        public int _ID;
        public String TITLE;
        public boolean STATE;
        public int GRADE;
        public String PATH;
        public String QUESTION_ANSWERS;

        public int BOOK_ID;
        public String USER_LOGIN;
        public BookDBManager.Book BOOK;

        public List<Question> questions;

        public Test(int id, String t, int s, int g, int idb, String pt, String q, String ul) {
            _ID = id; TITLE = t;
            STATE = s == 1; GRADE = g;
            BOOK_ID = idb; PATH = pt;
            BOOK = null;
            QUESTION_ANSWERS = q;
            USER_LOGIN = ul;
        }
        public void setTest(int id, String t, boolean s, int g, String q, String ul) {
            _ID = id; TITLE = t;
            STATE = s; GRADE = g; QUESTION_ANSWERS = q;
            USER_LOGIN = ul;
        }
        public Test(){}


        public static class Question {
            public int id;
            public String text;
            public String type; // "radio" или "checkbox"
            public List<Answer> answers;

            public int displayOrder;

            public Question(String t, String te, List<Answer> a) {
                text = t; type = te; answers = a;


            }

            public boolean checkAnswer(List<Answer> selectedAnswers) {
                if ("radio".equals(type)) {
                    if (selectedAnswers.size() != 1) return false;
                    Answer selected = selectedAnswers.get(0);
                    return selected.isCorrect;
                }
                else if ("checkbox".equals(type)) {
                    List<Answer> correctAnswers = new ArrayList<>();
                    for (Answer ans : answers) {
                        if (ans.isCorrect) correctAnswers.add(ans);
                    }
                    if (selectedAnswers.size() != correctAnswers.size()) return false;
                    for (Answer correct : correctAnswers) {
                        boolean found = false;
                        for (Answer selected : selectedAnswers) {
                            if (selected.order == correct.order) {
                                found = true; break;
                            }
                        }
                        if (!found) return false;
                    }
                    return true;
                }
                return false;
            }

            public static class Answer {
                public String text;
                public boolean isCorrect;
                public int order;
                public int displayOrder;

                public Answer (String t, boolean i) {
                    text = t; isCorrect = i; order = -1;
                }
            }
        }


        @Override public int getID() { return _ID; }
        @Override public String getTitle() { return TITLE; }
        @Override public String getImage() { return null; }
        @Override public int getColor() { return -1; }
        @Override public int getType() { return IS_TEST; }
        @Override public String getDescription() {
            if (BOOK != null) return BOOK.TITLE;
            else return null;
        }
    }


    public static class Bookmark implements UniversalAdapter.DisplayableItem {

        public static final int BASE_COLOR = Color.parseColor("#FFC107");
        public static final int RED_COLOR = Color.parseColor("#FF5722");
        public static final int GREEN_COLOR = Color.parseColor("#8BC34A");
        public static final int BLUE_COLOR = Color.parseColor("#2196F3");
        public static final int PURPLE_COLOR = Color.parseColor("#9C27B0");
        public static final int PINK_COLOR = Color.parseColor("#EC409F");


        public int ID;
        public String TITLE;
        public String DESCRIPTION;
        public int COLOR;

        public int PAGE;
        public float POSITION;

        public BookmarkView bookmarkView;
        private final Context context;

        public BookDBManager.Book BOOK;


        public Bookmark(int page, float position, Context c){
            PAGE = page;
            POSITION = position;
            context = c;

            bookmarkView = new BookmarkView(context);
            BOOK = null;
        }

        public void setBookmarkAttributes(int id, String title, String desc, int color, BookDBManager.Book b){
            ID = id; TITLE = title;
            DESCRIPTION = desc;
            COLOR = color;
            BOOK = b;
            bookmarkView.setAttributes(ID, TITLE, COLOR);
        }

        @Override public int getID() { return ID; }
        @Override public String getTitle() { return TITLE; }
        @Override public String getImage() { return null; }
        @Override public int getColor() { return COLOR; }
        @Override public int getType() { return IS_BOOKMARK; }
        @Override public String getDescription() { return BOOK.TITLE; }


        /**
         * Класс, представляющий xlm файл книжной закладки.
         * Отвечает за установку в разметке цвета и заголовка закладки.
         */
        public static class BookmarkView extends LinearLayout {
            private TextView titleTextView;
            private FrameLayout linearLayout;
            public View touchOverlay;
            public ImageView colorView;

            public BookmarkView(Context context) {
                super(context);
                init(context);
            }
            public BookmarkView(Context context, AttributeSet attrs) {
                super(context, attrs);
                init(context);
            }
            public BookmarkView(Context context, AttributeSet attrs, int defStyleAttr) {
                super(context, attrs, defStyleAttr);
                init(context);
            }

            private void init(Context context) {
                LayoutInflater.from(context).inflate(R.layout.bookmark_book, this, true);
                titleTextView = findViewById(R.id.title);
                linearLayout = findViewById(R.id.bookmark);
                touchOverlay = findViewById(R.id.touchOverlay);
                colorView = findViewById(R.id.colorBookmark);
            }

            public void setAttributes(int id, String t, int c) {
                linearLayout.setId(id);
                titleTextView.setText(t);
                colorView.setBackgroundColor(c);
                colorView.setAlpha(0.5f);
            }
        }
    }

}