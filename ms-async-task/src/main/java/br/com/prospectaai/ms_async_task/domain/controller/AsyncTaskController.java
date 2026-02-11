package br.com.prospectaai.ms_async_task.domain.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;

import br.com.prospectaai.ms_async_task.domain.dto.ProspectRequest;
import br.com.prospectaai.ms_async_task.domain.dto.ProspectionDetailDto;
import br.com.prospectaai.ms_async_task.domain.dto.ProspectionSummaryDto;
import br.com.prospectaai.ms_async_task.domain.dto.ProspectionUsageDto;
import br.com.prospectaai.ms_async_task.domain.service.ProspectTaskService;
import br.com.prospectaai.ms_async_task.domain.util.JwtUtil;
import br.com.prospectaai.shared.dto.async.AsyncTaskPanelDto;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/async")
@RequiredArgsConstructor
public class AsyncTaskController {
    private final ProspectTaskService prospectTaskService;
    private final JwtUtil jwtUtil;
    
    @GetMapping("/prospect/usage")
    public ResponseEntity<ProspectionUsageDto> getUsage(@RequestHeader(value = "Authorization", required = true) String token) {
        String userEmail = jwtUtil.extractUserId(token);
        var dto = prospectTaskService.getUsage(userEmail);
        return ResponseEntity.ok().body(dto);
    }

    @PostMapping("/prospect/call")
    public ResponseEntity<Void> dispatch(@RequestBody ProspectRequest request, @RequestHeader(value = "Authorization", required = true) String token) {
        String userEmail = jwtUtil.extractUserId(token);
        prospectTaskService.call(request, userEmail);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/prospect/get-all-processing")
    public ResponseEntity<List<AsyncTaskPanelDto>> getAllProcessing(@RequestHeader(value = "Authorization", required = true) String token) {
        String userEmail = jwtUtil.extractUserId(token);
        List<AsyncTaskPanelDto> dto = prospectTaskService.getAllProcessing(userEmail);
        return ResponseEntity.ok().body(dto);
    }

    @GetMapping("/prospect/get-all-processed")
    public ResponseEntity<List<AsyncTaskPanelDto>> getAllProcessed(@RequestHeader(value = "Authorization", required = true) String token) {
        String userEmail = jwtUtil.extractUserId(token);
        List<AsyncTaskPanelDto> dto = prospectTaskService.getAllProcessed(userEmail);
        return ResponseEntity.ok().body(dto);
    }

    @GetMapping("/prospect/get-all-results")
    public ResponseEntity<List<ProspectionSummaryDto>> getAllResults(@RequestHeader(value = "Authorization", required = true) String token) {
        String userEmail = jwtUtil.extractUserId(token);
        var dto = prospectTaskService.getAllResultsSummary(userEmail);
        return ResponseEntity.ok().body(dto);
    }

    @GetMapping("/prospect/result/{taskId}")
    public ResponseEntity<ProspectionDetailDto> getResultDetail(@PathVariable("taskId") Long taskId, @RequestHeader(value = "Authorization", required = true) String token) {
        String userEmail = jwtUtil.extractUserId(token);
        var dto = prospectTaskService.getResultDetail(taskId, userEmail);
        if (dto == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.ok().body(dto);
    }

    @DeleteMapping("/prospect/result/{taskId}")
    public ResponseEntity<Void> deleteProspection(@PathVariable("taskId") Long taskId, @RequestHeader(value = "Authorization", required = true) String token) {
        String userEmail = jwtUtil.extractUserId(token);
        boolean ok = prospectTaskService.deleteProspection(taskId, userEmail);
        if (!ok) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/prospect/result/all")
    public ResponseEntity<Void> deleteAllProspection(@RequestHeader(value = "Authorization", required = true) String token) {
        String userEmail = jwtUtil.extractUserId(token);
        prospectTaskService.deleteAllProspection(userEmail);
        return ResponseEntity.noContent().build();
    }
}
