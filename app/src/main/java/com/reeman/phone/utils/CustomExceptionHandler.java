package com.reeman.phone.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.reeman.phone.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CustomExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Context context;
    private static String positionName;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    public CustomExceptionHandler(Context context,String PositionName) {
        this.context = context;
        this.positionName = PositionName;
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // 记录崩溃日志
        logCrash(throwable);

        // 如果有默认的异常处理器，则调用它
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        } else {
            // 否则终止应用程序
            System.exit(2);
        }
    }
    private static String getCurrentTimeStamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        return dateFormat.format(new Date());
    }
    private void logCrash(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        Timbers.w(context, "程序崩溃：" + throwable.toString());
        Timbers.w(context, "详细堆栈信息：" + stackTrace);
        appendToFile(context, "程序崩溃：" + throwable.toString(),"crashs_log.txt");
        appendToFile(context, "详细堆栈信息：" + stackTrace,"crashs_log.txt");
    }

    private static final String TAG = "FileHelper";
    /**
     * 将指定的内容写入到指定的文件中。
     * @param context 应用上下文
     * @param content 要写入的内容
     * @param fileName 文件名
     */
    public static void appendToFile(Context context, String content, String fileName) {
        try {
            File dir = context.getFilesDir(); // 获取应用内部文件目录
            File file = new File(dir, fileName);
            // 创建文件，如果它不存在
            if (!file.exists()) {
                boolean created = file.createNewFile();
                if (!created) {
                    return;
                }
            }
            String timestampedLog = positionName+"-"+getCurrentTimeStamp() + " ---- " + content + "\n";

            FileWriter writer = new FileWriter(file, true); // 追加模式
            writer.write(timestampedLog);
            writer.close();
        } catch (IOException e) {
            Log.d("ssaa", "Error appending to " + fileName, e);
        }
    }
}
