package com.techdevsolutions.nyt.beans;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NytArticle {
    private String title;
    private String headline;
    private String url;
    private String leadParagraph;
    private String source;
    private Date date;
    private String dateStr;
    private String documentType;
    private String newsDesk;
    private String section;
    private String subSection;
    private String materialType;
    private String text;
    private Map<String, String> tags = new HashMap<>();
    private GeoCode location;

    @Override
    public String toString() {
        return "NytArticle{" +
                "title='" + title + '\'' +
                ", headline='" + headline + '\'' +
                ", url='" + url + '\'' +
                ", leadParagraph='" + leadParagraph + '\'' +
                ", source='" + source + '\'' +
                ", date=" + date +
                ", dateStr='" + dateStr + '\'' +
                ", documentType='" + documentType + '\'' +
                ", newsDesk='" + newsDesk + '\'' +
                ", section='" + section + '\'' +
                ", subSection='" + subSection + '\'' +
                ", materialType='" + materialType + '\'' +
                ", text='" + text + '\'' +
                ", tags=" + tags +
                ", location=" + location +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NytArticle that = (NytArticle) o;
        return Objects.equals(title, that.title) &&
                Objects.equals(headline, that.headline) &&
                Objects.equals(url, that.url) &&
                Objects.equals(leadParagraph, that.leadParagraph) &&
                Objects.equals(source, that.source) &&
                Objects.equals(date, that.date) &&
                Objects.equals(dateStr, that.dateStr) &&
                Objects.equals(documentType, that.documentType) &&
                Objects.equals(newsDesk, that.newsDesk) &&
                Objects.equals(section, that.section) &&
                Objects.equals(subSection, that.subSection) &&
                Objects.equals(materialType, that.materialType) &&
                Objects.equals(text, that.text) &&
                Objects.equals(tags, that.tags) &&
                Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {

        return Objects.hash(title, headline, url, leadParagraph, source, date, dateStr, documentType, newsDesk, section, subSection, materialType, text, tags, location);
    }

    public GeoCode getLocation() {
        return location;
    }

    public void setLocation(GeoCode location) {
        this.location = location;
    }

    public String getText() {
        return text;
    }

    public NytArticle setText(String text) {
        this.text = text;
        return this;
    }

    public String getMaterialType() {
        return materialType;
    }

    public NytArticle setMaterialType(String materialType) {
        this.materialType = materialType;
        return this;
    }

    public String getSubSection() {
        return subSection;
    }

    public NytArticle setSubSection(String subSection) {
        this.subSection = subSection;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public NytArticle setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getHeadline() {
        return headline;
    }

    public NytArticle setHeadline(String headline) {
        this.headline = headline;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public NytArticle setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getLeadParagraph() {
        return leadParagraph;
    }

    public NytArticle setLeadParagraph(String leadParagraph) {
        this.leadParagraph = leadParagraph;
        return this;
    }

    public String getSource() {
        return source;
    }

    public NytArticle setSource(String source) {
        this.source = source;
        return this;
    }

    public Date getDate() {
        return date;
    }

    public NytArticle setDate(Date date) {
        this.date = date;
        return this;
    }

    public String getDateStr() {
        return dateStr;
    }

    public NytArticle setDateStr(String dateStr) {
        this.dateStr = dateStr;
        return this;
    }

    public String getDocumentType() {
        return documentType;
    }

    public NytArticle setDocumentType(String documentType) {
        this.documentType = documentType;
        return this;
    }

    public String getNewsDesk() {
        return newsDesk;
    }

    public NytArticle setNewsDesk(String newsDesk) {
        this.newsDesk = newsDesk;
        return this;
    }

    public String getSection() {
        return section;
    }

    public NytArticle setSection(String section) {
        this.section = section;
        return this;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public NytArticle setTags(Map<String, String> tags) {
        this.tags = tags;
        return this;
    }
}
