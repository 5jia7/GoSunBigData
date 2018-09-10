package com.hzgc.common.facedispatch;

import com.alibaba.fastjson.JSON;
import com.hzgc.common.facedispatch.table.DispatchTable;
import com.hzgc.common.hbase.HBaseHelper;
import com.hzgc.common.util.empty.IsEmpty;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public class DeviceUtilImpl implements Serializable {

    public Map<String, Map<String, String>> isWarnTypeBinding(String ipcID) {
        Table table = null;
        if (IsEmpty.strIsRight(ipcID)) {
            try {
                table = HBaseHelper.getTable(DispatchTable.TABLE_DEVICE);
                Get get = new Get(Bytes.toBytes(ipcID));
                Result result = table.get(get);
                if (result.containsColumn(DispatchTable.CF_DEVICE, DispatchTable.WARN)) {
                    byte[] map = result.getValue(DispatchTable.CF_DEVICE, DispatchTable.WARN);
                    if (map != null) {
                        String jsonString = Bytes.toString(map);
                        Map<String, Map<String, String>> warnMap = JSON.parseObject(jsonString, Map.class);
                        for (String t : warnMap.keySet()) {
                            String str = JSON.toJSONString(warnMap.get(t));
                            Map<String, String> map1 = JSON.parseObject(str, Map.class);
                            warnMap.put(t, map1);
                        }
                        return warnMap;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                HBaseHelper.closeTable(table);
            }
        }
        return null;
    }

    public Map<String, Map<String, String>> getThreshold() {
        try {
            Table table = HBaseHelper.getTable(DispatchTable.TABLE_DEVICE);
            Get get = new Get(DispatchTable.OFFLINERK);
            Result result = table.get(get);
            if (result.containsColumn(DispatchTable.CF_DEVICE, DispatchTable.OFFLINECOL)) {
                byte[] map = result.getValue(DispatchTable.CF_DEVICE, DispatchTable.OFFLINECOL);
                if (map != null) {
                    String jsonString = Bytes.toString(map);
                    Map<String, Map<String, String>> thresholdMap = JSON.parseObject(jsonString, Map.class);
                    for (String t : thresholdMap.keySet()) {
                        String str = JSON.toJSONString(thresholdMap.get(t));
                        Map<String, String> map1 = JSON.parseObject(str, Map.class);
                        thresholdMap.put(t, map1);
                    }
                    return thresholdMap;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
