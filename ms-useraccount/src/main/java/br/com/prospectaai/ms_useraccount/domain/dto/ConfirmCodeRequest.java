/*
 * @(#)ConfirmCodeRequest.java
 */

package br.com.prospectaai.ms_useraccount.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmCodeRequest {
    @Email(message = "email é inválido")
    @NotBlank(message = "email é obrigatório")
    private String email;

    @NotBlank(message = "code é obrigatório")
    private String code;
}