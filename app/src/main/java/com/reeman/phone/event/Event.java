package com.reeman.phone.event;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.reeman.phone.R;
import com.reeman.phone.mode.ReceiveHeartBeatMode;
import com.reeman.phone.utils.AESUtil;
import com.reeman.phone.utils.Timbers;
import com.reeman.phone.utils.ToastUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import timber.log.Timber;

public class Event {
    public static final OnHeartBeatEvent onHeartBeatEvent = new OnHeartBeatEvent();
    public static final OnMqttPayloadEvent onMqttPayloadEvent = new OnMqttPayloadEvent();
    public static final OnCallingEvent onCallingEvent = new OnCallingEvent();
    public static final OnNormalEvent onNormalEvent = new OnNormalEvent();
    public static final OnRouteEvent onRouteEvent = new OnRouteEvent();
    public static final OnQrcodeEvent onQrcodeEvent = new OnQrcodeEvent();
    public static final OnTaskResponse onTaskResponse = new OnTaskResponse();
    public static final OnResponseHeartBeat onResponseHeartBeat = new OnResponseHeartBeat();
    public static final OnReconnectSuccess onReconnectSuccess = new OnReconnectSuccess();

    //心跳
    public static OnHeartBeatEvent getOnHeartBeatEvent(String MacAddress) {
        onHeartBeatEvent.MacAddress = MacAddress;
        return onHeartBeatEvent;
    }
    //mqtt连接
    public static OnMqttPayloadEvent getOnMqttPayloadEvent(String topic,String payload){
        onMqttPayloadEvent.payload = payload;
        onMqttPayloadEvent.topic = topic;
        return onMqttPayloadEvent;
    }

    //呼叫模式点位
    public static OnCallingEvent getOnCallingEvent(String payload,String encryptKey) {
        try {
            JSONObject rootObject = new JSONObject(payload);
            // 设置初始解析的参数
            onCallingEvent.token = rootObject.getString("token");
            onCallingEvent.code = rootObject.getInt("code");
            onCallingEvent.body = rootObject.getString("body");
            if(onCallingEvent.code==0){
                String bodyDataCall = AESUtil.decrypt(encryptKey, onCallingEvent.body);
                JSONObject bodyObject = new JSONObject(bodyDataCall);
                onCallingEvent.elevatorModeSwitch = bodyObject.getBoolean("elevatorModeSwitch");
                onCallingEvent.model = bodyObject.getString("model");
                onCallingEvent.abnormalState=false;
            }else {
                onCallingEvent.body=RemoveSuffix(onCallingEvent.body);
            }
        } catch (Exception e) {
            onCallingEvent.abnormalState=true;
            e.printStackTrace();
        }
        return onCallingEvent;
    }

    //自动模式点位
    public static OnNormalEvent getOnNormalEvent(String payload,String encryptKey){
        try {
            JSONObject rootObject = new JSONObject(payload);
            onNormalEvent.token = rootObject.getString("token");
            onNormalEvent.code = rootObject.getInt("code");
            onNormalEvent.body = rootObject.getString("body");
            if(onNormalEvent.code==0){
                String bodyDataAuto =AESUtil.decrypt(encryptKey, onNormalEvent.body);
                JSONObject bodyObject = new JSONObject(bodyDataAuto);
                onNormalEvent.elevatorModeSwitch = bodyObject.getBoolean("elevatorModeSwitch");
                onNormalEvent.model = bodyObject.getString("model");
            }else {
                onNormalEvent.body=RemoveSuffix(onNormalEvent.body);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return onNormalEvent;
    }

    //手动模式点位
    public static OnQrcodeEvent getOnQrcodeEvent(String payload,String encryptKey){
        try {

            JSONObject rootObject = new JSONObject(payload);
            onQrcodeEvent.token = rootObject.getString("token");
            onQrcodeEvent.code = rootObject.getInt("code");
            onQrcodeEvent.body = rootObject.getString("body");
            if(onQrcodeEvent.code==0){
                String bodyDataModel =AESUtil.decrypt(encryptKey, onQrcodeEvent.body);
                JSONObject bodyObject = new JSONObject(bodyDataModel);
                onQrcodeEvent.elevatorModeSwitch = bodyObject.getBoolean("elevatorModeSwitch");
                onQrcodeEvent.model = bodyObject.getString("model");
            }else {
                onQrcodeEvent.body=RemoveSuffix(onQrcodeEvent.body);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return onQrcodeEvent;
    }

    //本地任务列表
    public static OnRouteEvent getOnRouteEvent(String payload,String encryptKey){
        try {

            JSONObject rootObject = new JSONObject(payload);
            onRouteEvent.token = rootObject.getString("token");
            onRouteEvent.code = rootObject.getInt("code");
            onRouteEvent.body = rootObject.getString("body");
            if(onRouteEvent.code==0){
                onRouteEvent.body =AESUtil.decrypt(encryptKey, onRouteEvent.body);
            }else {
                onRouteEvent.body=RemoveSuffix(onRouteEvent.body);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return onRouteEvent;
    }

    //叉车和AGV的任务执行响应
    public static OnTaskResponse getOnTaskResponse(String payload,String encryptKey){
        try {
            JSONObject rootObject = new JSONObject(payload);
            onTaskResponse.token = rootObject.getString("token");
            onTaskResponse.code = rootObject.getInt("code");
            onTaskResponse.body = rootObject.getString("body");
            onTaskResponse.body =AESUtil.decrypt(encryptKey, onTaskResponse.body);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return onTaskResponse;
    }

    //接收心跳
    public static OnResponseHeartBeat getOnResponseHeartBeat(String payload,String encryptKey){
        onResponseHeartBeat.payload =payload;
        return onResponseHeartBeat;
    }
    //恢复心跳
    public static OnReconnectSuccess getOnReconnectSuccess(String payload){
        onReconnectSuccess.payload = payload;
        return onReconnectSuccess;
    }

    private static String  RemoveSuffix(String body){
        if (body.indexOf(':') != -1) {
            String result = body.substring(body.indexOf(':') + 1).trim();
           return result;
        } else {
            return body;
        }
    }


    public static class OnHeartBeatEvent {
        public String MacAddress;
    }
    public static class OnMqttPayloadEvent {
        public String topic;
        public String payload;
    }

    public static class OnCallingEvent{
        public String payload;
        public String token;
        public int code;
        public String body;
        public boolean elevatorModeSwitch;
        public String model;
        public boolean abnormalState=false;
    }
    public static class OnNormalEvent{
        public String payload;
        public String token;
        public int code;
        public String body;
        public boolean elevatorModeSwitch;
        public String model;
    }
    public static class OnRouteEvent{
        public String payload;
        public String token;
        public int code;
        public String body;
        public String model;;
    }
    public static class OnQrcodeEvent{
        public String payload;
        public String token;
        public int code;
        public boolean elevatorModeSwitch;
        public String body;
        public String model;
    }

    public static class OnTaskResponse{
        public String payload;
        public String token;
        public int code;
        public String body;
    }

    public static class OnResponseHeartBeat{
        public String payload;
    }
    public static class OnReconnectSuccess{
        public String payload;
    }

}
