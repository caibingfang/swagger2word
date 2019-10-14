package org.word.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.media.jfxmedia.logging.Logger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.word.dto.Parameter;
import org.word.dto.Request;
import org.word.service.ParameterService;
import org.word.utils.JsonUtils;

import java.io.IOException;
import java.util.*;

/**
 * @author :caibingfang
 * @description :
 * @date : 2019/10/14 10:01
 * @since 1.0.0
 **/
@Slf4j
@Service
public class ParameterServiceImpl implements ParameterService {

    @Override
    public Map<String, Object> buildParamMap(List<Request> list, Map<String, Object> map) throws IOException {
        Map<String, Object> paramMap = new HashMap<>(8);
        if (list != null && list.size() > 0) {
            for (Request request : list) {
                String name = request.getName();
                String type = request.getType();
                switch (type) {
                    case "string":
                        paramMap.put(name, "string");
                        break;
                    case "integer":
                        paramMap.put(name, 0);
                        break;
                    case "number":
                        paramMap.put(name, 0.0);
                        break;
                    case "boolean":
                        paramMap.put(name, true);
                        break;
                    case "body":
                        String paramType = request.getParamType();
                        ObjectNode objectNode = parseRef2Json(paramType, map);
                        paramMap = JsonUtils.readValue(objectNode.toString(), Map.class);
                        break;
                    default:
                        paramMap.put(name, null);
                        break;
                }
            }
        }
        return paramMap;
    }

    @Override
    public ObjectNode parseRef2Json(String ref, Map<String, Object> definitions) {
        log.info(ref);
        ObjectNode objectNode = JsonUtils.createObjectNode();
        if (StringUtils.isNotEmpty(ref) && ref.startsWith("#")) {
            String[] refs = ref.split("/");
            Map<String, Object> objectMap = (Map<String, Object>)definitions.get(refs[2]);
            //取出ref最后一个参数 end
            //取出参数
            if (objectMap == null) {
                return objectNode;
            }
            Object properties = objectMap.get("properties");
            if (properties == null) {
                return objectNode;
            }
            Map<String, Object> propertiesMap = (Map<String, Object>) properties;
            Set<String> keys = propertiesMap.keySet();
            //遍历key
            for (String key : keys) {
                Map<String, Object> keyMap = (Map) propertiesMap.get(key);
                if ("array".equals(keyMap.get("type"))) {
                    //数组的处理方式
                    String sonRef = (String) ((Map) keyMap.get("items")).get("$ref");
                    ArrayNode arrayNode = JsonUtils.createArrayNode();
                    if(sonRef == null) {
                        arrayNode.add((String)((Map) keyMap.get("items")).get("type"));
                        objectNode.set(key, arrayNode);
                    } else {
                        if(sonRef.equals(ref)) {
                            arrayNode.add(refs[2]);
                        } else {
                            JsonNode jsonNode = parseRef2Json(sonRef, definitions);
                            arrayNode.add(jsonNode);
                        }
                        objectNode.set(key, arrayNode);
                    }

                } else if (keyMap.get("$ref") != null) {
                    //对象的处理方式
                    String sonRef = (String) keyMap.get("$ref");

                    if(sonRef.equals(ref)) {
                        objectNode.put(key, refs[2]);
                    } else {
                        ObjectNode object = parseRef2Json(sonRef, definitions);
                        objectNode.set(key, object);
                    }


                } else {
                    //其他参数的处理方式，string、int
                    String str = "";
                    if (keyMap.get("description") != null) {
                        str = str + keyMap.get("description");
                    }
                    if (keyMap.get("format") != null) {
                        str = str + String.format("格式为(%s)", keyMap.get("format"));
                    }
                    objectNode.put(key, str);
                }
            }
        }
        return objectNode;
    }

    @Override
    public void parseRef2Table(String ref, Map<String, Object> definitions, LinkedHashMap<String, List<Parameter>> linkedHashMap) {
        if (StringUtils.isNotEmpty(ref) && ref.startsWith("#")) {
            String[] refs = ref.split("/");
            Map<String, Object> objectMap = (Map<String, Object>)definitions.get(refs[2]);
            //取出ref最后一个参数 end
            //取出参数
            if (objectMap == null) {
                return;
            }

            List requiredList = (List)objectMap.get("required");
            Object properties = objectMap.get("properties");
            if (properties == null) {
                return;
            }
            //防止自我依赖的类
            if (linkedHashMap.containsKey(refs[2])){
                return;
            }

            Map<String, Object> propertiesMap = (Map<String, Object>) properties;

            List<Parameter> list = new ArrayList<>();
            //外层的引用先put
            linkedHashMap.put(refs[2],list);
            Parameter parameter = null;
            //遍历key
            for (Map.Entry<String, Object> entry : propertiesMap.entrySet()) {
                parameter = new Parameter();
                Map<String, Object> keyMap = (Map) entry.getValue();
                String key = entry.getKey();
                parameter.setName(key);
                parameter.setRemark((String)keyMap.get("description"));
                if(requiredList == null) {
                    parameter.setRequire(false);
                } else {
                    parameter.setRequire(requiredList.contains(key));
                }

                if ("array".equals(keyMap.get("type"))) {

                    //数组的处理方式
                    String sonRef = (String) ((Map) keyMap.get("items")).get("$ref");
                    if(sonRef == null) {
                        parameter.setType("array:"+(String)((Map) keyMap.get("items")).get("type"));
                    } else {
                        parameter.setType("array:"+sonRef.replace("#/definitions/",""));
                        parseRef2Table(sonRef, definitions,linkedHashMap);
                    }

                } else if (keyMap.get("$ref") != null) {
                    //对象的处理方式
                    String sonRef = (String) keyMap.get("$ref");
                    parameter.setType(sonRef.replace("#/definitions/",""));
                    parseRef2Table(sonRef, definitions,linkedHashMap);
                } else {
                    parameter.setType((String)keyMap.get("type"));
                }
                list.add(parameter);
            }

        }
    }
}
