package com.microservice.movie_info_service.model;

public class MovieSummary {
    private String Title;
    private String Year;
    private String Plot;

    public MovieSummary() {}

    public String getTitle() {
        return Title;
    }

    public String getPlot() {
        return Plot;
    }

    public String getYear() {
        return Year;
    }
}
