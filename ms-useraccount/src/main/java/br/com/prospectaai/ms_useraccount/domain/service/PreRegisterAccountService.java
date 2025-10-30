/*
 * @(#)PreRegisterAccountService.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import br.com.prospectaai.ms_useraccount.domain.dto.RegisterRequest;
import br.com.prospectaai.ms_useraccount.domain.dto.RegisterResponse;
import br.com.prospectaai.ms_useraccount.domain.entity.PreRegisterAccountEntity;
import br.com.prospectaai.ms_useraccount.domain.mapper.PreRegisterAccountMapper;
import br.com.prospectaai.ms_useraccount.domain.repository.PreRegisterAccountRepository;
import br.com.prospectaai.ms_useraccount.domain.validation.PreRegisterAccountValidation;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PreRegisterAccountService {
    private final PreRegisterAccountRepository preRegisterAccountRepository;
    private final PreRegisterAccountMapper preRegisterAccountMapper;

    /**
     * Pre-register a new account.
     * 
     * @param registerRequest the registration request containing account details
     * @author Victor Barberino
     * @return the response with a success message
     * @throws IllegalArgumentException if the email is already registered or the password does not meet the requirements
     */
    public RegisterResponse doPreRegister(@Validated RegisterRequest registerRequest) {
        PreRegisterAccountValidation.validatePreRegister(registerRequest, preRegisterAccountRepository);
        
        // Verificar se já existe um pré-cadastro com este email
        PreRegisterAccountEntity entity;
        if (preRegisterAccountRepository.existsByEmail(registerRequest.getEmail())) {
            // Buscar o pré-cadastro existente
            entity = preRegisterAccountRepository.findByEmail(registerRequest.getEmail());
            
            // Atualizar os dados do pré-cadastro
            entity.setDisplayName(registerRequest.getDisplayName());
            entity.setScope(registerRequest.getScope());

            // Validar e atualizar a senha se fornecida
            PreRegisterAccountValidation.validatePreRegisterData(registerRequest);
            entity.setPasswordHash(registerRequest.getPasswordHash());
            
            // Salvar as alterações
            entity = preRegisterAccountRepository.save(entity);
        } else {
            // Criar um novo pré-cadastro
            entity = preRegisterAccountRepository.save(preRegisterAccountMapper.toEntity(registerRequest));
        }
        
        return preRegisterAccountMapper.toResponseWithMessage(entity);
    }
}
