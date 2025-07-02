package com.example.bookelectrone;

import static com.example.bookelectrone.HelpClass.StyleTemplates.createAlphaAnimation;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bookelectrone.HelpClass.FilesManipulated;
import com.example.bookelectrone.HelpClass.LocaleHelper;
import com.example.bookelectrone.HelpClass.NetworkBookElectrone;
import com.example.bookelectrone.HelpClass.StyleTemplates;
import com.example.bookelectrone.db.BookDBManager;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment implements MainActivity.FragmentCommunicator {

    SharedPreferences sharedPreferences;

    String[] FONTS = {"Roboto", "Arial", "Montserrat", "Times New Roman"};
    String[] LANGUAGES;

    private BookDBManager.User USER;
    LinearLayout userCardLinearLayout;
    LinearLayout linearLayout;
    LinearLayout MAIN;

    Context context;


    @Override public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }

    @SuppressLint("MissingInflatedId")
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        userCardLinearLayout = view.findViewById(R.id.UserCardLayout);
        linearLayout = view.findViewById(R.id.linearLayout);
        MAIN = view.findViewById(R.id.main);
        context = getContext();

        MAIN.setVisibility(View.GONE);

        LANGUAGES = new String[]{getString(R.string.english_label), getString(R.string.russian_label)};


        // color theme
//        themeSwitch = view.findViewById(R.id.switchTheme);
//        sharedPreferences = getActivity().getSharedPreferences("MODE", Context.MODE_PRIVATE);
//        nightMODE = sharedPreferences.getBoolean("night", false);
//        if(nightMODE){ themeSwitch.setChecked(true); }
//        themeSwitch.setOnClickListener(new View.OnClickListener() {
//                @SuppressLint("ApplySharedPref")
//                @Override public void onClick(View v) {
//                    sharedPreferences = getActivity().getSharedPreferences("MODE", Context.MODE_PRIVATE);
//                    SharedPreferences.Editor editor = sharedPreferences.edit();
//
//                    if (!themeSwitch.isChecked()) {
////                        if ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
////                                == Configuration.UI_MODE_NIGHT_YES) {
////                            Toast.makeText(getContext(),
////                                    "Вы не можете отключить темную тему, так как она установлена на вашем устройстве",
////                                    Toast.LENGTH_SHORT).show();
////                            themeSwitch.setChecked(true);
////                        } else {
//                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
//                            editor.putBoolean("night", false);
//                            editor.apply();
//                            getActivity().recreate();
////                        }
//                    } else {
//                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
//                        editor.putBoolean("night", true);
//                        editor.apply();
//                        getActivity().recreate();
//                    }
//                }
//            });


        Spinner spinnerFont = view.findViewById(R.id.spinnerFont);
        ArrayAdapter<String> adapterFont = new ArrayAdapter(getActivity(), android.R.layout.simple_spinner_item, FONTS);
        adapterFont.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFont.setAdapter(adapterFont);
        spinnerFont.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String selectedFont = FONTS[position];
                sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplication());
                sharedPreferences.edit().putString("selected_font", selectedFont).apply();

                StyleTemplates.applySavedFont(getContext());
                StyleTemplates.applyFontToAllTextViews((ViewGroup) view, getContext());
                ((MainActivity) getActivity()).SetFontInBottomMenu();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String selectedFont = sharedPreferences.getString("selected_font", "Roboto");
        int spinnerPosition = adapterFont.getPosition(selectedFont);
        spinnerFont.setSelection(spinnerPosition);



        Spinner spinnerLan = view.findViewById(R.id.spinnerLanguage);
        ArrayAdapter<String> adapterLan = new ArrayAdapter(getActivity(), android.R.layout.simple_spinner_item, LANGUAGES);
        adapterLan.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLan.setAdapter(adapterLan);
        spinnerLan.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String selectedLan = LANGUAGES[position];
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplication());
                String currentLang = sharedPreferences.getString("selected_lan", "English");

                StyleTemplates.applyFontToAllTextViews(spinnerLan, getContext());

                if (!selectedLan.equals(currentLang)) {
                    sharedPreferences.edit().putString("selected_lan", selectedLan).apply();
                    getActivity().recreate();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String selectedLan = sharedPreferences.getString("selected_lan", "English");
        int spinnerPositionLan = adapterLan.getPosition(selectedLan);
        spinnerLan.setSelection(spinnerPositionLan);


        // font size
//        SeekBar seekBar = view.findViewById(R.id.seekBarFontSize);
//        TextView previewText = view.findViewById(R.id.testTextFontSize);
//        int savedFontSize = sharedPreferences.getInt("font_size", 18);
//
//        seekBar.setProgress(savedFontSize);
//        float textSize = previewText.getTextSize();
//        previewText.setText(String.valueOf(savedFontSize));
//        previewText.setTextSize(textSize + (savedFontSize - textSize));
//
//        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                previewText.setTextSize(progress);
//                previewText.setText(String.valueOf(progress));
//            }
//            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
//            @Override public void onStopTrackingTouch(SeekBar seekBar) {
//                SharedPreferences.Editor editor = sharedPreferences.edit();
//                editor.putInt("font_size", seekBar.getProgress());
//                editor.apply();
//            }
//        });


        view.findViewById(R.id.appSettings).setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                Toast.makeText(getContext(), R.string.settings_information, Toast.LENGTH_LONG).show();
                return true;
            }
        });


        // synchronization in profile
        view.findViewById(R.id.synchronization_button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!App.isConnected(context)) { userCard(USER); return; }

                if (USER != null) new GetUserDataTask().execute();
                else Toast.makeText(getContext(), R.string.you_no_login_label, Toast.LENGTH_SHORT).show();
            }
        });


        // reports on book
        view.findViewById(R.id.button_report).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                BookDBManager db = new BookDBManager(context);
                db.openDB();
                App.showBottomSheetMenuSelectItemOnList(db.getBooks(-1), context, (selectedBook) -> {
                    Intent intent = new Intent(getActivity(), ReportBookActivity.class);
                    intent.putExtra("EXTRA_BOOK", String.valueOf(selectedBook.getID()));
                    startActivity(intent);
                }, getString(R.string.select_a_book_label));
                db.closeDB();
            }
        });


        // download material
        view.findViewById(R.id.button_download).setOnClickListener(v -> {
            if (USER == null) {
                Toast.makeText(context, R.string.login_or_register_label, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!App.isConnected(context)) { return; }
            new GetBooksTask().execute();
        });

        return view;
    }

    @Override public void onResume() {
        super.onResume();
        USER = ((MainActivity) requireContext()).USER;
        if (USER == null) userCardNoUser();
        else userCard(USER);

        userCardLinearLayout.startAnimation(createAlphaAnimation(0f, 1f, null, 500));
        linearLayout.startAnimation(createAlphaAnimation(0f, 1f, null, 500));
        MAIN.setVisibility(View.VISIBLE);
    }
    @Override public void updateData() { onResume(); }

    private void userCardNoUser() {
        userCardLinearLayout.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View cardView = inflater.inflate(R.layout.card_book, userCardLinearLayout, false);

        ((TextView) cardView.findViewById(R.id.NameBook)).setText("Войти");
        ((TextView) cardView.findViewById(R.id.DescriptionBook)).setText(null);
        ((ImageView) cardView.findViewById(R.id.ImageBook)).setImageResource(R.drawable.user_icon_24);

        ImageView arrowImageView = cardView.findViewById(R.id.point_button);
        arrowImageView.setVisibility(View.GONE);
        arrowImageView.setClickable(false);
        arrowImageView.setEnabled(false);

        cardView.findViewById(R.id.Card_0)
                .setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (!App.isConnected(context)) return;

                        Intent intent = new Intent(getActivity(), RegisterActivity.class);
                        startActivity(intent);
                    }
                });

        userCardLinearLayout.addView(cardView);
        StyleTemplates.applyFontToAllTextViews((ViewGroup) userCardLinearLayout, getContext());
    }
    private void userCard(BookDBManager.User user) {
        userCardLinearLayout.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View cardView = inflater.inflate(R.layout.card_book, userCardLinearLayout, false);

        ((TextView) cardView.findViewById(R.id.NameBook)).setText(user.NAME);
        ((TextView) cardView.findViewById(R.id.DescriptionBook)).setText(user.LOGIN);

        if (user.IMAGE == null)
            ((ImageView) cardView.findViewById(R.id.ImageBook)).setImageResource(R.drawable.user_icon_24);
        else ((ImageView) cardView.findViewById(R.id.ImageBook)).setImageBitmap(
            new BitmapDrawable(String.valueOf(
            new File(
            new File(context.getFilesDir(), "images"), USER.IMAGE))).getBitmap());

        ImageView arrowImageView = cardView.findViewById(R.id.point_button);
        arrowImageView.setVisibility(View.GONE);
        arrowImageView.setClickable(false);
        arrowImageView.setEnabled(false);

        cardView.findViewById(R.id.Card_0)
                .setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), UserInfoActivity.class);
                        startActivity(intent);
                    }
                });

        userCardLinearLayout.addView(cardView);
        StyleTemplates.applyFontToAllTextViews((ViewGroup) userCardLinearLayout, getContext());
    }



    private class GetUserDataTask extends AsyncTask<String, Void, String> {
        private String login;

        @Override protected String doInBackground(String... params) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(NetworkBookElectrone.BASE_URL + "get_user_data.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                SharedPreferences prefs = context.getSharedPreferences("TOKEN", Context.MODE_PRIVATE);
                String token = prefs.getString("jwt_token", "");
                if (!token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);

                int responseCode = connection.getResponseCode();
                StringBuilder response = new StringBuilder();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    while ((line = in.readLine()) != null) response.append(line);
                    in.close();
                }

                return response.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally { if(connection != null) connection.disconnect(); }
        }

        private ProgressDialog progressDialog;

        @Override protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(getString(R.string.please_wait_label));
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override protected void onPostExecute(String result) {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            if (result == null || result.trim().isEmpty()) return;
            try {
                JSONObject jsonResponse = new JSONObject(result);
                boolean success = jsonResponse.getBoolean("success");
                if (success) {
                    JSONObject userObject = jsonResponse.getJSONObject("user");

                    login = userObject.getString("login");
                    String name = userObject.getString("name");
                    String email = userObject.getString("email");
                    String image = userObject.optString("image", "");

                    ((MainActivity) context)
                        .dbManager.updateUser(login, null, null, name, email, null,
                            (s) -> {
                                ((MainActivity) context).setUSER();
                                USER = ((MainActivity) context).USER;
                                if (image != null && !image.isEmpty()) {
                                    FilesManipulated.DownloadFileTask d = new FilesManipulated.DownloadFileTask();
                                    d.setParams(context ,(path) ->
                                        ((MainActivity) requireContext()).dbManager
                                            .updateUser(USER.LOGIN, null, null, null, null, path,
                                                (su) -> {
                                                    ((MainActivity) requireContext()).setUSER();
                                                    USER = ((MainActivity) requireContext()).USER;
                                                    onResume();
                                                }
                                            )
                                    );
                                    d.execute(image, "images");
                                } else { onResume(); }
                            });
                } else {
                    String error = jsonResponse.optString("error");
                    if ("USER NOT FOUND".equals(error)) {
                        Toast.makeText(context, R.string.you_account_delete_label, Toast.LENGTH_SHORT).show();
                        ((MainActivity) getActivity()).deleteUSER(USER.LOGIN);
                        USER = ((MainActivity) getActivity()).USER;
                        onResume();
                    } else Toast.makeText(context, R.string.error_label + error, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) { e.printStackTrace(); }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class GetBooksTask extends AsyncTask<Void, Void, List<BookDBManager.Book>> {
        @Override protected List<BookDBManager.Book> doInBackground(Void... voids) {
            List<BookDBManager.Book> books = new ArrayList<>();
            HttpURLConnection connection = null;
            try {
                URL url = new URL(NetworkBookElectrone.BASE_URL + "get_books.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                if(responseCode == HttpURLConnection.HTTP_OK){
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null){
                        response.append(line);
                    }
                    in.close();

                    JSONObject jsonObject = new JSONObject(response.toString());
                    if (jsonObject.getBoolean("success")) {
                        JSONArray booksArray = jsonObject.getJSONArray("books");
                        for (int i = 0; i < booksArray.length(); i++){
                            JSONObject bookObj = booksArray.getJSONObject(i);
                            BookDBManager.Book book = new BookDBManager.Book();
                            book._ID = bookObj.getInt("id");
                            book.TITLE = bookObj.getString("title");
                            book.PATH = bookObj.getString("filename");
                            books.add(book);
                        }
                    }
                }
            } catch(Exception ex){ ex.printStackTrace();
            } finally { if(connection != null) connection.disconnect(); }
            return books;
        }

        private ProgressDialog progressDialog;

        @Override protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(context.getString(R.string.please_wait_label));
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override protected void onPostExecute(List<BookDBManager.Book> books) {
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();

            App.showBottomSheetMenuSelectItemOnList(books, context, (selectedBook) ->
                new AlertDialog.Builder(context)
                        .setTitle(R.string.download_book_label)
                        .setMessage(getString(R.string.download_book_ok_label) + " \"" + selectedBook.TITLE + "\"?")
                        .setPositiveButton(R.string.ok_label, (d, w) ->
                                new GetBookDetailsTask(selectedBook).execute())
                        .setNegativeButton(R.string.cancel_label, null)
                        .show()
            , getString(R.string.select_a_book_label));

        }
    }

    @SuppressLint("StaticFieldLeak")
    private class GetBookDetailsTask extends AsyncTask<Void, Void, JSONObject> {
        private final BookDBManager.Book selectedBook;
        public GetBookDetailsTask(BookDBManager.Book selectedBook) {
            this.selectedBook = selectedBook;
        }

        @Override protected JSONObject doInBackground(Void... voids) {
            HttpURLConnection connection = null;
            JSONObject resultJson = null;
            try {
                URL url = new URL(NetworkBookElectrone.BASE_URL + "download_and_tests.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                SharedPreferences prefs = context.getSharedPreferences("TOKEN", Context.MODE_PRIVATE);
                String token = prefs.getString("jwt_token", "");
                if (!token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);

                String params = "book_id=" + URLEncoder.encode(String.valueOf(selectedBook._ID), "UTF-8");
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(params.getBytes("UTF-8"));
                    os.flush();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) response.append(line);
                    in.close();
                    resultJson = new JSONObject(response.toString());
                }

            } catch(Exception e){ e.printStackTrace(); }
            finally { if (connection != null) connection.disconnect(); }

            Log.e("resultJson", "resultJson: " + resultJson);

            return resultJson;
        }

        private ProgressDialog progressDialog;

        @Override protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(context.getString(R.string.please_wait_label));
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override protected void onPostExecute(JSONObject jsonObject) {
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();

            try {
                if (jsonObject != null && jsonObject.getBoolean("success")) {
                    String downloadUrl = jsonObject.getString("download_url");
                    JSONArray testsArray = jsonObject.getJSONArray("tests");

                    FilesManipulated.DownloadFileTask d = new FilesManipulated.DownloadFileTask();
                    d.setParams(context, (q) -> {
                        BookDBManager db = new BookDBManager(context);
                        db.openDB();

                        String jsonBook = null;
                        int idBook = -1;
                        try {
                            jsonBook = jsonObject.getString("book_name");
                            idBook = db.getBookIDInPath(q);
                            if (idBook == -1) {
                                db.insertBook(jsonBook, q,  null, null);
                                idBook = db.getBookIDInPath(q);
                                db.closeDB();
                            }
                            else db.updateBook(idBook, jsonBook, null, -1, (s) -> db.closeDB());
                        } catch (JSONException e) { db.closeDB(); throw new RuntimeException(e); }

                        for (int i = 0; i < testsArray.length(); i++) {
                            JSONObject testObj = null;
                            try {
                                testObj = testsArray.getJSONObject(i);
                                int idNetwork = testObj.getInt("id");
                                String title = testObj.getString("title");
                                String path = testObj.getString("path");
                                int grade = testObj.optInt("grade", -1);
                                String questions_answers = testObj.getString("questions_answer");

                                FilesManipulated.DownloadFileTask t = new FilesManipulated.DownloadFileTask();
                                int finalIdBook = idBook;
                                t.setParams(context, (s) -> {
                                    BookDBManager bd = new BookDBManager(context);
                                    bd.openDB();
                                    BookDBManager.Test Test = bd.getTest(idNetwork);
                                    if (Test._ID != -1) bd.updateTest(idNetwork, title, 2, grade, questions_answers, (ss) -> bd.closeDB());
                                    else {
                                        if (grade == -1) bd.insertTest(idNetwork, title, false, 0, finalIdBook, path, "");
                                        else bd.insertTest(idNetwork, title, true, grade, finalIdBook, path, questions_answers);
                                        bd.closeDB();
                                    }
                                    ((MainActivity) context).onResumeData();
                                });
                                t.execute(path, "tests");
                            } catch (JSONException e) { db.closeDB(); throw new RuntimeException(e); }
                        }
                        ((MainActivity) context).onResumeData();
                    });
                    d.execute(downloadUrl, "books");
                }
                else Toast.makeText(context, getString(R.string.data_download_error_label), Toast.LENGTH_SHORT).show();
            } catch(JSONException e) { e.printStackTrace(); }
        }
    }



}