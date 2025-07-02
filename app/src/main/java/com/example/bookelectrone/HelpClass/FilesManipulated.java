package com.example.bookelectrone.HelpClass;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.widget.Toast;

import com.example.bookelectrone.R;
import com.example.bookelectrone.db.BookDBManager;

import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FilesManipulated
{

    /**
     * Выдает имя файла из пути, когда возможности создать сам файл нету.
     * В основном вызываеться при извлечении имени файла в {@code intent}.
     */
    public static String getFileNameInPath(String filePath) {
        File file = new File(filePath);
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex != 0) {
            return fileName.substring(0, dotIndex);
        } else {
            return fileName;
        }
    }


    /**
     * Удаляет файл по заданному пути.
     * @return {@code true | false}
     */
    public static boolean deleteFileByPath(String filePath, String dir, Context context) {
        File file = new File(new File(context.getFilesDir(), dir), filePath);
        if (file.exists()) return file.delete();
        return false;
    }


    /**
     * Извлекает путь до файла из объекта {@code Uri}.
     */
    public static String getPathFromUri(Uri uri, Context context) {
        String filePath = null;

        if (DocumentsContract.isDocumentUri(context, uri)) {
            String documentId = DocumentsContract.getDocumentId(uri);
            String[] split = documentId.split(":");
            String type = split[0];

            if ("primary".equalsIgnoreCase(type)) {
                filePath = Environment.getExternalStorageDirectory() + "/" + split[1];
            } else {
                String[] projection = { MediaStore.Files.FileColumns.DATA };
                Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
                    if (cursor.moveToFirst()) {
                        filePath = cursor.getString(columnIndex);
                    }
                    cursor.close();
                }
            }
        }

        if (filePath == null) {
            String[] projection = { MediaStore.Files.FileColumns.DISPLAY_NAME };
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                String displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME));
                try {
                    InputStream inputStream = context.getContentResolver().openInputStream(uri);
                    File file = new File(context.getCacheDir(), displayName);
                    FileOutputStream outputStream = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, read);
                    }
                    inputStream.close();
                    outputStream.close();
                    filePath = file.getAbsolutePath();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                cursor.close();
            }
        }
        return filePath;
    }


    /**
     * Метод, отвечающий за копирование файла {@code книги} в приложение для дальнейшего использования.
     */
    public static String copyFileToAppDirectory(String sourcePath, Context context, String dir) {
        try {
            File sourceFile = new File(sourcePath);
            String fileName = sourceFile.getName();

            File booksDir = new File(context.getFilesDir(), dir);
            int counter = 1;
            String newFileName = fileName;
            File destinationFile = new File(booksDir, fileName);
            while (destinationFile.exists()) {
                String fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
                String extension = fileName.substring(fileName.lastIndexOf('.'));
                newFileName = fileNameWithoutExt + "_" + counter + extension;
                destinationFile = new File(booksDir, newFileName);
                counter++;
            }

            copyFile(sourceFile, destinationFile);
            return destinationFile.getName();

        } catch (IOException e) { return null; }
    }

    private static void copyFile(File sourceFile, File destinationFile) throws IOException {
        try (InputStream inputStream = new FileInputStream(sourceFile);
             OutputStream outputStream = new FileOutputStream(destinationFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
    }


    public static int ID;
    public static String PATH;

    public static final int PICK_IMAGE_COLLECTION_REQUEST_CODE = 2;
    public static final int PICK_IMAGE_BOOK_REQUEST_CODE = 3;
    public static final int PICK_IMAGE_USER_REQUEST_CODE = 4;

    /**
     * Открывает файловый менеджер для взятия файла изображения. В зависимости от {@link com.example.bookelectrone.db.BookDBManager} определяет,
     * для чего был взят файл: книги или сборника.
     * <p> Возвращает код в onActivityResult.
     */
    public static void openFileManagerImage(Activity activity, int id, String path, int flag) {
        ID = id; PATH = path;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");

        if (flag == BookDBManager.IS_BOOK)
            activity.startActivityForResult(Intent.createChooser(intent, activity.getString(R.string.select_image_label)), PICK_IMAGE_BOOK_REQUEST_CODE);
        else if (flag == BookDBManager.IS_COLLECTION)
            activity.startActivityForResult(Intent.createChooser(intent, activity.getString(R.string.select_image_label)), PICK_IMAGE_COLLECTION_REQUEST_CODE);
        else if (flag == BookDBManager.IS_USER)
            activity.startActivityForResult(Intent.createChooser(intent, activity.getString(R.string.select_image_label)), PICK_IMAGE_USER_REQUEST_CODE);
    }


    public static final int PICK_FILE_REQUEST_CODE = 1;

    /**
     * Открывает файловый менеджер для взятия файла книги. Возвращает код в onActivityResult.
     */
    public static void openFileManagerBook(Activity activity, int id) {
        ID = id;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf"});
        activity.startActivityForResult(Intent.createChooser(intent, activity.getString(R.string.select_a_book_label)), PICK_FILE_REQUEST_CODE);
    }



    public static class DownloadFileTask extends AsyncTask<String, Integer, String> {
        EndDownload listener;
        @SuppressLint("StaticFieldLeak")
        Context context;

        public void setParams(Context c, EndDownload l) {
            context = c;
            listener = l;
        }

        @Override protected String doInBackground(String... params) {
            String fileName = params[0];
            String dirName = params[1];
            try {
                URL url = new URL(NetworkBookElectrone.BASE_URL + dirName + "/" + fileName);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.connect();

                int fileLength = connection.getContentLength();
                InputStream input = connection.getInputStream();

                File Dir = new File(context.getFilesDir(), dirName);
                if (!Dir.exists()) Dir.mkdirs();
                File file = new File(Dir, fileName);
                FileOutputStream output = new FileOutputStream(file);

                byte[] data = new byte[4096];
                int count;
                int total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress(total, fileLength);
                    output.write(data, 0, count);
                }
                output.flush(); output.close(); input.close();
                return file.getName();
            } catch (Exception e) { e.printStackTrace(); }
            return null;
        }

        private ProgressDialog progressDialog;

        @Override protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(context.getString(R.string.please_wait_label));
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override protected void onProgressUpdate(Integer... values) {
            int totalBytes = values[0];
            int length = values[1];

            String percent = "0";
            if (length > 0) percent = String.valueOf((int) ((totalBytes * 100) / length));
            progressDialog.setMessage(context.getString(R.string.please_wait_label) + "\n" + percent + "%");
        }

        @Override protected void onPostExecute(String localFilePath) {
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();

            if (localFilePath != null && listener != null) {
                listener.onEnd(localFilePath);
                Toast.makeText(context, context.getString(R.string.successfully_label), Toast.LENGTH_SHORT).show();
            }
            else Toast.makeText(context, context.getString(R.string.error_label), Toast.LENGTH_SHORT).show();
        }
    }

    public interface EndDownload {
        void onEnd(String path);
    }





}
