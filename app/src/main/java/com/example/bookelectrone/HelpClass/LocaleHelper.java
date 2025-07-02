package com.example.bookelectrone.HelpClass;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import androidx.preference.PreferenceManager;

import java.util.Locale;

public class LocaleHelper {
    public static Context updateLocale(Context context, String selectedLanguage) {
        Locale newLocale;

        if ("Русский".equals(selectedLanguage)) {
            newLocale = new Locale("ru");
        } else {
            newLocale = new Locale("en");
        }
        Locale.setDefault(newLocale);

        Resources res = context.getResources();
        Configuration config = res.getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(newLocale);
            context = context.createConfigurationContext(config);
        } else {
            config.locale = newLocale;
            res.updateConfiguration(config, res.getDisplayMetrics());
        }
        return context;
    }

    public static Context getContextToLanguage(Context newBase) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
        String selectedLan = prefs.getString("selected_lan", "English");
        return LocaleHelper.updateLocale(newBase, selectedLan);
    }
}

