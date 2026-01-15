package br.com.prospectaai.ms_async_task.domain.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.com.prospectaai.ms_async_task.domain.dto.ProspectTemplateDto;
import br.com.prospectaai.ms_async_task.domain.dto.ProspectTemplateRequest;
import br.com.prospectaai.ms_async_task.domain.entity.ProspectTemplate;
import br.com.prospectaai.ms_async_task.domain.repository.ProspectTemplateRepository;
import br.com.prospectaai.ms_async_task.domain.util.JwtUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProspectTemplateService {
    private final ProspectTemplateRepository templateRepository;
    private final JwtUtil jwtUtil;
    private final UserAccountClient userAccountClient;

    public ProspectTemplateDto create(ProspectTemplateRequest request, String authorization) {
        String email = jwtUtil.extractUserId(authorization);
        UUID userId = userAccountClient.resolveUserIdByEmail(email);
        if (userId == null) {
            return null;
        }
        ProspectTemplate t = new ProspectTemplate();
        t.setTitle(request.getTitle());
        t.setDataJson(request.getDataJson());
        t.setUserId(userId);
        t.setCreatedAt(Instant.now());
        t.setUpdatedAt(Instant.now());
        t = templateRepository.save(t);
        return toDto(t);
    }

    public ProspectTemplateDto update(java.util.UUID id, ProspectTemplateRequest request, String authorization) {
        String email = jwtUtil.extractUserId(authorization);
        UUID userId = userAccountClient.resolveUserIdByEmail(email);
        if (userId == null) {
            return null;
        }
        var opt = templateRepository.findByIdAndUserId(id, userId);
        if (opt.isEmpty()) {
            return null;
        }
        ProspectTemplate t = opt.get();
        t.setTitle(request.getTitle());
        t.setDataJson(request.getDataJson());
        t.setUpdatedAt(Instant.now());
        t = templateRepository.save(t);
        return toDto(t);
    }

    public ProspectTemplateDto getOne(java.util.UUID id, String authorization) {
        String email = jwtUtil.extractUserId(authorization);
        UUID userId = userAccountClient.resolveUserIdByEmail(email);
        if (userId == null) {
            return null;
        }
        var opt = templateRepository.findByIdAndUserId(id, userId);
        if (opt.isEmpty()) {
            return null;
        }
        return toDto(opt.get());
    }

    public List<ProspectTemplateDto> list(String authorization) {
        String email = jwtUtil.extractUserId(authorization);
        UUID userId = userAccountClient.resolveUserIdByEmail(email);
        if (userId == null) {
            return java.util.List.of();
        }
        return templateRepository.findByUserId(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public boolean delete(java.util.UUID id, String authorization) {
        String email = jwtUtil.extractUserId(authorization);
        UUID userId = userAccountClient.resolveUserIdByEmail(email);
        if (userId == null) {
            return false;
        }
        var opt = templateRepository.findByIdAndUserId(id, userId);
        if (opt.isEmpty()) {
            return false;
        }
        templateRepository.delete(opt.get());
        return true;
    }

    private ProspectTemplateDto toDto(ProspectTemplate t) {
        return ProspectTemplateDto.builder()
                .id(t.getId().toString())
                .title(t.getTitle())
                .dataJson(t.getDataJson())
                .createdAt(t.getCreatedAt().toString())
                .updatedAt(t.getUpdatedAt().toString())
                .build();
    }
}
