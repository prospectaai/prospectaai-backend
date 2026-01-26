package br.com.prospectaai.ms_async_task.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProspectionRecordDto {
    private String query;
    private String platform;
    private String nomeEmpresa;
    private String telefone;
    private String endereco;
    private String website;
    private String imageUrl;
    private Double rating;
    private Integer reviews;
    private String especialidades;
    private String createdAt;
}
