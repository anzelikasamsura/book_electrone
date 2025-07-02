package com.example.bookelectrone;

import static com.example.bookelectrone.HelpClass.StyleTemplates.createAlphaAnimation;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.example.bookelectrone.HelpClass.FilesManipulated;
import com.example.bookelectrone.HelpClass.LocaleHelper;
import com.example.bookelectrone.HelpClass.NetworkBookElectrone;
import com.example.bookelectrone.HelpClass.StyleTemplates;
import com.example.bookelectrone.db.BookDBManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    Context context = this;

    public BookDBManager dbManager;
    public BookDBManager.User USER;

    @Override protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
        String selectedLan = prefs.getString("selected_lan", "English");
        Context context = LocaleHelper.updateLocale(newBase, selectedLan);
        super.attachBaseContext(context);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbManager = new BookDBManager(this);

        // delete margin
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), new OnApplyWindowInsetsListener() {
            @NonNull @Override public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                params.bottomMargin = insets.getSystemWindowInsetBottom();
                v.setLayoutParams(params);
                return insets.consumeSystemWindowInsets();
            }
        });

        // back in activity
        findViewById(R.id.backButton).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.putExtra("EXTRA_BTN", "Settings");
                startActivity(intent);
            }
        });

        // hide | show password
        EditText editPasswordA = findViewById(R.id.inputPasswordA);
        ((CheckBox) findViewById(R.id.password_show_a)).setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
                editPasswordA.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            else
                editPasswordA.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            editPasswordA.setSelection(editPasswordA.getText().length());
        });

        EditText editPassword = findViewById(R.id.inputPassword);
        ((CheckBox) findViewById(R.id.password_show)).setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
                editPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            else
                editPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            editPassword.setSelection(editPassword.getText().length());
        });


        loadInterface();

        // theme font and color
        StyleTemplates.appColorTheme(this);
        StyleTemplates.applyFontToAllTextViews((ViewGroup) findViewById(R.id.main), this);
    }

    @Override protected void onResume() {
        super.onResume();
        dbManager.openDB();
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        dbManager.closeDB();
    }


    /**
     * @param enabled true - для авторизации, false - для регистрации!
     */
    private void AuthorizationEnabled(boolean enabled) {
        // Регистрируем — видимость зависит от того, выключен ли режим авторизации
        findViewById(R.id.RegistrationLayout).setVisibility(enabled ? View.GONE : View.VISIBLE);
        findViewById(R.id.RegistrationLayout).setEnabled(!enabled);
        findViewById(R.id.RegistrationLayout).setClickable(!enabled);

        // Метки переключения
        findViewById(R.id.RegistrationLabel).setClickable(enabled);
        findViewById(R.id.RegistrationLabel).setEnabled(enabled);
        findViewById(R.id.AuthorizationLabel).setClickable(!enabled);
        findViewById(R.id.AuthorizationLabel).setEnabled(!enabled);

        // Кнопки показа пароля (аналогично)
        findViewById(R.id.password_show_a).setClickable(enabled);
        findViewById(R.id.password_show_a).setEnabled(enabled);
        findViewById(R.id.password_show).setClickable(!enabled);
        findViewById(R.id.password_show).setEnabled(!enabled);

        // Окно авторизации — видим только когда разрешен режим авторизации
        findViewById(R.id.AuthorizationLayout).setVisibility(enabled ? View.VISIBLE : View.GONE);
        findViewById(R.id.AuthorizationLayout).setEnabled(enabled);
        findViewById(R.id.AuthorizationLayout).setClickable(enabled);

        findViewById(R.id.buttonRegistration).setClickable(!enabled);
        findViewById(R.id.buttonAuthorization).setClickable(enabled);

        // Поля ввода авторизации
        findViewById(R.id.inputLoginA).setEnabled(enabled);
        findViewById(R.id.inputPasswordA).setEnabled(enabled);

        // Поля ввода регистрации
        findViewById(R.id.inputLogin).setEnabled(!enabled);
        findViewById(R.id.inputPassword).setEnabled(!enabled);
        findViewById(R.id.inputName).setEnabled(!enabled);
        findViewById(R.id.inputEmail).setEnabled(!enabled);
    }
    private void loadInterface() {
        AuthorizationEnabled(true);

        findViewById(R.id.AuthorizationLabel).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                findViewById(R.id.RegistrationLayout).startAnimation(createAlphaAnimation(1f, 0f, null, 300));
                findViewById(R.id.AuthorizationLayout).startAnimation(createAlphaAnimation(0f, 1f, null, 300));
                AuthorizationEnabled(true);
            }
        });
        findViewById(R.id.RegistrationLabel).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                findViewById(R.id.AuthorizationLayout).startAnimation(createAlphaAnimation(1f, 0f, null, 300));
                findViewById(R.id.RegistrationLayout).startAnimation(createAlphaAnimation(0f, 1f, null, 300));
                AuthorizationEnabled(false);
            }
        });

        findViewById(R.id.buttonRegistration).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!App.isConnected(context)) return;

                String login = ((EditText) findViewById(R.id.inputLogin)).getText().toString().trim();
                if (login.isEmpty()) {
                    Toast.makeText(context, R.string.enter_correct_login_label, Toast.LENGTH_SHORT).show();
                    return;
                }

                String pass = ((EditText) findViewById(R.id.inputPassword)).getText().toString().trim();
                if (pass.isEmpty()) {
                    Toast.makeText(context, R.string.enter_password_label, Toast.LENGTH_SHORT).show();
                    return;
                }

                String name = ((EditText) findViewById(R.id.inputName)).getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(context, R.string.enter_correct_name_label, Toast.LENGTH_SHORT).show();
                    return;
                }

                String email = ((EditText) findViewById(R.id.inputEmail)).getText().toString().trim();
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(context, R.string.enter_correct_email_label, Toast.LENGTH_SHORT).show();
                    return;
                }

                USER = new BookDBManager.User(login, pass, name, email, null);
                new RegisterTask().execute(login, pass, name, email);
            }
        });
        findViewById(R.id.buttonAuthorization).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!App.isConnected(context)) return;

                String login = ((EditText) findViewById(R.id.inputLoginA)).getText().toString().trim();
                if (login.isEmpty()) {
                    Toast.makeText(context, R.string.enter_correct_login_label, Toast.LENGTH_SHORT).show();
                    return;
                }

                String pass = ((EditText) findViewById(R.id.inputPasswordA)).getText().toString().trim();
                if (pass.isEmpty()) {
                    Toast.makeText(context, R.string.enter_password_label, Toast.LENGTH_SHORT).show();
                    return;
                }

                new AuthorizationTask().execute(login, pass, null);
            }
        });
    }



    // manipulate on db

    private class RegisterTask extends AsyncTask<String, Void, String> {
        @Override protected String doInBackground(String... params) {
            String login = params[0];
            String pass  = params[1];
            String name  = params[2];
            String email = params[3];

            HttpURLConnection connection = null;
            try {
                URL url = new URL(NetworkBookElectrone.BASE_URL + "registration.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("login", login)
                        .appendQueryParameter("password", pass)
                        .appendQueryParameter("name", name)
                        .appendQueryParameter("email", email);
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
                    Toast.makeText(context, R.string.successfully_label, Toast.LENGTH_SHORT).show();

                    new AuthorizationTask().execute(USER.LOGIN, USER.PASSWORD, "1");
                } else {
                    String error = jsonResponse.optString("error");
                    USER = null;

                    if ("UNIQUE LOGIN OR EMAIL".equals(error))
                        Toast.makeText(context, R.string.login_or_email_already_label, Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(context, R.string.error_label + error, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                USER = null;
                Toast.makeText(context, R.string.error_processing_data_label, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class AuthorizationTask extends AsyncTask<String, Void, String> {
        private String login;
        private String pass;
        private String flag;

        @Override protected String doInBackground(String... params) {
            login = params[0];
            pass  = params[1];
            flag = params[2];

            HttpURLConnection connection = null;
            try {
                URL url = new URL(NetworkBookElectrone.BASE_URL + "authorization.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("login", login)
                        .appendQueryParameter("password", pass);
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

            if ((result == null || result.trim().isEmpty()) && !Objects.equals(flag, "1")) {
                Toast.makeText(context, R.string.error_empty_response_label, Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONObject jsonResponse = new JSONObject(result);
                boolean success = jsonResponse.getBoolean("success");
                if (success) {
                    if (flag == null) Toast.makeText(context, R.string.successfully_label, Toast.LENGTH_SHORT).show();
                    JSONObject userObject = jsonResponse.getJSONObject("user");

                    String token = userObject.getString("token");
                    SharedPreferences prefs = context.getSharedPreferences("TOKEN", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("jwt_token", token);
                    editor.apply();

                    String name = userObject.getString("name");
                    String email = userObject.getString("email");
                    String image = userObject.optString("image", "");

                    USER = new BookDBManager.User(login, pass, name, email, null);
                    dbManager.insertUser(USER);

                    if ((image != null && !image.isEmpty() &&
                            image != "" && !image.equals("null") &&
                            !image.equals("[null]")) && !Objects.equals(flag, "1")) {
                        FilesManipulated.DownloadFileTask d = new FilesManipulated.DownloadFileTask();
                        d.setParams(context ,(path) ->
                            dbManager.updateUser(USER.LOGIN, null, null, null, null, path,
                                (s) -> {
                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                intent.putExtra("EXTRA_BTN", "Settings");
                                startActivity(intent);
                            })
                        );
                        d.execute(image, "images");
                    } else {
                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        intent.putExtra("EXTRA_BTN", "Settings");
                        startActivity(intent);
                    }
                } else {
                    String error = jsonResponse.optString("error");

                    if ("NO PASSWORD".equals(error))
                        Toast.makeText(context, R.string.wrong_password_label, Toast.LENGTH_SHORT).show();
                    else if ("USER NOT FOUND".equals(error))
                        Toast.makeText(context, R.string.error_login_exist_label, Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(context, R.string.error_label + error, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(context, R.string.error_processing_data_label, Toast.LENGTH_SHORT).show();
            }
        }
    }


}