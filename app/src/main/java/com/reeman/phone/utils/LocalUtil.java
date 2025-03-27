package com.reeman.phone.utils;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LocalUtil {
    public static List<String> getLocal() {
        List<String> languageList = new ArrayList<>(Arrays.asList(
                "English",
                "中文(简体)",
                "日本語",
                "한국인",
                "Español",
                "ภาษาไทย",
                "粤语",
                "Nederlands",
                "Français",
                "Deutsch",
                "بالعربية",
                "中文(繁體)",
                "по-русски",
                "Tiếng Việt",
                "Magyar"
        ));

        return languageList;
    }

    public static Locale getLocalName(int index) {
        Locale locale = null;
        switch (index) {
            case 0:
                locale = Locale.ENGLISH;
                break;
            case 1:
                locale = Locale.CHINA;
                break;
            case 2:
                locale = Locale.JAPAN;
                break;
            case 3:
                locale = Locale.KOREA;
                break;
            case 4:
                locale = new Locale.Builder()
                        .setLanguage("es")
                        .build();
                break;
            case 5:
                locale = new Locale.Builder()
                        .setLanguage("th")
                        .build();
                break;
            case 6:
                locale = new Locale.Builder()
                        .setLanguage("zh")
                        .setRegion("HK")
                        .build();
                break;
            case 7:
                locale = new Locale.Builder()
                        .setLanguage("nl")
                        .build();
                break;
            case 8:
                locale = Locale.FRENCH;
                break;
            case 9:
                locale = new Locale.Builder()
                        .setLanguage("de")
                        .build();
                break;
                case 10:
                    locale = new Locale.Builder()
                        .setLanguage("ar")
                            .build();
                    break;
            case 11:
                locale = new Locale.Builder()
                        .setLanguage("zh")
                        .setRegion("TW")
                        .build();
                break;
            case 12:
                locale = new Locale.Builder()
                        .setLanguage("ru")
                        .build();
                break;
            case 13:
                locale = new Locale.Builder()
                        .setLanguage("vi")
                        .build();
                break;
            case 14:
                locale = new Locale.Builder()
                        .setLanguage("hu") // 匈牙利语
                        .build();
                break;
            default:
                locale = Locale.ENGLISH;
                break;
        }
        return locale;
    }
    public static void changeAppLanguage(Resources resources, Locale locale) {
        Configuration configuration = resources.getConfiguration();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        configuration.locale = locale;
        resources.updateConfiguration(configuration, displayMetrics);
    }
}
