/*
 * @(#)RegisterController.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.domain.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.prospectaai.ms_useraccount.domain.dto.RegisterRequest;
import br.com.prospectaai.ms_useraccount.domain.dto.RegisterResponse;
import br.com.prospectaai.ms_useraccount.domain.dto.ResendCodeRequest;
import br.com.prospectaai.ms_useraccount.domain.dto.ConfirmCodeRequest;
import br.com.prospectaai.ms_useraccount.domain.dto.PreRegisterValidatedResponse;
import br.com.prospectaai.ms_useraccount.domain.enums.PreRegisterScope;
import br.com.prospectaai.ms_useraccount.domain.service.PreRegisterAccountService;
import br.com.prospectaai.ms_useraccount.jwt.JwtTokenProvider;
import br.com.prospectaai.shared.dto.BooleanResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class RegisterController {

    private final PreRegisterAccountService preRegisterAccountService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        // Definir o escopo como LOCAL para registros internos
        request.setScope(PreRegisterScope.INTERNAL);
        RegisterResponse response = preRegisterAccountService.doPreRegister(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm-code")
    public ResponseEntity<PreRegisterValidatedResponse> confirmCode(@RequestBody ConfirmCodeRequest request) {
        RegisterResponse res = preRegisterAccountService.validateConfirmationCode(UUID.fromString(request.getPreRegisterId()), request.getCode());
        String token = jwtTokenProvider.generatePreRegisterValidatedToken(res.getPreRegisterId(), res.getEmail(), res.getScope());
        PreRegisterValidatedResponse response = new PreRegisterValidatedResponse(
                res.getPreRegisterId(),
                token,
                LocalDateTime.ofInstant(jwtTokenProvider.extractExpiration(token).toInstant(), ZoneId.systemDefault()),
                res.getMessage()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-code")
    public ResponseEntity<RegisterResponse> resendCode(@RequestBody ResendCodeRequest request) {
        java.util.UUID id = java.util.UUID.fromString(request.getPreRegisterId());
        RegisterResponse res = preRegisterAccountService.resendConfirmationCode(id);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/validate-pre-register-id")
    public ResponseEntity<BooleanResponse> validatePreRegisterId(@RequestParam(name = "preRegisterId", required = true) String preRegisterId) {
        boolean isValid = preRegisterAccountService.validatePreRegisterId(UUID.fromString(preRegisterId));
        return ResponseEntity.ok(BooleanResponse.builder().value(isValid).build());
    }
}
