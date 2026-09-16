package cn.iocoder.yudao.module.dcc.service.file;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class DccControlledFileVersion implements Comparable<DccControlledFileVersion> {

    private static final Pattern LEGACY_PATTERN = Pattern.compile("[Vv]?\\d+(?:\\.\\d+)*");
    private static final Pattern SLASH_PATTERN = Pattern.compile("[A-Za-z]+(?:/[1-9][0-9]*)+");
    private static final Pattern NUMERIC_SEGMENT_PATTERN = Pattern.compile("[1-9][0-9]*");

    private final List<Integer> numericSegments;
    private final List<String> slashSegments;

    private DccControlledFileVersion(List<Integer> numericSegments, List<String> slashSegments) {
        this.numericSegments = numericSegments;
        this.slashSegments = slashSegments;
    }

    static DccControlledFileVersion parse(String rawVersion) {
        if (rawVersion == null) {
            return null;
        }
        String trimmed = rawVersion.trim();
        if (SLASH_PATTERN.matcher(trimmed).matches()) {
            return new DccControlledFileVersion(List.of(),
                    List.of(trimmed.toUpperCase(Locale.ROOT).split("/")));
        }
        if (!LEGACY_PATTERN.matcher(trimmed).matches()) {
            return null;
        }
        String normalizedVersion = trimmed;
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
        return new DccControlledFileVersion(parsedSegments, null);
    }

    @Override
    public int compareTo(DccControlledFileVersion other) {
        if (slashSegments != null && other.slashSegments != null) {
            return compareSlashSegments(slashSegments, other.slashSegments);
        }
        if (slashSegments != null || other.slashSegments != null) {
            return slashSegments != null ? 1 : -1;
        }
        int maxSize = Math.max(numericSegments.size(), other.numericSegments.size());
        for (int i = 0; i < maxSize; i++) {
            int left = i < numericSegments.size() ? numericSegments.get(i) : 0;
            int right = i < other.numericSegments.size() ? other.numericSegments.get(i) : 0;
            if (left != right) {
                return Integer.compare(left, right);
            }
        }
        return 0;
    }

    private static int compareSlashSegments(List<String> left, List<String> right) {
        int maxSize = Math.max(left.size(), right.size());
        for (int i = 0; i < maxSize; i++) {
            String leftSegment = i < left.size() ? left.get(i) : "0";
            String rightSegment = i < right.size() ? right.get(i) : "0";
            int segmentCompare = compareSlashSegment(leftSegment, rightSegment);
            if (segmentCompare != 0) {
                return segmentCompare;
            }
        }
        return 0;
    }

    private static int compareSlashSegment(String left, String right) {
        boolean leftNumeric = NUMERIC_SEGMENT_PATTERN.matcher(left).matches() || "0".equals(left);
        boolean rightNumeric = NUMERIC_SEGMENT_PATTERN.matcher(right).matches() || "0".equals(right);
        if (leftNumeric && rightNumeric) {
            return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
        }
        if (leftNumeric != rightNumeric) {
            return leftNumeric ? -1 : 1;
        }
        int lengthCompare = Integer.compare(left.length(), right.length());
        return lengthCompare != 0 ? lengthCompare : left.compareTo(right);
    }
}
