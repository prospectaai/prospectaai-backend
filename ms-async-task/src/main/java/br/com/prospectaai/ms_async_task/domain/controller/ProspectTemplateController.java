package br.com.prospectaai.ms_async_task.domain.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.prospectaai.ms_async_task.domain.dto.ProspectTemplateDto;
import br.com.prospectaai.ms_async_task.domain.dto.ProspectTemplateRequest;
import br.com.prospectaai.ms_async_task.domain.service.ProspectTemplateService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/async/prospect/templates")
@RequiredArgsConstructor
public class ProspectTemplateController {
    private final ProspectTemplateService templateService;

    @PostMapping
    public ResponseEntity<ProspectTemplateDto> create(@RequestBody ProspectTemplateRequest request, @RequestHeader(value = "Authorization", required = true) String token) {
        ProspectTemplateDto dto = templateService.create(request, token);
        if (dto == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProspectTemplateDto> update(@PathVariable("id") String id, @RequestBody ProspectTemplateRequest request, @RequestHeader(value = "Authorization", required = true) String token) {
        java.util.UUID uuid;
        try {
            uuid = java.util.UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        ProspectTemplateDto dto = templateService.update(uuid, request, token);
        if (dto == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProspectTemplateDto> getOne(@PathVariable("id") String id, @RequestHeader(value = "Authorization", required = true) String token) {
        java.util.UUID uuid;
        try {
            uuid = java.util.UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        ProspectTemplateDto dto = templateService.getOne(uuid, token);
        if (dto == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<List<ProspectTemplateDto>> list(@RequestHeader(value = "Authorization", required = true) String token) {
        List<ProspectTemplateDto> list = templateService.list(token);
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String id, @RequestHeader(value = "Authorization", required = true) String token) {
        java.util.UUID uuid;
        try {
            uuid = java.util.UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        boolean ok = templateService.delete(uuid, token);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.noContent().build();
    }
}
