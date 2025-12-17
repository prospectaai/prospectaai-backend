package br.com.prospectaai.ms_useraccount.domain.service;

import br.com.prospectaai.shared.billing.PlanType;
import br.com.prospectaai.shared.dto.billing.ChargeRequest;
import br.com.prospectaai.shared.dto.billing.ChargeResponse;
import br.com.prospectaai.shared.dto.billing.SubscriptionStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingClient {
    private final RestTemplate restTemplate;

    @Value("${billing.service.base-url:http://localhost:8082}")
    private String baseUrl;

    public ChargeResponse charge(String email, PlanType plan, String cardNumber, String cardName, String expiry, String cvv) {
        ChargeRequest req = new ChargeRequest(email, plan, cardNumber, cardName, expiry, cvv);
        ResponseEntity<ChargeResponse> res = restTemplate.postForEntity(baseUrl + "/api/v1/billing/charge", req, ChargeResponse.class);
        return res.getBody();
    }

    public SubscriptionStatusResponse createSubscription(UUID userId, PlanType plan) {
        String url = baseUrl + "/api/v1/billing/subscriptions?userId=" + userId + "&plan=" + plan.name();
        ResponseEntity<SubscriptionStatusResponse> res = restTemplate.postForEntity(url, null, SubscriptionStatusResponse.class);
        return res.getBody();
    }

    public SubscriptionStatusResponse getSubscriptionStatus(java.util.UUID userId) {
        String url = baseUrl + "/api/v1/billing/subscriptions/status?userId=" + userId;
        org.springframework.http.ResponseEntity<SubscriptionStatusResponse> res =
                restTemplate.getForEntity(url, SubscriptionStatusResponse.class);
        return res.getBody();
    }
}
