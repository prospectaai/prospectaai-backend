package br.com.prospectaai.ms_billing_sbs.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.prospectaai.ms_billing_sbs.domain.entity.UserSubscriptionEntity;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscriptionEntity, UUID>{

}
