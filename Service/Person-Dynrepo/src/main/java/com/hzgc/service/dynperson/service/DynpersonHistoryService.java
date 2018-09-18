package com.hzgc.service.dynperson.service;

import com.hzgc.common.service.facedynrepo.PersonTable;
import com.hzgc.common.util.basic.UuidUtil;
import com.hzgc.jniface.PersonAttributes;
import com.hzgc.service.dynperson.bean.*;
import com.hzgc.service.dynperson.dao.ElasticSearchDao;
import com.hzgc.service.dynperson.dao.EsSearchParam;
import com.hzgc.service.dynperson.util.DeviceToIpcs;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DynpersonHistoryService {

    @Autowired
    @SuppressWarnings("unused")
    private ElasticSearchDao elasticSearchDao;

    @Autowired
    @SuppressWarnings("unused")
    private DypersonServiceHelper dypersonServiceHelper;

    public SingleResults getCaptureHistory(CaptureOption captureOption) {
        String sortParam = EsSearchParam.DESC;
        List <Integer> sortList = captureOption.getSort();
        List <SortParam> sortParams = sortList.stream().map(param -> SortParam.values()[param]).collect(Collectors.toList());
        for (SortParam param : sortParams) {
            if (SortParam.DESC.equals(param)) {
                sortParam = EsSearchParam.DESC;
            } else if (SortParam.ASC.equals(param)) {
                sortParam = EsSearchParam.ASC;
            }
        }
        SingleResults singleResults = getDefaultCaptureHistory(captureOption, sortParam);
        if (sortParams.get(0).name().equals(SortParam.IPC.toString())) {
            log.info("The current query needs to be grouped by ipcid");
            singleResults = getCaptureHistory(captureOption, sortParam, singleResults);
        } else if (!sortParams.get(0).name().equals(SortParam.IPC.toString())) {
            log.info("The current query don't needs to be grouped by ipcid");
            singleResults = getCaptureHistory(captureOption, DeviceToIpcs.getIpcs(captureOption.getDevices()), sortParam, singleResults);
        }
        return singleResults;
    }

    private SingleResults getDefaultCaptureHistory(CaptureOption captureOption, String sortParam) {
        SingleResults singleResults = new SingleResults();
        SearchResponse searchResponse = elasticSearchDao.getCaptureHistory(captureOption, sortParam);
        SearchHits searchHits = searchResponse.getHits();
        int totalCount = (int) searchHits.getTotalHits();
        List <Pictures> picturesList = new ArrayList <>();
        for (SearchHit hit : searchHits) {
            Pictures pictures = new Pictures();
            String surl = (String) hit.getSource().get(PersonTable.SURL);
            String burl = (String) hit.getSource().get(PersonTable.BURL);
            String ipcid = (String) hit.getSource().get(PersonTable.IPCID);
            String timestamp = (String) hit.getSource().get(PersonTable.TIMESTAMP);
            pictures.setSurl(surl);
            pictures.setBurl(burl);
            pictures.setDeviceId(ipcid);
            pictures.setDeviceName(captureOption.getIpcMapping().get(ipcid).getDeviceName());
            pictures.setTime(timestamp);

            List <PersonAttributes> personAttributes = getPersonAttributes(hit);

            pictures.setPersonAttributes(personAttributes);
            picturesList.add(pictures);
        }
        singleResults.setTotal(totalCount);
        singleResults.setPictures(picturesList);
        singleResults.setSearchId(UuidUtil.getUuid());
        return singleResults;
    }


    private SingleResults getCaptureHistory(CaptureOption captureOption, List <String> deviceIpcs, String sortParam, SingleResults singleResults) {
        SearchResponse searchResponse = elasticSearchDao.getCaptureHistory(captureOption, deviceIpcs, sortParam);
        SearchHits searchHits = searchResponse.getHits();
        int totalCount = (int) searchHits.getTotalHits();
        SearchHit[] hits = searchHits.getHits();
        List <Pictures> picturesList = new ArrayList <>();
        Pictures pictures;
        if (hits.length > 0) {
            for (SearchHit hit : hits) {
                pictures = new Pictures();
                String surl = (String) hit.getSource().get(PersonTable.SURL);
                String burl = (String) hit.getSource().get(PersonTable.BURL);
                String ipc = (String) hit.getSource().get(PersonTable.IPCID);
                String timestamp = (String) hit.getSource().get(PersonTable.TIMESTAMP);
                pictures.setSurl(surl);
                pictures.setBurl(burl);
                pictures.setDeviceId(ipc);
                pictures.setDeviceName(captureOption.getIpcMapping().get(ipc).getDeviceName());
                pictures.setTime(timestamp);
                List <PersonAttributes> personAttributes = getPersonAttributes(hit);
                pictures.setPersonAttributes(personAttributes);
                picturesList.add(pictures);
            }
        }
        singleResults.setPictures(picturesList);
        singleResults.setSearchId(UuidUtil.getUuid());
        singleResults.setTotal(totalCount);
        singleResults = getCaptureHistory(captureOption, sortParam, singleResults);
        return singleResults;
    }

    private SingleResults getCaptureHistory(CaptureOption captureOption, String sortParam, SingleResults singleResults) {

        List <DevicePictures> devicePicturesList = new ArrayList <>();
        for (String ipcId : DeviceToIpcs.getIpcs(captureOption.getDevices())) {
            DevicePictures devicePictures = new DevicePictures();
            List <Pictures> pictureList = new ArrayList <>();
            SearchResponse searchResponse = elasticSearchDao.getCaptureHistory(captureOption, ipcId, sortParam);
            SearchHits searchHits = searchResponse.getHits();
            int totalCount = (int) searchHits.getTotalHits();
            Pictures pictures;
            if (totalCount > 0) {
                for (SearchHit hit : searchHits) {
                    pictures = new Pictures();
                    String surl = (String) hit.getSource().get(PersonTable.SURL);
                    String burl = (String) hit.getSource().get(PersonTable.BURL);
                    String ipc = (String) hit.getSource().get(PersonTable.IPCID);
                    String timestamp = (String) hit.getSource().get(PersonTable.TIMESTAMP);
                    pictures.setSurl(surl);
                    pictures.setBurl(burl);
                    if (null != captureOption.getIpcMapping().get(ipc)) {
                        pictures.setDeviceId(ipc);
                        pictures.setDeviceName(captureOption.getIpcMapping().get(ipc).getDeviceName());
                    }

                    List <PersonAttributes> personAttributes = getPersonAttributes(hit);
                    pictures.setPersonAttributes(personAttributes);

                    pictures.setTime(timestamp);
                    if (ipcId.equals(ipc)) {
                        pictureList.add(pictures);
                    }
                }
            } else {
                pictures = new Pictures();
                pictureList.add(pictures);
            }
            devicePictures.setDeviceId(ipcId);
            devicePictures.setDeviceName(captureOption.getIpcMapping().get(ipcId).getDeviceName());
            devicePictures.setPictures(pictureList);
            devicePictures.setTotal(pictureList.size());
            devicePicturesList.add(devicePictures);

            singleResults.setTotal((int) searchHits.getTotalHits());
            singleResults.setSearchId(UuidUtil.getUuid());
            singleResults.setDevicePicturesList(devicePicturesList);
        }
        return singleResults;
    }

    private List <PersonAttributes> getPersonAttributes(SearchHit hit) {
        List <PersonAttributes> personAttributes = new ArrayList <>();
        PersonAttributes personAttribute = new PersonAttributes();
        personAttribute.setAge((String) hit.getSource().get(PersonTable.AGE));
        personAttribute.setHair((String) hit.getSource().get(PersonTable.HAIR));
        personAttribute.setBaby((String) hit.getSource().get(PersonTable.BABY));
        personAttribute.setBag((String) hit.getSource().get(PersonTable.BAG));
        personAttribute.setBottomColor((String) hit.getSource().get(PersonTable.BOTTOMCOLOR));
        personAttribute.setBottomType((String) hit.getSource().get(PersonTable.BOTTOMTYPE));
        personAttribute.setCarType((String) hit.getSource().get(PersonTable.CTYPE));
        personAttribute.setHat((String) hit.getSource().get(PersonTable.HAT));
        personAttribute.setKnapSack((String) hit.getSource().get(PersonTable.KNAPSACK));
        personAttribute.setMessengerBag((String) hit.getSource().get(PersonTable.MESSENGERBAG));
        personAttribute.setOrientation((String) hit.getSource().get(PersonTable.ORIENTATION));
        personAttribute.setSex((String) hit.getSource().get(PersonTable.SEX));
        personAttribute.setShoulderBag((String) hit.getSource().get(PersonTable.SHOULDERBAG));
        personAttribute.setUmbrella((String) hit.getSource().get(PersonTable.UMBRELLA));
        personAttribute.setUpperColor((String) hit.getSource().get(PersonTable.UPPERCOLOR));
        personAttribute.setUpperType((String) hit.getSource().get(PersonTable.UPPERTYPE));

        personAttributes.add(personAttribute);
        return personAttributes;
    }
}
