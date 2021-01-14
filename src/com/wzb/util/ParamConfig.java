package com.wzb.util;

public class ParamConfig {
    private String serialName;
    private int baudRate;
    private int checkoutBit;
    private int dataBit;
    private int stopBit;

    public int getFlowControl() {
        return flowControl;
    }

    public void setFlowControl(int flowControl) {
        this.flowControl = flowControl;
    }

    private int flowControl;

    public ParamConfig(){ }

    /**
     * @param serialName  串口号
     * @param baudRate      波特率
     * @param dataBit       数据位
     * @param checkoutBit   校验位
     * @param stopBit       停止位
     * @param flowControl   流控
     */
    public ParamConfig(String serialName, int baudRate, int dataBit, int checkoutBit, int stopBit, int flowControl){
        this.serialName = serialName;
        this.baudRate = baudRate;
        this.dataBit = dataBit;
        this.checkoutBit = checkoutBit;
        this.stopBit = stopBit;
        this.flowControl = flowControl;
    }
    public String getSerialName() {
        return serialName;
    }

    public void setSerialName(String serialName) {
        this.serialName = serialName;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    public int getCheckoutBit() {
        return checkoutBit;
    }

    public void setCheckoutBit(int checkoutBit) {
        this.checkoutBit = checkoutBit;
    }

    public int getDataBit() {
        return dataBit;
    }

    public void setDataBit(int dataBit) {
        this.dataBit = dataBit;
    }

    public int getStopBit() {
        return stopBit;
    }

    public void setStopBit(int stopBit) {
        this.stopBit = stopBit;
    }

}
