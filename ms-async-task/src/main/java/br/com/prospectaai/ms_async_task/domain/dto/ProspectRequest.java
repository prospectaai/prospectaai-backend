package br.com.prospectaai.ms_async_task.domain.dto;

import br.com.prospectaai.shared.dto.async.AsyncTaskPlatform;
import lombok.Data;

@Data
public class ProspectRequest {
    private String query;
    private AsyncTaskPlatform platform;
    private String location;
    private String businessType;
    private Integer radiusKm;
    private String companySize;
    private Boolean useAddress;
    private String addressStreet;
    private String addressNumber;
    private String addressCity;
    private String addressNeighborhood;
    private String addressState;
    private String addressZip;
    private Double latitude;
    private Double longitude;
    private String stateId;
    private String stateSigla;
    private String cityName;
}
