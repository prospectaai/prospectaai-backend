/*
 * @(#)OAuth2SuccessHandler.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.oauth;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import br.com.prospectaai.ms_useraccount.domain.dto.RegisterRequest;
import br.com.prospectaai.ms_useraccount.domain.dto.RegisterResponse;
import br.com.prospectaai.ms_useraccount.domain.entity.UserAccountEntity;
import br.com.prospectaai.ms_useraccount.domain.enums.PreRegisterScope;
import br.com.prospectaai.ms_useraccount.domain.repository.UserAccountRepository;
import br.com.prospectaai.ms_useraccount.domain.service.PreRegisterAccountService;
import br.com.prospectaai.ms_useraccount.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserAccountRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PreRegisterAccountService preRegisterAccountService;
    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    /**
     * Trata o sucesso da autenticação OAuth2.
     * Se o usuário não estiver registrado, faz o pré-registro.
     * Gera um token JWT e redireciona para a página de checkout com o token.
     * Se o usuário já estiver registrado, gera um token JWT e redireciona para a página de sucesso com o token.
     * 
     * @param request a requisição HTTP
     * @param response a resposta HTTP
     * @param authentication a autenticação OAuth2
     * @throws IOException se ocorrer um erro de I/O
     * 
     * @author Victor Barberino
     * @since 2025-10-30
     * @see AuthenticationSuccessHandler#onAuthenticationSuccess(HttpServletRequest, HttpServletResponse, Authentication)
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String provider = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String providerUserId = oAuth2User.getAttribute("sub");
        if (providerUserId == null) {
            providerUserId = String.valueOf(Objects.requireNonNull(oAuth2User.getAttribute("id")));
        }

        // procura user por e-mail ou faz o pre registro
        UserAccountEntity user = userRepository.findByEmail(email).orElseGet(() -> null );
        boolean isNeedPreRegister = user == null;
        RegisterResponse res = isNeedPreRegister ? preRegisterAccountService.doPreRegister(
                RegisterRequest.builder()
                    .provider(provider)
                    .providerUserId(providerUserId)
                    .displayName(oAuth2User.getAttribute("name"))
                    .email(email)
                    .avatarUrl(oAuth2User.getAttribute("picture"))
                    .scope(PreRegisterScope.OAUTH2)
                    .build()
            ) : null;

        // Gera JWT
        String jwt = isNeedPreRegister ? res.getPreRegisterId().toString() : jwtTokenProvider.generateToken(user);
        
        // Redireciona para checkout se for pré-cadastro ou para página de sucesso se já tiver conta
        if (isNeedPreRegister) {
            // Redireciona para checkout do frontend com o preRegisterId
            response.sendRedirect(frontendBaseUrl + "/checkout?prID=" + jwt);
        } else {
            LocalDateTime expiredAt = LocalDateTime.ofInstant(jwtTokenProvider.extractExpiration(jwt).toInstant(), ZoneId.systemDefault());
            // Usuário já tem conta, redireciona para dashboard do frontend com token JWT
            response.sendRedirect(frontendBaseUrl + "/auth/callback?token=" + jwt + "&expiredAt=" + expiredAt);
        }
    }
}
