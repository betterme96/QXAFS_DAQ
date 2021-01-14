package com.wzb.service;

import com.wzb.interfaces.ReadOut;
import com.wzb.models.Information;
import com.wzb.util.FileOp;
import com.wzb.util.RingBuffer;
import com.wzb.util.Time;
import com.wzb.util.WriteLog;
import javafx.collections.ObservableList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class QXAFSReadOut implements Runnable{
    private RingBuffer ringBuffer;
    private InputStream dataIn;

    private FileOutputStream rawDataFile;//保存原始二进制数据的文件
    private WriteLog runLogFile;//保存run信息的log文件
    private ObservableList<Information> tableData;//tableView信息填充

    public volatile boolean exit = false;
    public volatile boolean isError = false;
    public volatile boolean over = false;

    public QXAFSReadOut(Socket dataSocket, RingBuffer ringBuffer) throws IOException {
        this.ringBuffer = ringBuffer;
        this.dataIn = dataSocket.getInputStream();
    }

    public void setRawDataFile(String fileName) throws IOException {
        this.rawDataFile = FileOp.createFileOutputStream(fileName+".dat");
    }

    public void setLogFile(WriteLog runLogFile){
        this.runLogFile = runLogFile;
    }

    public void setTableData(ObservableList<Information> tableData){
        this.tableData = tableData;
    }

    @Override
    public void run(){
        try {
            runLogFile.writeContent(Time.getCurTime());
            runLogFile.writeContent("   Readout module start work\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //tableData.add(new Information(Time.getCurTime(), "INFO", "Readout module working....." ));

        System.out.println("Readout module working......");

        //start命令的回包，丢掉
        byte[] start = new byte[8];
        try {
            dataIn.read(start);
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] data = new byte[16000000];
        boolean first = true;
        int len = 0;
        while (!exit){
            try {
                if((len = dataIn.read(data, 0, data.length)) != -1){
                    //写文件
                    rawDataFile.write(data, 0, len);
                    //写ringbuffer
                    int wirte = ringBuffer.write(data, 0, len, "ReadOut");

                    //写超时，数据读出部分阻塞，进行提示
                    if(wirte == -1){
                        System.out.println("ReadOut data block!");
                    }
                }
            } catch (SocketTimeoutException e) {
                if(first){
                    tableData.add(new Information(Time.getCurTime(), "INFO", "Readout module ：Socket RunTime out"));
                    first = false;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //将文件输出流缓冲区的数据强制写到文件中
        try {
            rawDataFile.flush();
            rawDataFile.close();
            runLogFile.writeContent(Time.getCurTime());
            runLogFile.writeContent("   Readout module stop work\n");
            //tableData.add(new Information(Time.getCurTime(), "INFO", "Readout module stop work"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        over = true;
    }
}
