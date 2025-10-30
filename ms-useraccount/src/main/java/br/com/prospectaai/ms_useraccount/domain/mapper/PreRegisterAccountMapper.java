/*
 * @(#)PreRegisterAccountMapper.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.domain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import br.com.prospectaai.ms_useraccount.domain.dto.RegisterRequest;
import br.com.prospectaai.ms_useraccount.domain.dto.RegisterResponse;
import br.com.prospectaai.ms_useraccount.domain.entity.PreRegisterAccountEntity;

@Mapper(componentModel = "spring")
public interface PreRegisterAccountMapper {
    
    PreRegisterAccountMapper INSTANCE = Mappers.getMapper(PreRegisterAccountMapper.class);
    
    @Mapping(target = "preRegisterId", ignore = true)
    PreRegisterAccountEntity toEntity(RegisterRequest request);
    
    RegisterResponse toResponse(PreRegisterAccountEntity entity);
    
    @Mapping(target = "message", constant = "Pré-registro realizado com sucesso")
    RegisterResponse toResponseWithMessage(PreRegisterAccountEntity entity);
}