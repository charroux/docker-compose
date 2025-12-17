package com.rental.controller;

import com.rental.model.Car;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/cars")
public class RentalController {

    private final List<Car> cars;

    public RentalController() {
        // Initialisation de la liste des voitures en mémoire
        this.cars = new ArrayList<>();
        cars.add(new Car("AA-123-BB", "Renault", 45.0));
        cars.add(new Car("CC-456-DD", "Peugeot", 50.0));
        cars.add(new Car("EE-789-FF", "Citroën", 42.0));
        cars.add(new Car("GG-012-HH", "BMW", 85.0));
        cars.add(new Car("II-345-JJ", "Mercedes", 95.0));
    }

    @GetMapping
    public List<Car> getCars() {
        return cars;
    }
}
