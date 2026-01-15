package br.com.prospectaai.sdk.prospection;

import java.util.Map;

import br.com.prospectaai.shared.dto.async.AsyncTaskPlatform;

public final class ProspectorFactory {
    private ProspectorFactory() {}

    public static Prospector create(AsyncTaskPlatform platform, Map<String, String> config) {
        if (platform == AsyncTaskPlatform.GOOGLE_MAPS) {
            return new GoogleMapsProspector(config != null ? config : java.util.Map.of());
        }
        throw new IllegalArgumentException("Unsupported platform: " + platform);
    }
}
