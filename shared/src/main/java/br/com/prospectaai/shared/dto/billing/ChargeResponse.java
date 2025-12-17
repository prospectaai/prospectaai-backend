package br.com.prospectaai.shared.dto.billing;

import br.com.prospectaai.shared.billing.PlanType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChargeResponse {
    private boolean success;
    private String message;
    private String subscriptionId; // pode ser preenchido após criação da assinatura
    private PlanType plan;
    private String nextBillingDate; // ISO-8601 string
}