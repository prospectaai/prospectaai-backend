package br.com.prospectaai.ms_useraccount.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import br.com.prospectaai.ms_useraccount.domain.dto.RegisterRequest;
import br.com.prospectaai.ms_useraccount.domain.dto.RegisterResponse;
import br.com.prospectaai.ms_useraccount.domain.entity.PreRegisterAccountEntity;
import br.com.prospectaai.ms_useraccount.domain.enums.PreRegisterScope;
import br.com.prospectaai.ms_useraccount.domain.mapper.PreRegisterAccountMapper;
import br.com.prospectaai.ms_useraccount.domain.repository.PreRegisterAccountRepository;

public class PreRegisterAccountServiceTest {

    @Mock
    private PreRegisterAccountRepository preRegisterAccountRepository;

    @Mock
    private PreRegisterAccountMapper preRegisterAccountMapper;

    @InjectMocks
    private PreRegisterAccountService preRegisterAccountService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testDoPreRegister_NewAccount() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setDisplayName("Test User");
        request.setEmail("test@example.com");
        request.setScope(PreRegisterScope.INTERNAL);
        request.setPasswordHash("Senha@1234");
        
        PreRegisterAccountEntity entity = new PreRegisterAccountEntity();
        entity.setPreRegisterId(UUID.randomUUID());
        entity.setDisplayName("Test User");
        entity.setEmail("test@example.com");
        entity.setPasswordHash("Senha@1234");
        entity.setScope(PreRegisterScope.INTERNAL);
        
        RegisterResponse expectedResponse = new RegisterResponse();
        expectedResponse.setPreRegisterId(entity.getPreRegisterId());
        expectedResponse.setDisplayName("Test User");
        expectedResponse.setEmail("test@example.com");
        expectedResponse.setMessage("Pré-cadastro realizado com sucesso");
        expectedResponse.setScope(PreRegisterScope.INTERNAL);
        
        when(preRegisterAccountRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(preRegisterAccountMapper.toEntity(request)).thenReturn(entity);
        when(preRegisterAccountRepository.save(entity)).thenReturn(entity);
        when(preRegisterAccountMapper.toResponseWithMessage(entity)).thenReturn(expectedResponse);
        
        // Act
        RegisterResponse response = preRegisterAccountService.doPreRegister(request);
        
        // Assert
        assertNotNull(response);
        assertEquals(expectedResponse.getPreRegisterId(), response.getPreRegisterId());
        assertEquals(expectedResponse.getDisplayName(), response.getDisplayName());
        assertEquals(expectedResponse.getEmail(), response.getEmail());
        assertEquals(expectedResponse.getMessage(), response.getMessage());
    }

    @Test
    public void testDoPreRegister_ExistingAccount() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setDisplayName("Updated User");
        request.setEmail("existing@example.com");
        request.setScope(PreRegisterScope.INTERNAL);
        request.setPasswordHash("Senha@1234UPDATE");

        PreRegisterAccountEntity existingEntity = new PreRegisterAccountEntity();
        existingEntity.setPreRegisterId(UUID.randomUUID());
        existingEntity.setDisplayName("Original User");
        existingEntity.setEmail("existing@example.com");
        existingEntity.setScope(PreRegisterScope.INTERNAL);
        existingEntity.setPasswordHash("Senha@1234");

        PreRegisterAccountEntity updatedEntity = new PreRegisterAccountEntity();
        updatedEntity.setPreRegisterId(existingEntity.getPreRegisterId());
        updatedEntity.setDisplayName("Updated User");
        updatedEntity.setEmail("existing@example.com");
        updatedEntity.setScope(PreRegisterScope.INTERNAL);
        updatedEntity.setPasswordHash("Senha@1234UPDATE");

        RegisterResponse expectedResponse = new RegisterResponse();
        expectedResponse.setPreRegisterId(updatedEntity.getPreRegisterId());
        expectedResponse.setDisplayName("Updated User");
        expectedResponse.setEmail("existing@example.com");
        expectedResponse.setMessage("Pré-cadastro atualizado com sucesso");
        expectedResponse.setScope(PreRegisterScope.INTERNAL);
        
        when(preRegisterAccountRepository.existsByEmail(request.getEmail())).thenReturn(true);
        when(preRegisterAccountRepository.findByEmail(request.getEmail())).thenReturn(existingEntity);
        when(preRegisterAccountRepository.save(any(PreRegisterAccountEntity.class))).thenReturn(updatedEntity);
        when(preRegisterAccountMapper.toResponseWithMessage(updatedEntity)).thenReturn(expectedResponse);
        
        // Act
        RegisterResponse response = preRegisterAccountService.doPreRegister(request);
        
        // Assert
        assertNotNull(response);
        assertEquals(expectedResponse.getPreRegisterId(), response.getPreRegisterId());
        assertEquals(expectedResponse.getDisplayName(), response.getDisplayName());
        assertEquals(expectedResponse.getEmail(), response.getEmail());
        assertEquals(expectedResponse.getMessage(), response.getMessage());
    }
}