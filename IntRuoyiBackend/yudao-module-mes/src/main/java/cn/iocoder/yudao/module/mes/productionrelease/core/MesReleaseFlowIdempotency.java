package cn.iocoder.yudao.module.mes.productionrelease.core;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public final class MesReleaseFlowIdempotency {

    private static final int MAX_KEY_LENGTH = 128;

    private MesReleaseFlowIdempotency() {
    }

    public static String requireKey(String key) {
        if (key == null || key.isEmpty() || key.length() > MAX_KEY_LENGTH
                || key.chars().anyMatch(character -> character < 0x21 || character > 0x7e)) {
            throw invalidKey();
        }
        return key;
    }

    public static String payloadHash(String... parts) {
        Objects.requireNonNull(parts, "parts must not be null");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String part : parts) {
                if (part == null) {
                    digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(-1).array());
                    continue;
                }
                byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static MesReleaseFlowBlockerException invalidKey() {
        return new MesReleaseFlowBlockerException("idempotencyKey must contain 1 to 128 visible ASCII characters",
                new MesReleaseFlowFailureRespVO()
                        .setBlockers(List.of(new MesReleaseFlowBlocker()
                                .setBlockerType(MesReleaseFlowBlockerType.IDEMPOTENCY_KEY_INVALID)
                                .setObjectType("IDEMPOTENCY_KEY")
                                .setReason("idempotencyKey is missing or contains unsupported characters")
                                .setSuggestion("provide a stable visible ASCII key with at most 128 characters"))));
    }
}
