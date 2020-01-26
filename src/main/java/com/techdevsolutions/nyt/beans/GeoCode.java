package com.techdevsolutions.nyt.beans;

import java.util.Objects;

public class GeoCode {
    private String location;
    private String name;
    private Boolean cacheHit = false;

    public GeoCode() {
    }

    public GeoCode(String location, String name) {
        this.location = location;
        this.name = name;
    }

    public GeoCode(String location, String name, Boolean cacheHit) {
        this.location = location;
        this.name = name;
        this.cacheHit = cacheHit;
    }

    @Override
    public String toString() {
        return "GeoCode{" +
                "location='" + location + '\'' +
                ", name='" + name + '\'' +
                ", cacheHit=" + cacheHit +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeoCode geoCode = (GeoCode) o;
        return Objects.equals(location, geoCode.location) &&
                Objects.equals(name, geoCode.name) &&
                Objects.equals(cacheHit, geoCode.cacheHit);
    }

    @Override
    public int hashCode() {

        return Objects.hash(location, name, cacheHit);
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getCacheHit() {
        return cacheHit;
    }

    public void setCacheHit(Boolean cacheHit) {
        this.cacheHit = cacheHit;
    }
}
