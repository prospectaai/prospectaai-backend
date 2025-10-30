/*
 * @(#)RegisterResponse.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.domain.dto;

import java.util.UUID;

import br.com.prospectaai.ms_useraccount.domain.enums.PreRegisterScope;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterResponse {
    private UUID preRegisterId;
    private String displayName;
    private String email;
    private String message;
    private PreRegisterScope scope;
}
