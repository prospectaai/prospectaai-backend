/*
 * @(#)RegisterRequest.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import br.com.prospectaai.ms_useraccount.domain.enums.PreRegisterScope;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterRequest {
    @NotBlank(message = "displayName is required")
    private String displayName;

    @Email(message = "email is invalid")
    @NotBlank(message = "email is required")
    private String email;

    private String passwordHash;

    // OAuth2 scope is used to identify if the user is registering through OAuth2
    @JsonIgnore
    private String provider;
    @JsonIgnore
    private PreRegisterScope scope;
    @JsonIgnore
    private String avatarUrl;
    @JsonIgnore
    private String providerUserId;
}
