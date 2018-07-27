package com.hzgc.cluster.spark.alarm

import java.text.SimpleDateFormat
import java.util
import java.util.Date

import com.google.gson.Gson
import com.hzgc.cluster.spark.consumer.{AlarmMessage, PutDataToEs}
import com.hzgc.common.facedispatch.DeviceUtilImpl
import com.hzgc.cluster.spark.message.{AddAlarmMessage, Item, RecognizeAlarmMessage}
import com.hzgc.common.rocketmq.RocketMQProducer
import com.hzgc.common.facestarepo.table.alarm.StaticRepoUtil
import com.hzgc.cluster.spark.util.{FaceObjectUtil, PropertiesUtil}
import com.hzgc.common.facedispatch.table.DispatchTable
import com.hzgc.jni.FaceFunction
import kafka.serializer.StringDecoder
import org.apache.log4j.Logger
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Durations, StreamingContext}

import scala.collection.JavaConverters
import scala.collection.mutable.ArrayBuffer

object FaceAlarmJob {

  case class Json(staticID: String,
                  staticObjectType: String,
                  sim: Float)

  val LOG: Logger = Logger.getLogger(FaceAlarmJob.getClass)

  def main(args: Array[String]): Unit = {
    val deviceUtilI = new DeviceUtilImpl()
    val properties = PropertiesUtil.getProperties
    val appName = properties.getProperty("job.alarm.appName")
    val mqTopic = properties.getProperty("rocketmq.topic.name")
    val nameServer = properties.getProperty("rocketmq.nameserver")
    val grouId = properties.getProperty("rocketmq.group.id")
    val itemNum = properties.getProperty("job.recognizeAlarm.items.num").toInt
    val timeInterval = Durations.seconds(properties.getProperty("job.alarm.timeInterval").toLong)
    val conf = new SparkConf()
      .setAppName(appName)
    val ssc = new StreamingContext(conf, timeInterval)
    val switch = ssc.sparkContext.broadcast(properties.getProperty("alarm.store.switch"))
    val kafkaBootStrapBroadCast = ssc.sparkContext.broadcast(properties.getProperty("kafka.metadata.broker.list"))
    val jdbcUrlBroadCast = ssc.sparkContext.broadcast(properties.getProperty("phoenix.jdbc.url"))
    val kafkaGroupId = properties.getProperty("kafka.faceAlarmJob.group.id")
    val topics = Set(properties.getProperty("kafka.topic.name"))
    val brokers = properties.getProperty("kafka.metadata.broker.list")
    val kafkaParams = Map(
      "metadata.broker.list" -> brokers,
      "group.id" -> kafkaGroupId
    )
    val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val kafkaDynamicPhoto = KafkaUtils.
      createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topics)
    val jsonResult = kafkaDynamicPhoto.map(data => (data._1, FaceObjectUtil.jsonToObject(data._2)))
      .filter(obj => obj._2.getAttribute.getFeature != null && obj._2.getAttribute.getFeature.length == 512)
      .map(massage => {
        val totalList = JavaConverters.
          asScalaBufferConverter(StaticRepoUtil.getInstance(kafkaBootStrapBroadCast.value, jdbcUrlBroadCast.value).getTotalList).asScala
        val faceObj = massage._2
        LOG.info("faceObject is " + faceObj.getBurl)
        val ipcID = faceObj.getIpcId
        val alarmRule = deviceUtilI.isWarnTypeBinding(ipcID)
        val filterResult = new ArrayBuffer[Json]()
        if (alarmRule != null && !alarmRule.isEmpty) {
          totalList.foreach(record => {
            val threshold = FaceFunction.featureCompare(record(2).asInstanceOf[Array[Float]], faceObj.getAttribute.getFeature)
            filterResult += Json(record(0).asInstanceOf[String], record(1).asInstanceOf[String], threshold)
          })
        } else {
          LOG.error("Device [" + ipcID + "] does not bind alarm rules,current time [" + df.format(new Date()) + "]")
        }
        (massage._2, ipcID, "0001", filterResult)
      }).filter(jsonResultFilter => jsonResultFilter._4.nonEmpty)

    jsonResult.foreachRDD(resultRDD => {
      resultRDD.foreachPartition(parRDD => {
        val rocketMQProducer = RocketMQProducer.getInstance(nameServer, mqTopic, grouId)
        val gson = new Gson()
        val putDataToEs: PutDataToEs = PutDataToEs.getInstance()
        parRDD.foreach(result => {
          val alarmRule = deviceUtilI.isWarnTypeBinding(result._2)
          val recognizeWarnRule = alarmRule.get(DispatchTable.IDENTIFY)
          val addWarnRule = alarmRule.get(DispatchTable.ADDED)
          val recognizeItems = new ArrayBuffer[Item]()
          val addItems = new ArrayBuffer[Item]()
          result._4.foreach(record => {
            //识别告警
            if (recognizeWarnRule != null && !recognizeWarnRule.isEmpty) {
              if (recognizeWarnRule.containsKey(record.staticObjectType)) {
                if (record.sim > recognizeWarnRule.get(record.staticObjectType).toFloat) {
                  val item = new Item()
                  item.setSimilarity(record.sim.toString)
                  item.setStaticID(record.staticID)
                  item.setObjType(record.staticObjectType)
                  recognizeItems += item
                }
              }
            }
            //新增告警
            if (addWarnRule != null && !addWarnRule.isEmpty) {
              if (addWarnRule.containsKey(record.staticObjectType)) {
                if (record.sim > addWarnRule.get(record.staticObjectType).toFloat) {
                  val item = new Item()
                  item.setSimilarity(record.sim.toString)
                  item.setStaticID(record.staticID)
                  item.setObjType(record.staticObjectType)
                  addItems += item
                }
              }
            }
          })
          if (recognizeItems != null && recognizeItems.nonEmpty) {
            val itemsResult = recognizeItems.sortWith(_.getSimilarity > _.getSimilarity).take(itemNum)
            val updateTimeList = new util.ArrayList[String]()
            val offLineWarnRule = alarmRule.get(DispatchTable.OFFLINE)
            if (offLineWarnRule != null && !offLineWarnRule.isEmpty) {
              itemsResult.foreach(record => {
                if (offLineWarnRule.containsKey(record.getObjType)) {
                  updateTimeList.add(record.getStaticID)
                }
              })
            }
            if (!updateTimeList.isEmpty) {
              StaticRepoUtil.getInstance(kafkaBootStrapBroadCast.value, jdbcUrlBroadCast.value).updateObjectInfoTime(updateTimeList)
            }
            val recognizeAlarmMessage = new RecognizeAlarmMessage()
            val AlarmMessage = new AlarmMessage()
            val dateStr = df.format(new Date())
            val surl = result._1.getRelativePath
            val burl = surl.substring(0, surl.length - 5) + "0.jpg"
            val esSurl = result._1.getSurl
            val esBurl = result._1.getBurl
            val staticId = result._4(0).staticID
            val sim = result._4(0).sim.toString
            val staticObjectType = result._4(0).staticObjectType
            AlarmMessage.setAlarmType(DispatchTable.IDENTIFY)
            AlarmMessage.setIpcID(result._2)
            AlarmMessage.setSmallPictureURL(esSurl)
            AlarmMessage.setBigPictureURL(esBurl)
            AlarmMessage.setAlarmTime(dateStr)
            AlarmMessage.setHostName(result._1.getHostname)
            AlarmMessage.setStaticID(staticId)
            AlarmMessage.setSim(sim)
            AlarmMessage.setObjectType(staticObjectType)
            AlarmMessage.setFlag(0)
            AlarmMessage.setConfirm(1)
            recognizeAlarmMessage.setAlarmType(DispatchTable.IDENTIFY)
            recognizeAlarmMessage.setSmallPictureURL(surl)
            recognizeAlarmMessage.setBigPictureURL(burl)
            recognizeAlarmMessage.setItems(itemsResult.toArray)
            recognizeAlarmMessage.setHostName(result._1.getHostname)
            recognizeAlarmMessage.setDynamicDeviceID(result._2)
            recognizeAlarmMessage.setAlarmTime(dateStr)
            if (switch.value.equals("true")) {
              val status = putDataToEs.putAlarmDataToEs(surl, AlarmMessage)
              if (status == 1) {
                LOG.info("Put data to es succeed! And the ftpurl is " + surl)
              }
            }
            rocketMQProducer.send(result._3,
              "alarm_" + DispatchTable.IDENTIFY,
              surl,
              gson.toJson(recognizeAlarmMessage).getBytes(),
              null)
          }
          if (addItems.isEmpty) {
            val addAlarmMessage = new AddAlarmMessage()
            val AlarmMessage = new AlarmMessage()
            val dateStr = df.format(new Date())
            val surl = result._1.getRelativePath
            val burl = surl.substring(0, surl.length - 5) + "0.jpg"
            val esSurl = result._1.getSurl
            val esBurl = result._1.getBurl
            AlarmMessage.setAlarmType(DispatchTable.ADDED)
            AlarmMessage.setIpcID(result._2)
            AlarmMessage.setSmallPictureURL(esSurl)
            AlarmMessage.setBigPictureURL(esBurl)
            AlarmMessage.setAlarmTime(dateStr)
            AlarmMessage.setHostName(result._1.getHostname)
            AlarmMessage.setFlag(0)
            AlarmMessage.setConfirm(1)
            addAlarmMessage.setAlarmTime(dateStr)
            addAlarmMessage.setAlarmType(DispatchTable.ADDED)
            addAlarmMessage.setSmallPictureURL(surl)
            addAlarmMessage.setBigPictureURL(burl)
            addAlarmMessage.setDynamicDeviceID(result._2)
            addAlarmMessage.setHostName(result._1.getHostname)
            if (switch.value.equals("true")) {
              val status = putDataToEs.putAlarmDataToEs(surl, AlarmMessage)
              if (status == 1) {
                LOG.info("Put data to es succeed! And the ftpurl is " + surl)
              }
            }
            rocketMQProducer.send(result._3,
              "alarm_" + DispatchTable.ADDED,
              surl,
              gson.toJson(addAlarmMessage).getBytes(),
              null)
          }
        })
      })
    })
    ssc.start()
    ssc.awaitTermination()
  }
}