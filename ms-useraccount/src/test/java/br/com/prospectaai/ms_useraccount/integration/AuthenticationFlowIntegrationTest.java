package br.com.prospectaai.ms_useraccount.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.prospectaai.ms_useraccount.domain.dto.LoginRequest;
import br.com.prospectaai.ms_useraccount.domain.dto.RegisterRequest;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthenticationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testRegisterAndLoginFlow() throws Exception {
        // 1. Registrar um novo usuário
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setDisplayName("Usuário Teste");
        registerRequest.setEmail("teste@example.com");
        registerRequest.setPasswordHash("Senha@123");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("teste@example.com"))
                .andExpect(jsonPath("$.displayName").value("Usuário Teste"))
                .andExpect(jsonPath("$.message").exists());

        // 2. Tentar login com o usuário pré-registrado
        // Nota: Em um fluxo real, o usuário precisaria completar o checkout antes de poder fazer login
        // Este teste simula apenas o fluxo de pré-registro
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("teste@example.com");
        loginRequest.setPassword("Senha@123");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().is4xxClientError()); // Deve falhar pois é apenas pré-registro
    }
}