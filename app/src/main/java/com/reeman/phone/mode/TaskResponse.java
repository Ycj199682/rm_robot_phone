package com.reeman.phone.mode;

public class TaskResponse {
    private int code;
    private String macAddress;
    private String result;

    public TaskResponse() {
        // 默认构造函数
    }

    public TaskResponse(int code, String macAddress, String result) {
        this.code = code;
        this.macAddress = macAddress;
        this.result = result;
    }

    // Getter 和 Setter 方法
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
