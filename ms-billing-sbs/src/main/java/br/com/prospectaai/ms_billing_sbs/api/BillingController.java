package br.com.prospectaai.ms_billing_sbs.api;

import br.com.prospectaai.shared.dto.billing.ChargeRequest;
import br.com.prospectaai.shared.dto.billing.ChargeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    @PostMapping("/charge")
    public ResponseEntity<ChargeResponse> charge(@RequestBody ChargeRequest request) {
        // Pagamento falso: apenas validações simples de formato
        boolean cardValid = request.getCardNumber() != null && request.getCardNumber().matches("^\\d{16}$");
        boolean expiryValid = request.getCardExpiry() != null && request.getCardExpiry().matches("^(0[1-9]|1[0-2])\\/\\d{2}$");
        boolean cvvValid = request.getCardCvv() != null && request.getCardCvv().matches("^\\d{3,4}$");

        if (!cardValid || !expiryValid || !cvvValid) {
            return ResponseEntity.ok(new ChargeResponse(false, "Invalid card data", null, request.getPlan(), null));
        }

        // Sucesso simulado
        return ResponseEntity.ok(new ChargeResponse(true, "Charge approved", null, request.getPlan(), null));
    }
}