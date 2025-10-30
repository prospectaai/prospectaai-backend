/*
 * @(#)PreRegisterAccountRepository.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.prospectaai.ms_useraccount.domain.entity.PreRegisterAccountEntity;

public interface PreRegisterAccountRepository extends JpaRepository<PreRegisterAccountEntity, UUID> {
    boolean existsByEmail(String email);
}
