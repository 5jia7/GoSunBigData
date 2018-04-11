package com.hzgc.common.util.uuid;

import java.io.Serializable;
import java.util.UUID;

public class UuidUtil implements Serializable {
    private String uuid;

    public static String setUuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
