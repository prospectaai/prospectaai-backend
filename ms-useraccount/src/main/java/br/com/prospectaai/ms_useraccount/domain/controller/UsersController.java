package br.com.prospectaai.ms_useraccount.domain.controller;

import br.com.prospectaai.ms_useraccount.domain.entity.PreRegisterAccountEntity;
import br.com.prospectaai.ms_useraccount.domain.entity.UserAccountEntity;
import br.com.prospectaai.ms_useraccount.domain.repository.PreRegisterAccountRepository;
import br.com.prospectaai.ms_useraccount.domain.repository.UserAccountRepository;
import br.com.prospectaai.ms_useraccount.domain.service.BillingClient;
import br.com.prospectaai.ms_useraccount.jwt.JwtTokenProvider;
import br.com.prospectaai.shared.billing.PlanType;
import br.com.prospectaai.shared.dto.billing.SubscriptionStatusResponse;
import br.com.prospectaai.shared.dto.user.UserProfileResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UsersController {
    private final UserAccountRepository userAccountRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BillingClient billingClient;
    private final PreRegisterAccountRepository preRegisterAccountRepository;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(
        @RequestHeader(value = "X-Auth-User-Id", required = false) String xAuthUserId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        System.out.println("[UserAccount][UsersController.me] start");
        System.out.println("[UserAccount][UsersController.me] header X-Auth-User-Id=" + xAuthUserId + ", Authorization=" + authorizationHeader);
        String email = null;
        if (xAuthUserId != null && !xAuthUserId.isBlank()) {
            email = xAuthUserId;
            System.out.println("[UserAccount][UsersController.me] using X-Auth-User-Id as email");
        } else {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                System.out.println("[UserAccount][UsersController.me] missing Authorization and X-Auth-User-Id");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            String token = authorizationHeader.substring("Bearer ".length());
            String tokenPreview = token.length() > 12 ? token.substring(0,6) + "..." + token.substring(token.length()-6) : token;
            System.out.println("[UserAccount][UsersController.me] token preview=" + tokenPreview);
            boolean valid = jwtTokenProvider.validateToken(token);
            System.out.println("[UserAccount][UsersController.me] token valid=" + valid);
            if (!valid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            email = jwtTokenProvider.extractUsername(token);
        }
        System.out.println("[UserAccount][UsersController.me] extracted email=" + email);
        UserAccountEntity user =
                userAccountRepository.findByEmail(email).orElse(null);
        if (user == null) {
            System.out.println("[UserAccount][UsersController.me] user not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        System.out.println("[UserAccount][UsersController.me] user found id=" + user.getAccountId());

        // Buscar data de criação aproximada via pré-registro (validatedAt)
        PreRegisterAccountEntity pre =
                preRegisterAccountRepository.findByEmail(email);
        String accountCreatedAt = pre != null && pre.getPreRegisterValidatedAt() != null
                ? pre.getPreRegisterValidatedAt().toString()
                : null;
        System.out.println("[UserAccount][UsersController.me] accountCreatedAt=" + accountCreatedAt);

        // Buscar status da assinatura no ms-billing-sbs
        SubscriptionStatusResponse sub =
                billingClient.getSubscriptionStatus(user.getAccountId());
        boolean subActive = sub != null && sub.isActive();
        PlanType subPlan = sub != null ? sub.getPlan() : null;
        String nextBilling = sub != null ? sub.getNextBillingDate() : null;
        System.out.println("[UserAccount][UsersController.me] subscription active=" + subActive + ", plan=" + subPlan + ", nextBilling=" + nextBilling);

        // Montar resposta
        UserProfileResponse body =
            UserProfileResponse.builder()
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .active(user.isActive())
                .role(user.getRole() != null ? user.getRole().getTag() : null)
                .accountCreatedAt(accountCreatedAt)
                .subscriptionActive(subActive)
                .subscriptionPlan(subPlan)
                .subscriptionNextBillingDate(nextBilling)
                .build();

        System.out.println("[UserAccount][UsersController.me] returning 200");
        return ResponseEntity.ok(body);
    }

    @GetMapping("/resolve-id")
    public ResponseEntity<String> resolveId(@RequestParam("email") String email) {
        System.out.println("[UserAccount][UsersController.resolveId] start email=" + email);
        UserAccountEntity user = userAccountRepository.findByEmail(email).orElse(null);
        if (user == null) {
            System.out.println("[UserAccount][UsersController.resolveId] user not found");
            return ResponseEntity.notFound().build();
        }
        UUID id = user.getAccountId();
        System.out.println("[UserAccount][UsersController.resolveId] resolved id=" + id);
        return ResponseEntity.ok(id.toString());
    }
}
