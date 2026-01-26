package br.com.prospectaai.shared.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsOverview {
    private long empresasProspectadasTotal;
    private double empresasProspectadasVariationPercentMonth;
    private long buscasAtivasTotal;
    private long buscasAgendadas;
    private String plataformaMaisUsada;
    private long cidadesTotal;
}
