package br.com.prospectaai.ms_async_task.domain.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAccountClient {
    private final RestTemplate restTemplate;

    @Value("${useraccount.service.base-url:http://ms-useraccount}")
    private String userAccountBaseUrl;

    public UUID resolveUserIdByEmail(String email) {
        try {
            ResponseEntity<String> res = restTemplate.getForEntity(userAccountBaseUrl + "/api/v1/users/resolve-id?email=" + email, String.class);
            if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null && !res.getBody().isBlank()) {
                return UUID.fromString(res.getBody().trim());
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}

