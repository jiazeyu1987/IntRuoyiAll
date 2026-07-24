package cn.iocoder.yudao.module.dcc.signature.service.retention;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public record SignatureGovernanceRetentionStoredObject(
        String objectKey,
        String versionId,
        String retentionMode,
        Instant retainUntil,
        byte[] content,
        Map<String, String> metadata) {

    public SignatureGovernanceRetentionStoredObject {
        content = content == null ? new byte[0] : content.clone();
        metadata = normalizeMetadata(metadata);
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    public String metadataValue(String key) {
        if (key == null) {
            return null;
        }
        String normalizedKey = normalizeMetadataKey(key);
        String value = metadata.get(normalizedKey);
        if (value != null) {
            return value;
        }
        value = metadata.get("sg" + normalizedKey);
        if (value != null) {
            return value;
        }
        return switch (normalizedKey) {
            case "evidencehash" -> firstMetadataValue("sgdccevidencehash", "sgdomainhash");
            case "archivesha256" -> firstMetadataValue("sgarchivehash", "sgdomainhash");
            case "backupid" -> firstMetadataValue("sgrecoverybackupid");
            case "recoveryruntime" -> firstMetadataValue("sgrecoveryruntime");
            default -> null;
        };
    }

    private String firstMetadataValue(String... keys) {
        for (String key : keys) {
            String value = metadata.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Map<String, String> normalizeMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return metadata.entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.toUnmodifiableMap(
                        entry -> normalizeMetadataKey(entry.getKey()),
                        Map.Entry::getValue,
                        (left, right) -> right));
    }

    private static String normalizeMetadataKey(String key) {
        return key.replace("-", "").toLowerCase(Locale.ROOT);
    }
}
