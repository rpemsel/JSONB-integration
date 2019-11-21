package com.jackis.jsonintegration.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/products", consumes = "application/json", produces = "application/json")
public class ProductController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductRepository productRepository;

    @GetMapping()
    public final List<Product> getProductByAttribute(@RequestParam String attribute, @RequestParam String value) {

        final String searchParamter = "{ \"" + attribute + "\":\"" + value + "\"}";

        LOGGER.info("Search Parameter: {}", searchParamter);

        return productRepository.findByProductAttribute(searchParamter)
                .orElse(Collections.emptyList())
                .stream()
                .map(product -> new Product(product.getSku(), null))
                .collect(Collectors.toList());
    }
}
