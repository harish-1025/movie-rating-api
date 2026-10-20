package com.microservice.movie_catalog_service.controller;

import com.microservice.movie_catalog_service.model.CatalogItem;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/catalog")
public class MovieCatalogController {

    @GetMapping("/{userId}")
    public List<CatalogItem> getCatalog(@PathVariable int userId){
        return Collections.singletonList(
                new CatalogItem("Titanic","testing",4)
        );

    }
}
