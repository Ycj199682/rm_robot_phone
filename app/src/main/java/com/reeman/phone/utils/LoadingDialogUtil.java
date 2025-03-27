package com.reeman.phone.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.reeman.phone.R;


/**
 * Created by sgf
 * 对话框工具类
 */

public class LoadingDialogUtil {
    @SuppressLint("StaticFieldLeak")
    public static LoadingDialogUtil mInstance;
    private static AlertDialog dlg;
    private static Animation operatingAnim;
    @SuppressLint("StaticFieldLeak")
    private static ImageView imageView;
    private static boolean isLoading;

    private LoadingDialogUtil() {
    }

    /**
     * 加载话框
     *
     * @param context
     * @param msg
     */
    public void showLoadingDialog(Context context, String msg) {
        if (context == null || ((Activity) context).isFinishing()) return;
        if (isLoading()) return;
        dlg = new AlertDialog.Builder(context, R.style.dialogStyle).create();
        View view = LayoutInflater.from(context).inflate(R.layout.layout_dialog_loading, null);
        TextView text = view.findViewById(R.id.dialog_loading_text);
        imageView = view.findViewById(R.id.dialog_loading_img);
        text.setText(msg);
        Window window = dlg.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setWindowAnimations(R.style.dialogWindowAnim);
        }
        dlg.show();
        dlg.setCancelable(true); // 允许取消对话框
        dlg.setCanceledOnTouchOutside(false); // 允许点击外部取消对话框
        dlg.setContentView(view);

        // 监听返回按钮
        dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    closeLoadingDialog();
                    return true; // 返回true表示事件已处理
                }
                return false;
            }
        });

        operatingAnim = AnimationUtils.loadAnimation(context, R.anim.loading);
        operatingAnim.setInterpolator(new LinearInterpolator());
        openAnim();
        isLoading = true;
    }


    /**
     * 加载状态
     */
    public boolean isLoading() {
        return isLoading;
    }

    /**
     * 开始旋转
     */
    public void openAnim() {
        if (operatingAnim != null) {
            imageView.startAnimation(operatingAnim);
        }
    }

    /**
     * 停止旋转
     */
    public void closeAnim() {
        if (operatingAnim != null) {
            imageView.clearAnimation();
        }
    }

    /**
     * 关闭请求对话框
     */
    public void closeLoadingDialog() {
        if (dlg != null) {
            //closeAnim();
            try {
                dlg.dismiss();
            }catch (Throwable ignored){

            }
        }
        isLoading = false;
    }

    public interface PressCallBack {
        void onPressButton(int buttonIndex);
    }

    /**
     * 单一实例
     *
     * @return
     */
    public static LoadingDialogUtil getInstance() {
        if (mInstance == null) {
            synchronized (LoadingDialogUtil.class) {
                if (mInstance == null) {
                    mInstance = new LoadingDialogUtil();
                    return mInstance;
                }
            }
        }else {
            mInstance.closeLoadingDialog();
        }
        return mInstance;
    }
}
