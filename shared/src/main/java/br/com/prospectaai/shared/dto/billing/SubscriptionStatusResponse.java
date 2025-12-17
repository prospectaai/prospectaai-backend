package br.com.prospectaai.shared.dto.billing;

import br.com.prospectaai.shared.billing.PlanType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionStatusResponse {
    private boolean active;
    private PlanType plan;
    private String nextBillingDate; // ISO-8601 string
}