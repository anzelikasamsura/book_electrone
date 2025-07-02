package com.example.bookelectrone;

import static com.example.bookelectrone.HelpClass.StyleTemplates.createAlphaAnimation;
import static com.example.bookelectrone.HelpClass.StyleTemplates.setPointColorInImageView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bookelectrone.HelpClass.FilesManipulated;
import com.example.bookelectrone.HelpClass.HeaderView;
import com.example.bookelectrone.HelpClass.NetworkBookElectrone;
import com.example.bookelectrone.HelpClass.StyleTemplates;
import com.example.bookelectrone.HelpClass.UniversalAdapter;
import com.example.bookelectrone.db.BookDBManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class TestsFragment extends Fragment implements MainActivity.FragmentCommunicator {

    private LinearLayout linearLayout;
    private Context context;
    private List<BookDBManager.Test> tests;

    private View view;

    @Override public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_tests, container, false);

        linearLayout = view.findViewById(R.id.layout_tests);
        context = getContext();

        StyleTemplates.applyFontToAllTextViews((ViewGroup) view, getContext());
        return view;
    }

    private <T extends UniversalAdapter.DisplayableItem> void setHeader(View view) {
        HeaderView<T> headerView = view.findViewById(R.id.header_view);
        headerView.init((List<T>) tests, context,
                (objects) -> showTestsOnDB((List<BookDBManager.Test>) objects),
                () -> {
                    BookDBManager db = new BookDBManager(context);
                    db.openDB();
                    App.showBottomSheetMenuSelectItemOnList(db.getBooks(-1), context, (selectedTest) -> {
                        EditText searchInput = view.findViewById(R.id.search_input);
                        searchInput.setText(selectedTest.getTitle());
                        showTestsOnDB(App.performSearch(tests, selectedTest.getTitle()));
                        searchInput.requestFocus();
                        searchInput.setSelection(searchInput.getText().length());
                    }, getString(R.string.select_a_book_label));
                    db.closeDB();
                }
        );
        headerView.setTitle(getString(R.string.tests_label));
        headerView.startAnimation(createAlphaAnimation(0f, 1f, null, 300));
    }

    @Override public void onResume() {
        super.onResume();
        tests = ((MainActivity) context).getTestsList();
        showTestsOnDB(tests);
        setHeader(view);
    }
    @Override public void updateData() { onResume(); }


    public void showTestsOnDB(List<BookDBManager.Test> TESTS) {
        linearLayout.removeAllViews();

        if (TESTS.isEmpty()){
            TextView textView = new TextView(getContext(), null, 0, R.style.LABEL_EMPTY);
            textView.setText(getString(R.string.list_is_empty_label));
            linearLayout.addView(textView);
        } else for (BookDBManager.Test t : TESTS) addCard(t);

        StyleTemplates.applyFontToAllTextViews((ViewGroup) linearLayout, getContext());
    }

    @SuppressLint("ResourceType")
    private void addCard(BookDBManager.Test test) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View cardView = inflater.inflate(R.layout.card_book, linearLayout, false);

        ((TextView) cardView.findViewById(R.id.NameBook)).setText(test.getTitle());
        ((TextView) cardView.findViewById(R.id.DescriptionBook)).setText(test.getDescription());

        ((ImageView) cardView.findViewById(R.id.ImageBook)).setImageResource(R.drawable.test);

        int cardID = test.getID();
        cardView.findViewById(R.id.Card_0).setId(cardID);

        ImageButton b = cardView.findViewById(R.id.point_button);
        b.setEnabled(false);
        b.setClickable(false);
        b.setPadding(16,20,16,20);

        if (test.STATE) setPointColorInImageView(b, BookDBManager.Bookmark.GREEN_COLOR);
        else setPointColorInImageView(b, BookDBManager.Bookmark.RED_COLOR);

        cardView.findViewById(cardID).setOnClickListener(v -> agoToTest(cardID));

        cardView.setOnLongClickListener(v -> {
            String score = test.STATE ? String.valueOf(test.GRADE) : getString(R.string.take_test_label);
            String state = test.STATE ? getString(R.string.passed_label) : getString(R.string.no_passed_label);

            String desc = getString(R.string.state_test_label) + state + "\n" +
                getString(R.string.score_test_label) + score;
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle(getString(R.string.state_label))
                .setMessage(desc)
                .setPositiveButton(R.string.close_label, null);

            if (test.STATE) builder
                .setNeutralButton(R.string.send_to_curator_label, (d, w) -> {
                    if (!App.isConnected(context)) { return; }
                    new SendTestTask().execute(
                        String.valueOf(test.GRADE),
                        String.valueOf(test._ID),
                        String.valueOf(test.QUESTION_ANSWERS)
                    );
                });

            builder.show();
            return true;
        });

        cardView.startAnimation(createAlphaAnimation(0f, 1f, null, 300));
        linearLayout.addView(cardView);
    }

    private void agoToTest(int id) {
        Intent intent = new Intent(context, TestActivity.class);
        intent.putExtra("EXTRA_ID", id);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    private class SendTestTask extends AsyncTask<String, Void, String> {
        @Override protected String doInBackground(String... params) {
            String grade = params[0];
            String testId = params[1];
            String que = params[2];

            Log.e("que", que);

            HttpURLConnection connection = null;
            try {
                URL url = new URL(NetworkBookElectrone.BASE_URL + "save_grade_in_user.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                SharedPreferences prefs = context.getSharedPreferences("TOKEN", Context.MODE_PRIVATE);
                String token = prefs.getString("jwt_token", "");
                if (!token.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("grade", grade)
                        .appendQueryParameter("questions_answer", que)
                        .appendQueryParameter("testId", testId);
                String query = builder.build().getEncodedQuery();

                OutputStream os = connection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(query); writer.flush(); writer.close();
                os.close();

                int responseCode = connection.getResponseCode();
                StringBuilder response = new StringBuilder();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) response.append(inputLine);
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
            if (result == null || result.trim().isEmpty()) {
                Toast.makeText(context, R.string.error_empty_response_label, Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONObject jsonResponse = new JSONObject(result);
                boolean success = jsonResponse.getBoolean("success");
                if (success) {
                    Toast.makeText(context, getString(R.string.successfully_label), Toast.LENGTH_SHORT).show();
                } else {
                    String error = jsonResponse.optString("error");
                    Toast.makeText(context, getString(R.string.error_label) + error, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(context, R.string.error_processing_data_label, Toast.LENGTH_SHORT).show();
            }
        }
    }

}