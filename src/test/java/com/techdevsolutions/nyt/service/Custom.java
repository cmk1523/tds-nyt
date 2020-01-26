package com.techdevsolutions.nyt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techdevsolutions.common.service.core.FileUtils;
import com.techdevsolutions.common.service.core.HashUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Ignore
public class Custom {
    @Test
    public void countries() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File("C:\\Users\\Chris\\Desktop\\countries.json");
        Map<String, Object> map = objectMapper.readValue(file, Map.class);
        List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("countries");
        System.out.println(map);

        String elasticIndex = "countries";
        StringBuilder sb = new StringBuilder();

        list.forEach((i)->{
            try {
                String json = objectMapper.writeValueAsString(i);
                String id = HashUtils.md5(json.getBytes());
                sb.append("{ \"index\" : { \"_index\" : \"" + elasticIndex + "\", \"_id\" : \"" + id + "\" } }\n");
                sb.append(json + "\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        org.apache.commons.io.FileUtils.writeStringToFile(
                new File("C:\\Users\\Chris\\Desktop\\countries-elastic-bulk.json"), sb.toString());
    }

    @Test
    public void cities() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File("C:\\Users\\Chris\\Desktop\\cities.json");
        Map<String, Object> map = objectMapper.readValue(file, Map.class);
        List<Map<String, Object>> cities = (List<Map<String, Object>>) map.get("cities");
        System.out.println(map);

        String elasticIndex = "cities";
        StringBuilder sb = new StringBuilder();

        cities.forEach((i)->{
            try {
                String json = objectMapper.writeValueAsString(i);
                String id = HashUtils.md5(json.getBytes());
                i.put("lat", Double.valueOf((String) i.get("lat")));
                i.put("lng", Double.valueOf((String) i.get("lng")));
                json = objectMapper.writeValueAsString(i);
                sb.append("{ \"index\" : { \"_index\" : \"" + elasticIndex + "\", \"_id\" : \"" + id + "\" } }\n");
                sb.append(json + "\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        org.apache.commons.io.FileUtils.writeStringToFile(
                new File("C:\\Users\\Chris\\Desktop\\cities-elastic-bulk.json"), sb.toString());
    }
}

