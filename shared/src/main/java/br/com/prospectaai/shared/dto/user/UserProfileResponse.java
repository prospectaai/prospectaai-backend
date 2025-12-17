package br.com.prospectaai.shared.dto.user;

import br.com.prospectaai.shared.billing.PlanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private String email;
    private String displayName;
    private String avatarUrl;
    private boolean active;
    private String role;
    private String accountCreatedAt;

    private boolean subscriptionActive;
    private PlanType subscriptionPlan;
    private String subscriptionNextBillingDate;
}