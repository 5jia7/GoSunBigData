package com.hzgc.service.people.dao;

import com.hzgc.service.people.model.House;

public interface HouseMapper {
    int deleteByPrimaryKey(Long id);

    int insert(House record);

    int insertSelective(House record);

    House selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(House record);

    int updateByPrimaryKey(House record);
}