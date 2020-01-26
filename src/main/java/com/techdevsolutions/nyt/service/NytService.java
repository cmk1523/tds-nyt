package com.techdevsolutions.nyt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techdevsolutions.common.dao.elasticsearch.BaseElasticsearchHighLevel;
import com.techdevsolutions.common.service.core.*;
import com.techdevsolutions.common.service.core.Timer;
import com.techdevsolutions.nyt.beans.GeoCode;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NytService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private long INTERVAL = 6000;
    private double MAX_PER_PAGE = 10D;
    private CloseableHttpClient httpClient;
    private ObjectMapper objectMapper = new ObjectMapper();
    private Environment environment;
    private GeocodeService geocodeService;

    @Autowired
    public NytService(Environment environment, GeocodeService geocodeService) {
        this.environment = environment;
        this.geocodeService = geocodeService;

        this.httpClient = HttpClients
                .custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .build();
    }

    public GeoCode enrichLocationStringWithGeocodeService(String locationStr, String apiKey) throws IOException, NoSuchAlgorithmException {
        if (StringUtils.isEmpty(locationStr)) {
            this.logger.warn("locationStr is empty");
            return null;
        } else if (StringUtils.isEmpty(apiKey)) {
            this.logger.warn("apiKey is empty");
            return null;
        }

        String locationStrEncoded = UriUtils.encodePath(locationStr, StandardCharsets.UTF_8.name());
        String cacheId = HashUtils.sha1(locationStrEncoded.getBytes());
        String geocodeStr = this.geocodeService.cache.getIfPresent(cacheId);
        GeoCode geoCode = new GeoCode();

        if (geocodeStr == null) {
            geocodeStr = geocodeService.placeToCoordinate(locationStrEncoded, apiKey);
            geoCode.setCacheHit(false);
        } else {
            geoCode.setCacheHit(true);
        }

        if (StringUtils.isNotEmpty(geocodeStr)) {
            String geocodeObj = "{\"data\": " + geocodeStr + "}";
//            System.out.println(geocodeObj);

            try {
                Map<String, Object> geocodeMap = objectMapper.readValue(geocodeObj, Map.class);

                List<Map<String, Object>> geoCodeList = (List<Map<String, Object>>) geocodeMap.get("data");

                Map<String, Object> geoCodeItem = geoCodeList.get(0);

                if (geoCodeItem != null) {
                    Double latitude = geoCodeItem.get("lat") != null ? Double.valueOf((String) geoCodeItem.get("lat")) : null;
                    Double longitude = geoCodeItem.get("lon") != null ? Double.valueOf((String) geoCodeItem.get("lon")) : null;
                    String name = (String) geoCodeItem.get("display_name");
                    geoCode.setLocation(latitude + "," + longitude);
                    geoCode.setName(name);
                    return geoCode;
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.debug("locationStr: " + locationStr);
                logger.debug("geocodeObj: " + geocodeObj);
            }
        }

        return null;
    }

    public String enrichArticleWithText(String url) throws IOException {
        String text = this.getArticleText(url);

        if (StringUtils.isNotEmpty(text)) {
            text = text.replace(" Follow The New York Times Opinion section on Facebook, Twitter (@NYTopinion) and Instagram.", "");
        }

        return text;
    }




    public void autoDownload(String nytApiKey, String locationIqApiKey, String baseDirectory) throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        sdf.setTimeZone(TimeZone.getTimeZone(DateUtils.TIMEZONE_GMT));
        Date date = FileUtils.getYesterdaysDate(new Date(), TimeZone.getTimeZone(DateUtils.TIMEZONE_GMT));
        Map<String, Object> state = this.getState(baseDirectory);

        while(true) {
            String dateStr = sdf.format(date);
            String fullPath = baseDirectory + "/nyt_" + dateStr + ".json";
            Set<String> finishedDates = state.get("finishedDates") != null ? new HashSet<>((ArrayList<String>) state.get("finishedDates")) : new HashSet<>();

            if (dateStr.equals("20180828")) {
                break;
            } else if (finishedDates.contains(dateStr)) {
                logger.debug(dateStr + " exists within state database");
            } else if(FileUtils.doesFileOrDirectoryExist(fullPath)) {
                logger.debug(fullPath + " exists");
                this.addFinishedDateAndSaveState(baseDirectory, dateStr);
            } else {
                logger.debug(fullPath + " does not exist");

                try {
                    this.getAndStoreNews(date, nytApiKey, locationIqApiKey, baseDirectory);
                    this.addFinishedDateAndSaveState(baseDirectory, dateStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }

            date = FileUtils.getYesterdaysDate(date, TimeZone.getTimeZone(DateUtils.TIMEZONE_GMT));
        }
    }

    public void addFinishedDateAndSaveState(String baseDirectory, String dateStr) throws IOException {
        Map<String, Object> state = this.getState(baseDirectory);
        Set<String> finishedDates = state.get("finishedDates") != null ? new HashSet<>((ArrayList<String>) state.get("finishedDates")) : new HashSet<>();
        finishedDates.add(dateStr);
        state.put("finishedDates", finishedDates);
        this.saveState(baseDirectory, state);
    }

    public void getAndStoreNews(Date date, String nytApiKey, String locationIqApiKey, String baseDirectory) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        sdf.setTimeZone(TimeZone.getTimeZone(DateUtils.TIMEZONE_GMT));
        String startDateStr = sdf.format(date);
        String endDateStr = startDateStr;

        List<NytArticle> articleList = this.obtainNews(startDateStr, endDateStr, nytApiKey);

        logger.debug("enriching... ");

        for (int i = 0; i < articleList.size(); i++) {
            NytArticle nytArticle = articleList.get(i);

            Timer timer = new Timer().start();
            GeoCode geoCode = new GeoCode();

            try {
                Map<String, String> tags = nytArticle.getTags();
                String locationStr = tags.get("glocations");

                if (StringUtils.isNotEmpty(locationStr)) {
                    geoCode = this.enrichLocationStringWithGeocodeService(locationStr, locationIqApiKey);

                    if (geoCode != null) {
                        nytArticle.setLocation(geoCode);

//                        if (!geoCode.getCacheHit()) {
//                            logger.debug(".");
//                        }
                    }
                }

                if (StringUtils.isNotEmpty(nytArticle.getUrl())) {
                    String text = this.enrichArticleWithText(nytArticle.getUrl());
                    nytArticle.setText(text);
//                    logger.debug(".");
                }
            } catch (Exception e) {
                e.printStackTrace();
                i--;
            }

            long took = timer.stopAndGetDiff();

            if (took >= 0 && took < 501) {
                if (geoCode == null || (geoCode != null && !geoCode.getCacheHit())) {
                    try {
                        Thread.sleep(501 - took);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            double percent = (i / (articleList.size() * 1D)) * 100;
            percent = Math.round(percent * 10) / 10.0;

            if (i % 10 == 0) {
                this.logger.info("enriched " + i + "/" + articleList.size() + " (" + percent + "%), took: " + took + "ms...");
            }
        }

//        List<NytArticle> articleListEnriched = articleList.stream().map((nytArticle -> {
//            Timer timer = new Timer().start();
//            GeoCode geoCode = new GeoCode();
//
//            try {
//                Map<String, String> tags = nytArticle.getTags();
//                String locationStr = tags.get("glocations");
//
//                if (StringUtils.isNotEmpty(locationStr)) {
//                    geoCode = this.enrichLocationStringWithGeocodeService(locationStr, locationIqApiKey);
//
//                    if (geoCode != null) {
//                        nytArticle.setLocation(geoCode);
//
////                        if (!geoCode.getCacheHit()) {
////                            logger.debug(".");
////                        }
//                    }
//                }
//
//                if (StringUtils.isNotEmpty(nytArticle.getUrl())) {
//                    String text = this.enrichArticleWithText(nytArticle.getUrl());
//                    nytArticle.setText(text);
////                    logger.debug(".");
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            long diff = timer.stopAndGetDiff();
//
//            if (diff >= 0 && diff < 500) {
//                if (!geoCode.getCacheHit()) {
//                    try {
//                        Thread.sleep(500 - diff);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//            return nytArticle;
//        })).collect(Collectors.toList());

        logger.debug("enriching... DONE");

        this.articlesToFiles(articleList, baseDirectory, startDateStr);
//        this.nytService.articlesToElasticsearch(articleList, "localhost");
    }

    public String articlesToNewLineStringList(List<NytArticle> list) {
        StringBuilder sb = new StringBuilder();
        ObjectMapper objectMapper = new ObjectMapper();

        list.forEach((i)->{
            try {
                String itemAsString = objectMapper.writeValueAsString(i);
                sb.append(itemAsString + "\n");
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });

        return sb.toString();
    }

    public void saveState(String fullPath, Map<String, Object> state) throws IOException {
        String data = this.objectMapper.writeValueAsString(state);
        File file = new File(fullPath + "/state.json");
        org.apache.commons.io.FileUtils.writeStringToFile(file, data);
    }

    public Map<String, Object> getState(String fullPath) {
        try {
            File file = new File(fullPath + "/state.json");
            String data = org.apache.commons.io.FileUtils.readFileToString(file);
            return this.objectMapper.readValue(data, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> state = new HashMap<>();
            return state;
        }
    }

    public void articlesToFiles(List<NytArticle> list, String basePath, String dateStr) throws IOException {
        String data = this.articlesToNewLineStringList(list);
        File file = new File(basePath + "/nyt_" + dateStr + ".json");
        org.apache.commons.io.FileUtils.writeStringToFile(file, data);
    }

    public void articlesToElasticsearch(List<NytArticle> list, String host) throws IOException {
        String data = this.articlesToNewLineStringList(list);
        BaseElasticsearchHighLevel baseElasticsearchHighLevel = new BaseElasticsearchHighLevel(host);
        ElasticsearchUtils.bulkIngestFromString(host, "news-nyt", data);
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

        return this.obtainNews(startDateStr, endDateStr);
    }

    public List<NytArticle> obtainNews(String startDateStr, String endDateStr) throws Exception {
        String apiKey = this.environment.getProperty("nyt.apiKey");
        return this.obtainNews(startDateStr, endDateStr, apiKey);
    }

    public List<NytArticle> obtainNews(String startDateStr, String endDateStr, String nytApiKey) throws Exception {
        Integer page = 0;
        Integer pages = null;
        Integer hits = null;
        Integer docsProcessed = 0;
//        String sections = "fq=news_desk.contains:(\"Business\"%20\"Arts\"%20\"Automobiles\"%20\"Books\")";
        List<NytArticle> articles = new ArrayList<>();

        this.logger.info("obtaining articles from: " + startDateStr + "...");

        while((pages == null || page < pages)) {
            Timer timer = new Timer().start();

            String url = "https://api.nytimes.com/svc/search/v2/articlesearch.json" +
                    "?begin_date=" + startDateStr +
                    "&end_date=" + endDateStr +
                    "&api-key=" + nytApiKey +
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
            double percent = hits > 0 ? (docsProcessed / (hits * 1D)) * 100 : 0;
            percent = Math.round(percent * 10) / 10.0;

            this.logger.info("obtained " + (docsProcessed) + "/" + hits + " (" + percent + "%), took: " + took + "ms...");

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

        // this.logger.warn("Unable to obtain text from URL: " + url);
        return null;
    }

    public NytArticle toNytArticle(Map<String, Object> doc) throws ParseException, IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

        NytArticle i = new NytArticle();
        i.setTitle((String) doc.get("abstract"));

        if (StringUtils.isEmpty(i.getTitle())) {
            i.setTitle((String) doc.get("snippet"));
        }

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

        if (!i.getTags().containsKey("glocations")) {
            int pos = i.getLeadParagraph().indexOf(" — ");

            if (pos >= 1 && pos < 50) {
                String approxLocation = i.getLeadParagraph().substring(0, pos);
                i.getTags().put("glocations", approxLocation);
            }
        }

        return i;
    }
}
