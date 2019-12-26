package com.techdevsolutions.nyt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techdevsolutions.common.service.core.DateUtils;
import com.techdevsolutions.common.service.core.FileUtils;
import com.techdevsolutions.nyt.beans.NytArticle;
import org.junit.Assert;
import org.junit.Before;
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

    @Before
    public void before() {
        String apiKey = System.getenv("nyt.apiKey");
        environment.setProperty("nyt.apiKey", apiKey);
    }

    @Test
    public void getYesterdaysNews() throws Exception {
        List<NytArticle> articleList = this.nytService.getYesterdaysNews();

        StringBuilder sb = new StringBuilder();
        ObjectMapper objectMapper = new ObjectMapper();

        articleList.forEach((i)->{
            try {
                sb.append(objectMapper.writeValueAsString(i) + "\n");
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

        Assert.assertTrue(articleList.size() > 0);
    }
}