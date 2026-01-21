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
    private final String googlePlacesApiKey;
    private final String serperApiKey;
    private final String location;
    private final String businessType;
    private final String companySize;
    private final String languageHl;
    private final String glCountry;
    private final boolean randomize;
    private final String radiusKmStr;
    private final int baseMaxResults;
    private volatile String geoLat = null;
    private volatile String geoLon = null;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final java.util.Map<String, String[]> resultCoordsCache = new java.util.HashMap<>();

    GoogleMapsProspector(Map<String, String> config) {
        this.serpApiKey = config.getOrDefault("serpapi.apiKey", "");
        this.googlePlacesApiKey = nullToEmpty(config.getOrDefault("google.places.apiKey", ""));
        this.serperApiKey = nullToEmpty(config.getOrDefault("serper.apiKey", ""));
        this.location = nullToEmpty(config.get("location"));
        this.businessType = nullToEmpty(config.get("businessType"));
        this.companySize = nullToEmpty(config.get("companySize"));
        this.languageHl = nullToEmpty(config.getOrDefault("hl", "pt-BR"));
        this.glCountry = nullToEmpty(config.getOrDefault("gl", "br"));
        this.randomize = "true".equalsIgnoreCase(config.getOrDefault("randomize", "true"));
        this.radiusKmStr = nullToEmpty(config.get("radiusKm"));
        String mrStr = nullToEmpty(config.getOrDefault("maxResults", "60"));
        int mr = 60;
        try { mr = Integer.parseInt(mrStr); } catch (NumberFormatException ignored) {}
        this.baseMaxResults = Math.max(20, mr);
        String cfgLat = nullToEmpty(config.get("lat"));
        String cfgLon = nullToEmpty(config.get("lon"));
        if (!cfgLat.isBlank() && !cfgLon.isBlank()) {
            this.geoLat = cfgLat;
            this.geoLon = cfgLon;
        }
    }

    @Override
    public List<ProspectionResult> prospect(String query) throws Exception {
        String finalQuery = buildQuery(query);
        geocodeIfNeeded();
        System.out.println("[prospection-sdk] prospect start q=" + finalQuery + " bt=" + businessType + " loc=" + location + " lat=" + geoLat + " lon=" + geoLon);
        List<ProspectionResult> serpResults = new java.util.ArrayList<>();
        if (!serpApiKey.isBlank() && !"null".equalsIgnoreCase(serpApiKey.trim())) {
            System.out.println("[prospection-sdk] using provider=serpapi");
            serpResults = serpApiSearch(finalQuery);
            System.out.println("[prospection-sdk] serpapi results size=" + (serpResults != null ? serpResults.size() : 0));
        }
        if ((serpResults == null || serpResults.isEmpty()) && !googlePlacesApiKey.isBlank()) {
            System.out.println("[prospection-sdk] fallback to provider=google_places");
            List<ProspectionResult> r = googlePlacesSearch(finalQuery);
            r = mixByDistance(r);
            r = enforceRadiusOnSelection(r);
            return r;
        }
        if (serpResults == null || serpResults.isEmpty()) {
            System.out.println("[prospection-sdk] fallback to provider=serper places needed=" + (20 - (serpResults != null ? serpResults.size() : 0)));
            List<ProspectionResult> serperResults = serperPlacesSearch(finalQuery, 20 - (serpResults != null ? serpResults.size() : 0));
            System.out.println("[prospection-sdk] serper results size=" + (serperResults != null ? serperResults.size() : 0));
            if (serpResults == null || serpResults.isEmpty()) return serperResults;
            serpResults.addAll(serperResults);
        }
        serpResults = mixByDistance(serpResults);
        serpResults = enforceRadiusOnSelection(serpResults);
        System.out.println("[prospection-sdk] prospect end total=" + (serpResults != null ? serpResults.size() : 0));
        return serpResults;
    }

    private List<ProspectionResult> serpApiSearch(String finalQuery) throws Exception {
        List<ProspectionResult> results = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        java.util.List<ProspectionResult> whatsappCandidates = new java.util.ArrayList<>();
        java.util.List<ProspectionResult> others = new java.util.ArrayList<>();
        String[] queries = buildSerpQueries(finalQuery);
        int[] starts = new int[] {0, 10, 20};
        int zoom = computeZoom();
        int mapHeightMeters = computeMapHeightMeters();
        String llParam = (geoLat != null && geoLon != null)
                ? ("&ll=@" + geoLat + "," + geoLon + "," + (mapHeightMeters > 0 ? (mapHeightMeters + "m") : (zoom + "z")))
                : "";
        for (String q : queries) {
            if (q == null || q.isBlank()) continue;
            String encoded = URLEncoder.encode(q, StandardCharsets.UTF_8);
            for (int start : starts) {
                String nearbyParam = (!llParam.isBlank() && !queryHasLocation(q)) ? "&nearby=true" : "";
                String url = "https://serpapi.com/search?engine=google_maps"
                        + "&q=" + encoded
                        + "&type=search"
                        + "&hl=" + URLEncoder.encode(languageHl, StandardCharsets.UTF_8)
                        + "&gl=" + URLEncoder.encode(glCountry, StandardCharsets.UTF_8)
                        + "&google_domain=" + (glCountry.equalsIgnoreCase("br") ? "google.com.br" : "google.com")
                        + "&start=" + start
                        + llParam
                        + nearbyParam
                        + "&api_key=" + serpApiKey;
                System.out.println("[prospection-sdk] serpapi request q=" + q + " start=" + start + " ll=" + (geoLat != null && geoLon != null));
                HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println("[prospection-sdk] serpapi status=" + res.statusCode());
                JsonNode root = mapper.readTree(res.body());
                JsonNode localResults = root.path("local_results");
                if (localResults.isArray()) {
                    System.out.println("[prospection-sdk] serpapi local_results count=" + localResults.size());
                    for (JsonNode item : localResults) {
                        String rawPhone = item.path("phone").asText(null);
                        String telefone = sanitizePhoneBR(rawPhone);
                        String nomeEmpresa = item.path("title").asText(null);
                        String endereco = sanitizeAddress(item.path("address").asText(null));
                        String ilat = item.path("gps_coordinates").path("latitude").asText(null);
                        String ilon = item.path("gps_coordinates").path("longitude").asText(null);
                        if ((ilat == null || ilon == null) && (item.has("latitude") && item.has("longitude"))) {
                            ilat = item.path("latitude").asText(null);
                            ilon = item.path("longitude").asText(null);
                        }
                        if (!withinRadius(ilat, ilon)) {
                            continue;
                        }
                        String website = item.path("website").asText(null);
                        String rating = item.path("rating").asText(null);
                        String reviews = item.path("reviews").asText(null);
                        String types = item.path("types").isArray() ? joinTypes(item.path("types")) : item.path("types").asText(null);
                        if (shouldFilterBySize(website)) {
                            continue;
                        }
                        if (!isValidBusiness(telefone, website, rating, nomeEmpresa, types)) {
                            continue;
                        }
                        if (!matchesBusinessFilter(nomeEmpresa, types)) {
                            continue;
                        }
                        String key = (telefone != null && !telefone.isBlank())
                                ? telefone
                                : ((nomeEmpresa != null ? nomeEmpresa : "") + "|" + (endereco != null ? endereco : "")).toLowerCase();
                        if (ilat != null && ilon != null) {
                            resultCoordsCache.put(key, new String[] {ilat, ilon});
                        }
                        if (seen.contains(key)) continue;
                        seen.add(key);
                        ProspectionResult pr = ProspectionResult.builder()
                                .query(q)
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
                        if (candidate) {
                            whatsappCandidates.add(pr);
                        } else {
                            others.add(pr);
                        }
                        if ((whatsappCandidates.size() + others.size()) >= getMaxResults()) break;
                    }
                }
                if ((whatsappCandidates.size() + others.size()) >= getMaxResults()) break;
            }
            if ((whatsappCandidates.size() + others.size()) >= getMaxResults()) break;
        }
        results.addAll(whatsappCandidates);
        results.addAll(others);
        if (randomize) java.util.Collections.shuffle(results);
        System.out.println("[prospection-sdk] serpapi merged size=" + results.size());
        return results;
    }

    private List<ProspectionResult> enforceRadiusOnSelection(List<ProspectionResult> results) {
        if (results == null || results.isEmpty()) return results;
        int r;
        try { r = radiusKmStr != null && !radiusKmStr.isBlank() ? Integer.parseInt(radiusKmStr) : -1; } catch (NumberFormatException e) { r = -1; }
        if (r <= 0) return results;
        if (geoLat == null || geoLon == null) return results;
        double clat = Double.parseDouble(geoLat);
        double clon = Double.parseDouble(geoLon);
        java.util.List<ProspectionResult> kept = new java.util.ArrayList<>(results.size());
        for (ProspectionResult pr : results) {
            String phone = pr.getTelefone();
            String nome = pr.getNomeEmpresa();
            String addr = pr.getEndereco();
            String key = (phone != null && !phone.isBlank())
                    ? phone
                    : ((nome != null ? nome : "") + "|" + (addr != null ? addr : "")).toLowerCase();
            String[] coords = resultCoordsCache.get(key);
            if (coords == null) continue;
            if (coords == null) continue;
            try {
                double ilat = Double.parseDouble(coords[0]);
                double ilon = Double.parseDouble(coords[1]);
                double d = haversineKm(clat, clon, ilat, ilon);
                if (d <= r) kept.add(pr);
            } catch (Exception ignored) {
            }
        }
        return kept.isEmpty() ? results : kept;
    }
    
    private int getMaxResults() {
        int scaled = 0;
        try {
            int rk = radiusKmStr != null && !radiusKmStr.isBlank() ? Integer.parseInt(radiusKmStr) : -1;
            if (rk > 0) {
                scaled = Math.min(150, Math.max(40, rk * 3));
            }
        } catch (NumberFormatException ignored) {}
        return Math.max(baseMaxResults, scaled);
    }
    private String[] buildSerpQueries(String finalQuery) {
        String bt = businessType != null ? businessType.trim() : "";
        String loc = location != null ? location.trim() : "";
        boolean coordsLoc = looksLikeCoordinates(loc);
        String en = translateBusinessType(bt);
        String q1 = finalQuery;
        String q2 = (!bt.isBlank() && !loc.isBlank() && !coordsLoc) ? (bt + " em " + loc) : bt;
        String q3 = (!bt.isBlank() && !loc.isBlank() && !coordsLoc) ? (bt + " in " + loc) : bt;
        String q4 = (!bt.isBlank() && !loc.isBlank()) ? (bt + " " + loc) : bt;
        String q5 = (!en.isBlank() && !loc.isBlank() && !coordsLoc) ? (en + " in " + loc) : en;
        return new String[] {q1, q2, q3, q4, q5};
    }

    private List<ProspectionResult> serperPlacesSearch(String finalQuery, int minNeeded) throws Exception {
        List<ProspectionResult> results = new ArrayList<>();
        if (serperApiKey.isBlank()) return results;
        java.util.Set<String> seen = new java.util.HashSet<>();
        java.util.List<ProspectionResult> whatsappCandidates = new java.util.ArrayList<>();
        java.util.List<ProspectionResult> others = new java.util.ArrayList<>();
        int page = 1;
        while (results.size() < Math.max(20, minNeeded)) {
            com.fasterxml.jackson.databind.node.ObjectNode body = mapper.createObjectNode();
            String q = finalQuery;
            if (q == null || q.isBlank()) {
                String loc = location != null ? location.trim() : "";
                boolean coordsLoc = looksLikeCoordinates(loc);
                if (!businessType.isBlank() && !loc.isBlank() && !coordsLoc) {
                    q = businessType + " em " + loc;
                } else if (!businessType.isBlank()) {
                    q = businessType;
                } else {
                    q = "empresas";
                }
            }
            System.out.println("[prospection-sdk] serper request q=" + q + " page=" + page + " gl=" + glCountry + " hl=" + languageHl + " loc=" + location);
            body.put("q", q);
            if (!glCountry.isBlank()) body.put("gl", glCountry);
            if (!languageHl.isBlank()) body.put("hl", languageHl);
            if (!location.isBlank()) body.put("location", location);
            body.put("autocorrect", true);
            body.put("page", page);
            body.put("type", "search");
            String reqBody = mapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://google.serper.dev/places"))
                    .header("Content-Type", "application/json")
                    .header("X-API-KEY", serperApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(reqBody))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.println("[prospection-sdk] serper status=" + res.statusCode());
            JsonNode root = mapper.readTree(res.body());
            JsonNode places = root.path("places").isMissingNode() ? root.path("local_results") : root.path("places");
            if (places.isArray()) {
                System.out.println("[prospection-sdk] serper places count=" + places.size());
                for (JsonNode item : places) {
                    String nomeEmpresa = item.path("title").asText(null);
                    String endereco = sanitizeAddress(item.path("address").asText(null));
                    String ilat = item.path("gps_coordinates").path("latitude").asText(null);
                    String ilon = item.path("gps_coordinates").path("longitude").asText(null);
                    if ((ilat == null || ilon == null) && (item.has("latitude") && item.has("longitude"))) {
                        ilat = item.path("latitude").asText(null);
                        ilon = item.path("longitude").asText(null);
                    }
                    if ((ilat == null || ilon == null) && endereco != null && !endereco.isBlank()) {
                        String[] coords = geocodeAddress(endereco);
                        if (coords != null) {
                            ilat = coords[0];
                            ilon = coords[1];
                        }
                    }
                    if (!withinRadius(ilat, ilon)) {
                        continue;
                    }
                    String rating = item.path("rating").isNumber() ? item.path("rating").asText() : item.path("rating").asText(null);
                    String reviews = item.path("reviews").isNumber() ? item.path("reviews").asText() : item.path("reviews").asText(null);
                    String telefone = sanitizePhoneBR(item.path("phone").asText(null));
                    String website = item.path("website").asText(null);
                    String types = item.path("type").asText(null);
                    if (shouldFilterBySize(website)) {
                        continue;
                    }
                    if (!matchesBusinessFilter(nomeEmpresa, types)) {
                        continue;
                    }
                    if (!isValidBusiness(telefone, website, rating, nomeEmpresa, types)) {
                        continue;
                    }
                    String key = (telefone != null && !telefone.isBlank())
                            ? telefone
                            : ((nomeEmpresa != null ? nomeEmpresa : "") + "|" + (endereco != null ? endereco : "")).toLowerCase();
                    if (seen.contains(key)) continue;
                    seen.add(key);
                    ProspectionResult pr = ProspectionResult.builder()
                            .query(q)
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
                    if (candidate) {
                        whatsappCandidates.add(pr);
                    } else {
                        others.add(pr);
                    }
                }
            }
            if ((!whatsappCandidates.isEmpty() || !others.isEmpty()) && results.size() >= 20) {
                break;
            }
            page++;
            if (page > 5) break;
        }
        results.addAll(whatsappCandidates);
        results.addAll(others);
        if (randomize) java.util.Collections.shuffle(results);
        if (results.size() > 20) {
            System.out.println("[prospection-sdk] serper merged size=" + results.size() + " trunc=20");
            return results.subList(0, 20);
        }
        System.out.println("[prospection-sdk] serper merged size=" + results.size());
        return results;
    }

    private String translateBusinessType(String bt) {
        String s = bt.toLowerCase();
        if (s.contains("imobili")) return "real estate agency";
        if (s.contains("restaur")) return "restaurant";
        if (s.contains("academ")) return "gym";
        if (s.contains("clín") || s.contains("clin")) return "clinic";
        return bt;
    }

    private List<ProspectionResult> googlePlacesSearch(String finalQuery) throws Exception {
        int r = -1;
        try {
            r = radiusKmStr != null && !radiusKmStr.isBlank() ? Integer.parseInt(radiusKmStr) : -1;
        } catch (NumberFormatException ignored) {}
        String url;
        if (geoLat != null && geoLon != null && r > 0) {
            int radiusMeters = r * 1000;
            String kw = URLEncoder.encode(businessType, StandardCharsets.UTF_8);
            String type = URLEncoder.encode(mapPlaceType(businessType), StandardCharsets.UTF_8);
            url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + geoLat + "," + geoLon
                    + "&radius=" + radiusMeters
                    + "&keyword=" + kw
                    + (type.isBlank() ? "" : "&type=" + type)
                    + "&language=" + URLEncoder.encode(languageHl, StandardCharsets.UTF_8)
                    + "&key=" + googlePlacesApiKey;
            System.out.println("[prospection-sdk] places request type=nearby lat=" + geoLat + " lon=" + geoLon + " radius=" + r + " kw=" + businessType);
        } else {
            String q = finalQuery.isBlank() ? (businessType + " in " + location).trim() : finalQuery;
            String qs = URLEncoder.encode(q, StandardCharsets.UTF_8);
            String locBias = "";
            int radiusMeters = -1;
            try { radiusMeters = (radiusKmStr != null && !radiusKmStr.isBlank()) ? Integer.parseInt(radiusKmStr) * 1000 : -1; } catch (NumberFormatException ignored) {}
            if (geoLat != null && geoLon != null && radiusMeters > 0) {
                locBias = "&location=" + geoLat + "," + geoLon + "&radius=" + radiusMeters;
            }
            url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=" + qs + locBias
                    + "&language=" + URLEncoder.encode(languageHl, StandardCharsets.UTF_8)
                    + "&region=" + URLEncoder.encode(glCountry, StandardCharsets.UTF_8)
                    + "&key=" + googlePlacesApiKey;
            System.out.println("[prospection-sdk] places request type=text q=" + q + " region=" + glCountry);
        }
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("[prospection-sdk] places status=" + res.statusCode());
        JsonNode root = mapper.readTree(res.body());
        JsonNode resultsNode = root.path("results");
        List<ProspectionResult> results = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        java.util.List<ProspectionResult> whatsappCandidates = new java.util.ArrayList<>();
        java.util.List<ProspectionResult> others = new java.util.ArrayList<>();
        if (resultsNode.isArray()) {
            System.out.println("[prospection-sdk] places results count=" + resultsNode.size());
            for (JsonNode item : resultsNode) {
                String placeId = item.path("place_id").asText(null);
                String nomeEmpresa = item.path("name").asText(null);
                String ilat = item.path("geometry").path("location").path("lat").isNumber() ? item.path("geometry").path("location").path("lat").asText() : null;
                String ilon = item.path("geometry").path("location").path("lng").isNumber() ? item.path("geometry").path("location").path("lng").asText() : null;
                if (!withinRadius(ilat, ilon)) {
                    continue;
                }
                String endereco = sanitizeAddress(item.path("formatted_address").asText(item.path("vicinity").asText(null)));
                String types = item.path("types").isArray() ? joinTypes(item.path("types")) : item.path("types").asText(null);
                String rating = item.path("rating").isNumber() ? item.path("rating").asText() : null;
                String reviews = item.path("user_ratings_total").isNumber() ? item.path("user_ratings_total").asText() : null;
                String telefone = null;
                String website = null;
                if (placeId != null) {
                    String durl = "https://maps.googleapis.com/maps/api/place/details/json?place_id=" + URLEncoder.encode(placeId, StandardCharsets.UTF_8)
                            + "&fields=name,formatted_address,website,international_phone_number,types,rating,user_ratings_total"
                            + "&language=" + URLEncoder.encode(languageHl, StandardCharsets.UTF_8)
                            + "&key=" + googlePlacesApiKey;
                    HttpRequest dreq = HttpRequest.newBuilder(URI.create(durl)).GET().build();
                    HttpResponse<String> dres = http.send(dreq, HttpResponse.BodyHandlers.ofString());
                    System.out.println("[prospection-sdk] places details status=" + dres.statusCode());
                    JsonNode droot = mapper.readTree(dres.body()).path("result");
                    if (!droot.isMissingNode()) {
                        telefone = sanitizePhoneBR(droot.path("international_phone_number").asText(null));
                        website = droot.path("website").asText(null);
                        if (rating == null && droot.path("rating").isNumber()) rating = droot.path("rating").asText();
                        if (reviews == null && droot.path("user_ratings_total").isNumber()) reviews = droot.path("user_ratings_total").asText();
                        if (types == null && droot.path("types").isArray()) types = joinTypes(droot.path("types"));
                        if (endereco == null) endereco = sanitizeAddress(droot.path("formatted_address").asText(null));
                        if (nomeEmpresa == null) nomeEmpresa = droot.path("name").asText(null);
                    }
                }
                if (shouldFilterBySize(website)) {
                    continue;
                }
                if (!isValidBusiness(telefone, website, rating, nomeEmpresa, types)) {
                    continue;
                }
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
                if (candidate) {
                    whatsappCandidates.add(pr);
                } else {
                    others.add(pr);
                }
            }
        }
        results.addAll(whatsappCandidates);
        results.addAll(others);
        if (randomize) java.util.Collections.shuffle(results);
        System.out.println("[prospection-sdk] places merged size=" + results.size());
        return results;
    }
    private boolean shouldFilterBySize(String website) {
        if (companySize == null || companySize.isBlank()) return false;
        String lower = companySize.toLowerCase();
        // Check if user asked for Medium or Large
        boolean mediumOrLarge = lower.contains("méd") || lower.contains("med") || lower.contains("gran") || lower.contains("50");
        
        if (mediumOrLarge) {
            // If medium/large is requested, we require a website.
            // If website is null or blank, we filter it out (return true).
            return website == null || website.isBlank();
        }
        return false;
    }

    private String buildQuery(String original) {
        String base = original != null && !original.isBlank() ? original.trim() : "";
        String filter = businessType.trim();
        String loc = location.trim();
        boolean coordsLoc = looksLikeCoordinates(loc);
        if (base.isBlank()) {
            if (!filter.isBlank() && !loc.isBlank() && !coordsLoc) {
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
            if (!loc.isBlank() && !coordsLoc) {
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


    private void geocodeIfNeeded() {
        if (location.isBlank()) return;
        if (geoLat != null && geoLon != null) return;
        try {
            String loc = location.trim();
            if (looksLikeCoordinates(loc)) {
                String[] parts = loc.split(",");
                if (parts.length == 2) {
                    String lat = parts[0].trim();
                    String lon = parts[1].trim();
                    if (!lat.isEmpty() && !lon.isEmpty()) {
                        geoLat = lat;
                        geoLon = lon;
                        return;
                    }
                }
            }
            if (!googlePlacesApiKey.isBlank()) {
                String gurl = "https://maps.googleapis.com/maps/api/geocode/json?address=" + URLEncoder.encode(location, StandardCharsets.UTF_8)
                        + "&language=" + URLEncoder.encode(languageHl, StandardCharsets.UTF_8)
                        + "&key=" + googlePlacesApiKey;
                HttpRequest greq = HttpRequest.newBuilder(URI.create(gurl)).GET().build();
                HttpResponse<String> gres = http.send(greq, HttpResponse.BodyHandlers.ofString());
                JsonNode groot = mapper.readTree(gres.body());
                JsonNode results = groot.path("results");
                if (results.isArray() && results.size() > 0) {
                    JsonNode geom = results.get(0).path("geometry").path("location");
                    if (geom.has("lat") && geom.has("lng")) {
                        geoLat = geom.path("lat").asText();
                        geoLon = geom.path("lng").asText();
                        return;
                    }
                }
            }
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

    private boolean looksLikeCoordinates(String loc) {
        if (loc == null || loc.isBlank()) return false;
        String s = loc.trim();
        if (!s.contains(",")) return false;
        String[] parts = s.split(",");
        if (parts.length != 2) return false;
        try {
            Double.parseDouble(parts[0].trim());
            Double.parseDouble(parts[1].trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int computeZoom() {
        if (geoLat == null || geoLon == null) return 12;
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
        return z;
    }

    private int computeMapHeightMeters() {
        try {
            int r = radiusKmStr != null && !radiusKmStr.isBlank() ? Integer.parseInt(radiusKmStr) : -1;
            if (r > 0) return r * 1000;
        } catch (NumberFormatException ignored) {}
        return -1;
    }

    private String sanitizeAddress(String address) {
        if (address == null) return null;
        String s = address.trim();
        s = s.replaceAll("@-?\\d+\\.\\d+,-?\\d+\\.\\d+,\\d+z", "");
        s = s.replaceAll("\\(.*?\\d+\\.\\d+.*?\\)", "");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }
    
    private boolean queryHasLocation(String q) {
        if (q == null) return false;
        String s = q.toLowerCase();
        if (!location.isBlank() && s.contains(location.toLowerCase())) return true;
        return s.contains(" in ") || s.contains(" em ");
    }
    
    private final java.util.Map<String, String[]> geocodeCache = new java.util.HashMap<>();

    private double distanceKmForResult(ProspectionResult r) {
        try {
            if (geoLat == null || geoLon == null) return -1;
            String phone = r.getTelefone();
            String nome = r.getNomeEmpresa();
            String addr = r.getEndereco();
            String key = (phone != null && !phone.isBlank())
                    ? phone
                    : ((nome != null ? nome : "") + "|" + (addr != null ? addr : "")).toLowerCase();
            String[] coords = resultCoordsCache.get(key);
            if (coords == null) return -1;
            double clat = Double.parseDouble(geoLat);
            double clon = Double.parseDouble(geoLon);
            double ilat = Double.parseDouble(coords[0]);
            double ilon = Double.parseDouble(coords[1]);
            return haversineKm(clat, clon, ilat, ilon);
        } catch (Exception e) {
            return -1;
        }
    }

    private List<ProspectionResult> mixByDistance(List<ProspectionResult> results) {
        if (results == null || results.size() <= 1) return results;
        int rk = -1;
        try { rk = radiusKmStr != null && !radiusKmStr.isBlank() ? Integer.parseInt(radiusKmStr) : -1; } catch (NumberFormatException ignored) {}
        if (rk <= 0 || geoLat == null || geoLon == null) {
            java.util.Collections.shuffle(results);
            return results;
        }
        double b1 = rk * 0.25;
        double b2 = rk * 0.50;
        double b3 = rk * 0.75;
        java.util.List<ProspectionResult> bucket1 = new java.util.ArrayList<>();
        java.util.List<ProspectionResult> bucket2 = new java.util.ArrayList<>();
        java.util.List<ProspectionResult> bucket3 = new java.util.ArrayList<>();
        java.util.List<ProspectionResult> bucket4 = new java.util.ArrayList<>();
        for (ProspectionResult r : results) {
            double d = distanceKmForResult(r);
            if (d < 0) {
                bucket3.add(r);
            } else if (d <= b1) {
                bucket1.add(r);
            } else if (d <= b2) {
                bucket2.add(r);
            } else if (d <= b3) {
                bucket3.add(r);
            } else if (d <= rk) {
                bucket4.add(r);
            }
        }
        java.util.Collections.shuffle(bucket1);
        java.util.Collections.shuffle(bucket2);
        java.util.Collections.shuffle(bucket3);
        java.util.Collections.shuffle(bucket4);
        int target = results.size();
        double p1, p2, p3, p4;
        if (rk >= 50) {
            p1 = 0.05; p2 = 0.10; p3 = 0.20; p4 = 0.65;
        } else if (rk >= 30) {
            p1 = 0.10; p2 = 0.15; p3 = 0.20; p4 = 0.55;
        } else {
            p1 = 0.20; p2 = 0.25; p3 = 0.25; p4 = 0.30;
        }
        int q1 = Math.min(bucket1.size(), (int)Math.round(target * p1));
        int q2 = Math.min(bucket2.size(), (int)Math.round(target * p2));
        int q3 = Math.min(bucket3.size(), (int)Math.round(target * p3));
        int q4 = Math.min(bucket4.size(), (int)Math.round(target * p4));
        java.util.List<ProspectionResult> mixed = new java.util.ArrayList<>(target);
        for (int i = 0; i < q1; i++) mixed.add(bucket1.get(i));
        for (int i = 0; i < q2; i++) mixed.add(bucket2.get(i));
        for (int i = 0; i < q3; i++) mixed.add(bucket3.get(i));
        for (int i = 0; i < q4; i++) mixed.add(bucket4.get(i));
        int needed = target - mixed.size();
        if (needed > 0) {
            java.util.List<ProspectionResult> pool = new java.util.ArrayList<>();
            if (bucket4.size() > q4) pool.addAll(bucket4.subList(q4, bucket4.size()));
            if (bucket3.size() > q3) pool.addAll(bucket3.subList(q3, bucket3.size()));
            if (bucket2.size() > q2) pool.addAll(bucket2.subList(q2, bucket2.size()));
            if (bucket1.size() > q1) pool.addAll(bucket1.subList(q1, bucket1.size()));
            java.util.Collections.shuffle(pool);
            for (ProspectionResult r : pool) {
                if (mixed.size() >= target) break;
                if (!mixed.contains(r)) mixed.add(r);
            }
        }
        java.util.Collections.shuffle(mixed);
        if (mixed.size() < results.size()) {
            for (ProspectionResult r : results) {
                if (mixed.size() >= target) break;
                if (!mixed.contains(r)) mixed.add(r);
            }
        }
        return mixed;
    }
    
    private String[] geocodeAddress(String address) {
        try {
            String key = address.trim().toLowerCase();
            if (geocodeCache.containsKey(key)) {
                return geocodeCache.get(key);
            }
            if (!googlePlacesApiKey.isBlank()) {
                String gurl = "https://maps.googleapis.com/maps/api/geocode/json?address=" + URLEncoder.encode(address, StandardCharsets.UTF_8)
                        + "&language=" + URLEncoder.encode(languageHl, StandardCharsets.UTF_8)
                        + "&key=" + googlePlacesApiKey;
                HttpRequest greq = HttpRequest.newBuilder(URI.create(gurl)).GET().build();
                HttpResponse<String> gres = http.send(greq, HttpResponse.BodyHandlers.ofString());
                JsonNode groot = mapper.readTree(gres.body());
                JsonNode results = groot.path("results");
                if (results.isArray() && results.size() > 0) {
                    JsonNode geom = results.get(0).path("geometry").path("location");
                    if (geom.has("lat") && geom.has("lng")) {
                        String lat = geom.path("lat").asText();
                        String lon = geom.path("lng").asText();
                        String[] coords = new String[] {lat, lon};
                        geocodeCache.put(key, coords);
                        return coords;
                    }
                }
            }
            String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" + URLEncoder.encode(address, StandardCharsets.UTF_8);
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
                        String[] coords = new String[] {lat, lon};
                        geocodeCache.put(key, coords);
                        return coords;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
    
    private boolean isValidBusiness(String telefone, String website, String rating, String nomeEmpresa, String types) {
        if (nomeEmpresa == null || nomeEmpresa.isBlank()) return false;
        boolean hasSignal = (telefone != null && !telefone.isBlank())
                         || (website != null && !website.isBlank())
                         || (rating != null && !rating.isBlank());
        if (!hasSignal) return false;
        if (types != null) {
            String t = types.toLowerCase();
            if (t.contains("street_address") || t.contains("route") || t.contains("political")
                || t.contains("locality") || t.contains("premise") || t.contains("sublocality")
                || t.contains("neighborhood") || t.contains("plus_code")) {
                return false;
            }
        }
        return true;
    }
    
    private boolean withinRadius(String latStr, String lonStr) {
        try {
            int r = radiusKmStr != null && !radiusKmStr.isBlank() ? Integer.parseInt(radiusKmStr) : -1;
            if (r <= 0) return true;
            if (geoLat == null || geoLon == null) return true;
            if (latStr == null || lonStr == null) return true;
            double clat = Double.parseDouble(geoLat);
            double clon = Double.parseDouble(geoLon);
            double ilat = Double.parseDouble(latStr);
            double ilon = Double.parseDouble(lonStr);
            double d = haversineKm(clat, clon, ilat, ilon);
            return d <= r;
        } catch (Exception ignored) {
            return true;
        }
    }
    
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
    
    private boolean matchesBusinessFilter(String nomeEmpresa, String types) {
        String bt = businessType != null ? businessType.trim().toLowerCase() : "";
        if (bt.isBlank()) return true;
        String normTitle = simpleNormalize(nomeEmpresa);
        String normTypes = simpleNormalize(types);
        if (bt.contains("japon")) {
            String[] kws = new String[] {"japones", "japonesa", "japonês", "japonêses", "japanese", "sushi", "temaki", "ramen", "yakisoba"};
            for (String k : kws) {
                if ((normTitle != null && normTitle.contains(k)) || (normTypes != null && normTypes.contains(k))) {
                    return true;
                }
            }
            if (normTypes != null && (normTypes.contains("japanese_restaurant") || normTypes.contains("sushi_restaurant"))) {
                return true;
            }
            return false;
        }
        return true;
    }
    
    private String simpleNormalize(String s) {
        if (s == null) return null;
        String n = s.toLowerCase();
        n = n.replace('_', ' ');
        return n;
    }
    
    private String mapPlaceType(String bt) {
        if (bt == null) return "";
        String s = bt.toLowerCase();
        if (s.contains("restaur")) return "restaurant";
        if (s.contains("imobili")) return "real_estate_agency";
        if (s.contains("academ")) return "gym";
        if (s.contains("clín") || s.contains("clin")) return "doctor";
        if (s.contains("farm")) return "pharmacy";
        return "";
    }
}
