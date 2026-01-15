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
import java.util.Map;

final class GoogleMapsProspector implements Prospector {
    private final String serpApiKey;
    private final String location;
    private final String businessType;
    private final String companySize;
    private final String languageHl;
    private final String glCountry;
    private final boolean randomize;
    private final String radiusKmStr;
    private final String reachabilityBaseUrl;
    private volatile String geoLat = null;
    private volatile String geoLon = null;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    GoogleMapsProspector(Map<String, String> config) {
        this.serpApiKey = config.getOrDefault("serpapi.apiKey", "");
        this.location = nullToEmpty(config.get("location"));
        this.businessType = nullToEmpty(config.get("businessType"));
        this.companySize = nullToEmpty(config.get("companySize"));
        this.languageHl = nullToEmpty(config.getOrDefault("hl", "pt-BR"));
        this.glCountry = nullToEmpty(config.getOrDefault("gl", "br"));
        this.randomize = "true".equalsIgnoreCase(config.getOrDefault("randomize", "true"));
        this.radiusKmStr = nullToEmpty(config.get("radiusKm"));
        this.reachabilityBaseUrl = nullToEmpty(config.getOrDefault("reachability.baseUrl", ""));
    }

    @Override
    public List<ProspectionResult> prospect(String query) throws Exception {
        String finalQuery = buildQuery(query);
        geocodeIfNeeded();
        String encoded = URLEncoder.encode(finalQuery, StandardCharsets.UTF_8);
        int start = computeStartOffset();
        String llParam = (geoLat != null && geoLon != null) ? ("&ll=" + geoLat + "," + geoLon) : "";
        String zParam = computeZoomParam();
        String url = "https://serpapi.com/search?engine=google_maps"
                + "&q=" + encoded
                + "&type=search"
                + "&hl=" + URLEncoder.encode(languageHl, StandardCharsets.UTF_8)
                + "&gl=" + URLEncoder.encode(glCountry, StandardCharsets.UTF_8)
                + "&google_domain=" + (glCountry.equalsIgnoreCase("br") ? "google.com.br" : "google.com")
                + "&start=" + start
                + llParam
                + zParam
                + "&api_key=" + serpApiKey;
        URI uri = URI.create(url);
        HttpRequest req = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

        JsonNode root = mapper.readTree(res.body());
        JsonNode localResults = root.path("local_results");

        List<ProspectionResult> results = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        java.util.List<ProspectionResult> whatsappCandidates = new java.util.ArrayList<>();
        java.util.List<ProspectionResult> others = new java.util.ArrayList<>();
        if (localResults.isArray()) {
            for (JsonNode item : localResults) {
                String rawPhone = item.path("phone").asText(null);
                String telefone = sanitizePhoneBR(rawPhone);
                String nomeEmpresa = item.path("title").asText(null);
                String endereco = item.path("address").asText(null);
                String website = item.path("website").asText(null);
                String rating = item.path("rating").asText(null);
                String reviews = item.path("reviews").asText(null);
                String types = item.path("types").isArray()
                        ? joinTypes(item.path("types"))
                        : item.path("types").asText(null);

                String key = (telefone != null && !telefone.isBlank())
                        ? telefone
                        : ((nomeEmpresa != null ? nomeEmpresa : "") + "|" + (endereco != null ? endereco : "")).toLowerCase();
                if (seen.contains(key)) continue;
                seen.add(key);

                ProspectionResult pr = ProspectionResult.builder()
                        .query(finalQuery)
                        .type(AsyncTaskMessageType.PROCESSED)
                        .platform(AsyncTaskPlatform.GOOGLE_MAPS)
                        .telefone(telefone)
                        .nomeEmpresa(nomeEmpresa)
                        .endereco(endereco)
                        .website(website)
                        .rating(rating)
                        .reviews(reviews)
                        .especialidades(types)
                        .build();
                boolean candidate = isWhatsappCandidateBR(telefone);
                boolean reachable = candidate && isWhatsappReachable(telefone);
                if (reachable) {
                    whatsappCandidates.add(pr);
                } else {
                    others.add(pr);
                }
            }
        }
        results.addAll(whatsappCandidates);
        results.addAll(others);
        if (randomize) java.util.Collections.shuffle(results);
        return results;
    }

    private String buildQuery(String original) {
        String base = original != null && !original.isBlank() ? original.trim() : "";
        String filter = (businessType + " " + companySize).trim();
        String loc = location.trim();
        if (base.isBlank()) {
            if (!filter.isBlank() && !loc.isBlank()) {
                return filter + " in " + loc;
            } else if (!filter.isBlank()) {
                return filter;
            } else if (!loc.isBlank()) {
                return "near " + loc;
            }
            return "";
        } else {
            String q = base;
            if (!filter.isBlank()) {
                q += " " + filter;
            }
            if (!loc.isBlank()) {
                q += " in " + loc;
            }
            return q.trim();
        }
    }

    private String joinTypes(JsonNode typesArray) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < typesArray.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(typesArray.get(i).asText());
        }
        return sb.toString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private int computeStartOffset() {
        if (!randomize) return 0;
        int[] options = new int[] {0, 10, 20};
        int idx = (int)(System.currentTimeMillis() % options.length);
        return options[idx];
    }

    private String sanitizePhoneBR(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D+", "");
        if (digits.isBlank()) return null;
        if (digits.startsWith("55")) {
            return digits;
        }
        return "55" + digits;
    }

    private boolean isWhatsappCandidateBR(String e164) {
        if (e164 == null) return false;
        if (!e164.startsWith("55")) return false;
        String rest = e164.substring(2);
        if (rest.length() < 10) return false;
        String subscriber = rest.substring(rest.length() - 9);
        return subscriber.startsWith("9");
    }

    private boolean isWhatsappReachable(String e164) {
        if (reachabilityBaseUrl.isBlank()) {
            return true;
        }
        try {
            String url = reachabilityBaseUrl + "/validate?phone=" + URLEncoder.encode(e164, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                JsonNode root = mapper.readTree(res.body());
                JsonNode reachable = root.path("reachable");
                if (reachable.isBoolean()) {
                    return reachable.asBoolean();
                }
            }
        } catch (Exception ignored) {
        }
        return true;
    }

    private void geocodeIfNeeded() {
        if (location.isBlank()) return;
        if (geoLat != null && geoLon != null) return;
        try {
            String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" + URLEncoder.encode(location, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "prospectaai-sdk/1.0")
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                JsonNode arr = mapper.readTree(res.body());
                if (arr.isArray() && arr.size() > 0) {
                    JsonNode first = arr.get(0);
                    String lat = first.path("lat").asText(null);
                    String lon = first.path("lon").asText(null);
                    if (lat != null && lon != null) {
                        geoLat = lat;
                        geoLon = lon;
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String computeZoomParam() {
        if (geoLat == null || geoLon == null) return "";
        int z = 12;
        try {
            int r = radiusKmStr != null && !radiusKmStr.isBlank() ? Integer.parseInt(radiusKmStr) : -1;
            if (r > 0) {
                if (r <= 2) z = 15;
                else if (r <= 5) z = 14;
                else if (r <= 10) z = 13;
                else if (r <= 20) z = 12;
                else if (r <= 50) z = 11;
                else z = 10;
            }
        } catch (NumberFormatException ignored) {
        }
        return "&z=" + z;
    }
}
