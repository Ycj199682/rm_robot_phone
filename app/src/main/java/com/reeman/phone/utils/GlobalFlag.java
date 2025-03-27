package com.reeman.phone.utils;

public class GlobalFlag {

    private static GlobalFlag instance;
    private boolean flag = false; // 初始值为 false
    private boolean isElevatorMode = false; // 是否为电梯模式


    public void setElevatorMode(boolean elevatorMode) {
        isElevatorMode = elevatorMode;
    }
    public boolean getElevatorMode() {
        return isElevatorMode;
    }

    // 私有构造函数，确保只能通过getInstance方法获取实例
    private GlobalFlag() {
    }

    // 获取单例实例的方法
    public static GlobalFlag getInstance() {
        if (instance == null) {
            synchronized (GlobalFlag.class) {
                if (instance == null) {
                    instance = new GlobalFlag();
                }
            }
        }
        return instance;
    }

    // 设置标志的方法
    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    // 获取标志的方法
    public boolean getFlag() {
        return flag;
    }
}
