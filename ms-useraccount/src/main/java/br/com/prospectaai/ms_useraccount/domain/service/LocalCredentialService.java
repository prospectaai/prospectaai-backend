/*
 * @(#)LocalCredentialService.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.domain.service;

import br.com.prospectaai.ms_useraccount.domain.entity.LocalCredentialEntity;
import br.com.prospectaai.ms_useraccount.domain.entity.UserAccountEntity;
import br.com.prospectaai.ms_useraccount.domain.repository.LocalCredentialRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalCredentialService {
    private final LocalCredentialRepository localCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean createLocalCredential(UserAccountEntity account, String passwordHash) {
        LocalCredentialEntity localCredential = new LocalCredentialEntity();
        localCredential.setPasswordHash(passwordEncoder.encode(passwordHash));
        localCredential.setUserAccount(account);
        localCredentialRepository.save(localCredential);
        return true;
    }
}
