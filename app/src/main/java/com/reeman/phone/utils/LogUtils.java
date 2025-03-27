package com.reeman.phone.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.reeman.phone.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LogUtils {
    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private final Activity activity;
    private AlertDialog logFilesDialog;
    private List<String> machineNames;

    public LogUtils(Activity activity) {
        this.activity = activity;
    }

    // 检查权限
    public boolean checkPermissions() {
        int readPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        int writePermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED;
    }

    // 请求权限
    public void requestPermissions() {
        ActivityCompat.requestPermissions(activity, new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, REQUEST_CODE_PERMISSIONS);
    }

    // 处理权限请求结果
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // 初始化运行日志写入
                Timbers.w(activity, "权限获取成功，开始读取写入日志！");
                Log.d("ssaa", "权限获取成功，开始读取写入日志！");
            } else {
                Log.d("ssaa", "权限获取失败！");
                //ToastUtils.showShortToast(activity.getString(R.string.deny_permission));
            }
        }
    }

    // 显示日志文件对话框
    public void showLogFilesDialog() {
        if (machineNames == null || machineNames.isEmpty()) {
            // 如果没有机器，显示提示信息
            AlertDialog.Builder noMachinesBuilder = new AlertDialog.Builder(activity);
            noMachinesBuilder.setTitle(R.string.select_machine);
            noMachinesBuilder.setMessage(activity.getString(R.string.robot_listData));
            noMachinesBuilder.setPositiveButton(R.string.confirm, null);
            noMachinesBuilder.show();
            return;
        }
        String[] machineNamesArray = machineNames.toArray(new String[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.select_machine);
        builder.setItems(machineNamesArray, (dialog, which) -> {
            String selectedMachine = machineNamesArray[which];
            showLogsForMachine(selectedMachine);
        });
        builder.show();
    }

    // 显示指定机器的日志文件
    @SuppressLint("StringFormatInvalid")
    private void showLogsForMachine(String machineName) {
        File logDir = activity.getExternalFilesDir(null);
        final File[] logFiles = logDir.listFiles((dir, name) ->
                name.startsWith(machineName) && name.endsWith(".txt"));
        if (logFiles != null && logFiles.length > 0) {
            List<String> fileNames = new ArrayList<>();
            for (File file : logFiles) {
                fileNames.add(file.getName());
            }
            // 创建点击监听器
            LogFilesAdapter.LogFileClickListener listener = position -> {
                // 确保对话框不为空，并在点击时关闭对话框
                if (logFilesDialog != null) {
                    logFilesDialog.dismiss();
                }
                viewLogFile(logFiles[position]); // 打开对应的日志文件
            };
            // 创建日志文件列表适配器
            LogFilesAdapter adapter = new LogFilesAdapter(activity, fileNames, listener);
            ListView logFilesList = new ListView(activity);
            logFilesList.setAdapter(adapter);

            // 创建对话框构建器
            AlertDialog.Builder filesBuilder = new AlertDialog.Builder(activity, R.style.CustomDialogTheme);
            filesBuilder.setTitle(R.string.select_log_dialog);
            filesBuilder.setView(logFilesList);
            // 创建并显示对话框
            logFilesDialog = filesBuilder.create();
            logFilesDialog.show();
        } else {
            // 如果没有找到日志文件，显示提示信息
            AlertDialog.Builder noLogsBuilder = new AlertDialog.Builder(activity);
            noLogsBuilder.setTitle(R.string.not_log_dialog);
            noLogsBuilder.setMessage(activity.getString(R.string.not_find_log_dialog, machineName));
            noLogsBuilder.setPositiveButton(R.string.confirm, null);
            noLogsBuilder.show();
        }
    }

    // 查看日志文件
    private void viewLogFile(File logFile) {
        Uri logUri = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".provider", logFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(logUri, "text/plain");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(intent);
    }

    // 设置机器名称列表
    public void setMachineNames(List<String> machineNames) {
        this.machineNames = machineNames;
    }
}