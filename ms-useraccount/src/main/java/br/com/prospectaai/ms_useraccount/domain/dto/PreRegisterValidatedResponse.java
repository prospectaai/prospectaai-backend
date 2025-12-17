/*
 * @(#)PreRegisterValidatedResponse.java
 */

package br.com.prospectaai.ms_useraccount.domain.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PreRegisterValidatedResponse {
    private UUID preRegisterId;
    private String token;
    private LocalDateTime expiredAt;
    private String message;
}