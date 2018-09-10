package com.hzgc.service.dynrepo.dao;

import com.hzgc.service.dynrepo.bean.SearchCallBack;
import com.hzgc.service.dynrepo.bean.SearchOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.sql.*;

@Repository
@Slf4j
public class SparkJDBCDao {
    @Value("${hive.jdbc.driver}")
    private String hiveClass;
    @Value("${hive.jdbc.url}")
    private String hiveUrl;


    public SearchCallBack searchPicture(SearchOption option) throws SQLException {
        Connection connection = createConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet =  statement.executeQuery(parseByOption(option));
        return new SearchCallBack(connection, statement, resultSet);
    }

    public void closeConnection(Connection connection, Statement statement) {
        try {
            if (statement != null && !statement.isClosed()) {
                statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private String parseByOption(SearchOption option) throws SQLException {
        log.info("Query sql: " + ParseByOption.getFinalSQLwithOption(option, true));
        return ParseByOption.getFinalSQLwithOption(option, false);
    }

    private Connection createConnection() throws SQLException {
        Connection connection = null;
        long start = System.currentTimeMillis();
        try {
            Class.forName(hiveClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            connection = DriverManager.getConnection(hiveUrl);
        } catch (SQLException e) {
            if (e.getMessage().contains("Unable to read HiveServer2 uri from ZooKeeper")) {
                log.error("Please start Spark thriftserver Service !");
            } else {
                e.printStackTrace();
            }
        }
        if (connection == null) {
            log.error("Create spark jdbc connection faild, current hive class is [" + hiveClass + "], hive url is [" + hiveUrl + "]");
            throw new SQLException();
        }
        log.info("Get jdbc connection time is:" + (System.currentTimeMillis() - start));
        return connection;
    }
}
