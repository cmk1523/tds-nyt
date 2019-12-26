package com.techdevsolutions.nyt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techdevsolutions.common.service.core.DateUtils;
import com.techdevsolutions.nyt.beans.NytArticle;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.techdevsolutions.common.service.core.Timer;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class NytService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private long INTERVAL = 6000;
    private double MAX_PER_PAGE = 10D;
    private CloseableHttpClient httpClient;
    private ObjectMapper objectMapper = new ObjectMapper();
    private Environment environment;

    public NytService(Environment environment) {
        this.environment = environment;
        this.httpClient = HttpClients
                .custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .build();
    }

    public List<NytArticle> getYesterdaysNews() throws Exception {
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Date yesterdayDate = new Date(yesterday.toEpochMilli());
        return this.obtainNews(yesterdayDate, yesterdayDate);
    }

    public List<NytArticle> obtainNews(Date startDate, Date endDate) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        sdf.setTimeZone(TimeZone.getTimeZone(DateUtils.TIMEZONE_GMT));

        String startDateStr = sdf.format(startDate);
        String endDateStr = sdf.format(endDate);

        String apiKey = this.environment.getProperty("nyt.apiKey");
        Integer page = 0;
        Integer pages = null;
        Integer hits = null;
        Integer docsProcessed = 0;
//        String sections = "fq=news_desk.contains:(\"Business\"%20\"Arts\"%20\"Automobiles\"%20\"Books\")";
        List<NytArticle> articles = new ArrayList<>();

        this.logger.info("obtaining articles from: " + startDate + "...");

        while((pages == null || page < pages)) {
            Timer timer = new Timer().start();

            String url = "https://api.nytimes.com/svc/search/v2/articlesearch.json" +
                    "?begin_date=" + startDateStr +
                    "&end_date=" + endDateStr +
                    "&api-key=" + apiKey +
                    "&page=" + page;

            HttpGet request = new HttpGet(url);
            CloseableHttpResponse closeableHttpResponse = httpClient.execute(request);
            List<Map<String, Object>> docs = new ArrayList<>();

            try {
                HttpEntity entity = closeableHttpResponse.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    Map<String, Object> map = this.objectMapper.readValue(result, Map.class);
                    Map<String, Object> response = (Map<String, Object>) map.get("response");

                    if (response == null) {
                        throw new RuntimeException("Unable to obtain data. result: " + result);
                    }

                    Map<String, Object> meta = (Map<String, Object>) response.get("meta");
                    hits = (int) meta.get("hits");
                    pages = (int) Math.ceil(hits / MAX_PER_PAGE);
                    docs = (List<Map<String, Object>>) response.get("docs");
                    List<NytArticle> articleList = docs.parallelStream().map((i) -> {
                        try {
                            return this.toNytArticle(i);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }).collect(Collectors.toList());
                    articles.addAll(articleList);

                }
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
            }

            docsProcessed += docs.size();
            page++;
            long took = timer.stopAndGetDiff();

            this.logger.info("obtained " + (docsProcessed) + "/" + hits + ", took: " + took + "ms...");

            if (took < INTERVAL) {
                Thread.sleep(INTERVAL - took + 1);
            }
        }

        Map<String, NytArticle> articleMap = new HashMap<>();
        articles.forEach((i)->articleMap.put(i.getUrl(), i));

        articles = articleMap.entrySet().stream()
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparing(NytArticle::getDate))
                .collect(Collectors.toList());

//        Set<String> agg_sources = articles.stream().map(NytArticle::getSource).collect(Collectors.toSet());
//        Set<String> agg_sections = articles.stream().map(NytArticle::getSection).collect(Collectors.toSet());
//        Set<String> agg_newsDesk = articles.stream().map(NytArticle::getNewsDesk).collect(Collectors.toSet());

        this.logger.info("obtained " + docsProcessed + "/" + hits + "... DONE");
        this.logger.info("number of articles processed: " + docsProcessed);
        return articles;
    }

    public String getArticleText(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        CloseableHttpResponse closeableHttpResponse = httpClient.execute(request);
        List<Map<String, Object>> docs = new ArrayList<>();

        try {
            HttpEntity entity = closeableHttpResponse.getEntity();
            if (entity != null) {
                String html = EntityUtils.toString(entity);

                Document document = Jsoup.parse(html);
                Elements sections = document.getElementsByAttributeValue("name", "articleBody");

                if (sections.size() == 1) {
                    return sections.text();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.logger.warn("Unable to obtain text from URL: " + url);
        return null;
    }

    public NytArticle toNytArticle(Map<String, Object> doc) throws ParseException, IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

        NytArticle i = new NytArticle();
        i.setTitle((String) doc.get("abstract"));
        i.setUrl((String) doc.get("web_url"));
        i.setLeadParagraph((String) doc.get("lead_paragraph"));
        i.setSource((String) doc.get("source"));
        i.setDocumentType((String) doc.get("document_type"));
        i.setMaterialType((String) doc.get("type_of_material"));
        i.setNewsDesk((String) doc.get("news_desk"));
        i.setSection((String) doc.get("section_name"));
        i.setSubSection((String) doc.get("subsection_name"));
        i.setNewsDesk((String) doc.get("news_desk"));

        i.setDateStr((String) doc.get("pub_date"));
        i.setDate(sdf.parse(i.getDateStr()));
        i.setDateStr(DateUtils.DateToISO(i.getDate()));

        Map<String, Object> headline = (Map<String, Object>) doc.get("headline");
        i.setHeadline((String) headline.get("main"));

        List<Map<String, Object>> keywords = (List<Map<String, Object>>) doc.get("keywords");
        keywords.forEach((j)->{
            i.getTags().put((String) j.get("name"), (String) j.get("value"));
        });

        String text = this.getArticleText(i.getUrl());

        if (StringUtils.isNotEmpty(text)) {
            i.setText(text);
        }

        return i;
    }
}
