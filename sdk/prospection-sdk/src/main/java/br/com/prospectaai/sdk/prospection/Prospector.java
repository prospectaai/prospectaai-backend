package br.com.prospectaai.sdk.prospection;

import java.util.List;
import br.com.prospectaai.shared.dto.async.ProspectionResult;

public interface Prospector {
    List<ProspectionResult> prospect(String query) throws Exception;
}
