package com.hzgc.service.community.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@ApiModel(value ="小区迁入迁出人口查询（疑似与确认）出参")
@Data
public class NewAndOutPeopleSearchVO implements Serializable {
    @ApiModelProperty(value = "总条数")
    private int totalNum;
    @ApiModelProperty(value = "小区迁入迁出人口查询（疑似与确认）返回对象封装")
    private List<NewAndOutPeopleSearch> voList;
}
