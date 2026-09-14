package cn.iocoder.yudao.module.dcc.service.file;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class DccControlledFileVersion implements Comparable<DccControlledFileVersion> {

    private static final Pattern LEGACY_PATTERN = Pattern.compile("[Vv]?\\d+(?:\\.\\d+)*");
    private static final Pattern WINDCHILL_PATTERN = Pattern.compile("([A-Za-z]+)/([1-9][0-9]*)");

    private final List<Integer> segments;
    private final String revisionCode;
    private final Integer iterationNo;

    private DccControlledFileVersion(List<Integer> segments, String revisionCode, Integer iterationNo) {
        this.segments = segments;
        this.revisionCode = revisionCode;
        this.iterationNo = iterationNo;
    }

    static DccControlledFileVersion parse(String rawVersion) {
        if (rawVersion == null) {
            return null;
        }
        var windchillMatcher = WINDCHILL_PATTERN.matcher(rawVersion.trim());
        if (windchillMatcher.matches()) {
            try {
                return new DccControlledFileVersion(List.of(), windchillMatcher.group(1).toUpperCase(java.util.Locale.ROOT),
                        Integer.parseInt(windchillMatcher.group(2)));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        if (!LEGACY_PATTERN.matcher(rawVersion).matches()) {
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
        return new DccControlledFileVersion(parsedSegments, null, null);
    }

    @Override
    public int compareTo(DccControlledFileVersion other) {
        if (revisionCode != null && other.revisionCode != null) {
            int revisionCompare = compareRevisionLabels(revisionCode, other.revisionCode);
            return revisionCompare != 0 ? revisionCompare : Integer.compare(iterationNo, other.iterationNo);
        }
        if (revisionCode != null || other.revisionCode != null) {
            return revisionCode != null ? 1 : -1;
        }
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

    private static int compareRevisionLabels(String left, String right) {
        int lengthCompare = Integer.compare(left.length(), right.length());
        return lengthCompare != 0 ? lengthCompare : left.compareTo(right);
    }
}
