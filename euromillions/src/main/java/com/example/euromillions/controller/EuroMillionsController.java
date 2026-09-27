package com.example.euromillions.controller;

import com.example.euromillions.model.StatsResponse;
import com.example.euromillions.service.EuroMillionsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class EuroMillionsController {
    private final EuroMillionsService service;

    public EuroMillionsController(EuroMillionsService service) {
        this.service = service;
    }

    @GetMapping("/stats")
    public StatsResponse getStats() {
        return service.getStats();
    }

    @GetMapping("/refresh")
    public StatsResponse refreshStats() {
        return service.refreshStats();
    }
}