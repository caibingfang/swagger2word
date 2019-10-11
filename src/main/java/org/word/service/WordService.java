package org.word.service;

import org.word.dto.Table;

import java.util.List;
import java.util.Map;

/**
 * Created by XiuYin.Cui on 2018/1/12.
 */
public interface WordService {

    Map<String,List<Table>> tableList(String jsonUrl);
}
