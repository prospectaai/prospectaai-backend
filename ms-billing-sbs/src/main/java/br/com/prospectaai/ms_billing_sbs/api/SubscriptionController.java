package br.com.prospectaai.ms_billing_sbs.api;

import br.com.prospectaai.shared.billing.PlanType;
import br.com.prospectaai.shared.dto.billing.SubscriptionStatusResponse;
import br.com.prospectaai.shared.dto.BooleanResponse;
import br.com.prospectaai.ms_billing_sbs.domain.entity.SubscriptionPlanEntity;
import br.com.prospectaai.ms_billing_sbs.domain.entity.UserSubscriptionEntity;
import br.com.prospectaai.ms_billing_sbs.domain.repository.SubscriptionPlanRepository;
import br.com.prospectaai.ms_billing_sbs.domain.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @PostMapping
    public ResponseEntity<SubscriptionStatusResponse> createSubscription(
            @RequestParam("userId") UUID userId,
            @RequestParam("plan") PlanType planType
    ) {
        System.out.println("[Billing][SubscriptionController.create] start userId=" + userId + ", plan=" + planType);
        // Obter/seed do plano
        String planName = planType.name();
        SubscriptionPlanEntity plan = subscriptionPlanRepository
                .findAll().stream().filter(p -> planName.equalsIgnoreCase(p.getName())).findFirst()
                .orElseGet(() -> {
                    SubscriptionPlanEntity p = new SubscriptionPlanEntity();
                    p.setName(planName);
                    // preços simulados
                    p.setMonthlyPrice(planType == PlanType.MONTHLY ? 49.90 : 41.58); // anual ~499.00/12
                    p.setFeaturesJson("{\"basic\":true}");
                    return subscriptionPlanRepository.save(p);
                });

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime nextBilling = planType == PlanType.MONTHLY ? start.plusMonths(1) : start.plusYears(1);

        UserSubscriptionEntity sub = new UserSubscriptionEntity();
        sub.setUserAccount(userId);
        sub.setPlan(plan);
        sub.setStartDate(start);
        sub.setNextBillingDate(nextBilling);
        sub.setActive(true);
        userSubscriptionRepository.save(sub);

        System.out.println("[Billing][SubscriptionController.create] created, nextBilling=" + nextBilling);
        return ResponseEntity.ok(new SubscriptionStatusResponse(true, planType, nextBilling.toString()));
    }

    @GetMapping("/validate")
    public ResponseEntity<BooleanResponse> validateActive(@RequestParam("userId") UUID userId) {
        System.out.println("[Billing][SubscriptionController.validate] start userId=" + userId);
        Optional<UserSubscriptionEntity> sub = userSubscriptionRepository.findTopByUserAccountAndActiveTrueOrderByNextBillingDateDesc(userId);
        boolean active = sub.map(s -> s.getNextBillingDate().isAfter(LocalDateTime.now())).orElse(false);
        System.out.println("[Billing][SubscriptionController.validate] active=" + active);
        return ResponseEntity.ok(BooleanResponse.builder().value(active).build());
    }

    @GetMapping("/status")
    public ResponseEntity<SubscriptionStatusResponse> status(@RequestParam("userId") java.util.UUID userId) {
        System.out.println("[Billing][SubscriptionController.status] start userId=" + userId);
        java.util.Optional<UserSubscriptionEntity> sub = userSubscriptionRepository
                .findTopByUserAccountAndActiveTrueOrderByNextBillingDateDesc(userId);
        if (sub.isEmpty()) {
            System.out.println("[Billing][SubscriptionController.status] no subscription");
            return ResponseEntity.ok(new SubscriptionStatusResponse(false, null, null));
        }
        UserSubscriptionEntity s = sub.get();
        boolean active = s.getNextBillingDate().isAfter(java.time.LocalDateTime.now());
        PlanType planType = PlanType.fromString(s.getPlan().getName());
        System.out.println("[Billing][SubscriptionController.status] active=" + active + ", plan=" + planType + ", nextBilling=" + s.getNextBillingDate());
        return ResponseEntity.ok(new SubscriptionStatusResponse(active, planType, s.getNextBillingDate().toString()));
    }
    
    @PostMapping("/cancel")
    public ResponseEntity<BooleanResponse> cancel(@RequestParam("userId") UUID userId) {
        System.out.println("[Billing][SubscriptionController.cancel] start userId=" + userId);
        Optional<UserSubscriptionEntity> sub = userSubscriptionRepository.findTopByUserAccountAndActiveTrueOrderByNextBillingDateDesc(userId);
        if (sub.isEmpty()) {
            System.out.println("[Billing][SubscriptionController.cancel] no active subscription");
            return ResponseEntity.ok(BooleanResponse.builder().value(false).build());
        }
        UserSubscriptionEntity s = sub.get();
        s.setActive(false);
        userSubscriptionRepository.save(s);
        System.out.println("[Billing][SubscriptionController.cancel] canceled");
        return ResponseEntity.ok(BooleanResponse.builder().value(true).build());
    }
    
    @PostMapping("/renew")
    public ResponseEntity<SubscriptionStatusResponse> renew(@RequestParam("userId") UUID userId) {
        System.out.println("[Billing][SubscriptionController.renew] start userId=" + userId);
        Optional<UserSubscriptionEntity> sub = userSubscriptionRepository.findTopByUserAccountAndActiveTrueOrderByNextBillingDateDesc(userId);
        if (sub.isEmpty()) {
            System.out.println("[Billing][SubscriptionController.renew] no active subscription");
            return ResponseEntity.ok(new SubscriptionStatusResponse(false, null, null));
        }
        UserSubscriptionEntity s = sub.get();
        PlanType planType = PlanType.fromString(s.getPlan().getName());
        LocalDateTime base = s.getNextBillingDate().isAfter(LocalDateTime.now()) ? s.getNextBillingDate() : LocalDateTime.now();
        LocalDateTime next = planType == PlanType.MONTHLY ? base.plusMonths(1) : base.plusYears(1);
        s.setNextBillingDate(next);
        s.setActive(true);
        userSubscriptionRepository.save(s);
        System.out.println("[Billing][SubscriptionController.renew] renewed, nextBilling=" + next);
        return ResponseEntity.ok(new SubscriptionStatusResponse(true, planType, next.toString()));
    }
    
    @PostMapping("/update")
    public ResponseEntity<SubscriptionStatusResponse> update(@RequestParam("userId") UUID userId,
                                                             @RequestParam("plan") PlanType planType) {
        System.out.println("[Billing][SubscriptionController.update] start userId=" + userId + ", plan=" + planType);
        String planName = planType.name();
        SubscriptionPlanEntity plan = subscriptionPlanRepository
                .findAll().stream().filter(p -> planName.equalsIgnoreCase(p.getName())).findFirst()
                .orElseGet(() -> {
                    SubscriptionPlanEntity p = new SubscriptionPlanEntity();
                    p.setName(planName);
                    p.setMonthlyPrice(planType == PlanType.MONTHLY ? 49.90 : 41.58);
                    p.setFeaturesJson("{\"basic\":true}");
                    return subscriptionPlanRepository.save(p);
                });
        Optional<UserSubscriptionEntity> sub = userSubscriptionRepository.findTopByUserAccountAndActiveTrueOrderByNextBillingDateDesc(userId);
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime nextBilling = planType == PlanType.MONTHLY ? start.plusMonths(1) : start.plusYears(1);
        if (sub.isPresent()) {
            UserSubscriptionEntity s = sub.get();
            s.setPlan(plan);
            s.setStartDate(start);
            s.setNextBillingDate(nextBilling);
            s.setActive(true);
            userSubscriptionRepository.save(s);
            System.out.println("[Billing][SubscriptionController.update] updated, nextBilling=" + nextBilling);
            return ResponseEntity.ok(new SubscriptionStatusResponse(true, planType, nextBilling.toString()));
        } else {
            UserSubscriptionEntity s = new UserSubscriptionEntity();
            s.setUserAccount(userId);
            s.setPlan(plan);
            s.setStartDate(start);
            s.setNextBillingDate(nextBilling);
            s.setActive(true);
            userSubscriptionRepository.save(s);
            System.out.println("[Billing][SubscriptionController.update] created, nextBilling=" + nextBilling);
            return ResponseEntity.ok(new SubscriptionStatusResponse(true, planType, nextBilling.toString()));
        }
    }
}
