package com.techdevsolutions.nyt;

import com.techdevsolutions.common.service.core.FileUtils;
import com.techdevsolutions.nyt.service.NytService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.io.IOException;

@SpringBootApplication
public class SpringBootConsoleApplication implements CommandLineRunner {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private Environment environment;
    private NytService nytService;

    public SpringBootConsoleApplication(Environment environment, NytService nytService) {
        this.environment = environment;
        this.nytService = nytService;
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringBootConsoleApplication.class, args);
    }

    @Override
    public void run(String... args) throws IOException {
        String nytApiKey = System.getenv("nyt.apiKey") != null ? System.getenv("nyt.apiKey")
                : this.environment.getProperty("nyt.apiKey");

        if (StringUtils.isEmpty(nytApiKey)) {
            logger.error("\n\nNYT apiKey is null or empty. Please run this test with an environment. i.g. nyt.apiKey=123456\n\n");
            return;
        }

        String locationIqApiKey = System.getenv("locationiq.apiKey") != null ? System.getenv("locationiq.apiKey")
                : this.environment.getProperty("locationiq.apiKey");

        if (StringUtils.isEmpty(locationIqApiKey)) {
            logger.error("\n\nLocationIQ apiKey is null or empty. Please run this test with an environment. i.g. locationiq.apiKey=123456\n\n");
            return;
        }

        String directory = "";

        if (FileUtils.doesFileOrDirectoryExist("/Users/chris/Dropbox/")) {
            logger.debug("On Mac OS...");
            directory = "/Users/chris/Dropbox/Data/nyt";
        } else {
            logger.debug("On Windows...");
            directory = "C:/Users/Chris/Dropbox/Data/nyt";
        }

        this.nytService.autoDownload(nytApiKey, locationIqApiKey, directory);
        System.exit(0);
    }
}