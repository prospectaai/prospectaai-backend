/*
 * @(#)PreRegisterAccountService.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.domain.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import br.com.prospectaai.ms_useraccount.domain.dto.RegisterRequest;
import br.com.prospectaai.ms_useraccount.domain.dto.RegisterResponse;
import br.com.prospectaai.ms_useraccount.domain.entity.PreRegisterAccountEntity;
import br.com.prospectaai.ms_useraccount.domain.enums.PreRegisterScope;
import br.com.prospectaai.ms_useraccount.domain.mapper.PreRegisterAccountMapper;
import br.com.prospectaai.ms_useraccount.domain.repository.PreRegisterAccountRepository;
import br.com.prospectaai.ms_useraccount.domain.repository.UserAccountRepository;
import br.com.prospectaai.ms_useraccount.domain.validation.PreRegisterAccountValidation;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PreRegisterAccountService {
    private final UserAccountRepository userAccountRepository;
    private final PreRegisterAccountRepository preRegisterAccountRepository;
    private final PreRegisterAccountMapper preRegisterAccountMapper;

    @Autowired(required = false)
    private EmailService emailService;

    /**
     * Pre-register a new account.
     * 
     * @param registerRequest the registration request containing account details
     * @author Victor Barberino
     * @return the response with a success message
     * @throws IllegalArgumentException if the email is already registered or the password does not meet the requirements
     */
    public RegisterResponse doPreRegister(@Validated RegisterRequest registerRequest) {
        PreRegisterAccountValidation.validatePreRegister(registerRequest, userAccountRepository, preRegisterAccountRepository);
        
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

        if(registerRequest.getScope().equals(PreRegisterScope.INTERNAL)) {
            // Gerar código de confirmação de 6 dígitos e validade de 30 minutos
            String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
            entity.setConfirmationCode(code);
            entity.setConfirmationCodeExpiresAt(LocalDateTime.now().plusMinutes(30));
            entity.setPreRegisterValidated(Boolean.FALSE);
            entity.setPreRegisterValidatedAt(null);
            entity = preRegisterAccountRepository.save(entity);

            System.out.println("[UserAccount][PreRegisterAccountService] Código de confirmação gerado: preRegisterId=" +
                    entity.getPreRegisterId() + ", code=" + code);

            // Enviar email de confirmação (se serviço estiver disponível)
            if (emailService != null) {
                emailService.sendPreRegisterConfirmationEmail(entity);
            }
        }
        else {
            entity.setPreRegisterValidated(Boolean.TRUE);
            entity.setPreRegisterValidatedAt(LocalDateTime.now());
            entity = preRegisterAccountRepository.save(entity);
        }

        return preRegisterAccountMapper.toResponseWithMessage(entity);
    }

     /**
     * Valida se o preRegisterId existe no banco de dados.
     */
    public boolean validatePreRegisterId(UUID preRegisterId) {
        return preRegisterAccountRepository.existsById(preRegisterId) && 
        preRegisterAccountRepository.findById(preRegisterId).get().getPreRegisterValidated();
    }

    /**
     * Valida o código de confirmação enviado ao email.
     */
    public RegisterResponse validateConfirmationCode(UUID preRegisterId, String code) {
        PreRegisterAccountEntity entity = preRegisterAccountRepository.findById(preRegisterId).orElse(null);
        if (entity == null) {
            throw new IllegalArgumentException("Pré-cadastro não encontrado para o ID fornecido");
        }

        if (entity.getPreRegisterValidated() != null && entity.getPreRegisterValidated()) {
            RegisterResponse res = preRegisterAccountMapper.toResponse(entity);
            res.setMessage("Pré-cadastro já validado");
            return res;
        }

        if (entity.getConfirmationCode() == null || entity.getConfirmationCodeExpiresAt() == null) {
            throw new IllegalArgumentException("Código de confirmação não encontrado para este pré-cadastro");
        }

        if (!entity.getConfirmationCode().equals(code)) {
            throw new IllegalArgumentException("Código de confirmação inválido");
        }

        if (LocalDateTime.now().isAfter(entity.getConfirmationCodeExpiresAt())) {
            throw new IllegalArgumentException("Código de confirmação expirado");
        }

        entity.setPreRegisterValidated(Boolean.TRUE);
        entity.setPreRegisterValidatedAt(LocalDateTime.now());
        entity = preRegisterAccountRepository.save(entity);

        RegisterResponse res = preRegisterAccountMapper.toResponse(entity);
        res.setMessage("Código validado com sucesso");
        return res;
    }

    public RegisterResponse resendConfirmationCode(UUID preRegisterId) {
        PreRegisterAccountEntity entity = preRegisterAccountRepository.findById(preRegisterId).orElse(null);
        if (entity == null) {
            throw new IllegalArgumentException("Pré-cadastro não encontrado para o ID fornecido");
        }

        if (Boolean.TRUE.equals(entity.getPreRegisterValidated())) {
            RegisterResponse res = preRegisterAccountMapper.toResponse(entity);
            res.setMessage("Pré-cadastro já validado");
            return res;
        }

        String code = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));
        entity.setConfirmationCode(code);
        entity.setConfirmationCodeExpiresAt(java.time.LocalDateTime.now().plusMinutes(30));
        entity = preRegisterAccountRepository.save(entity);

        if (emailService != null) {
            emailService.sendPreRegisterConfirmationEmail(entity);
        }

        RegisterResponse res = preRegisterAccountMapper.toResponse(entity);
        res.setMessage("Código reenviado com sucesso");
        return res;
    }
}
