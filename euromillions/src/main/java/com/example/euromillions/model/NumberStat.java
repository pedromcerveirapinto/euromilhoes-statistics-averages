package com.example.euromillions.model;

public record NumberStat(
        int number,
        int hits,
        String percentage,
        String lastDrawnDate,
        String expectedDrawDate,
        String overallAvg,
        String avg3Months,
        String avg6Months,
        String avg9Months,
        String avg12Months
) {}