package com.hzgc.service.imsi.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ImsiParam implements Serializable {
    //开始时间
    private String startTime;
    //结束时间
    private String endTime;
}