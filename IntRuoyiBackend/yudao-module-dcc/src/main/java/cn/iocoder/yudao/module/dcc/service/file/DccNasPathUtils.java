package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

final class DccNasPathUtils {

    private DccNasPathUtils() {
    }

    static String normalizeRelativePath(String path) {
        String raw = StrUtil.nullToEmpty(path).replace('\\', '/');
        List<String> parts = new ArrayList<>();
        for (String part : raw.split("/")) {
            if (part.isBlank() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (!parts.isEmpty()) {
                    parts.remove(parts.size() - 1);
                }
                continue;
            }
            parts.add(part);
        }
        return String.join("/", parts);
    }

    static String pathHash(String nasShareName, String normalizedRelativePath) {
        String canonical = StrUtil.trimToEmpty(nasShareName).toLowerCase(Locale.ROOT)
                + "|"
                + normalizeRelativePath(normalizedRelativePath).toLowerCase(Locale.ROOT);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }
}
