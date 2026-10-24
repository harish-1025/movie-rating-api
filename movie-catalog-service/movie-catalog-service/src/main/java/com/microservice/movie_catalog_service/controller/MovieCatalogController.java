package com.microservice.movie_catalog_service.controller;

import com.microservice.movie_catalog_service.model.CatalogItem;
import com.microservice.movie_catalog_service.model.Movie;
import com.microservice.movie_catalog_service.model.Rating;
import com.microservice.movie_catalog_service.model.UserRating;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/catalog")
public class MovieCatalogController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @GetMapping("/{userId}")
    public List<CatalogItem> getCatalog(@PathVariable int userId){


        UserRating ratings=restTemplate.getForObject("http://rating-data-service/ratings/user/"+userId, UserRating.class);

        return ratings.getRatings().stream().map(rating ->{
            Movie movie=restTemplate.getForObject("http://movie-info-service/movies/"+rating.getMovieId(),Movie.class);
            return new CatalogItem(movie.getName(), "test",rating.getRating());
        }).collect(Collectors.toList());


    }
}
