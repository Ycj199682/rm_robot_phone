package com.reeman.phone.utils;

import static android.content.Context.MODE_PRIVATE;
import static com.reeman.phone.constant.Constants.FRONT_DEVICE_NAME;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Timbers {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final int MAX_LOG_FILES = 7;
    public static final String LOG_FILE_SUFFIX = ".txt";
    private static final String PREF_NAME = "log_device_name";
    private static final String DEVICE_NAME_KEY = "device_name_key";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    static int i = 0;

    public static void w(Context context, String log) {
        executor.execute(() -> {
            try {
                SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                Set<String> deviceNameSet = getDeviceNameSet(preferences);

                if (FRONT_DEVICE_NAME != null && !FRONT_DEVICE_NAME.isEmpty()) {
                    deviceNameSet.add(FRONT_DEVICE_NAME);
                    saveDeviceNameSet(preferences, deviceNameSet);
                    saveLog(context, FRONT_DEVICE_NAME, log);
                } else {
                    for (String deviceName : deviceNameSet) {
                        saveLog(context, deviceName, log);
                    }
                    i = 0;
                }
                cleanOldLogs(context);
            } catch (IOException e) {
                Log.e("Timbers", "写入日志时出错: " + e.getMessage());
            }
        });
    }

    private static void saveLog(Context context, String deviceName, String log) throws IOException {
        String date = DATE_FORMAT.format(new Date());
        File logFile = new File(context.getExternalFilesDir(null), deviceName + date + LOG_FILE_SUFFIX);
        String timestampedLog = getCurrentTimeStamp() + " ------- " + log + "\n";
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.append(timestampedLog);
        }
    }

    private static String getCurrentTimeStamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    private static void cleanOldLogs(Context context) {
        File logDir = context.getExternalFilesDir(null);
        File[] files = logDir.listFiles((dir, name) -> name.endsWith(LOG_FILE_SUFFIX));
        if (files != null && files.length > MAX_LOG_FILES) {
            // 按文件的最后修改时间排序
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));
            // 删除多余的文件
            for (int i = 0; i < files.length - MAX_LOG_FILES; i++) {
                if (!files[i].delete()) {
                    Log.e("Timbers", "(日志文件只保留7天的数据)无法删除日志文件: " + files[i].getName());
                }
            }
        }
    }

    private static Set<String> getDeviceNameSet(SharedPreferences preferences) {
        String jsonSet = preferences.getString(DEVICE_NAME_KEY, null);
        if (jsonSet == null) {
            return new HashSet<>();
        }
        Type type = new TypeToken<Set<String>>() {}.getType();
        return new Gson().fromJson(jsonSet, type);
    }

    private static void saveDeviceNameSet(SharedPreferences preferences, Set<String> deviceNameSet) {
        String jsonSet = new Gson().toJson(deviceNameSet);
        preferences.edit().putString(DEVICE_NAME_KEY, jsonSet).apply();
    }
}
