package com.example.euromillions.model;

import java.util.List;

public record StatsResponse(
        int totalDraws,
        String latestDbDate,
        int latestDbDrawNumber,
        boolean isNewDrawPending,
        List<NumberStat> mainNumbers,
        List<NumberStat> stars,
        String lastUpdated
) {}