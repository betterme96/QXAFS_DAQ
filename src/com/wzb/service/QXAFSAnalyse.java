package com.wzb.service;

import com.wzb.models.Information;
import com.wzb.util.*;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class QXAFSAnalyse implements Runnable {
    private RingBuffer preBuffer;
    private RingBuffer nextBuffer;
    private String fileName;
    private PrintWriter computeDataFile;//保存计算结果的数据文件


    private WriteLog runLogFile;//保存run信息的log文件
    private ObservableList<Information> tableData;//tableView信息填充

    private int fileNo = 1;
    private int freq = 0;//抽样频率
    private int count = 0;//数据压缩比值
    private int start = 0;
    private int end = 0;
    private int ch1 = 0;
    private int ch2 = 1;

    private List<List<Integer>> adcList = new ArrayList<>();//存放四路ADC数据
    private List<double[]> nodes;

    public volatile boolean isError = false;
    public volatile boolean over = false;
    public volatile boolean exit = false;
    public volatile int tang;
    public volatile int removeSize = 0;

    public QXAFSAnalyse(RingBuffer preBuffer, RingBuffer nextBuffer){
        this.preBuffer = preBuffer;
        this.nextBuffer = nextBuffer;
     }

    public void setParam(int start, int end, int count, int freq, int ch1, int ch2, List<double[]> nodes){
        this.start = start;
        this.end = end;
        this.count = count;
        this.freq = freq;
        this.ch1 = ch1;
        this.ch2 = ch2;
        this.nodes = nodes;
    }

    public void setComputeDataFile(String fileName) throws IOException {
        this.fileName = fileName;
        this.computeDataFile = new PrintWriter(FileOp.createFileOutputStream(fileName+"_"+fileNo + ".txt"));
    }

    public void setLogFile(WriteLog runLogFile){
        this.runLogFile = runLogFile;
    }

    public void setTableData(ObservableList<Information> tableData){
        this.tableData = tableData;
    }
    @Override
    public void run() {
        try {
            runLogFile.writeContent(Time.getCurTime());
            runLogFile.writeContent("   Analyse module start work\n");
            //tableData.add(new Information(Time.getCurTime(), "INFO", "Analyse module working....." ));
            System.out.println("Analyse module working......");
            //checkNotStore();
            check();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //uncheck();
    }
    private void checkNotStore() {
        int predirect = 0;
        int direct = 0;
        int pulse = 0;
        int channel = 0;
        int adc = 0;

        byte[] curData = new byte[4];//用于从preBuffer中读取数据

        int pointCount = 0;//读取的数据个数
        double i0 = 0, i1 = 0, res = 0;
        int adcCount = 0;
        int time = 0;
        int minCh = Integer.MAX_VALUE;
        try {
            while(!exit && preBuffer.read(curData, 0, 4, "Analyse Module") !=-1 ){
                if(curData[0] == (byte) 0xff){
                    if(i0 > 0 && i1 > 0){
                        if(pointCount == 0 || pointCount % freq == 0){
                            i0 = i0/(adcCount * count);
                            i1 = i1/(adcCount * count);
                            res = Math.log(i0/i1);
                            nodes.add(new double[]{time, res});
                            time++;
                        }
                        i0 = 0;
                        i1 = 0;
                    }
                    pointCount++;
                }else{
                    channel = curData[0] & 0xf;

                    adc = ((curData[1] & 0xff) << 16) | ((curData[2] & 0xff) << 8) | (curData[3] & 0xff);
                    //System.out.println("channel : " + channel);
                    if(channel < 1 || channel > 4){
                        isError = true;
                        tableData.add(new Information(Time.getCurTime(), "ERROR", "Data Recv Wrong!!"));
                        //return;
                    }
                    minCh = Math.min(minCh, channel);
                    if(channel == minCh){
                        i0 += adc;
                    }else{
                        i1 += adc;
                        adcCount ++;
                    }
                }
            }
            System.out.println("count:" + pointCount);
            runLogFile.writeContent(Time.getCurTime());
            runLogFile.writeContent("   Analyse module stop work\n");
            tableData.add(new Information(Time.getCurTime(), "INFO", "Analyse module stop work"));
        } catch (InterruptedException e) {
            e.printStackTrace();
            isError = true;
        } catch (IOException e) {
            e.printStackTrace();
            isError = true;
        }finally {
            over = true;
        }
    }


    private void uncheck() {
        byte[] data = new byte[5000000];
        try{
            while (preBuffer.read(data, 0, data.length, "Analyse Module") != -1){
                nextBuffer.write(data, 0, data.length, "Analyse Module");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void check() {
            int predirect = 0;
            int direct = 0;
            int pulse = 0;
            int channel = 0;
            int adc = 0;

            byte[] curData = new byte[4];//用于从preBuffer中读取数据
            byte[] nextData = null;
            StringBuilder computData = new StringBuilder();//存储解析数据
            StringBuilder analyseData = new StringBuilder();

            int pointCount = 0;//读取的数据个数
            double i0 = 0, i1 = 0, res = 0;
            int adcCount = 0;
            int time = 0;
            int minCh = Integer.MAX_VALUE;
            tang = 1;
            try {
                //System.out.println("analyse module ch1:" + ch1 + "  ch2:" + ch2);
                computeDataFile.println("pulse adc1 adc2 absorption");
                while(preBuffer.read(curData, 0, 4, "Analyse Module") !=-1 && !exit){
                    //System.out.println(StringOp.byte2string(curData));
                    //清空用于写入文件的字符串
                    computData.delete(0,computData.length());
                    //清空用于写入下一级buffer的字符串
                    analyseData.delete(0, analyseData.length());

                    if(curData[0] == (byte) 0xff){
                        //System.out.println("head");
                        if(i0 > 0 && i1 > 0) {
                            //System.out.println("----");
                            //目前只有通道1和通道2有数据，并且只有如何计算这两个通道的算法
                            adcCount = adcCount * count;
                            i0 = i0 / adcCount;
                            i1 = i1 / adcCount;
                            //res = Math.log(i0/i1);
                            res = i0 / i1;
                            computData.append(pulse + " " + i0 + " " + i1 + " " + res);
                            computeDataFile.println(computData);
                            if (pointCount == 0 || pointCount % freq == 0) {
                                nodes.add(new double[]{time, res});
                                time++;
                            }
                        }
                        pointCount++;

                        pulse = (curData[1] << 16 & 0xffffff) | (curData[2] << 8 & 0xffff) | (curData[3] & 0xff);
                    }else{

                        direct = ((curData[0] >> 4) & 0xf) - 13;

                        if(predirect != 0 && predirect != direct){
                            tang++;
                            if(i0 > 0 && i1 > 0) {
                                //System.out.println("----");
                                //目前只有通道1和通道2有数据，并且只有如何计算这两个通道的算法
                                adcCount = adcCount * count;
                                i0 = i0 / adcCount;
                                i1 = i1 / adcCount;
                                //res = Math.log(i0 / i1);
                                res = i0 / i1;
                                computData.append(pulse + " " + i0 + " " + i1 + " " + res);
                                computeDataFile.println(computData);
                                if (pointCount == 0 || pointCount % freq == 0) {
                                    nodes.add(new double[]{time, res});
                                    time++;
                                }
                            }
                            if(tang % 2 != 0){
                                removeSize = time;
                                time = 0;
                            }
                            computeDataFile.close();
                            fileNo++;
                            computeDataFile = new PrintWriter(FileOp.createFileOutputStream(fileName+"_"+fileNo + ".txt"));
                            computeDataFile.println("pulse adc1 adc2 absorption");

                        }

                        predirect = direct;

                        channel = curData[0] & 0xf;

                        adc = ((curData[1] & 0xff) << 16) | ((curData[2] & 0xff) << 8) | (curData[3] & 0xff);
                        if(channel < 1 || channel > 4){
                            isError = true;
                            tableData.add(new Information(Time.getCurTime(), "ERROR", "Data Recv Wrong!!"));
                            return;
                        }
                        minCh = Math.min(minCh, channel);
                        if(channel == minCh){
                            i0 += adc;
                            analyseData.append(pulse + " "+ adc + " ");
                        }else {
                            i1 += adc;
                            analyseData.append(adc + "\n");
                            adcCount++;
                        }
                    }

                    nextData = analyseData.toString().getBytes();
                    nextBuffer.write(nextData, 0, nextData.length, "Analyse Module");
                }
                if(i0 > 0 && i1 > 0) {
                    //System.out.println("----");
                    //目前只有通道1和通道2有数据，并且只有如何计算这两个通道的算法
                    adcCount = adcCount * count;
                    i0 = i0 / adcCount;
                    i1 = i1 / adcCount;
                    //res = Math.log(i0 / i1);
                    res = i0 / i1;
                    computData.append(pulse + " " + i0 + " " + i1 + " " + res);
                    computeDataFile.println(computData);
                }
                runLogFile.writeContent(Time.getCurTime());
                runLogFile.writeContent("   Analyse module stop work\n");
                tableData.add(new Information(Time.getCurTime(), "INFO", new String("total pulse:" + pointCount)));
                computeDataFile.flush();
                computeDataFile.close();

            } catch (InterruptedException e) {
                e.printStackTrace();
                isError = true;
            } catch (IOException e) {
                e.printStackTrace();
                isError = true;
            }finally {
                over = true;
            }
    }

    private void clear(List<List<Integer>> adcList) {
        for(int i = 0; i < adcList.size(); ++i){
            adcList.get(i).clear();
        }
    }

    private double getAvg(List<Integer> adcList) {
        int len = (adcList.size() - start - end) * count;
        int sum = 0;
        for(int i = start; i < adcList.size()-end; ++i){
            sum += adcList.get(i);
        }
        return (double)(sum/len);
    }

    private boolean isEmpty(List<List<Integer>> adcList, int ch1, int ch2) {
        if(adcList.get(ch1).size() == 0 || adcList.get(ch2).size() == 0){
            return true;
        }
        return false;
    }

}
