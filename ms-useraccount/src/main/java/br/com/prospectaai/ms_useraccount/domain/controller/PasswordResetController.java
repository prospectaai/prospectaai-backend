package br.com.prospectaai.ms_useraccount.domain.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.prospectaai.ms_useraccount.domain.dto.ForgotPasswordRequest;
import br.com.prospectaai.ms_useraccount.domain.dto.ResetPasswordRequest;
import br.com.prospectaai.ms_useraccount.domain.service.PasswordResetService;
import br.com.prospectaai.shared.dto.BooleanResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<BooleanResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        boolean ok = passwordResetService.initiate(request.getEmail());
        return ResponseEntity.ok(BooleanResponse.builder().value(ok).build());
    }

    @GetMapping("/reset-password/validate")
    public ResponseEntity<BooleanResponse> validateToken(@RequestParam("token") String token) {
        boolean valid = passwordResetService.validateToken(token);
        return ResponseEntity.ok(BooleanResponse.builder().value(valid).build());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<BooleanResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        boolean changed = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(BooleanResponse.builder().value(changed).build());
    }
}