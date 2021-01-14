package com.wzb.util;


import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;
import javafx.scene.control.Label;

public class SerialPortUtil {
    //保存系统所有的可用串口
    private SerialPort[] portList;
    //当前可用port
    private SerialPort serialPort;


    public double getEnergy() {
        return energy;
    }

    //当前energy
    private double energy;

    private byte[] data = new byte[20];
    private int offset = 0;
    public void init(ParamConfig paramConfig){
        //获取系统中所有的通讯端口
        portList = SerialPort.getCommPorts();

        //记录知否有指定串口
        boolean isExsit = false;

        for(SerialPort temp : portList){
            //找到指定端口
            if(temp.getSystemPortName().equals(paramConfig.getSerialName())){
                isExsit = true;
                //System.out.println(temp.getSystemPortName());
                try{
                    //设置串口参数
                    serialPort = temp;
                    //打开串口
                    serialPort.openPort();

                    serialPort.setComPortParameters(paramConfig.getBaudRate(), paramConfig.getDataBit(), paramConfig.getStopBit(), paramConfig.getCheckoutBit());
                    serialPort.setFlowControl(paramConfig.getFlowControl());

                    //添加串口监听
                    serialPort.addDataListener(new SerialPortDataListener() {
                        @Override
                        public int getListeningEvents() {
                            return SerialPort.LISTENING_EVENT_DATA_RECEIVED | SerialPort.LISTENING_EVENT_DATA_WRITTEN;
                        }

                        @Override
                        public void serialEvent(SerialPortEvent serialPortEvent) {
                            if(serialPortEvent.getEventType() == SerialPort.LISTENING_EVENT_DATA_WRITTEN){
                                System.out.println("data send");
                            }
                            if(serialPortEvent.getEventType() == SerialPort.LISTENING_EVENT_DATA_RECEIVED){
                                byte[] dataIn = serialPortEvent.getReceivedData();

                                System.out.println(dataIn.length + " bytes data once recv: " + new String(dataIn));

                                System.arraycopy(dataIn, 0, data, offset, dataIn.length);
                                offset += dataIn.length;
                                if(offset == 20){
                                    onReceived(data);
                                }
                            }
                        }
                    });

                }catch (SerialPortInvalidPortException e){
                    System.out.println("错误原因：" + e.getCause());
                    System.out.println("错误信息：" + e.getMessage());
                }
            }

        }
        if(!isExsit){
            System.out.println("不存在该串口");
        }
        if(!serialPort.isOpen()){
            System.out.println("串口未打开，请重新尝试！");
        }
    }


    private void onReceived(byte[] dataIn) {
        boolean isNeg = new String(dataIn).charAt(0) == '-' ? true : false;
        String[] angleStr = new String(dataIn).substring(1, 19).trim().split("\\.");

        int len = angleStr[3].length();
        double miao = Double.parseDouble(angleStr[2]) + Double.parseDouble(angleStr[3])/(len*10);
        double fen = Double.parseDouble(angleStr[1]);
        double du = Double.parseDouble(angleStr[0]);
        double angle = du + fen/60 + miao/3600;
        angle = (double)Math.round(angle*10000)/10000;
        System.out.println("angle:" + angle);
        energy = 1977.1/(Math.sin(Math.toRadians(angle)));
        energy = (double)Math.round(energy*100)/100;
        if(isNeg){
            energy = 0.0 - energy;
        }
        System.out.println("energy:" + energy);

        int pulse = (int)Math.toDegrees(Math.asin(1977.1/energy)) * 30000;
        System.out.println("pulse:" + pulse);

        data = new byte[20];
        offset = 0;
    }

    public void sendData(byte[] dataOut){
        serialPort.writeBytes(dataOut, dataOut.length);
        data = new byte[20];
        offset = 0;
    }


    public void closePort(){
        if(serialPort != null){
            serialPort.closePort();
        }
    }
}
