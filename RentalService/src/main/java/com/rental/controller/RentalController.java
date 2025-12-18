package com.rental.controller;

import com.rental.model.Car;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@RestController
public class RentalController {

    @Value("${customer.service.url}")
    private String customerServiceUrl;

    private final List<Car> cars;

    Logger logger = org.slf4j.LoggerFactory.getLogger(RentalController.class);

    public RentalController() {
        // Initialisation de la liste des voitures en mémoire
        this.cars = new ArrayList<>();
        cars.add(new Car("AA-123-BB", "Renault", 45.0));
        cars.add(new Car("CC-456-DD", "Peugeot", 50.0));
        cars.add(new Car("EE-789-FF", "Citroën", 42.0));
        cars.add(new Car("GG-012-HH", "BMW", 85.0));
        cars.add(new Car("II-345-JJ", "Mercedes", 95.0));
    }

    @GetMapping("/cars")
    public List<Car> getCars() {
        return cars;
    }

    @GetMapping("/customer/{name}")
    public String bonjour(@PathVariable String name) {
        RestTemplate restTemplate = new RestTemplate();
        String url = customerServiceUrl + "/customers/" + name + "/address";
        logger.info("Requesting URL: " + url);
        String response = restTemplate.getForObject(url, String.class);
        return response;
    }
}
