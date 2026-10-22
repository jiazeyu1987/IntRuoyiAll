package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ReleaseDigestContract {

    private ReleaseDigestContract() {
    }

    public static PackageDigestResult calculate(List<ArtifactBytes> artifacts) {
        if (artifacts == null) {
            throw invalidPath("artifact list is null");
        }
        Set<String> caseFoldedPaths = new HashSet<>();
        List<ArtifactDigest> digests = new ArrayList<>();
        for (ArtifactBytes artifact : artifacts) {
            if (artifact == null) {
                throw invalidPath("artifact is null");
            }
            String path = normalizePath(artifact.path());
            if (!caseFoldedPaths.add(asciiCaseFold(path))) {
                throw invalidPath("case-folded path collision: " + path);
            }
            byte[] content = artifact.content();
            digests.add(new ArtifactDigest(path, content.length, sha256Hex(content)));
        }
        digests.sort(Comparator.comparing(item -> item.path().getBytes(StandardCharsets.UTF_8),
                ReleaseDigestContract::compareUnsignedBytes));
        ByteArrayOutputStream input = new ByteArrayOutputStream();
        for (ArtifactDigest artifact : digests) {
            input.writeBytes((artifact.path() + "\t" + artifact.bytes() + "\t" + artifact.sha256() + "\n")
                    .getBytes(StandardCharsets.UTF_8));
        }
        byte[] canonicalInput = input.toByteArray();
        return new PackageDigestResult(List.copyOf(digests), canonicalInput, sha256Hex(canonicalInput));
    }

    public static String manifestDigest(byte[] manifestBytes) {
        if (manifestBytes == null) {
            throw new IllegalArgumentException("MANIFEST_BYTES_MISSING");
        }
        return sha256Hex(manifestBytes);
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank() || !path.equals(path.trim()) || path.contains("\\")
                || path.startsWith("/") || path.matches("^[A-Za-z]:.*") || hasControlCharacter(path)) {
            throw invalidPath(String.valueOf(path));
        }
        String[] segments = path.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw invalidPath(path);
            }
        }
        return String.join("/", segments);
    }

    private static boolean hasControlCharacter(String value) {
        return value.codePoints().anyMatch(codePoint -> codePoint < 32 || codePoint == 127);
    }

    private static String asciiCaseFold(String value) {
        StringBuilder folded = new StringBuilder(value.length());
        value.codePoints().forEach(codePoint ->
                folded.appendCodePoint(codePoint >= 'A' && codePoint <= 'Z' ? codePoint + ('a' - 'A') : codePoint));
        return folded.toString();
    }

    private static IllegalArgumentException invalidPath(String message) {
        return new IllegalArgumentException("PACKAGE_PATH_INVALID: " + message);
    }

    private static int compareUnsignedBytes(byte[] left, byte[] right) {
        int length = Math.min(left.length, right.length);
        for (int index = 0; index < length; index++) {
            int difference = Integer.compare(Byte.toUnsignedInt(left[index]), Byte.toUnsignedInt(right[index]));
            if (difference != 0) {
                return difference;
            }
        }
        return Integer.compare(left.length, right.length);
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    public record ArtifactBytes(String path, byte[] content) {
        public ArtifactBytes {
            if (content == null) {
                throw invalidPath("artifact content is null");
            }
            content = content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }

    public record ArtifactDigest(String path, long bytes, String sha256) { }

    public record PackageDigestResult(List<ArtifactDigest> artifacts, byte[] canonicalInput,
                                      String packageDigest) {
        public PackageDigestResult {
            artifacts = List.copyOf(artifacts);
            canonicalInput = canonicalInput.clone();
        }

        @Override
        public byte[] canonicalInput() {
            return canonicalInput.clone();
        }
    }
}
