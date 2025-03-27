package com.reeman.phone.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

public class BlurBuilder {

    private static final float BITMAP_SCALE = 0.4f;
    private static final float BLUR_RADIUS = 5.5f;

    public static Bitmap blur(Context context, Bitmap image) {
        // 确保模糊半径在允许的范围内
        float radius = Math.min(BLUR_RADIUS, 25.0f);
        int width = Math.round(image.getWidth() * BITMAP_SCALE);
        int height = Math.round(image.getHeight() * BITMAP_SCALE);

        Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);
        RenderScript rs = RenderScript.create(context);
        try {
            ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            script.setRadius(radius);
            Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
            Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
            script.setInput(tmpIn);
            script.forEach(tmpOut);
            tmpOut.copyTo(outputBitmap);
        } finally {
            rs.destroy();
        }

        return outputBitmap;
    }
}
