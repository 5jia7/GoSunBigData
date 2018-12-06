package com.hzgc.cloud.dynrepo.bean;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchCollection implements Serializable {
    private SearchOption searchOption;
    private SearchResult searchResult;
}
