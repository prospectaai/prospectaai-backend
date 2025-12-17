/*
 * @(#)ConfirmCodeRequest.java
 */

package br.com.prospectaai.ms_useraccount.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmCodeRequest {
    @NotBlank(message = "preRegisterId é obrigatório")
    private String preRegisterId;

    @NotBlank(message = "code é obrigatório")
    private String code;
}