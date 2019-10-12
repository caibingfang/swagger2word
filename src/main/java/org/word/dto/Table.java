package org.word.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by XiuYin.Cui on 2018/1/11.
 */
@Data
public class Table {

    /**
     * 大标题
     */
    private String title;
    /**
     * 小标题
     */
    private String tag;
    /**
     * url
     */
    private String url;

    /**
     * 描述
     */
    private String description;

    /**
     * 请求参数格式
     */
    private String requestForm;

    /**
     * 响应参数格式
     */
    private String responseForm;

    /**
     * 请求方式
     */
    private String requestType;

    /**
     * 请求体
     */
    private List<Request> requestList;

    /**
     * 返回体
     */
    private List<Response> responseList;

    /**
     * 将入参的对象以表格的方式展示
     */
    private LinkedHashMap<String,List<Parameter>> requestStructure;

    /**
     * 将出参的对象以表格的方式展示
     */
    private LinkedHashMap<String,List<Parameter>> responseStructure;

    /**
     * 请求参数str
     */
    private String requestParam;

    /**
     * 返回参数str
     */
    private String responseParam;


}
