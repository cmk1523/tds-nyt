package com.techdevsolutions.nyt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techdevsolutions.common.dao.elasticsearch.BaseElasticsearchHighLevel;
import com.techdevsolutions.common.service.core.DateUtils;
import com.techdevsolutions.common.service.core.ElasticsearchUtils;
import com.techdevsolutions.nyt.beans.NytArticle;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class NytServiceTest {
    @Autowired
    Environment env;

    @Spy
    MockEnvironment environment = new MockEnvironment();

    NytService nytService = new NytService(environment);

    @Ignore
    @Test
    public void getYesterdaysNews() throws Exception {
        String apiKey = System.getenv("nyt.apiKey");

        if (StringUtils.isEmpty(apiKey)) {
            throw new Exception("NYT apiKey is null or empty. Please run this test with an environment. i.g. nyt.apiKey=123456");
        }

        environment.setProperty("nyt.apiKey", apiKey);

        List<NytArticle> articleList = this.nytService.getYesterdaysNews();

        StringBuilder sb = new StringBuilder();
        ObjectMapper objectMapper = new ObjectMapper();

        articleList.forEach((i)->{
            try {
                String itemAsString = objectMapper.writeValueAsString(i);
                sb.append(itemAsString + "\n");
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });

        String data = sb.toString();

        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Date date = new Date(yesterday.toEpochMilli());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        sdf.setTimeZone(TimeZone.getTimeZone(DateUtils.TIMEZONE_GMT));
        String startDateStr = sdf.format(date);

        File file = new File("/Users/chris/Downloads/nyt/nyt_" + startDateStr + ".json");
        org.apache.commons.io.FileUtils.writeStringToFile(file, data);

        String host = "localhost";
        BaseElasticsearchHighLevel baseElasticsearchHighLevel = new BaseElasticsearchHighLevel(host);
        ElasticsearchUtils.bulkIngestFromString(host, "news-nyt", data);

        Assert.assertTrue(articleList.size() > 0);
    }

//    @Ignore
    @Test
    public void getNews() throws Exception {
        String apiKey = System.getenv("nyt.apiKey");

        if (StringUtils.isEmpty(apiKey)) {
            throw new Exception("NYT apiKey is null or empty. Please run this test with an environment. i.g. nyt.apiKey=123456");
        }

        environment.setProperty("nyt.apiKey", apiKey);

//        Instant now = Instant.now();
//        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
//        Date date = new Date(yesterday.toEpochMilli());

        String startDateStr = "20191224";
        String endDateStr = "20191224";

        List<NytArticle> articleList = this.nytService.obtainNews(startDateStr, endDateStr);

        StringBuilder sb = new StringBuilder();
        ObjectMapper objectMapper = new ObjectMapper();

        articleList.forEach((i)->{
            try {
                String itemAsString = objectMapper.writeValueAsString(i);
                sb.append(itemAsString + "\n");
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });

        String data = sb.toString();

//        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
//        sdf.setTimeZone(TimeZone.getTimeZone(DateUtils.TIMEZONE_GMT));
//        String startDateStr = sdf.format(date);

        File file = new File("/Users/chris/Downloads/nyt/nyt_" + startDateStr + ".json");
        org.apache.commons.io.FileUtils.writeStringToFile(file, data);

//        String host = "localhost";
//        BaseElasticsearchHighLevel baseElasticsearchHighLevel = new BaseElasticsearchHighLevel(host);
//        ElasticsearchUtils.bulkIngestFromString(host, "news-nyt", data);

        Assert.assertTrue(articleList.size() > 0);
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