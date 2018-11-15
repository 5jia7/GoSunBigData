#!/bin/bash
IP=${IP}
PORT=${PORT}
 mysql -u root -h ${IP} -P ${PORT} -pHzgc@123 << EOF
use people;

INSERT INTO t_imsi_blacklist (imsi, currenttime)
SELECT DISTINCT f1.imsi, f2.currenttime
FROM (SELECT imsi, savetime, FROM_UNIXTIME(savetime/1000,"%Y%m%d")
      FROM (
            SELECT imsi, savetime
            FROM t_imsi_all
            WHERE FROM_UNIXTIME(savetime/1000,"%Y%m%d")=DATE_FORMAT(DATE_SUB(NOW(),INTERVAL 1 DAY),"%Y%m%d")) AS t1
      GROUP BY imsi,FROM_UNIXTIME(savetime/1000,"%Y%m%d")
      HAVING COUNT(imsi)>=10)AS f1 ,t_imsi_filter AS f2
WHERE f1.imsi = f2.imsi
AND FROM_UNIXTIME(f1.savetime/1000,"%Y%m%d")=f2.currenttime;
EOF
if [ $? != 0 ];then
 echo "`date "+%Y-%m-%d %H:%M:%S"`: exec blacklist sql failed" >> /var/log/mysql.log 2>&1 &
else
 echo "`date "+%Y-%m-%d %H:%M:%S"`: exec blacklist sql success" >> /var/log/mysql.log 2>&1 &
fi
