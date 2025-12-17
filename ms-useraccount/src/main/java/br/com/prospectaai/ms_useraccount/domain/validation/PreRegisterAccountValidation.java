/*
 * @(#)PreRegisterAccountValidation.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.domain.validation;

import br.com.prospectaai.ms_useraccount.domain.dto.RegisterRequest;
import br.com.prospectaai.ms_useraccount.domain.enums.PreRegisterScope;
import br.com.prospectaai.ms_useraccount.domain.repository.PreRegisterAccountRepository;
import br.com.prospectaai.ms_useraccount.domain.repository.UserAccountRepository;

public class PreRegisterAccountValidation {
    public static void validatePreRegister(RegisterRequest registerRequest, UserAccountRepository userAccountRepository, PreRegisterAccountRepository preRegisterAccountRepository) {
        // Verificamos se já existe um usuário com este email no repositório de usuários
        // Se existir, lançamos exceção indicando que o usuário já está registrado
        if (userAccountRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email já registrado");
        }
        
        // Verificamos se já existe um usuário com este email no repositório de pré-cadastros
        // Se existir, em vez de lançar exceção, vamos atualizar o pré-cadastro existente
        // Isso permite que o usuário tente novamente o checkout se não completou anteriormente
        if (preRegisterAccountRepository.existsByEmail(registerRequest.getEmail())) {
            // Não lançamos exceção, permitindo a atualização do pré-cadastro existente
            return;
        }

        validatePreRegisterData(registerRequest);
    }

    public static void validatePreRegisterData(RegisterRequest registerRequest) {
        if(registerRequest.getScope().equals(PreRegisterScope.OAUTH2)) {
            return;
        }
        // validar senha: não pode ser nula ou vazia
        String passwordHash = registerRequest.getPasswordHash();
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash é obrigatório para escopo INTERNAL");
        }

        // validar senha, deve conter pelo menos 8 caracteres, 1 letra maiúscula, 1 letra minúscula, 1 número e 1 caractere especial
        if (!passwordHash.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")) {
            throw new IllegalArgumentException("A senha deve conter ao menos 8 caracteres, 1 letra maiúscula, 1 letra minúscula, 1 número e 1 caractere especial");
        }
    }
}
