package com.example.euromillions.service;

import com.example.euromillions.entity.Draw;
import com.example.euromillions.entity.DrawRepository;
import com.example.euromillions.model.NumberStat;
import com.example.euromillions.model.StatsResponse;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EuroMillionsService {

    private static final String API_ALL_URL = "https://api-euromillions.nunofcguerreiro.com/results?format=plaintext&result=all";
    private static final String API_DATE_URL = "https://api-euromillions.nunofcguerreiro.com/results/date/%s?format=plaintext";

    private final DrawRepository drawRepository;
    private final HttpClient client;

    public EuroMillionsService(DrawRepository drawRepository) {
        this.drawRepository = drawRepository;
        this.client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(15)).build();
    }

    @PostConstruct
    public void init() {
        initialLoad();
    }

    public StatsResponse initialLoad() {
        LocalDate latestStoredDate = drawRepository.findLatestDrawDate();
        try {
            if (latestStoredDate == null) {
                fetchAndSave(API_ALL_URL);
            }
        } catch (Exception e) {
            System.err.println("Warning: Network sync failed. " + e.getMessage());
        }
        return getStats();
    }

    public StatsResponse refreshStats(){
        LocalDate latestStoredDate = drawRepository.findLatestDrawDate();
        try {
            if (latestStoredDate == null) {
                fetchAndSave(API_ALL_URL);
            } else {
                List<LocalDate> missingDates = getMissingDrawDates(latestStoredDate);
                for (LocalDate date : missingDates) {
                    fetchAndSave(String.format(API_DATE_URL, date.toString()));
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Network sync failed. " + e.getMessage());
        }
        return getStats();
    }

    public StatsResponse getStats() {
        List<Draw> allDraws = drawRepository.findAll();
        LocalDate latestStoredDate = drawRepository.findLatestDrawDate();

        if (latestStoredDate == null) {
            return new StatsResponse(0, "N/A", 0, true, List.of(), List.of(), LocalDateTime.now().toString());
        }

        int totalDraws = allDraws.size();

        LocalDate c3M = latestStoredDate.minusMonths(3);
        LocalDate c6M = latestStoredDate.minusMonths(6);
        LocalDate c9M = latestStoredDate.minusMonths(9);
        LocalDate c12M = latestStoredDate.minusMonths(12);

        int t3M = 0, t6M = 0, t9M = 0, t12M = 0;

        // Array index meaning: [0]=Total Hits, [1]=3M, [2]=6M, [3]=9M, [4]=12M
        Map<Integer, int[]> mainCounts = new HashMap<>();
        Map<Integer, int[]> starCounts = new HashMap<>();
        Map<Integer, LocalDate> mainLatestDates = new HashMap<>();
        Map<Integer, LocalDate> starLatestDates = new HashMap<>();

        for (int i = 1; i <= 50; i++) mainCounts.put(i, new int[5]);
        for (int i = 1; i <= 12; i++) starCounts.put(i, new int[5]);

        for (Draw draw : allDraws) {
            LocalDate date = draw.getDrawDate();

            boolean is3M = !date.isBefore(c3M);
            boolean is6M = !date.isBefore(c6M);
            boolean is9M = !date.isBefore(c9M);
            boolean is12M = !date.isBefore(c12M);

            if (is3M) t3M++;
            if (is6M) t6M++;
            if (is9M) t9M++;
            if (is12M) t12M++;

            for (int num : draw.getMainNumbers()) {
                assignValuesToAverageGroups(mainCounts, mainLatestDates, date, is3M, is6M, is9M, is12M, num);
            }

            for (int star : draw.getStars()) {
                assignValuesToAverageGroups(starCounts, starLatestDates, date, is3M, is6M, is9M, is12M, star);
            }
        }

        List<NumberStat> mainStats = buildStatsList(mainCounts, mainLatestDates, totalDraws, t3M, t6M, t9M, t12M);
        List<NumberStat> starStats = buildStatsList(starCounts, starLatestDates, totalDraws, t3M, t6M, t9M, t12M);

        boolean isNewDrawPending = !LocalDate.now().isBefore(getNextDrawDay(latestStoredDate));
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return new StatsResponse(totalDraws, latestStoredDate.toString(), totalDraws, isNewDrawPending, mainStats, starStats, timeStr);
    }

    private void assignValuesToAverageGroups(Map<Integer, int[]> mainCounts, Map<Integer, LocalDate> mainLatestDates, LocalDate date, boolean is3M, boolean is6M, boolean is9M, boolean is12M, int num) {
        int[] c = mainCounts.get(num);
        c[0]++;
        if (is3M) c[1]++;
        if (is6M) c[2]++;
        if (is9M) c[3]++;
        if (is12M) c[4]++;

        LocalDate existing = mainLatestDates.get(num);
        if (existing == null || date.isAfter(existing)) mainLatestDates.put(num, date);
    }

    private List<NumberStat> buildStatsList(Map<Integer, int[]> countsMap, Map<Integer, LocalDate> latestDates, int total, int t3, int t6, int t9, int t12) {
        List<NumberStat> list = new ArrayList<>();
        for (int num = 1; num <= countsMap.size(); num++) {
            int[] c = countsMap.get(num);
            int hits = c[0];

            double exactAvg = hits == 0 ? 0 : (double) total / hits;
            String overallAvg = hits == 0 ? "N/A" : String.format(Locale.US, "%.1f", exactAvg);
            String percentStr = total == 0 ? "0.00%" : String.format(Locale.US, "%.2f%%", ((double) hits / total) * 100.0);

            String avg3 = c[1] == 0 ? "N/A" : String.format(Locale.US, "%.1f", (double) t3 / c[1]);
            String avg6 = c[2] == 0 ? "N/A" : String.format(Locale.US, "%.1f", (double) t6 / c[2]);
            String avg9 = c[3] == 0 ? "N/A" : String.format(Locale.US, "%.1f", (double) t9 / c[3]);
            String avg12 = c[4] == 0 ? "N/A" : String.format(Locale.US, "%.1f", (double) t12 / c[4]);

            LocalDate lastDate = latestDates.get(num);
            String lastDrawnStr = lastDate != null ? lastDate.toString() : "N/A";

            String expectedDateStr = "N/A";
            if (lastDate != null && hits > 0) {
                long drawsToAdd = Math.round(exactAvg);
                LocalDate expectedDate = lastDate;
                for (int i = 0; i < drawsToAdd; i++) expectedDate = getNextDrawDay(expectedDate);
                expectedDateStr = expectedDate.toString();
            }

            list.add(new NumberStat(num, hits, percentStr, lastDrawnStr, expectedDateStr, overallAvg, avg3, avg6, avg9, avg12));
        }
        return list;
    }

    private void fetchAndSave(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                parseAndSaveToDb(response.body());
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch URL " + url + ": " + e.getMessage());
        }
    }

    private void parseAndSaveToDb(String data) {
        // BUG FIX: Ordered two-digit matchers BEFORE single-digit matchers,
        // and added trailing word boundary \b to prevent truncated parsing at the end of the line.
        String dateRegex = "(\\d{4}-\\d{2}-\\d{2})";
        String n50 = "(50|[1-4]\\d|0?[1-9])";
        String n12 = "(1[0-2]|0?[1-9])";
        String sep = "\\D+";
        String regex = dateRegex + sep + n50 + sep + n50 + sep + n50 + sep + n50 + sep + n50 + sep + n12 + sep + n12 + "\\b";

        Matcher matcher = Pattern.compile(regex).matcher(data);
        List<Draw> drawsToSave = new ArrayList<>();
        while (matcher.find()) {
            drawsToSave.add(new Draw(
                    LocalDate.parse(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)),
                    Integer.parseInt(matcher.group(4)), Integer.parseInt(matcher.group(5)),
                    Integer.parseInt(matcher.group(6)),
                    Integer.parseInt(matcher.group(7)), Integer.parseInt(matcher.group(8))
            ));
        }
        if (!drawsToSave.isEmpty()) drawRepository.saveAll(drawsToSave);
    }

    private List<LocalDate> getMissingDrawDates(LocalDate latestStoredDate) {
        List<LocalDate> missing = new ArrayList<>();
        LocalDate next = latestStoredDate.plusDays(1);
        LocalDate today = LocalDate.now();
        while (!next.isAfter(today)) {
            if (next.getDayOfWeek() == DayOfWeek.TUESDAY || next.getDayOfWeek() == DayOfWeek.FRIDAY) {
                missing.add(next);
            }
            next = next.plusDays(1);
        }
        return missing;
    }

    private LocalDate getNextDrawDay(LocalDate currentDate) {
        LocalDate nextDate = currentDate.plusDays(1);
        while (nextDate.getDayOfWeek() != DayOfWeek.TUESDAY && nextDate.getDayOfWeek() != DayOfWeek.FRIDAY) {
            nextDate = nextDate.plusDays(1);
        }
        return nextDate;
    }
}