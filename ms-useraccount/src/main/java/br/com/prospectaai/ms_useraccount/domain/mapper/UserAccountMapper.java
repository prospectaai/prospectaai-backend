/*
 * @(#)UserAccountMapper.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.domain.mapper;

import br.com.prospectaai.ms_useraccount.domain.entity.PreRegisterAccountEntity;
import br.com.prospectaai.ms_useraccount.domain.entity.UserAccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserAccountMapper {
    UserAccountMapper INSTANCE = Mappers.getMapper(UserAccountMapper.class);

    @Mapping(target = "accountId" , ignore = true)
    @Mapping(target = "active" , ignore = true)
    @Mapping(target = "role" , ignore = true)
    @Mapping(target = "localCredential" , ignore = true)
    @Mapping(target = "oatuhCredentials" , ignore = true)
    UserAccountEntity fromPreRegister(PreRegisterAccountEntity preRegisterAccountEntity);
}
