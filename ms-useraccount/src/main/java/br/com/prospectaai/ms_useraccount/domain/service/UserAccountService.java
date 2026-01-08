/*
 * @(#)UserAccountService.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.domain.service;

import br.com.prospectaai.ms_useraccount.domain.dto.LoginResponse;
import br.com.prospectaai.ms_useraccount.domain.entity.PreRegisterAccountEntity;
import br.com.prospectaai.ms_useraccount.domain.entity.UserAccountEntity;
import br.com.prospectaai.ms_useraccount.domain.mapper.UserAccountMapper;
import br.com.prospectaai.ms_useraccount.domain.repository.PreRegisterAccountRepository;
import br.com.prospectaai.ms_useraccount.domain.repository.UserAccountRepository;
import br.com.prospectaai.ms_useraccount.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAccountService {
    private final PreRegisterAccountRepository preRegisterAccountRepository;
    private final UserAccountRepository userAccountRepository;
    private final LocalCredentialService localCredentialService;
    private final OAuthCredentialService oAuthCredentialService;
    private final UserAccountMapper userAccountMapper;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public LoginResponse completeRegister(UUID preRegisterId) {
        Optional<PreRegisterAccountEntity> optionalPreRegisterAccountEntity = preRegisterAccountRepository.findById(preRegisterId);

        if(optionalPreRegisterAccountEntity.isEmpty()) {
            throw new IllegalArgumentException("Pré-cadastro não encontrado para o ID fornecido");
        }
        PreRegisterAccountEntity preRegisterAccountEntity = optionalPreRegisterAccountEntity.get();
        // Se já existir usuário com este email, não criar novamente (idempotente)
        Optional<UserAccountEntity> existingUserOpt = this.userAccountRepository.findByEmail(preRegisterAccountEntity.getEmail());
        if (existingUserOpt.isPresent()) {
            UserAccountEntity existingUser = existingUserOpt.get();

            switch (preRegisterAccountEntity.getScope()) {
                case INTERNAL -> {
                    // Cria credencial local apenas se ainda não existir
                    if (existingUser.getLocalCredential() == null) {
                        if(!localCredentialService.createLocalCredential(existingUser, preRegisterAccountEntity.getPasswordHash()))
                            throw new IllegalArgumentException("Erro ao criar credenciais locais");
                    }
                }
                case OAUTH2 -> {
                    // Cria credencial OAuth2 apenas se não existir para o provedor
                    if(!oAuthCredentialService.createOAuthCredential(existingUser, preRegisterAccountEntity))
                        throw new IllegalArgumentException("Erro ao criar credenciais OAuth2");
                }
                default -> throw new IllegalArgumentException("Escopo de registro desconhecido");
            }

            System.out.println("[Checkout] Usuário já existente, não será reinserido: " + existingUser.getEmail());
            String token = jwtTokenProvider.generateToken(existingUser);
            return new LoginResponse(
                token,
                LocalDateTime.ofInstant(jwtTokenProvider.extractExpiration(token).toInstant(), ZoneId.systemDefault())
            );
        }

        // Não existe usuário: criar novo
        UserAccountEntity userAccountEntity = this.userAccountRepository.save(userAccountMapper.fromPreRegister(preRegisterAccountEntity));

        switch (preRegisterAccountEntity.getScope()) {
            case INTERNAL -> {
                if(!localCredentialService.createLocalCredential(userAccountEntity, preRegisterAccountEntity.getPasswordHash()))
                    throw new IllegalArgumentException("Erro ao criar credenciais locais");
            }
            case OAUTH2 -> {
                if(!oAuthCredentialService.createOAuthCredential(userAccountEntity, preRegisterAccountEntity))
                    throw new IllegalArgumentException("Erro ao criar credenciais OAuth2");
            }
            default -> throw new IllegalArgumentException("Escopo de registro desconhecido");
        }

        // enviar um email de boas vindas

        System.out.println("Usuario com email completou seu cadastro e finalmente eh um usuário oficial do prospectaai: " + userAccountEntity.getEmail());
        // autenticar o usuário
        String token = jwtTokenProvider.generateToken(userAccountEntity);
        return new LoginResponse(
            token,
            LocalDateTime.ofInstant(jwtTokenProvider.extractExpiration(token).toInstant(), ZoneId.systemDefault())
        );
    }

    public boolean isUserExist(String email) {
        return userAccountRepository.findByEmail(email).isPresent();
    }
}
