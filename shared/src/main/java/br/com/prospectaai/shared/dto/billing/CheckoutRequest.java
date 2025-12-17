package br.com.prospectaai.shared.dto.billing;

import br.com.prospectaai.shared.billing.PlanType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    private String cardNumber;
    private String cardName;
    private String cardExpiry; // MM/YY
    private String cardCvv;
    private PlanType plan;
    private String preRegisterId; // UUID string
}