package br.com.prospectaai.ms_useraccount.domain.controller;

import br.com.prospectaai.ms_useraccount.domain.dto.LoginResponse;
import br.com.prospectaai.ms_useraccount.domain.entity.PreRegisterAccountEntity;
import br.com.prospectaai.ms_useraccount.domain.repository.PreRegisterAccountRepository;
import br.com.prospectaai.ms_useraccount.domain.service.BillingClient;
import br.com.prospectaai.ms_useraccount.domain.service.UserAccountService;
import br.com.prospectaai.ms_useraccount.jwt.JwtTokenProvider;
import br.com.prospectaai.shared.dto.billing.CheckoutRequest;
import br.com.prospectaai.shared.dto.billing.ChargeResponse;
import br.com.prospectaai.shared.billing.PlanType;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class CheckoutController {
    private final BillingClient billingClient;
    private final UserAccountService userAccountService;
    private final PreRegisterAccountRepository preRegisterAccountRepository;
    private final br.com.prospectaai.ms_useraccount.domain.repository.UserAccountRepository userAccountRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/checkout")
    public ResponseEntity<LoginResponse> checkout(@RequestBody CheckoutRequest request) {
        UUID preRegisterId = UUID.fromString(request.getPreRegisterId());

        // Validar pré-registro
        PreRegisterAccountEntity pre = preRegisterAccountRepository.findById(preRegisterId)
                .orElseThrow(() -> new IllegalArgumentException("Pré-cadastro não encontrado"));
        if (pre.getPreRegisterValidated() == null || !pre.getPreRegisterValidated()) {
            throw new IllegalArgumentException("Pré-cadastro não validado");
        }

        // Cobrança falsa via ms-billing-sbs
        ChargeResponse charge = billingClient.charge(
                pre.getEmail(),
                request.getPlan(),
                request.getCardNumber(),
                request.getCardName(),
                request.getCardExpiry(),
                request.getCardCvv()
        );

        if (charge == null || !charge.isSuccess()) {
            throw new IllegalArgumentException("Pagamento recusado: " + (charge != null ? charge.getMessage() : "erro"));
        }

        // Criar usuário e autenticar
        LoginResponse login = userAccountService.completeRegister(preRegisterId);
        
        // Criar assinatura vinculada ao usuário recém criado
        PlanType plan = request.getPlan();
        br.com.prospectaai.ms_useraccount.domain.entity.UserAccountEntity createdUser =
                userAccountRepository.findByEmail(jwtTokenProvider.extractUsername(login.getToken())).orElse(null);
        if (createdUser != null) {
            billingClient.createSubscription(createdUser.getAccountId(), plan);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(login);
    }
}