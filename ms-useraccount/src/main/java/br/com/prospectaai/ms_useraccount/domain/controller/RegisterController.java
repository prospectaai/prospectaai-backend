/*
 * @(#)RegisterController.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.domain.controller;

import br.com.prospectaai.ms_useraccount.domain.dto.LoginResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.prospectaai.ms_useraccount.domain.dto.RegisterRequest;
import br.com.prospectaai.ms_useraccount.domain.dto.RegisterResponse;
import br.com.prospectaai.ms_useraccount.domain.dto.ConfirmCodeRequest;
import br.com.prospectaai.ms_useraccount.domain.dto.PreRegisterValidatedResponse;
import br.com.prospectaai.ms_useraccount.domain.enums.PreRegisterScope;
import br.com.prospectaai.ms_useraccount.domain.service.PreRegisterAccountService;
import br.com.prospectaai.ms_useraccount.jwt.JwtTokenProvider;
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

    public ResponseEntity<LoginResponse> completeRegister(@RequestParam(required = true) String preRegisterId) {

        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm-code")
    public ResponseEntity<PreRegisterValidatedResponse> confirmCode(@RequestBody ConfirmCodeRequest request) {
        RegisterResponse res = preRegisterAccountService.validateConfirmationCode(request.getEmail(), request.getCode());
        String token = jwtTokenProvider.generatePreRegisterValidatedToken(res.getPreRegisterId(), res.getEmail(), res.getScope());
        PreRegisterValidatedResponse response = new PreRegisterValidatedResponse(
                res.getPreRegisterId(),
                res.getEmail(),
                token,
                res.getMessage()
        );
        return ResponseEntity.ok(response);
    }
}