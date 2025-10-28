package br.com.prospectaai.ms_useraccount.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.prospectaai.ms_useraccount.domain.entity.UserAccountEntity;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, UUID> {
    Optional<UserAccountEntity> findByEmail(String email);
}
