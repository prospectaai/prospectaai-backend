package br.com.prospectaai.ms_async_task.domain.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

import org.springframework.stereotype.Service;

import br.com.prospectaai.shared.dto.analytics.AnalyticsOverview;
import br.com.prospectaai.ms_async_task.domain.enums.AsyncTaskStatus;
import br.com.prospectaai.ms_async_task.domain.repository.ProspectTaskRepository;
import br.com.prospectaai.ms_async_task.domain.repository.ProspectionRecordRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final ProspectionRecordRepository recordRepository;
    private final ProspectTaskRepository taskRepository;

    public AnalyticsOverview getOverview() {
        long totalProspectadas = recordRepository.count();

        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        Instant startOfMonth = now.withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
        java.util.List<br.com.prospectaai.ms_async_task.domain.entity.ProspectionRecord> currentMonthRecords =
                recordRepository.findByCreatedAtBetween(startOfMonth, endOfMonth);
        long currentMonth = countDistinctCitiesFromQueries(currentMonthRecords);

        LocalDate prev = now.minusMonths(1);
        Instant prevStart = prev.withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant prevEnd = prev.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
        java.util.List<br.com.prospectaai.ms_async_task.domain.entity.ProspectionRecord> previousMonthRecords =
                recordRepository.findByCreatedAtBetween(prevStart, prevEnd);
        long previousMonth = countDistinctCitiesFromQueries(previousMonthRecords);

        double variation = previousMonth == 0 ? (currentMonth > 0 ? 100.0 : 0.0)
                : ((double) (currentMonth - previousMonth) / (double) previousMonth) * 100.0;

        long buscasAtivas = taskRepository.countByStatus(AsyncTaskStatus.PROCESSING);
        long agendadas = taskRepository.countProcessingWithoutRecords();

        String topPlatform = mostUsedPlatform(currentMonthRecords);
        long cities = currentMonth;

        return new AnalyticsOverview(
            totalProspectadas,
            roundOneDecimal(variation),
            buscasAtivas,
            agendadas,
            topPlatform,
            cities
        );
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private long countDistinctCitiesFromQueries(java.util.List<br.com.prospectaai.ms_async_task.domain.entity.ProspectionRecord> records) {
        if (records == null || records.isEmpty()) return 0L;
        java.util.Set<String> cities = new java.util.HashSet<>();
        for (var r : records) {
            String q = r.getQuery();
            String city = extractCityFromQuery(q);
            if (city != null && !city.isBlank()) {
                cities.add(city.toLowerCase().trim());
            }
        }
        return cities.size();
    }

    private String extractCityFromQuery(String query) {
        if (query == null) return null;
        String s = query.trim();
        String lower = s.toLowerCase();
        int idx = lower.lastIndexOf(" em ");
        String tail = idx >= 0 ? s.substring(idx + 4).trim() : s;
        tail = tail.replaceAll("(?i)até\\s*\\d+\\s*km", "").trim();
        String[] parts = tail.split(",");
        if (parts.length >= 2) {
            for (int i = parts.length - 1; i >= 0; i--) {
                String part = parts[i].trim();
                if (part.matches("[A-Z]{2}")) {
                    for (int j = i - 1; j >= 0; j--) {
                        String prev = parts[j].trim();
                        if (!prev.isBlank()) return prev;
                    }
                }
            }
            return parts[parts.length - 1].trim();
        }
        String[] dashSplit = tail.split("\\s-\\s");
        if (dashSplit.length >= 2) {
            return dashSplit[dashSplit.length - 1].trim();
        }
        return tail;
    }

    private String mostUsedPlatform(java.util.List<br.com.prospectaai.ms_async_task.domain.entity.ProspectionRecord> records) {
        if (records == null || records.isEmpty()) return null;
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (var r : records) {
            String p = r.getPlatform();
            if (p == null || p.isBlank()) continue;
            String key = p.trim();
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }
        String best = null;
        int max = 0;
        for (var e : counts.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }
}
