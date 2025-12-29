package br.com.prospectaai.sdk.prospection;

import java.util.Map;

import br.com.prospectaai.shared.dto.async.AsyncTaskPlatform;

public final class ProspectorFactory {
    private ProspectorFactory() {}

    public static Prospector create(AsyncTaskPlatform platform, Map<String, String> config) {
        if (platform == AsyncTaskPlatform.GOOGLE_MAPS) {
            String apiKey = config != null ? config.getOrDefault("serpapi.apiKey", "") : "";
            return new GoogleMapsProspector(apiKey);
        }
        throw new IllegalArgumentException("Unsupported platform: " + platform);
    }
}
