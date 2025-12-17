/*
 * @(#)OAuthCredentialService.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.domain.service;

import br.com.prospectaai.ms_useraccount.domain.entity.OAuthCredentialEntity;
import br.com.prospectaai.ms_useraccount.domain.entity.PreRegisterAccountEntity;
import br.com.prospectaai.ms_useraccount.domain.entity.UserAccountEntity;
import br.com.prospectaai.ms_useraccount.domain.repository.OAuthCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthCredentialService {
    private final OAuthCredentialRepository oAuthCredentialRepository;

    public boolean createOAuthCredential(UserAccountEntity account, PreRegisterAccountEntity preRegister) {
        // Evita duplicação: se já existir credencial para esse usuário e provedor, não cria novamente
        if (oAuthCredentialRepository.existsByUserAccountAndProvider(account, preRegister.getProvider())) {
            System.out.println("[OAuth] Credencial já vinculada para provedor " + preRegister.getProvider() + ": " + account.getEmail());
            return true;
        }

        OAuthCredentialEntity oAuthCredential = new OAuthCredentialEntity();
        oAuthCredential.setProviderUserId(preRegister.getProviderUserId());
        oAuthCredential.setUserAccount(account);
        oAuthCredential.setProvider(preRegister.getProvider());
        oAuthCredentialRepository.save(oAuthCredential);
        return true;
    }
}
