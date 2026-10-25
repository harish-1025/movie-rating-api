package com.microservice.movie_catalog_service.controller;

import com.microservice.movie_catalog_service.model.CatalogItem;
import com.microservice.movie_catalog_service.model.Movie;
import com.microservice.movie_catalog_service.model.UserRating;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
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
    @CircuitBreaker(name="MovieCatalogService",fallbackMethod ="catlogFallback")
    public List<CatalogItem> getCatalog(@PathVariable int userId){


        UserRating ratings=restTemplate.getForObject("http://rating-data-service/ratings/user/"+userId, UserRating.class);

        return ratings.getRatings().stream().map(rating ->{
            Movie movie=restTemplate.getForObject("http://movie-info-service/movies/"+rating.getMovieId(),Movie.class);
            return new CatalogItem(movie.getName(), movie.getDescription(), rating.getRating());
        }).collect(Collectors.toList());


    }

    public List<CatalogItem> catlogFallback(int userId,Throwable e){
        System.out.println("Error msg:"+e.getMessage());
        return List.of(new CatalogItem("fallback","This is a fallback method",0));
    }

    }
