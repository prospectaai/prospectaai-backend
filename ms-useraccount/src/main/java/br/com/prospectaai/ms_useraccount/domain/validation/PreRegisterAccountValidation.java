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

public class PreRegisterAccountValidation {
    public static void validatePreRegister(RegisterRequest registerRequest, PreRegisterAccountRepository preRegisterAccountRepository) {
        if (preRegisterAccountRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        if(registerRequest.getScope().equals(PreRegisterScope.OAUTH2)) {
            return;
        }

        // validar senha, deve conter pelo menos 8 caracteres, 1 letra maiúscula, 1 letra minúscula, 1 número e 1 caractere especial
        if (!registerRequest.getPasswordHash().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")) {
            throw new IllegalArgumentException("Password must contain at least 8 characters, 1 uppercase letter, 1 lowercase letter, 1 number and 1 special character");
        }
        
    }
}
