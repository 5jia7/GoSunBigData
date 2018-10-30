#!/bin/bash
################################################################################
## Copyright:   HZGOSUN Tech. Co, BigData
## Filename:    springCloud start peoman-worker
## Description: 启动peoman-worker服务
## Author:      chenke
## Created:     2018-09-19
################################################################################
#set -x

cd `dirname $0`
BIN_DIR=`pwd`                         ##bin目录地址
cd ..
PEOMAN_WORKER_DIR=`pwd`                     ##peoman-worker目录地址
LIB_DIR=${PEOMAN_WORKER_DIR}/lib            ##lib目录地址
CONF_DIR=${PEOMAN_WORKER_DIR}/conf          ##conf目录地址
PEOMAN_WORKER_JAR_NAME=`ls ${LIB_DIR} | grep ^peoman-worker-[0-9].[0-9].jar$`          ##获取peoman-worker的jar包名称
PEOMAN_WORKER_JAR=${LIB_DIR}/${PEOMAN_WORKER_JAR_NAME}                        ##获取jar包的全路径




#-----------------------------------------------------------------------------#
#                              SpringCloud的配置                              #
#-----------------------------------------------------------------------------#
KAFKA_HOST=172.18.18.100:9092
COMPARE_NUMBER=3
BIT_THRESHOLD=10
FLOAT_THRESHOLD=90
FLOAT_NEW_THRESHOLD=73
FLOAT_COMPARE_OPEN=true
ZK_ADDRESS=172.18.18.100
MYSQL_HOST=172.18.18.119:4000
#MYSQL_USERNAME=
#MYSQL_PASSWORD=

#------------------------------------------------------------------------------#
#                                定义函数                                      #
#------------------------------------------------------------------------------#
#####################################################################
# 函数名: start_clustering
# 描述: 启动 springCloud peoman-worker服务
# 参数: N/A
# 返回值: N/A
# 其他: N/A
#####################################################################
function start_springCloud()
{
   PEOMAN_WORKER_PID=`jps | grep ${PEOMAN_WORKER_JAR_NAME} | awk '{print $1}'`
   if [  -n "${PEOMAN_WORKER_PID}" ];then
      echo "FaceDispatch service already started!!"
   else
      nohup java -jar ${PEOMAN_WORKER_JAR} --spring.profiles.active=pro \
      --kafka.host=${KAFKA_HOST} \
      --zookeeper.address=${ZK_ADDRESS} \
      --compare.number=${COMPARE_NUMBER} \
      --bit.threshold=${BIT_THRESHOLD} \
      --float.threshold=${FLOAT_THRESHOLD} \
      --float.new.threshold=${FLOAT_NEW_THRESHOLD} \
      --float.compare.open=${FLOAT_COMPARE_OPEN} \
      --mysql.host=${MYSQL_HOST} \
      --kafka.host=${KAFKA_HOST} \
      --spring.cloud.config.enabled=false  2>&1 &
   fi
}
#####################################################################
# 函数名: start_spring_cloud
# 描述: 启动 springCloud face-dispatch服务
# 参数: N/A
# 返回值: N/A
# 其他: N/A
#####################################################################
function prepare_resource_file()
{
  cp ${CONF_DIR}/hbase-site.xml .
  jar -uf ${PEOMAN_WORKER_JAR} hbase-site.xml
  rm -rf hbase-site.xml
}
#####################################################################
# 函数名: main
# 描述: 脚本主要业务入口
# 参数: N/A
# 返回值: N/A
# 其他: N/A
#####################################################################
function main()
{
#    prepare_resource_file
    start_springCloud
}

main