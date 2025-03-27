package com.reeman.phone.utils;

import android.util.Base64;
import android.util.Log;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private static byte[] getKeyBytes(String key) {
        byte[] bytes = key.getBytes();
        return bytes.length == 16 ? bytes : Arrays.copyOf(bytes, 16);
    }
    public static byte[] decrypt(String key, byte[] crypted) throws GeneralSecurityException {
        byte[] keyBytes = getKeyBytes(key);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, ALGORITHM), new IvParameterSpec(keyBytes));
        return cipher.doFinal(crypted);
    }

    public static String decrypt(String key, String val) {
        try {
            byte[] crypted = Base64.decode(val, Base64.DEFAULT);
            byte[] origData = decrypt(key, crypted);
            return new String(origData);
        } catch (Exception e) {
            Log.d("Mainprocesses","AES解密失败:");
            return val;
        }
    }


    public static byte[] encrypt(String key,byte[] origData) throws GeneralSecurityException {
        byte[] keyBytes = getKeyBytes(key);
        byte[] buf = new byte[16];
        System.arraycopy(keyBytes, 0, buf, 0, Math.max(keyBytes.length, buf.length));
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(buf, "AES"), new IvParameterSpec(keyBytes));
        return cipher.doFinal(origData);

    }
    public static String encrypt(String key, String val) throws GeneralSecurityException {
        byte[] origData = val.getBytes();
        byte[] crypted = encrypt(key,origData);
        return Base64.encodeToString(crypted,Base64.NO_WRAP);
    }
}
