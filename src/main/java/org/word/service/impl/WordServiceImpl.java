package org.word.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.word.dto.Parameter;
import org.word.dto.Request;
import org.word.dto.Response;
import org.word.dto.Table;
import org.word.service.ParameterService;
import org.word.service.WordService;
import org.word.utils.JsonUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author XiuYin.Cui
 * @Date 2018/1/12
 **/
@Slf4j
@Service
public class WordServiceImpl implements WordService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${swagger.url}")
    private String swaggerUrl;

    @Autowired
    private ParameterService parameterService;

    @Override
    public Map<String,List<Table>> tableList(String swaggerUi) {
        swaggerUi = Optional.ofNullable(swaggerUi).orElse(swaggerUrl);
        List<Table> result = new ArrayList<>();
        try {
            String jsonUrl = swaggerUi.replace("swagger-ui.html","v2/api-docs");
            String jsonStr = restTemplate.getForObject(jsonUrl, String.class);
            // convert JSON string to Map
            Map<String, Object> map = JsonUtils.readValue(jsonStr, HashMap.class);

            //tag层
            ArrayList<LinkedHashMap> allTags = (ArrayList) map.get("tags");
            Map<String,String> tagMap = buildTagMap(allTags);

            //解析paths
            Map<String, LinkedHashMap> paths = (LinkedHashMap) map.get("paths");

            //参数对象的描述
            Map<String, Object> definitions = (Map<String, Object>) map.get("definitions");
            if (paths != null) {
                Iterator<Map.Entry<String, LinkedHashMap>> it = paths.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, LinkedHashMap> path = it.next();

                    Iterator<Map.Entry<String, LinkedHashMap>> it2 = path.getValue().entrySet().iterator();
                    // 1.请求路径
                    String url = path.getKey();
                    // 2.请求方式，类似为 get,post,delete,put 这样
                    String requestType = StringUtils.join(path.getValue().keySet(), ",");
                    //不管有几种请求方式，都只解析第一种
                    Map.Entry<String, LinkedHashMap> firstRequest = it2.next();
                    Map<String, Object> content = firstRequest.getValue();
                    // 4. 大标题（类说明）
                    String title = String.valueOf(((List) content.get("tags")).get(0));
                    // 5.小标题 （方法说明）
                    String tag = String.valueOf(content.get("summary"));
                    // 6.接口描述
                    String description = String.valueOf(content.get("summary"));
                    // 7.请求参数格式，类似于 multipart/form-data
                    String requestForm = "";
                    List<String> consumes = (List) content.get("consumes");
                    if (consumes != null && consumes.size() > 0) {
                        requestForm = StringUtils.join(consumes, ",");
                    }
                    // 8.返回参数格式，类似于 application/json
                    String responseForm = "";
                    List<String> produces = (List) content.get("produces");
                    if (produces != null && produces.size() > 0) {
                        responseForm = StringUtils.join(produces, ",");
                    }
                    // 9. 请求体
                    List<Request> requestList = new ArrayList<>();
                    List<LinkedHashMap> parameters = (ArrayList) content.get("parameters");
                    if (!CollectionUtils.isEmpty(parameters)) {
                        for (Map<String, Object> param : parameters) {
                            Request request = new Request();
                            request.setName(String.valueOf(param.get("name")));
                            Object in = param.get("in");
                            if (in != null && "body".equals(in)) {
                                request.setType(String.valueOf(in));
                                Map<String, Object> schema = (Map) param.get("schema");
                                Object ref = schema.get("$ref");
                                // 数组情况另外处理
                                if (schema.get("type") != null && "array".equals(schema.get("type"))) {
                                    ref = ((Map) schema.get("items")).get("$ref");
                                }
                                request.setParamType(ref == null ? "{}" : ref.toString());
                            } else {
                                request.setType(String.valueOf(in));
                                request.setParamType(param.get("type") == null ? "Object" : param.get("type").toString());
                            }
                            request.setRequire((Boolean) param.get("required"));
                            request.setRemark(String.valueOf(param.get("description")));
                            requestList.add(request);
                        }
                    }
                    // 10.返回体
                    List<Response> responseList = new ArrayList<>();
                    Map<String, Object> responses = (LinkedHashMap) content.get("responses");
                    Iterator<Map.Entry<String, Object>> it3 = responses.entrySet().iterator();

                    while (it3.hasNext()) {
                        Response response = new Response();
                        Map.Entry<String, Object> entry = it3.next();
                        // 状态码 200 201 401 403 404 这样
                        response.setName(entry.getKey());
                        LinkedHashMap<String, Object> statusCodeInfo = (LinkedHashMap) entry.getValue();
                        response.setDescription(String.valueOf(statusCodeInfo.get("description")));
                        response.setRemark(String.valueOf(statusCodeInfo.get("description")));
                        responseList.add(response);
                    }

                    //封装Table
                    Table table = new Table();
                    table.setTitle(tagMap.get(title)==null?title:tagMap.get(title));
                    table.setUrl(url);
                    table.setTag(tag);
                    table.setDescription(description);
                    table.setRequestForm(requestForm);
                    table.setResponseForm(responseForm);
                    table.setRequestType(requestType);
                    table.setResponseList(responseList);
                    table.setRequestParam(JsonUtils.prettyString(parameterService.buildParamMap(requestList, definitions)));
                    LinkedHashMap<String,List<Parameter>> requestLinkedHashMap = new LinkedHashMap<String,List<Parameter>>();
                    for (Request request : requestList) {
                        parameterService.parseRef2Table(request.getParamType(), definitions,requestLinkedHashMap);
                        request.setParamType(request.getParamType().replaceAll("#/definitions/", ""));
                    }
                    table.setRequestList(requestList);
                    table.setRequestStructure(requestLinkedHashMap);
                    // 取出来状态是200时的返回值
                    Object obj = responses.get("200");
                    if (obj == null) {
                        table.setResponseParam("");
                        result.add(table);
                        continue;
                    }
                    Object schema = ((Map) obj).get("schema");
                    if(schema!=null) {
                        if (((Map) schema).get("$ref") != null) {
                            //非数组类型返回值
                            String ref = (String) ((Map) schema).get("$ref");
                            //解析swagger2 ref链接
                            ObjectNode objectNode = parameterService.parseRef2Json(ref, definitions);
                            LinkedHashMap<String,List<Parameter>> responseLinkedHashMap = new LinkedHashMap<String,List<Parameter>>();
                            parameterService.parseRef2Table(ref, definitions,responseLinkedHashMap);
                            table.setResponseParam(JsonUtils.prettyString(objectNode));
                            table.setResponseStructure(responseLinkedHashMap);
                            result.add(table);
                            continue;
                        }
                        Object items = ((Map) schema).get("items");
                        if (items != null && ((Map) items).get("$ref") != null) {
                            //数组类型返回值
                            String ref = (String) ((Map) items).get("$ref");
                            //解析swagger2 ref链接
                            ObjectNode objectNode = parameterService.parseRef2Json(ref, definitions);
                            ArrayNode arrayNode = JsonUtils.createArrayNode();
                            arrayNode.add(objectNode);
                            LinkedHashMap<String,List<Parameter>> responseLinkedHashMap = new LinkedHashMap<String,List<Parameter>>();
                            parameterService.parseRef2Table(ref, definitions,responseLinkedHashMap);
                            table.setResponseStructure(responseLinkedHashMap);
                            table.setResponseParam(JsonUtils.prettyString(arrayNode));
                            result.add(table);
                            continue;
                        }
                    }
                    result.add(table);

                }
            }
        } catch (Exception e) {
            log.error("parse error", e);
        }
        Map<String,List<Table>> tableMap = result.stream().collect(Collectors.groupingBy(Table::getTitle));
        return tableMap;
    }

    /**
     * 讲tags标签包含的数组转为map
     * @param allTags
     * @return
     */
    private Map<String,String> buildTagMap(ArrayList<LinkedHashMap> allTags) {
        Map<String,String> tagMap = new HashMap<>();
        for(LinkedHashMap map:allTags) {
            tagMap.put((String)map.get("name"),(String)map.get("description"));
        }
        return tagMap;

    }

}
