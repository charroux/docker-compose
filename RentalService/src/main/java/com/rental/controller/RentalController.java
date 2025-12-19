package com.rental.controller;

import com.rental.model.Car;
import com.rental.repository.CarRepository;

import jakarta.annotation.PostConstruct;
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

    private final CarRepository carRepository;

    Logger logger = org.slf4j.LoggerFactory.getLogger(RentalController.class);

    public RentalController(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    @PostConstruct
    public void initDatabase() {
        // Initialisation de la base de données avec des voitures si elle est vide
        if (carRepository.count() == 0) {
            logger.info("Initializing database with cars...");
            carRepository.save(new Car("AA-123-BB", "Renault", 45.0));
            carRepository.save(new Car("CC-456-DD", "Peugeot", 50.0));
            carRepository.save(new Car("EE-789-FF", "Citroën", 42.0));
            carRepository.save(new Car("GG-012-HH", "BMW", 85.0));
            carRepository.save(new Car("II-345-JJ", "Mercedes", 95.0));
            logger.info("Database initialized with {} cars", carRepository.count());
        }
    }

    @GetMapping("/cars")
    public List<Car> getCars() {
        return carRepository.findAll();
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
