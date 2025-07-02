package com.example.bookelectrone;

import android.app.Activity;
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
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.example.bookelectrone.HelpClass.PermissionsHelp;
import com.example.bookelectrone.HelpClass.StyleTemplates;
import com.example.bookelectrone.db.BookDBManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class UserInfoActivity extends AppCompatActivity {

    Context context = this;

    public BookDBManager dbManager;

    public BookDBManager.User USER;
    public void setUSER() { USER = dbManager.getUser(); }


    @Override protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
        String selectedLan = prefs.getString("selected_lan", "English");
        Context context = LocaleHelper.updateLocale(newBase, selectedLan);
        super.attachBaseContext(context);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_info);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbManager = new BookDBManager(this);

        // delete margin
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), new OnApplyWindowInsetsListener() {
            @NonNull
            @Override public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                params.bottomMargin = insets.getSystemWindowInsetBottom();
                v.setLayoutParams(params);
                return insets.consumeSystemWindowInsets();
            }
        });


        // back in activity
        findViewById(R.id.backButton).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(UserInfoActivity.this, MainActivity.class);
                intent.putExtra("EXTRA_BTN", "Settings");
                startActivity(intent);
            }
        });


        // hide | show password
        EditText editPassword = findViewById(R.id.edit_password);
        ((CheckBox) findViewById(R.id.password_show)).setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
                editPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            else
                editPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            editPassword.setSelection(editPassword.getText().length());
        });


        // delete account
        findViewById(R.id.button_delete_account).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!App.isConnected(context)) return;

                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setLayoutParams(StyleTemplates.getTextEditOnDialogStyle());

                LinearLayout layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.addView(input);

                new AlertDialog.Builder(UserInfoActivity.this)
                        .setTitle(R.string.delete_account_label)
                        .setMessage(R.string.delete_account_question_label)
                        .setPositiveButton(R.string.delete_label, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String pass = input.getText().toString().trim();
                                if (!pass.isEmpty() && pass.equals(USER.PASSWORD))
                                    new DeleteTask().execute(pass);
                                else if (pass.equals(USER.PASSWORD))
                                    Toast.makeText(context, R.string.wrong_password_label, Toast.LENGTH_SHORT).show();
                                else
                                    Toast.makeText(context, R.string.error_empty_field_label, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(R.string.cancel_label, null)
                        .setView(layout)
                        .show();
            }
        });


        // logout user
        findViewById(R.id.button_logout).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!App.isConnected(context)) return;

                new AlertDialog.Builder(UserInfoActivity.this)
                        .setTitle(R.string.logout_label)
                        .setMessage(R.string.logout_question_label)
                        .setPositiveButton(R.string.logout_label, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(context, R.string.you_logout_label, Toast.LENGTH_SHORT).show();

                                dbManager.deleteUser(USER.LOGIN);
                                if (USER.IMAGE != null) FilesManipulated.deleteFileByPath(USER.IMAGE, "images", context);

                                Intent intent = new Intent(UserInfoActivity.this, MainActivity.class);
                                intent.putExtra("EXTRA_BTN", "Settings");
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton(R.string.cancel_label, null)
                        .show();
            }
        });


        // update user
        findViewById(R.id.button_save).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!App.isConnected(context)) return;

                String name = ((EditText) findViewById(R.id.edit_name)).getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(context, R.string.enter_correct_name_label, Toast.LENGTH_SHORT).show();
                    return;
                }
                String email = ((EditText) findViewById(R.id.edit_email)).getText().toString().trim();
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(context, R.string.enter_correct_email_label, Toast.LENGTH_SHORT).show();
                    return;
                }

                new UpdateTask().execute(USER.LOGIN, name, email);
            }
        });


        // update image
        findViewById(R.id.button_image).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (PermissionsHelp.checkPermissions(context))
                    FilesManipulated.openFileManagerImage((Activity) context, -1, USER.IMAGE, BookDBManager.IS_USER);
                else
                    PermissionsHelp.requestPermissions((Activity) context, context);
            }
        });


        // theme font and color
        StyleTemplates.appColorTheme(this);
        StyleTemplates.applyFontToAllTextViews((ViewGroup) findViewById(R.id.main), this);
    }

    @Override protected void onResume() {
        super.onResume();
        dbManager.openDB();
        setUSER();
        setInformationOnUser();
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        dbManager.closeDB();
    }


    private void setInformationOnUser() {
        ((EditText) findViewById(R.id.edit_login)).setText(USER.LOGIN);
        ((EditText) findViewById(R.id.edit_password)).setText(USER.PASSWORD);
        ((EditText) findViewById(R.id.edit_name)).setText(USER.NAME);
        ((EditText) findViewById(R.id.edit_email)).setText(USER.EMAIL);

        if (USER.IMAGE == null) ((ImageView) findViewById(R.id.image_user)).setImageResource(R.drawable.user_icon_24);
        else ((ImageView) findViewById(R.id.image_user)).setImageBitmap(
            new BitmapDrawable(String.valueOf(
            new File(
            new File(context.getFilesDir(), "images"), USER.IMAGE))).getBitmap());
    }


    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FilesManipulated.PICK_IMAGE_USER_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            String imagePath = FilesManipulated.getPathFromUri( data.getData(), this);
            String savedImagePath = FilesManipulated.copyFileToAppDirectory(imagePath, this, "images");

            if (savedImagePath == null || savedImagePath.isEmpty()) {
                Toast.makeText(context, R.string.error_label, Toast.LENGTH_SHORT).show();
                return;
            }

            new UpdateImageTask().execute(USER.LOGIN, savedImagePath);
        }
    }


    private class DeleteTask extends AsyncTask<String, Void, String> {
        @Override protected String doInBackground(String... params) {
            String pass  = params[0];

            HttpURLConnection connection = null;
            try {
                URL url = new URL(NetworkBookElectrone.BASE_URL + "delete_user.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                SharedPreferences prefs = context.getSharedPreferences("TOKEN", Context.MODE_PRIVATE);
                String token = prefs.getString("jwt_token", "");
                if (!token.isEmpty())
                    connection.setRequestProperty("Authorization", "Bearer " + token);


                Uri.Builder builder = new Uri.Builder()
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

            if (result == null || result.trim().isEmpty()) {
                Toast.makeText(context, R.string.error_empty_response_label, Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONObject jsonResponse = new JSONObject(result);
                boolean success = jsonResponse.getBoolean("success");
                if (success) {
                    Toast.makeText(context, R.string.successfully_label, Toast.LENGTH_SHORT).show();

                    dbManager.deleteUser(USER.LOGIN);
                    if (USER.IMAGE != null) FilesManipulated.deleteFileByPath(USER.IMAGE, "images", context);

                    Intent intent = new Intent(UserInfoActivity.this, MainActivity.class);
                    intent.putExtra("EXTRA_BTN", "Settings");
                    startActivity(intent);

                } else {
                    String error = jsonResponse.optString("error");

                    if ("NO PASSWORD".equals(error))
                        Toast.makeText(context, R.string.wrong_password_label, Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(context, getString(R.string.error_label) + error, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(context, R.string.error_processing_data_label, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class UpdateTask extends AsyncTask<String, Void, String> {
        private String login;
        private String name;
        private String email;

        @Override protected String doInBackground(String... params) {
            login = params[0];
            name  = params[1];
            email  = params[2];

            HttpURLConnection connection = null;
            try {
                URL url = new URL(NetworkBookElectrone.BASE_URL + "update_user.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                SharedPreferences prefs = context.getSharedPreferences("TOKEN", Context.MODE_PRIVATE);
                String token = prefs.getString("jwt_token", "");
                if (!token.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("login", login)
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
                    dbManager.updateUser(login, null, null, name, email, USER.IMAGE, new BookDBManager.EndCallback() {
                        @Override public void onEndCompleted(int success) {
                            Toast.makeText(context, R.string.successfully_label, Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(UserInfoActivity.this, MainActivity.class);
                            intent.putExtra("EXTRA_BTN", "Settings");
                            startActivity(intent);
                        }
                    });

                    String newToken = jsonResponse.getString("token");
                    SharedPreferences prefs = context.getSharedPreferences("TOKEN", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("jwt_token", newToken);
                    editor.apply();

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

    private class UpdateImageTask extends AsyncTask<String, Void, String> {
        private static final String LINE_FEED = "\r\n";
        private String login;
        private String image;

        @Override protected String doInBackground(String... params) {
            login = params[0];
            image = params[1];

            String boundary = "===" + System.currentTimeMillis() + "===";
            String charset = "UTF-8";
            HttpURLConnection httpConn = null;
            DataOutputStream request = null;
            String responseString = null;

            try {
                URL url = new URL(NetworkBookElectrone.BASE_URL + "upload_image.php");
                httpConn = (HttpURLConnection) url.openConnection();
                httpConn.setUseCaches(false);
                httpConn.setDoOutput(true);
                httpConn.setDoInput(true);
                httpConn.setRequestMethod("POST");
                httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                httpConn.setRequestProperty("Connection", "Keep-Alive");

                SharedPreferences prefs = context.getSharedPreferences("TOKEN", Context.MODE_PRIVATE);
                String token = prefs.getString("jwt_token", "");
                if (!token.isEmpty()) {
                    httpConn.setRequestProperty("Authorization", "Bearer " + token);
                }

                request = new DataOutputStream(httpConn.getOutputStream());

                addFormField(request, "login", login, boundary, charset);
                addFilePart(request, "image", new File(image), boundary, charset);

                request.writeBytes("--" + boundary + "--" + LINE_FEED);
                request.flush(); request.close();


                int responseCode = httpConn.getResponseCode();
                StringBuilder sb = new StringBuilder();
                BufferedReader reader;
                if (responseCode == HttpURLConnection.HTTP_OK)
                    reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
                else
                    reader = new BufferedReader(new InputStreamReader(httpConn.getErrorStream()));
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                responseString = sb.toString();
            }
            catch (Exception e) { e.printStackTrace(); } finally { if (httpConn != null) httpConn.disconnect(); }
            return responseString;
        }

        private void addFormField(DataOutputStream request, String name, String value, String boundary, String charset) throws IOException {
            request.writeBytes("--" + boundary + LINE_FEED);
            request.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"" + LINE_FEED);
            request.writeBytes("Content-Type: text/plain; charset=" + charset + LINE_FEED);
            request.writeBytes(LINE_FEED);
            request.writeBytes(value + LINE_FEED);
        }

        private void addFilePart(DataOutputStream request, String fieldName, File uploadFile, String boundary, String charset) throws IOException {
            String fileName = uploadFile.getName();
            request.writeBytes("--" + boundary + LINE_FEED);
            request.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"" + LINE_FEED);
            request.writeBytes("Content-Type: " + URLConnection.guessContentTypeFromName(fileName) + LINE_FEED);
            request.writeBytes("Content-Transfer-Encoding: binary" + LINE_FEED);
            request.writeBytes(LINE_FEED);

            FileInputStream inputStream = new FileInputStream(uploadFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                request.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            request.writeBytes(LINE_FEED);
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
                    if (FilesManipulated.PATH != null && FilesManipulated.PATH.isEmpty())
                        FilesManipulated.deleteFileByPath(FilesManipulated.PATH, "images", context);

                    dbManager.updateUser(USER.LOGIN,
                            null, null, null, null, (new File(image)).getName(), (s) -> onResume());
                    Toast.makeText(context, R.string.successfully_label, Toast.LENGTH_SHORT).show();
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