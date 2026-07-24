package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class ShowroomPublicReleaseReadbackVerifier {

    private final String publicWebsiteOrigin;
    private final HttpClient httpClient;

    @Autowired
    public ShowroomPublicReleaseReadbackVerifier(
            @Value("${showroom.release.public-website-origin:}") String publicWebsiteOrigin) {
        this(publicWebsiteOrigin, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    ShowroomPublicReleaseReadbackVerifier(String publicWebsiteOrigin, HttpClient httpClient) {
        this.publicWebsiteOrigin = publicWebsiteOrigin;
        this.httpClient = httpClient;
    }

    public void verify(String siteKey, String stage, String releaseId, String manifestHash, String rootDocumentId) {
        String origin = requireConfiguredOrigin();
        String scopePath = "/showroom/sites/" + pathSegment(siteKey) + "/stages/" + pathSegment(stage);

        Map<String, Object> current = fetchJson(origin + scopePath + "/release/current", "current release");
        requireFieldEquals(current, "releaseId", releaseId, "current release");
        requireFieldEquals(current, "manifestHash", manifestHash, "current release");

        Map<String, Object> manifest = fetchJson(origin + scopePath + "/release/" + pathSegment(releaseId)
                + "/manifest", "release manifest");
        requireFieldEquals(manifest, "releaseId", releaseId, "release manifest");
        requireFieldEquals(manifest, "manifestHash", manifestHash, "release manifest");

        Map<String, Object> rootDocument = fetchJson(origin + scopePath + "/release/" + pathSegment(releaseId)
                + "/documents/" + pathSegment(rootDocumentId) + ".json", "root document");
        requireFieldEquals(rootDocument, "releaseId", releaseId, "root document");
        requireFieldEquals(rootDocument, "documentId", rootDocumentId, "root document");
    }

    private String requireConfiguredOrigin() {
        if (publicWebsiteOrigin == null || publicWebsiteOrigin.isBlank()) {
            throw new IllegalStateException(
                    "SHOWROOM_RELEASE_PUBLIC_READBACK_CONFIG_MISSING: showroom.release.public-website-origin is required");
        }
        return publicWebsiteOrigin.trim().replaceAll("/+$", "");
    }

    private Map<String, Object> fetchJson(String url, String purpose) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .header("Cache-Control", "no-cache")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException("HTTP " + response.statusCode());
            }
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (!contentType.toLowerCase(Locale.ROOT).contains("application/json")) {
                throw new IllegalStateException("expected application/json but received " + contentType);
            }
            Map<String, Object> payload = JsonUtils.parseObject(response.body(), Map.class);
            if (payload == null || payload.isEmpty()) {
                throw new IllegalStateException("empty JSON payload");
            }
            return payload;
        } catch (RuntimeException exception) {
            throw new IllegalStateException("SHOWROOM_RELEASE_PUBLIC_READBACK_FAILED: Website " + purpose
                    + " readback failed for " + url + ": " + exception.getMessage(), exception);
        } catch (Exception exception) {
            throw new IllegalStateException("SHOWROOM_RELEASE_PUBLIC_READBACK_FAILED: Website " + purpose
                    + " readback failed for " + url + ": " + exception.getMessage(), exception);
        }
    }

    private static void requireFieldEquals(Map<String, Object> payload, String fieldName, String expectedValue,
                                           String purpose) {
        String actualValue = Objects.toString(payload.get(fieldName), "");
        if (!Objects.equals(expectedValue, actualValue)) {
            throw new IllegalStateException("SHOWROOM_RELEASE_PUBLIC_READBACK_FAILED: Website " + purpose
                    + " returned " + fieldName + "=" + actualValue + " but expected " + expectedValue);
        }
    }

    private static String pathSegment(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("SHOWROOM_RELEASE_PUBLIC_READBACK_FAILED: path segment is required");
        }
        return URLEncoder.encode(value.trim(), StandardCharsets.UTF_8).replace("+", "%20");
    }
}
