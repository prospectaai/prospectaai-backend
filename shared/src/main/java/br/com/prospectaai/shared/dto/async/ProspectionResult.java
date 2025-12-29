package br.com.prospectaai.shared.dto.async;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProspectionResult {
    private String query;
    private AsyncTaskMessageType type;
    private AsyncTaskPlatform platform;

    private String telefone;
    private String nomeEmpresa;
    private String endereco;
    private String website;
    private String rating;
    private String reviews;
    private String especialidades;
}

