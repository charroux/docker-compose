package com.customer.controller;

import com.customer.model.Customer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final List<Customer> customers;

    public CustomerController() {
        // Initialisation de la liste des customers en mémoire
        this.customers = new ArrayList<>();
        customers.add(new Customer("Jean Dupont", "12 Rue de la Paix, Paris"));
        customers.add(new Customer("Marie Martin", "34 Avenue des Champs, Lyon"));
        customers.add(new Customer("Pierre Bernard", "56 Boulevard Victor Hugo, Marseille"));
        customers.add(new Customer("Sophie Dubois", "78 Rue du Commerce, Toulouse"));
        customers.add(new Customer("Luc Petit", "90 Place de la République, Nice"));
    }

    @GetMapping
    public List<Customer> getAllCustomers() {
        return customers;
    }

    @GetMapping("/{name}/address")
    public String getCustomerAddress(@PathVariable String name) {
        return customers.stream()
                .filter(customer -> customer.getName().equals(name))
                .findFirst()
                .map(Customer::getAddress)
                .orElse("Customer not found");
    }
}
