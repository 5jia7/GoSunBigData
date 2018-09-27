CREATE TABLE t_people(
id VARCHAR(32) PRIMARY KEY NOT NULL COMMENT '人员全局ID',
name VARCHAR(10) NOT NULL COMMENT '人员姓名',
idcard VARCHAR(18) NOT NULL COMMENT '身份证',
region BIGINT(20) NOT NULL COMMENT '区域ID(省市区)',
household VARCHAR(100) COMMENT '户籍',
address VARCHAR(100) COMMENT '现居地',
sex VARCHAR(2) DEFAULT 0 COMMENT '性别',
age INT(2) COMMENT '年龄',
birthday VARCHAR(10) COMMENT '出生日期',
politic VARCHAR(10) COMMENT '政治面貌',
edulevel VARCHAR(10) COMMENT '文化程度',
job VARCHAR(10) COMMENT '职业',
birthplace VARCHAR(10) COMMENT '籍贯',
community BIGINT(20) COMMENT '小区ID',
lasttime TIMESTAMP COMMENT '最后出现时间',
createtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
updatetime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
UNIQUE KEY (id),
UNIQUE KEY (idcard)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '人口库';


CREATE TABLE t_flag(
id BIGINT(20) PRIMARY KEY AUTO_INCREMENT NOT NULL COMMENT 'ID',
peopleid VARCHAR(32) NOT NULL COMMENT '人员全局ID',
flagid INT(2) NOT NULL COMMENT '标签ID',
flag VARCHAR(10) NOT NULL COMMENT '标签',
UNIQUE KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '标签表';


CREATE TABLE t_picture(
id BIGINT(20) PRIMARY KEY AUTO_INCREMENT NOT NULL COMMENT 'ID',
peopleid VARCHAR(32) NOT NULL COMMENT '人员全局ID',
idcardpic LONGBLOB COMMENT '证件照片',
capturepic LONGBLOB COMMENT '实际采集照片',
feature VARCHAR(8192) NOT NULL COMMENT '特征值',
bitfeature VARCHAR(512) NOT NULL COMMENT 'bit特征值',
UNIQUE KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '照片信息库';


CREATE TABLE t_imsi(
id BIGINT(20) PRIMARY KEY AUTO_INCREMENT NOT NULL COMMENT 'ID',
peopleid VARCHAR(32) NOT NULL COMMENT '人员全局ID',
imsi VARCHAR(20) NOT NULL COMMENT 'IMSI码',
UNIQUE KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT 'IMSI码表';


CREATE TABLE t_phone(
id BIGINT(20) PRIMARY KEY AUTO_INCREMENT NOT NULL COMMENT 'ID',
peopleid VARCHAR(32) NOT NULL COMMENT '人员全局ID',
phone VARCHAR(15) NOT NULL COMMENT '联系方式',
UNIQUE KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '联系方式表';


CREATE TABLE t_house(
id BIGINT(20) PRIMARY KEY AUTO_INCREMENT NOT NULL COMMENT 'ID',
peopleid VARCHAR(32) NOT NULL COMMENT '人员全局ID',
house VARCHAR(100) NOT NULL COMMENT '房产信息',
UNIQUE KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '房产信息表';


CREATE TABLE t_car(
id BIGINT(20) PRIMARY KEY AUTO_INCREMENT NOT NULL COMMENT 'ID',
peopleid VARCHAR(32) NOT NULL COMMENT '人员全局ID',
car VARCHAR(100) NOT NULL COMMENT '车辆信息',
UNIQUE KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '车辆信息表';







CREATE TABLE t_people_recognize(
id BIGINT(20) PRIMARY KEY AUTO_INCREMENT NOT NULL COMMENT 'ID',
peopleid VARCHAR(32) NOT NULL COMMENT '人口库人员全局唯一ID',
community BIGINT(20) NOT NULL COMMENT '小区ID',
pictureid BIGINT(20) NOT NULL COMMENT '人口库图片ID',
deviceid VARCHAR(50) NOT NULL COMMENT '抓拍设备ID',
capturetime TIMESTAMP NOT NULL COMMENT '抓拍时间',
surl VARCHAR(255) NOT NULL COMMENT '小图ftp路径(带hostname的ftpurl)',
burl VARCHAR(255) NOT NULL COMMENT '大图ftp路径(带hostname的ftpurl)',
flag INT(2) NOT NULL COMMENT '识别标签(0:未知, 1:实名, 2:新增 ,10:原图)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '人口识别记录表';


CREATE TABLE t_fusion_imsi(
id BIGINT(20) PRIMARY KEY AUTO_INCREMENT NOT NULL COMMENT 'ID',
peopleid VARCHAR(32) NOT NULL COMMENT '人员全局ID',
community BIGINT(20) NOT NULL COMMENT '小区ID',
deviceid VARCHAR(50) NOT NULL COMMENT '帧码设备ID',
receivetime TIMESTAMP NOT NULL COMMENT '接收时间',
imsi VARCHAR(20) NOT NULL COMMENT 'imsi码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '数据融合记录表';


CREATE TABLE t_device_recognize(
id BIGINT(20) PRIMARY KEY AUTO_INCREMENT NOT NULL COMMENT 'ID',
peopleid VARCHAR(32) NOT NULL COMMENT '人员全局ID',
community BIGINT(20) NOT NULL COMMENT '小区ID',
deviceid VARCHAR(50) NOT NULL COMMENT '设备ID',
currenttime VARCHAR(8) NOT NULL COMMENT '当天日期(yyyyMMdd)',
count INT(10) NOT NULL COMMENT '统计次数',
flag INT(2) NOT NULL COMMENT '设备类型(1：人脸相机，2：侦码设备)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '设备抓拍次数记录表';


CREATE TABLE t_imsi_blacklist(
id BIGINT(20) PRIMARY KEY AUTO_INCREMENT NOT NULL COMMENT 'ID',
imsi VARCHAR(20) NOT NULL COMMENT 'imsi码',
currenttime VARCHAR(8) NOT NULL COMMENT '当天日期(yyyyMMdd)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '侦码黑名单表';


CREATE TABLE t_imsi_filter(
id BIGINT(20) PRIMARY KEY AUTO_INCREMENT NOT NULL COMMENT 'ID',
imsi VARCHAR(20) NOT NULL COMMENT 'imsi码',
currenttime VARCHAR(8) NOT NULL COMMENT '当天日期(yyyyMMdd)',
count INT(10) NOT NULL COMMENT '统计次数'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '侦码过滤记录表';


CREATE TABLE t_people_new(
id BIGINT(20) PRIMARY KEY AUTO_INCREMENT NOT NULL COMMENT 'ID',
peopleid VARCHAR(32) NOT NULL COMMENT '人员全局ID',
community BIGINT(20) NOT NULL COMMENT '小区ID',
month VARCHAR(6) NOT NULL COMMENT '疑似迁入月份:yyyyMM',
deviceid VARCHAR(50) NOT NULL COMMENT '设备ID',
isconfirm INT(2) NOT NULL COMMENT '是否确认迁入(1:未确认，2：已确认迁入，3：确认未迁入)',
flag INT(2) NOT NULL COMMENT '标签(1:预实名, 2:新增)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '疑似迁入记录表';


CREATE TABLE t_people_out(
id BIGINT(20) PRIMARY KEY AUTO_INCREMENT NOT NULL COMMENT 'ID',
peopleid VARCHAR(32) NOT NULL COMMENT '人员全局ID',
community BIGINT(20) NOT NULL COMMENT '小区ID',
month VARCHAR(6) NOT NULL COMMENT '疑似迁出月份:yyyyMM',
isconfirm INT(2) NOT NULL COMMENT '是否确认迁出(1:未确认，2：已确认迁出，3：确认未迁出)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '疑似迁出记录表';


CREATE TABLE t_24hour_count(
id BIGINT(20) PRIMARY KEY AUTO_INCREMENT NOT NULL COMMENT 'ID',
peopleid VARCHAR(32) NOT NULL COMMENT '人员全局ID',
community BIGINT(20) NOT NULL COMMENT '小区ID',
hour VARCHAR(10) NOT NULL COMMENT '抓拍小时:yyyyMMddHH',
count INT(10) NOT NULL COMMENT '抓拍次数'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '24小时抓拍统计表';