package com.hzgc.service.starepo.service;

import com.hzgc.common.hbase.HBaseHelper;
import com.hzgc.common.table.dynrepo.SearchRecordTable;
import com.hzgc.common.table.starepo.ObjectInfoTable;
import com.hzgc.common.util.object.ObjectUtil;
import com.hzgc.service.starepo.bean.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.io.IOException;
import java.sql.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ObjectInfoHandlerTool {

    public void saveSearchRecord(JdbcTemplate jdbcTemplate, ObjectSearchResult objectSearchResult) {
        if (objectSearchResult == null || objectSearchResult.getSearchStatus() == 1
                || objectSearchResult.getFinalResults() == null || objectSearchResult.getFinalResults().size() == 0) {
            log.info("获取的结果为空");
            return;
        }
        List<PersonSingleResult> personSingleResults = objectSearchResult.getFinalResults();

        String sql = "upsert into " + SearchRecordTable.TABLE_NAME + "(" + SearchRecordTable.ID
                + ", " +SearchRecordTable.RESULT + ", " + SearchRecordTable.RECORDDATE + ") values(?,?,?)";
        if (jdbcTemplate != null) {
            try{
                for (PersonSingleResult personSingleResult : personSingleResults) {
                    String id;
                    if (personSingleResults.size() == 1) {
                        id = objectSearchResult.getSearchTotalId();
                    } else {
                        id = personSingleResult.getSearchRowkey();
                    }
                    jdbcTemplate.update(sql,id, ObjectUtil.objectToByte(personSingleResult),new Timestamp(System.currentTimeMillis()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public  PersonObject getPersonObjectFromResultSet(ResultSet resultSet) {
        PersonObject personObject = new PersonObject();
        try {
            while (resultSet.next()) {
                personObject.setId(resultSet.getString(ObjectInfoTable.ROWKEY));
                personObject.setPkey(resultSet.getString(ObjectInfoTable.PKEY));
                personObject.setPlatformid(resultSet.getString(ObjectInfoTable.PLATFORMID));
                personObject.setName(resultSet.getString(ObjectInfoTable.NAME));
                personObject.setSex(resultSet.getInt(ObjectInfoTable.SEX));
                personObject.setIdcard(resultSet.getString(ObjectInfoTable.IDCARD));
                personObject.setPhoto(resultSet.getBytes(ObjectInfoTable.PHOTO));
                Array array = resultSet.getArray(ObjectInfoTable.FEATURE);
                if (array != null) {
                    personObject.setFeature((float[]) array.getArray());
                }
                personObject.setCreator(resultSet.getString(ObjectInfoTable.CREATOR));
                personObject.setCphone(resultSet.getString(ObjectInfoTable.CPHONE));
                personObject.setUpdatetime(resultSet.getTimestamp(ObjectInfoTable.UPDATETIME));
                personObject.setCreatetime(resultSet.getTimestamp(ObjectInfoTable.UPDATETIME));
                personObject.setReason(resultSet.getString(ObjectInfoTable.REASON));
                personObject.setTag(resultSet.getString(ObjectInfoTable.TAG));
                personObject.setImportant(resultSet.getInt(ObjectInfoTable.IMPORTANT));
                personObject.setStatus(resultSet.getInt(ObjectInfoTable.STATUS));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return personObject;
    }

    public  PreparedStatement getStaticPrepareStatementV1(Connection conn, PersonObject person, String sql) {
        PreparedStatement pstm;
        try {
            pstm = conn.prepareStatement(sql);
            pstm.setString(1, person.getId());
            pstm.setString(2, person.getName());
            pstm.setString(3, person.getPlatformid());
            pstm.setString(4, person.getTag());
            pstm.setString(5, person.getPkey());
            pstm.setString(6, person.getIdcard());
            pstm.setInt(7, person.getSex());
            pstm.setBytes(8, person.getPhoto());
            if (person.getFeature() != null && person.getFeature().length == 512) {
                pstm.setArray(9,
                        conn.createArrayOf("FLOAT", PersonObject.otherArrayToObject(person.getFeature())));
            } else {
                pstm.setArray(9, null);
            }
            pstm.setString(10, person.getReason());
            pstm.setString(11, person.getCreator());
            pstm.setString(12, person.getCphone());
            long dateNow = System.currentTimeMillis();
            pstm.setTimestamp(13, new Timestamp(dateNow));
            pstm.setTimestamp(14, person.getUpdatetime());
            pstm.setInt(15, person.getImportant());
            pstm.setInt(16, person.getStatus());
            return pstm;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void getPersonSingleResult(PersonSingleResult personSingleResult, SqlRowSet resultSet, boolean searchByPics) {
        List<PersonObject> personObjects = new ArrayList<>();
        try {
            while (resultSet.next()) {
                PersonObject personObject = new PersonObject();
                personObject.setId(resultSet.getString(ObjectInfoTable.ROWKEY));
                personObject.setPkey(resultSet.getString(ObjectInfoTable.PKEY));
                personObject.setPlatformid(resultSet.getString(ObjectInfoTable.PLATFORMID));
                personObject.setName(resultSet.getString(ObjectInfoTable.NAME));
                personObject.setSex(resultSet.getInt(ObjectInfoTable.SEX));
                personObject.setIdcard(resultSet.getString(ObjectInfoTable.IDCARD));
                personObject.setCreator(resultSet.getString(ObjectInfoTable.CREATOR));
                personObject.setCphone(resultSet.getString(ObjectInfoTable.CPHONE));
                personObject.setUpdatetime(resultSet.getTimestamp(ObjectInfoTable.UPDATETIME));
                personObject.setCreatetime(resultSet.getTimestamp(ObjectInfoTable.CREATETIME));
                personObject.setReason(resultSet.getString(ObjectInfoTable.REASON));
                personObject.setTag(resultSet.getString(ObjectInfoTable.TAG));
                personObject.setImportant(resultSet.getInt(ObjectInfoTable.IMPORTANT));
                personObject.setStatus(resultSet.getInt(ObjectInfoTable.STATUS));
                personObject.setLocation(resultSet.getString(ObjectInfoTable.LOCATION));
                if (searchByPics) {
                    personObject.setSim(resultSet.getFloat(ObjectInfoTable.RELATED));
                }
                personObjects.add(personObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        personSingleResult.setPersons(personObjects);
        personSingleResult.setSearchNums(personObjects.size());
    }

    /**
     * 根据请求参数，进行分页处理
     */
    public void formatTheObjectSearchResult(ObjectSearchResult objectSearchResult, int start, int size) {
        if (objectSearchResult == null) {
            return;
        }
        List<PersonSingleResult> personSingleResults = objectSearchResult.getFinalResults();

        List<PersonSingleResult> finalPersonSingleResults = new ArrayList<>();
        if (personSingleResults != null) {
            for (PersonSingleResult personSingleResult : personSingleResults) {
                List<PersonObject> personObjects = personSingleResult.getPersons();
                if(personObjects != null) {
                    if ((start + size) > personObjects.size()) {
                        personSingleResult.setPersons(personObjects.subList(start, personObjects.size()));
                    } else {
                        personSingleResult.setPersons(personObjects.subList(start, start + size));
                    }
                }

                List<GroupByPkey> groupByPkeys = personSingleResult.getGroupByPkeys();
                List<GroupByPkey> finaLGroupByPkeys = new ArrayList<>();

                if (groupByPkeys != null) {
                    for (GroupByPkey groupByPkey : groupByPkeys) {
                        List<PersonObject> personObjectsV1 = groupByPkey.getPersons();
                        if (personObjectsV1 != null) {
                            if ((start + size) > personObjectsV1.size()) {
                                groupByPkey.setPersons(personObjectsV1.subList(start, personObjectsV1.size()));
                            } else {
                                groupByPkey.setPersons(personObjectsV1.subList(start, start + size));
                            }
                            finaLGroupByPkeys.add(groupByPkey);
                        }
                    }
                    personSingleResult.setGroupByPkeys(finaLGroupByPkeys);
                }
                finalPersonSingleResults.add(personSingleResult);
            }
        }
        objectSearchResult.setFinalResults(finalPersonSingleResults);

    }

    /**
     *  更新hbase 表格中的一行，用来表示静态库中的数据有变动
     */
    public void updateTotalNumOfHbase() {
        Table objectinfo = HBaseHelper.getTable(ObjectInfoTable.TABLE_NAME);
        //总记录数加1，用于标志HBase 数据库中的数据有变动
        Put putOfTNums = new Put(Bytes.toBytes(ObjectInfoTable.TOTAL_NUMS_ROW_NAME));
        putOfTNums.setDurability(Durability.ASYNC_WAL);
        Get getOfTNums = new Get(Bytes.toBytes(ObjectInfoTable.TOTAL_NUMS_ROW_NAME));
        Result resultTNums;
        try {
            resultTNums = objectinfo.get(getOfTNums);
            long tatalNums = Bytes.toLong(resultTNums.getValue(Bytes.toBytes(ObjectInfoTable.PERSON_COLF),
                    Bytes.toBytes(ObjectInfoTable.TOTAL_NUMS)));
            putOfTNums.addColumn(Bytes.toBytes(ObjectInfoTable.PERSON_COLF),
                    Bytes.toBytes(ObjectInfoTable.TOTAL_NUMS),
                    Bytes.toBytes(tatalNums + 1));
            objectinfo.put(putOfTNums);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 对结果进行排序
     * @param personObjects 最终返回的一个人员列表
     * @param staticSortParams 排序参数
     */
    void sortPersonObject(List<PersonObject> personObjects, List<StaticSortParam> staticSortParams) {
        if (staticSortParams != null) {
            if (staticSortParams.contains(StaticSortParam.RELATEDDESC)) {
                personObjects.sort((o1, o2) -> {
                    float sim1 = o1.getSim();
                    float sim2 = o2.getSim();
                    return Float.compare(sim2, sim1);
                });
            }
            if (staticSortParams.contains(StaticSortParam.RELATEDASC)) {
                personObjects.sort((o1, o2) -> {
                    float sim1 = o1.getSim();
                    float sim2 = o2.getSim();
                    return Float.compare(sim1, sim2);
                });
            }
            if (staticSortParams.contains(StaticSortParam.IMPORTANTASC)) {
                personObjects.sort((o1, o2) -> {
                    int important1 = o1.getImportant();
                    int important2 = o2.getImportant();
                    return Integer.compare(important1, important2);
                });
            }
            if (staticSortParams.contains(StaticSortParam.IMPORTANTDESC)) {
                personObjects.sort((o1, o2) -> {
                    int important1 = o1.getImportant();
                    int important2 = o2.getImportant();
                    return Integer.compare(important2, important1);
                });
            }
            if (staticSortParams.contains(StaticSortParam.TIMEASC)) {
                personObjects.sort((o1, o2) -> {
                    Timestamp timestamp1 = o1.getCreatetime();
                    Timestamp timestamp2 = o2.getCreatetime();
                    return Long.compare(timestamp1.getTime(), timestamp2.getTime());
                });
            }
            if (staticSortParams.contains(StaticSortParam.TIMEDESC)) {
                personObjects.sort((o1, o2) -> {
                    Timestamp timestamp1 = o1.getCreatetime();
                    Timestamp timestamp2 = o2.getCreatetime();
                    return Long.compare(timestamp2.getTime(), timestamp1.getTime());
                });
            }
        }
    }

}
