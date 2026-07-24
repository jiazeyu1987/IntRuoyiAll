package cn.iocoder.yudao.module.dcc.service.file;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class DccControlledFileVersion implements Comparable<DccControlledFileVersion> {

    private static final Pattern PATTERN = Pattern.compile("[Vv]?\\d+(?:\\.\\d+)*");

    private final List<Integer> segments;

    private DccControlledFileVersion(List<Integer> segments) {
        this.segments = segments;
    }

    static DccControlledFileVersion parse(String rawVersion) {
        if (rawVersion == null || !PATTERN.matcher(rawVersion).matches()) {
            return null;
        }
        String normalizedVersion = rawVersion;
        if (normalizedVersion.startsWith("V") || normalizedVersion.startsWith("v")) {
            normalizedVersion = normalizedVersion.substring(1);
        }
        String[] rawSegments = normalizedVersion.split("\\.");
        List<Integer> parsedSegments = new ArrayList<>(rawSegments.length);
        try {
            for (String rawSegment : rawSegments) {
                parsedSegments.add(Integer.parseInt(rawSegment));
            }
        } catch (NumberFormatException ex) {
            return null;
        }
        return new DccControlledFileVersion(parsedSegments);
    }

    @Override
    public int compareTo(DccControlledFileVersion other) {
        int maxSize = Math.max(segments.size(), other.segments.size());
        for (int i = 0; i < maxSize; i++) {
            int left = i < segments.size() ? segments.get(i) : 0;
            int right = i < other.segments.size() ? other.segments.get(i) : 0;
            if (left != right) {
                return Integer.compare(left, right);
            }
        }
        return 0;
    }
}
