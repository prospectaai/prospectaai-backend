package br.com.prospectaai.ms_async_task.domain.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.prospectaai.ms_async_task.domain.dto.ProspectRequest;
import br.com.prospectaai.ms_async_task.domain.service.ProspectTaskService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/async")
@RequiredArgsConstructor
public class AsyncTaskController {
    private final ProspectTaskService prospectTaskService;

    @PostMapping("/prospect/call")
    public ResponseEntity<Void> dispatch(@RequestBody ProspectRequest request) {
        prospectTaskService.call(request.getQuery(), request.getPlatform());
        return ResponseEntity.ok().build();
    }
}

