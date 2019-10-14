package org.word.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.word.dto.Parameter;
import org.word.dto.Request;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author :caibingfang
 * @description :
 * @date : 2019/10/14 9:57
 * @since 1.0.0
 **/
public interface ParameterService {

    /**
     * 封装请求示例
     * @param list
     * @param map
     * @return
     * @throws IOException
     */
    Map<String, Object> buildParamMap(List<Request> list, Map<String, Object> map) throws IOException;


    /**
     * 解析引用类
     * @param ref ref链接 例如："#/definitions/PageInfoBT«Customer»"
     * @param definitions 所有的类的描述
     * @return
     */
    ObjectNode parseRef2Json(String ref, Map<String, Object> definitions);


    /**
     *
     * @param ref ref链接 例如："#/definitions/PageInfoBT«Customer»"
     * @param definitions
     * @param linkedHashMap
     */
    void parseRef2Table(String ref, Map<String, Object> definitions,LinkedHashMap<String,List<Parameter>> linkedHashMap);
}
