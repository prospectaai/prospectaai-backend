package br.com.prospectaai.ms_async_task.domain.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        long currentMonth = recordRepository.countByCreatedAtBetween(startOfMonth, endOfMonth);

        LocalDate prev = now.minusMonths(1);
        Instant prevStart = prev.withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant prevEnd = prev.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
        long previousMonth = recordRepository.countByCreatedAtBetween(prevStart, prevEnd);

        double variation = previousMonth == 0 ? (currentMonth > 0 ? 100.0 : 0.0)
                : ((double) (currentMonth - previousMonth) / (double) previousMonth) * 100.0;

        long buscasAtivas = taskRepository.countByStatus(AsyncTaskStatus.PROCESSING);
        long agendadas = taskRepository.countProcessingWithoutRecords();

        long distinctLocations = recordRepository.countDistinctEnderecos();
        long cities = countDistinctCities(recordRepository.findAllEnderecosNonNull());

        return new AnalyticsOverview(
            totalProspectadas,
            roundOneDecimal(variation),
            buscasAtivas,
            agendadas,
            distinctLocations,
            cities
        );
    }

    private long countDistinctCities(List<String> enderecos) {
        Set<String> cities = new HashSet<>();
        for (String e : enderecos) {
            String city = extractCity(e);
            if (city != null && !city.isBlank()) {
                cities.add(city.toLowerCase().trim());
            }
        }
        return cities.size();
    }

    private String extractCity(String endereco) {
        if (endereco == null) return null;
        String s = endereco.trim();
        int idxDash = s.lastIndexOf(" - ");
        if (idxDash > 0) {
            String left = s.substring(0, idxDash);
            int prevDash = left.lastIndexOf(" - ");
            if (prevDash >= 0) {
                return left.substring(prevDash + 3).trim();
            }
            int lastComma = left.lastIndexOf(',');
            if (lastComma >= 0) {
                return left.substring(lastComma + 1).trim();
            }
            return left.trim();
        }
        int lastComma = s.lastIndexOf(',');
        if (lastComma >= 0) {
            return s.substring(lastComma + 1).trim();
        }
        return null;
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
