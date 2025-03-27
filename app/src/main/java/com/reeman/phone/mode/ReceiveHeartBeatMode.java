package com.reeman.phone.mode;

import java.util.List;

public class ReceiveHeartBeatMode {
    private String hostname;
    private String token;
    private String alias;
    private int level;
    private boolean lowPower;
    private int emergencyButton;
    private int chargeState;
    private boolean isNavigating;
    private boolean isElevatorMode;
    private int robotType;
    private int liftModelState;
    private boolean isLifting;
    private boolean isMapping;
    private boolean taskExecuting;
    private CurrentTask currentTask;
    private List<Task> taskList;



    // 内部类，表示当前任务
    public static class CurrentTask {
        private long createTime;
        private long startTime;
        private int taskMode;
        private String token;
        private String targetPoint;

        // 省略 getter 和 setter 方法
        // 自动生成 getter 和 setter 方法
        public long getCreateTime() {
            return createTime;
        }

        public void setCreateTime(long createTime) {
            this.createTime = createTime;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public int getTaskMode() {
            return taskMode;
        }

        public void setTaskMode(int taskMode) {
            this.taskMode = taskMode;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getTargetPoint() {
            return targetPoint;
        }

        public void setTargetPoint(String targetPoint) {
            this.targetPoint = targetPoint;
        }
    }

    // 内部类，表示任务列表中的单个任务
    public static class Task {
        private long createTime;
        private long startTime;
        private int taskMode;
        private String macAddress;
        private String targetPoint;

        // 省略 getter 和 setter 方法
        // 自动生成 getter 和 setter 方法
        public long getCreateTime() {
            return createTime;
        }

        public void setCreateTime(long createTime) {
            this.createTime = createTime;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public int getTaskMode() {
            return taskMode;
        }

        public void setTaskMode(int taskMode) {
            this.taskMode = taskMode;
        }

        public String getMacAddress() {
            return macAddress;
        }

        public void setMacAddress(String macAddress) {
            this.macAddress = macAddress;
        }

        public String getTargetPoint() {
            return targetPoint;
        }

        public void setTargetPoint(String targetPoint) {
            this.targetPoint = targetPoint;
        }
    }

    // 自动生成 getter 和 setter 方法
    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isLowPower() {
        return lowPower;
    }

    public void setLowPower(boolean lowPower) {
        this.lowPower = lowPower;
    }

    public int getEmergencyButton() {
        return emergencyButton;
    }

    public void setEmergencyButton(int emergencyButton) {
        this.emergencyButton = emergencyButton;
    }

    public int getChargeState() {
        return chargeState;
    }

    public void setChargeState(int chargeState) {
        this.chargeState = chargeState;
    }

    public boolean isNavigating() {
        return isNavigating;
    }

    public void setNavigating(boolean navigating) {
        isNavigating = navigating;
    }

    public boolean isElevatorMode() {
        return isElevatorMode;
    }

    public void setElevatorMode(boolean elevatorMode) {
        isElevatorMode = elevatorMode;
    }

    public int getRobotType() {
        return robotType;
    }

    public void setRobotType(int robotType) {
        this.robotType = robotType;
    }

    public int getLiftModelState() {
        return liftModelState;
    }

    public void setLiftModelState(int liftModelState) {
        this.liftModelState = liftModelState;
    }

    public boolean isLifting() {
        return isLifting;
    }

    public void setLifting(boolean lifting) {
        isLifting = lifting;
    }

    public boolean isMapping() {
        return isMapping;
    }

    public void setMapping(boolean mapping) {
        isMapping = mapping;
    }

    public boolean isTaskExecuting() {
        return taskExecuting;
    }

    public void setTaskExecuting(boolean taskExecuting) {
        this.taskExecuting = taskExecuting;
    }

    public CurrentTask getCurrentTask() {
        return currentTask;
    }

    public void setCurrentTask(CurrentTask currentTask) {
        this.currentTask = currentTask;
    }

    public List<Task> getTaskList() {
        return taskList;
    }

    public void setTaskList(List<Task> taskList) {
        this.taskList = taskList;
    }


}
