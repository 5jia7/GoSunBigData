package com.hzgc.collect.expand.processer;

import com.hzgc.collect.expand.conf.CommonConf;
import com.hzgc.collect.expand.log.DataProcessLogWriter;
import com.hzgc.collect.expand.log.LogEvent;
import com.hzgc.collect.expand.util.*;
import com.hzgc.common.jni.FaceAttribute;
import com.hzgc.common.jni.FaceFunction;
import com.hzgc.common.util.json.JSONUtil;
import com.hzgc.common.util.searchtype.SearchType;

import java.util.concurrent.BlockingQueue;

public class ProcessThread implements Runnable {

    private BlockingQueue<LogEvent> queue;
    private DataProcessLogWriter writer;

    public ProcessThread(CommonConf conf, BlockingQueue<LogEvent> queue, String queueID) {
        this.queue = queue;
        writer = new DataProcessLogWriter(conf, queueID);
    }

    @Override
    public void run() {
        LogEvent event;
        try {
            while ((event = queue.take()) != null) {
                Sharpness sharpness = CollectProperties.getSharpness();
                System.out.println(sharpness.getWeight());
                System.out.println(sharpness.getHeight());
                FaceAttribute attribute =
                        FaceFunction.featureExtract(event.getAbsolutePath(), sharpness.getWeight(), sharpness.getHeight());
                FtpUrlMessage message = FtpUtils.getFtpUrlMessage(event.getFtpPath());
                if (attribute.getFeature() != null) {
                    String burl = FtpUtils.surlToBurl(event.getFtpPath());
                    FaceObject faceObject = new FaceObject(message.getIpcid(),
                            message.getTimeStamp(),
                            SearchType.PERSON,
                            message.getDate(),
                            message.getTimeslot(),
                            attribute,
                            event.getTimeStamp() + "",
                            event.getFtpPath(),
                            FtpUtils.surlToBurl(event.getFtpPath()),
                            message.getHostname());
                    ProcessCallBack callBack = new ProcessCallBack(event.getFtpPath(),
                            System.currentTimeMillis());
                    ProducerKafka.getInstance().sendKafkaMessage(
                            KafkaProperties.getTopicFeature(),
                            event.getFtpPath(),
                            JSONUtil.toJson(faceObject),
                            callBack);
                    writer.countCheckAndWrite(event);
                } else {
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
