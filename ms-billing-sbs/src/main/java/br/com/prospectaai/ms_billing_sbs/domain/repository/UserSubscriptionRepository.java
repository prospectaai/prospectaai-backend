package br.com.prospectaai.ms_billing_sbs.domain.repository;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.prospectaai.ms_billing_sbs.domain.entity.UserSubscriptionEntity;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscriptionEntity, UUID>{
    Optional<UserSubscriptionEntity> findTopByUserAccountAndActiveTrueOrderByNextBillingDateDesc(UUID userAccount);
}
