package br.com.prospectaai.ms_async_task.domain.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.prospectaai.shared.dto.analytics.AnalyticsOverview;
import br.com.prospectaai.ms_async_task.domain.service.AnalyticsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/async/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverview> overview() {
        return ResponseEntity.ok(analyticsService.getOverview());
    }
}
