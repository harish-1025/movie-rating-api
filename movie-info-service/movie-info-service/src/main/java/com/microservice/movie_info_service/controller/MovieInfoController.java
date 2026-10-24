package com.microservice.movie_info_service.controller;

import com.microservice.movie_info_service.model.Movie;
import com.microservice.movie_info_service.model.MovieSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/movies")
public class MovieInfoController {

    @Value("${api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping("/{movieId}")
    public Movie getMovieInfo(@PathVariable("movieId") String movieId) {
        MovieSummary movieSummary = restTemplate.getForObject(
                "http://www.omdbapi.com/?i=" + movieId + "&apikey=" + apiKey,
                MovieSummary.class
        );

        return new Movie(
                movieId,
                movieSummary.getTitle(),
                movieSummary.getPlot()
        );

    }


}
