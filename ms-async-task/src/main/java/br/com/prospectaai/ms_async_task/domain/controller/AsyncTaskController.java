package br.com.prospectaai.ms_async_task.domain.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import br.com.prospectaai.ms_async_task.domain.dto.ProspectRequest;
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
    
    @PostMapping("/prospect/call")
    public ResponseEntity<Void> dispatch(@RequestBody ProspectRequest request, @RequestHeader(value = "Authorization", required = true) String token) {
        String userEmail = jwtUtil.extractUserId(token);
        prospectTaskService.call(request.getQuery(), request.getPlatform(), userEmail);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/prospect/get-all-processing")
    public ResponseEntity<List<AsyncTaskPanelDto>> getAllProcessing(@RequestHeader(value = "Authorization", required = true) String token) {
        String userEmail = jwtUtil.extractUserId(token);
        List<AsyncTaskPanelDto> dto = prospectTaskService.getAllProcessing(userEmail);
        return ResponseEntity.ok().body(dto);
    }
}
