package com.techdevsolutions.nyt.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.techdevsolutions.common.service.core.HashUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class GeocodeService {
    private HttpClientConnectionManager poolingConnManager = new PoolingHttpClientConnectionManager();
    private CloseableHttpClient client = null;
    public Cache<String, String> cache = Caffeine.newBuilder()
            .maximumSize(100000)
            .build();

    public GeocodeService() {
        this.client = HttpClients.custom().setConnectionManager(this.poolingConnManager).build();
    }

    public String placeToCoordinate(String i, String apiKey) throws IOException, NoSuchAlgorithmException {
        String cacheId = HashUtils.sha1(i.getBytes());
        String json = this.cache.getIfPresent(cacheId);

        if (json == null) {
            HttpGet get = new HttpGet("https://us1.locationiq.com/v1/search.php?key=" + apiKey + "&q=" + i + "&format=json");
            HttpResponse response = client.execute(get);
            json = EntityUtils.toString(response.getEntity());

            if (!json.contains("Rate Limited Minute")) {
                this.cache.put(cacheId, json);
            }

            EntityUtils.consume(response.getEntity());
        }

        return json;
    }
}

