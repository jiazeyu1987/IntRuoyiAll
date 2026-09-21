package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

public final class ReleaseWorkflowContract {

    public static final String PUBLISH_SCOPE = "app-release";
    public static final String MANIFEST_FILE_NAME = "manifest.json";

    private static final DateTimeFormatter RELEASE_TIME = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss", Locale.ROOT)
            .withZone(ZoneOffset.UTC);

    private ReleaseWorkflowContract() {
    }

    public static ReleaseWorkflowIdentity createIdentity(Instant createdAt, String uniqueToken,
                                                          String presetId, String presetVersion) {
        Objects.requireNonNull(createdAt, "createdAt");
        String normalizedToken = requireToken(uniqueToken);
        String normalizedPresetId = requireText(presetId, "presetId");
        String normalizedPresetVersion = requireText(presetVersion, "presetVersion");
        return new ReleaseWorkflowIdentity(
                "rw-" + normalizedToken,
                "release-" + RELEASE_TIME.format(createdAt) + "-" + normalizedToken + "-app",
                PUBLISH_SCOPE,
                normalizedPresetId,
                normalizedPresetVersion);
    }

    private static String requireToken(String value) {
        String token = requireText(value, "uniqueToken").toLowerCase(Locale.ROOT);
        if (!token.matches("[a-z0-9]{8,32}")) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_TOKEN_INVALID");
        }
        return token;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_" + name.toUpperCase(Locale.ROOT) + "_INVALID");
        }
        return value;
    }

    public record ReleaseWorkflowIdentity(String workflowId, String releaseTag, String publishScope,
                                          String presetId, String presetVersion) {
    }
}
