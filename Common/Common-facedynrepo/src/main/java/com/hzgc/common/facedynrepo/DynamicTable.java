package com.hzgc.common.facedynrepo;

import java.io.Serializable;

/**
 * 动态库表属性
 */
public class DynamicTable implements Serializable {
    //es索引
    public static final String DYNAMIC_INDEX = "dynamic";
    //es类型
    public static final String PERSON_INDEX_TYPE = "person";
    //图片的ftp地址 xxx/xxx/xxx/
    public static final String FTPURL = "ftpurl";
    //设备id
    public static final String IPCID = "ipcid";
    //设备id keyword
    public static final String IPCID_KEYWORD="ipcid.keyword";
    //时间区间 数据格式 小时+分钟 例如:11:30用1130表示
    public static final String TIMESLOT = "timeslot";
    //时间戳 数据格式 xxxx-xx-xx xx:xx:xx(年-月-日 时:分:秒)
    public static final String TIMESTAMP = "exacttime";

    //日期 分区字段 数据格式 xxxx-xx-xx(年-月-日)
    public static final String DATE = "date";
    //人脸属性-是否戴眼镜
    public static final String EYEGLASSES = "eyeglasses";
    //人脸属性-性别 男或女
    public static final String GENDER = "gender";
    //人脸属性-头发颜色
    public static final String HAIRCOLOR = "haircolor";
    //人脸属性-头发类型
    public static final String HAIRSTYLE = "hairstyle";
    //人脸属性-是否带帽子
    public static final String HAT = "hat";
    //人脸属性-胡子类型
    public static final String HUZI = "huzi";
    //人脸属性-是否带领带
    public static final String TIE = "tie";
    //小文件合并后数据表
    public static final String PERSON_TABLE = "person_table";
    //小文件合并前数据表
    public static final String MID_TABLE = "mid_table";
    //特征值
    public static final String FEATURE = "feature";
    //特征值比对结果相似度
    public static final String SIMILARITY = "similarity";
    //hive中特征值比对udf函数
    public static final String FUNCTION_NAME = "compare";
    //人脸属性-图片是否清晰,0清晰1不清晰
    public static final String SHARPNESS = "sharpness";
    public static final String GROUP_FIELD = "id";
    public static final String ALARM_ID = "alarmid";
    public static final String ALARM_TIME = "alarmtime";
    public static final String CLUSTERING_ID = "clusterid";
}
