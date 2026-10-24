package com.microservice.rating_data_service.controller;

import com.microservice.rating_data_service.model.Rating;
import com.microservice.rating_data_service.model.UserRating;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/ratings")
public class RatingInfoController {

    @GetMapping("/{movieId}")
    public Rating getRating(@PathVariable("movieId") String movieId) {
        return new Rating(movieId, 4);
    }

    @GetMapping("/user/{userId}")
    public UserRating getMovies(@PathVariable String userId) {
        List<Rating> ratings = Arrays.asList(
                new Rating("1234",4),
                new Rating("5678",5)
        );
        UserRating userRating = new UserRating();
        userRating.setRatings(ratings);
        return  userRating;
    }


}
