package com.hzgc.collect.service.parser;

import com.hzgc.collect.config.CollectContext;

import java.text.SimpleDateFormat;

public abstract class AbstractParser implements Parser {
    private CollectContext collectContext;
    protected SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    protected AbstractParser(CollectContext collectContext) {
        this.collectContext = collectContext;
    }

    /**
     * 通过上传文件路径解析到ftpUrl
     *
     * @param filePath ftp接收数据路径
     * @return 带IP的ftpUrl
     */
    @Override
    public String getFtpUrl_ip(String filePath) {
        return "ftp://" + collectContext.getFtpIp() + ":" + collectContext.getFtpPort() + filePath;
    }

    /**
     * 通过上传文件路径解析到ftpUrl（ftp发送至kafka的key）
     *
     * @param filePath ftp接收数据路径
     * @return 带hostname的ftpUrl
     */
    @Override
    public String getFtpUrl_hostname(String filePath) {
        return "ftp://" + collectContext.getHostname() + ":" + collectContext.getFtpPort() + filePath;
    }
}
