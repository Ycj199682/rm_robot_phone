package com.reeman.phone.utils;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.reeman.phone.R;


public class ToastUtils {
    private static Toast sToast;
    private static Toast sToastIcon;
    private static Context sContext;
    private static TextView tvToastContent;
    private static TextView tvToastContentIcon;

    public static void init(Context context) {
        sContext = context;
    }

    public static void showShortToast(String content) {
        if (sToast == null) {
            sToast = new Toast(sContext);
            sToast.setDuration(Toast.LENGTH_SHORT);
            View rootToast = LayoutInflater.from(sContext).inflate(R.layout.layout_simple_toast, null);
            sToast.setView(rootToast);
            tvToastContent = rootToast.findViewById(R.id.tv_toast_content);
        }
        tvToastContent.setText(content);
        sToast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 300); // 设置Toast显示在底部居中
        sToast.show();
    }

    public static void showIconToast(String content) {
        if (sToastIcon == null) {
            sToastIcon = new Toast(sContext);
            sToastIcon.setDuration(Toast.LENGTH_SHORT);
            View rootToast = LayoutInflater.from(sContext).inflate(R.layout.layout_simple_toast_icon, null);
            sToastIcon.setView(rootToast);
            tvToastContentIcon = rootToast.findViewById(R.id.tv_toast_content_icon);
        }
        tvToastContentIcon.setText(content);
        sToastIcon.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 300); // 设置Toast显示在底部居中
        sToastIcon.show();
    }
}
