package com.hzgc.service.dynrepo.service;

import com.hzgc.common.collect.facedis.FtpRegisterClient;
import com.hzgc.common.collect.util.CollectUrlUtil;
import com.hzgc.common.service.facedynrepo.FaceTable;
import com.hzgc.common.util.json.JacksonUtil;
import com.hzgc.service.dynrepo.bean.CaptureOption;
import com.hzgc.service.dynrepo.bean.CapturedPicture;
import com.hzgc.service.dynrepo.bean.SingleCaptureResult;
import com.hzgc.service.dynrepo.bean.SortParam;
import com.hzgc.service.dynrepo.dao.ElasticSearchDao;
import com.hzgc.service.dynrepo.dao.EsSearchParam;
import com.hzgc.service.dynrepo.util.DeviceToIpcs;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CaptureHistoryService {

    @Autowired
    @SuppressWarnings("unused")
    private FtpRegisterClient ftpRegisterClient;
    @Autowired
    @SuppressWarnings("unused")
    private ElasticSearchDao elasticSearchDao;
    @Autowired
    @SuppressWarnings("unused")
    private Environment environment;
    @Autowired
    @SuppressWarnings("unused")
    private CaptureServiceHelper captureServiceHelper;
    @Value(value = "${ftp.port}")
    @SuppressWarnings("unused")
    private String ftpPort;

    public List<SingleCaptureResult> getCaptureHistory(CaptureOption option) {
        String sortParam = EsSearchParam.DESC;
        List<SortParam> sortParams = option.getSort()
                .stream().map(param -> SortParam.values()[param]).collect(Collectors.toList());
        for (SortParam s : sortParams) {
            if (s.name().equals(SortParam.TIMEDESC.toString())) {
                sortParam = EsSearchParam.DESC;
            } else if (s.name().equals(SortParam.SIMDASC.toString())) {
                sortParam = EsSearchParam.ASC;
            }
        }

        if (sortParams.get(0).name().equals(SortParam.IPC.toString())) {
            log.info("The current query needs to be grouped by ipcid");
            return getCaptureHistory(option, sortParam);
        } else if (!sortParams.get(0).name().equals(SortParam.IPC.toString())) {
            log.info("The current query don't needs to be grouped by ipcid");
            return getCaptureHistory(option, DeviceToIpcs.getIpcs(option.getDeviceIpcs()), sortParam);
        } else {
            log.info("The current query is default");
            return getDefaultCaptureHistory(option, sortParam);
        }
    }

    private List<SingleCaptureResult> getDefaultCaptureHistory(CaptureOption option, String sortParam) {
        List<SingleCaptureResult> results = new ArrayList<>();
        SingleCaptureResult singleResult = new SingleCaptureResult();
        SearchResponse searchResponse = elasticSearchDao.getCaptureHistory(option, sortParam);
        SearchHits searchHits = searchResponse.getHits();
        SearchHit[] hits = searchHits.getHits();
        int totallCount = (int) searchHits.getTotalHits();
        List<CapturedPicture> persons = new ArrayList<>();
        CapturedPicture capturePicture;
        if (hits.length > 0) {
            for (SearchHit hit : hits) {
                capturePicture = new CapturedPicture();
                String sabsolutepath = (String) hit.getSource().get(FaceTable.SABSOLUTEPATH);
                String babsolutepath = (String) hit.getSource().get(FaceTable.BABSOLUTEPATH);
                String ipcid = (String) hit.getSource().get(FaceTable.IPCID);
                String timestamp = (String) hit.getSource().get(FaceTable.TIMESTAMP);
                String hostname = (String) hit.getSource().get(FaceTable.HOSTNAME);
                Map <String, String> ftpIpMapping = ftpRegisterClient.getFtpIpMapping();
                String ip = ftpIpMapping.get(ftpIpMapping.get(hostname));
                capturePicture.setSabsolutepath(CollectUrlUtil.toHttpPath(ip,ftpPort,sabsolutepath));
                capturePicture.setBabsolutepath(CollectUrlUtil.toHttpPath(ip,ftpPort,babsolutepath));
                capturePicture.setDeviceId(ipcid);
                capturePicture.setTimeStamp(timestamp);
                persons.add(capturePicture);
            }
        }
        singleResult.setTotal(totallCount);
        singleResult.setPictures(persons);
        results.add(singleResult);
        return results;
    }

    private List<SingleCaptureResult> getCaptureHistory(CaptureOption option, String sortParam) {
        List<SingleCaptureResult> results = new ArrayList<>();
        for (String ipcId : DeviceToIpcs.getIpcs(option.getDeviceIpcs())) {
            SingleCaptureResult singleResult = new SingleCaptureResult();
            List<CapturedPicture> capturedPictureList = new ArrayList<>();
            SearchResponse searchResponse = elasticSearchDao.getCaptureHistory(option, ipcId, sortParam);
            SearchHits searchHits = searchResponse.getHits();

            SearchHit[] hits = searchHits.getHits();
            CapturedPicture capturePicture;
            if (hits.length > 0) {
                for (SearchHit hit : hits) {
                    capturePicture = new CapturedPicture();
                    String sabsolutepath = (String) hit.getSource().get(FaceTable.SABSOLUTEPATH);
                    String babsolutepath = (String) hit.getSource().get(FaceTable.BABSOLUTEPATH);
                    String ipc = (String) hit.getSource().get(FaceTable.IPCID);
                    String timestamp = (String) hit.getSource().get(FaceTable.TIMESTAMP);
                    String hostname = (String) hit.getSource().get(FaceTable.HOSTNAME);
                    Map <String, String> ftpIpMapping = ftpRegisterClient.getFtpIpMapping();
                    String ip = ftpIpMapping.get(hostname);
                    capturePicture.setSabsolutepath(CollectUrlUtil.toHttpPath(ip,ftpPort,sabsolutepath));
                    capturePicture.setBabsolutepath(CollectUrlUtil.toHttpPath(ip,ftpPort,babsolutepath));
                    capturePicture.setDeviceId(option.getIpcMapping().get(ipc).getIpc());
                    capturePicture.setDeviceName(option.getIpcMapping().get(ipc).getDeviceName());
                    capturePicture.setTimeStamp(timestamp);
                    if (ipcId.equals(ipc)) {
                        capturedPictureList.add(capturePicture);
                    }
                }
            } else {
                capturePicture = new CapturedPicture();
                capturedPictureList.add(capturePicture);
            }
            singleResult.setTotal((int) searchHits.getTotalHits());
            singleResult.setDeviceId(ipcId);
            singleResult.setDeviceName(option.getIpcMapping().get(ipcId).getDeviceName());
            singleResult.setPictures(capturedPictureList);
            results.add(singleResult);
        }
        log.info("Capture history results:" + JacksonUtil.toJson(results));
        return results;
    }

    private List<SingleCaptureResult> getCaptureHistory(CaptureOption option, List<String> deviceIds, String sortParam) {
        List<SingleCaptureResult> results = new ArrayList<>();
        SingleCaptureResult singleResult = new SingleCaptureResult();
        List<CapturedPicture> captureList = new ArrayList<>();

        SearchResponse searchResponse = elasticSearchDao.getCaptureHistory(option, deviceIds, sortParam);
        SearchHits searchHits = searchResponse.getHits();
        SearchHit[] hits = searchHits.getHits();
        CapturedPicture capturePicture;
        if (hits.length > 0) {
            for (SearchHit hit : hits) {
                capturePicture = new CapturedPicture();
                String sabsolutepath = (String) hit.getSource().get(FaceTable.SABSOLUTEPATH);
                String babsolutepath = (String) hit.getSource().get(FaceTable.BABSOLUTEPATH);
                String ipc = (String) hit.getSource().get(FaceTable.IPCID);
                String timestamp = (String) hit.getSource().get(FaceTable.TIMESTAMP);
                String hostname = (String) hit.getSource().get(FaceTable.HOSTNAME);
                Map <String, String> ftpIpMapping = ftpRegisterClient.getFtpIpMapping();
                String ip = ftpIpMapping.get(hostname);
                capturePicture.setSabsolutepath(CollectUrlUtil.toHttpPath(ip,ftpPort,sabsolutepath));
                capturePicture.setBabsolutepath(CollectUrlUtil.toHttpPath(ip,ftpPort,babsolutepath));
                capturePicture.setDeviceId(ipc);
                capturePicture.setTimeStamp(timestamp);
                capturePicture.setDeviceId(ipc);
                capturePicture.setDeviceName(option.getIpcMapping().get(ipc).getDeviceName());
                captureList.add(capturePicture);
            }
        }
        singleResult.setTotal((int) searchHits.getTotalHits());
        singleResult.setPictures(captureList);
        singleResult.setDeviceId(DeviceToIpcs.getIpcs(option.getDeviceIpcs()).get(0));
        singleResult.setDeviceName(option.getIpcMapping().get(option.getDeviceIpcs().get(0).getIpc()).getDeviceName());
        results.add(singleResult);
        return results;
    }
}