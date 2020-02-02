package com.techdevsolutions.nyt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techdevsolutions.common.dao.elasticsearch.BaseElasticsearchHighLevel;
import com.techdevsolutions.common.service.core.DateUtils;
import com.techdevsolutions.common.service.core.ElasticsearchUtils;
import com.techdevsolutions.common.service.core.FileUtils;
import com.techdevsolutions.common.service.core.HashUtils;
import com.techdevsolutions.nyt.beans.GeoCode;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class NytServiceTest {
    @Autowired
    Environment env;

    @Spy
    MockEnvironment environment = new MockEnvironment();

    GeocodeService geocodeService = new GeocodeService();
    NytService nytService = new NytService(this.environment, this.geocodeService);


    @Test
    public void autoDownload() throws Exception {
        String nytApiKey = System.getenv("nyt.apiKey");

        if (StringUtils.isEmpty(nytApiKey)) {
            System.out.println("\n\nNYT apiKey is null or empty. Please run this test with an environment. i.g. nyt.apiKey=123456\n\n");
            return;
        }

        String locationIqApiKey = System.getenv("locationiq.apiKey");

        if (StringUtils.isEmpty(locationIqApiKey)) {
            System.out.println("\n\nLocationIQ apiKey is null or empty. Please run this test with an environment. i.g. locationiq.apiKey=123456\n\n");
            return;
        }

        String directory = "";

        if (FileUtils.doesFileOrDirectoryExist("/Users/chris/Dropbox/")) {
            System.out.println("On Mac OS...");
            directory = "/Users/chris/Dropbox/Data/nyt";
        } else {
            System.out.println("On Windows...");
            directory = "C:/Users/Chris/Dropbox/Data/nyt";
        }

        this.nytService.autoDownload(nytApiKey, locationIqApiKey, directory);
    }

    @Ignore
    @Test
    public void ingestFromFile() throws Exception {
        String apiKey = System.getenv("locationiq.apiKey");

        if (StringUtils.isEmpty(apiKey)) {
            System.out.println("\n\nLocationIQ apiKey is null or empty. Please run this test with an environment. i.g. locationiq.apiKey=123456\n\n");
            return;
        }

        String host = "ubuntu-01";
        BaseElasticsearchHighLevel baseElasticsearchHighLevel = new BaseElasticsearchHighLevel(host);
        String baseIndexName = "nyt";
        ObjectMapper objectMapper = new ObjectMapper();
        GeocodeService geocodeService = new GeocodeService();

        String directoryStr = "C:/Users/Chris/Dropbox/Data/nyt";

        if (FileUtils.doesFileOrDirectoryExist("/Users/chris/Dropbox/")) {
            directoryStr = "/Users/chris/Dropbox/Data/nyt";
        }

        File directory = new File(directoryStr);
        Arrays.asList(directory.listFiles()).stream()
                .filter((i)->!i.isDirectory() && i.getName().startsWith("nyt_"))
                .forEach((i) -> {
                    try {
                        String dateAsMonthAsStr = i.getName()
                                .replace("nyt_", "")
                                .replace(".json","")
                                .substring(0,6);
                        String index = baseIndexName + "-" + dateAsMonthAsStr;
                        System.out.println("file: " + i.getName() + ", index: " + index);

                        File file = i.getAbsoluteFile();
                        String data = org.apache.commons.io.FileUtils.readFileToString(file, StandardCharsets.UTF_8.name());
                        List<String> split = new ArrayList<>(Arrays.asList(data.split("\n")));

                        split.forEach((articleAsJson)->{
                            try {
                                Map<String, Object> map = objectMapper.readValue(articleAsJson, Map.class);
                                map.put("@timestamp", map.get("dateStr"));
                                map.remove("dateStr");
                                map.remove("date");

                                Map<String, String> tags = (Map<String, String>) map.get("tags");
                                String locationStr =tags.get("glocations");

                                GeoCode geoCode = this.nytService.enrichLocationStringWithGeocodeService(locationStr, apiKey);
                                Map<String, Object> geoCodeMap = objectMapper.convertValue(geoCode, Map.class);
                                map.put("location", geoCodeMap);

                                String newJson = objectMapper.writeValueAsString(map);
                                String id = HashUtils.sha1(newJson.getBytes());

//                                IndexRequest indexRequest = (new IndexRequest(index))
//                                        .type("_doc")
//                                        .id(id)
//                                        .source(newJson, XContentType.JSON);
//                                baseElasticsearchHighLevel.getBulkProcessor().add(indexRequest);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            try {
                                Thread.sleep(501);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        Thread.sleep(501);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Ignore
    @Test
    public void testIngest() throws Exception {
        String fullPath = "/Users/chris/Downloads/nyt/nyt_20191225.json";
        String host = "localhost";
        BaseElasticsearchHighLevel baseElasticsearchHighLevel = new BaseElasticsearchHighLevel(host);
        ElasticsearchUtils.bulkIngestFromFile(host, "news-nyt", fullPath);
        Thread.sleep(3000);

        Assert.assertTrue(true);
    }
}