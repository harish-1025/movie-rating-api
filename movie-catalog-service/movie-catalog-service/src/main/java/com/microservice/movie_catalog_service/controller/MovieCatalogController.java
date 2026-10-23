package com.microservice.movie_catalog_service.controller;

import com.microservice.movie_catalog_service.model.CatalogItem;
import com.microservice.movie_catalog_service.model.Movie;
import com.microservice.movie_catalog_service.model.Rating;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

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

    @GetMapping("/{userId}")
    public List<CatalogItem> getCatalog(@PathVariable int userId){


        List<Rating>  ratings = Arrays.asList(
                new Rating("1234",4),
                new Rating("5678",5)
        );

        return ratings.stream().map(rating ->{
            Movie movie=restTemplate.getForObject("http://localhost:8082/movies/"+rating.getMovieId(),Movie.class);
            return new CatalogItem(movie.getName(), "test",rating.getRating());
        }).collect(Collectors.toList());


    }
}
