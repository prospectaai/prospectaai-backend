package br.com.prospectaai.sdk.prospection;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

import br.com.prospectaai.shared.dto.async.AsyncTaskMessageType;
import br.com.prospectaai.shared.dto.async.AsyncTaskPlatform;
import br.com.prospectaai.shared.dto.async.ProspectionResult;

final class GoogleMapsProspector implements Prospector {
    private final String serpApiKey;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    GoogleMapsProspector(String serpApiKey) {
        this.serpApiKey = serpApiKey;
    }

    @Override
    public List<ProspectionResult> prospect(String query) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        URI uri = URI.create("https://serpapi.com/search?engine=google_maps&q=" + encoded + "&api_key=" + serpApiKey);
        HttpRequest req = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

        JsonNode root = mapper.readTree(res.body());
        JsonNode localResults = root.path("local_results");

        List<ProspectionResult> results = new ArrayList<>();
        if (localResults.isArray()) {
            for (JsonNode item : localResults) {
                String telefone = item.path("phone").asText(null);
                String nomeEmpresa = item.path("title").asText(null);
                String endereco = item.path("address").asText(null);
                String website = item.path("website").asText(null);
                String rating = item.path("rating").asText(null);
                String reviews = item.path("reviews").asText(null);
                String types = item.path("types").isArray()
                        ? joinTypes(item.path("types"))
                        : item.path("types").asText(null);

                results.add(ProspectionResult.builder()
                        .query(query)
                        .type(AsyncTaskMessageType.PROCESSED)
                        .platform(AsyncTaskPlatform.GOOGLE_MAPS)
                        .telefone(telefone)
                        .nomeEmpresa(nomeEmpresa)
                        .endereco(endereco)
                        .website(website)
                        .rating(rating)
                        .reviews(reviews)
                        .especialidades(types)
                        .build());
            }
        }
        return results;
    }

    private String joinTypes(JsonNode typesArray) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < typesArray.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(typesArray.get(i).asText());
        }
        return sb.toString();
    }
}
