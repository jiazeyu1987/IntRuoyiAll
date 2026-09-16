package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class DccControlledFileVersionPolicy {

    private static final Pattern FIRST_SEGMENT_PATTERN = Pattern.compile("[A-Z]+");
    private static final Pattern NUMERIC_SEGMENT_PATTERN = Pattern.compile("[1-9][0-9]*");

    private final DccControlledFileVersionPolicyProperties properties;

    public DccControlledFileVersionPolicy(DccControlledFileVersionPolicyProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    public static DccControlledFileVersionPolicy defaultPolicy() {
        return new DccControlledFileVersionPolicy(new DccControlledFileVersionPolicyProperties());
    }

    public String initialForNewFile(String requestedVersion) {
        if (StrUtil.isBlank(requestedVersion)) {
            return initial().display();
        }
        VersionNumber parsed = parse(requestedVersion);
        if (parsed == null || !parsed.isFirstMinorIteration()) {
            throw new IllegalArgumentException("initial version must be the first configured minor iteration");
        }
        return parsed.display();
    }

    public VersionNumber initial() {
        List<String> segments = new ArrayList<>(majorIdentitySegmentCount() + 1);
        segments.add("A");
        while (segments.size() < majorIdentitySegmentCount() + 1) {
            segments.add("1");
        }
        return new VersionNumber(segments, majorIdentitySegmentCount());
    }

    public VersionNumber parse(String rawVersion) {
        String normalized = StrUtil.trimToNull(rawVersion);
        if (normalized == null) {
            return null;
        }
        String[] split = normalized.toUpperCase(Locale.ROOT).split("/", -1);
        if (split.length < 2 || !FIRST_SEGMENT_PATTERN.matcher(split[0]).matches()) {
            return null;
        }
        List<String> segments = new ArrayList<>(split.length);
        segments.add(split[0]);
        for (int i = 1; i < split.length; i++) {
            if (!NUMERIC_SEGMENT_PATTERN.matcher(split[i]).matches()) {
                return null;
            }
            segments.add(split[i]);
        }
        return new VersionNumber(segments, majorIdentitySegmentCount());
    }

    public VersionNumber parseStored(DccControlledFileDO file) {
        if (file == null) {
            return null;
        }
        VersionNumber parsed = parse(file.getVersionNo());
        if (parsed != null) {
            return parsed;
        }
        String majorIdentity = StrUtil.trimToNull(file.getRevisionCode());
        Integer iterationNo = file.getIterationNo();
        if (majorIdentity == null || iterationNo == null || iterationNo < 1) {
            return null;
        }
        return parse(majorIdentity + "/" + iterationNo);
    }

    public VersionNumber parseStoredInitial(DccControlledFileDO file) {
        VersionNumber parsed = parseStored(file);
        if (parsed != null) {
            return parsed;
        }
        if (file != null && "V1.0".equalsIgnoreCase(StrUtil.trim(file.getVersionNo()))) {
            return initial();
        }
        return null;
    }

    public VersionNumber requireStored(DccControlledFileDO file) {
        VersionNumber parsed = parseStored(file);
        if (parsed == null) {
            throw new IllegalArgumentException("controlled file version does not match configured policy");
        }
        return parsed;
    }

    public boolean isMajorVersionChange(DccControlledFileDO publishedFile, DccControlledFileDO previousActiveFile) {
        if (previousActiveFile == null) {
            return false;
        }
        VersionNumber publishedVersion = requireStored(publishedFile);
        VersionNumber previousVersion = requireStored(previousActiveFile);
        return !publishedVersion.majorIdentity().equals(previousVersion.majorIdentity());
    }

    public boolean matchesVersionChange(DccControlledFileDO base, DccControlledFileDO existing, boolean majorChange) {
        VersionNumber previous = parseStored(base);
        VersionNumber next = parseStored(existing);
        if (previous == null || next == null) {
            return false;
        }
        return majorChange
                ? next.compareMajorIdentityTo(previous) > 0
                : next.sameMajorIdentity(previous) && next.compareTo(previous) > 0;
    }

    public VersionNumber nextMinor(DccControlledFileDO file, Collection<DccControlledFileDO> chain) {
        VersionNumber current = requireStored(file);
        VersionNumber maximum = current;
        if (chain != null) {
            for (DccControlledFileDO history : chain) {
                VersionNumber candidate = parseStored(history);
                if (candidate != null && candidate.sameMajorIdentity(current) && candidate.compareTo(maximum) > 0) {
                    maximum = candidate;
                }
            }
        }
        return maximum.nextMinor();
    }

    public VersionNumber nextMajor(DccControlledFileDO file, Collection<DccControlledFileDO> chain) {
        VersionNumber maximum = requireStored(file);
        if (chain != null) {
            for (DccControlledFileDO history : chain) {
                VersionNumber candidate = parseStored(history);
                if (candidate == null) {
                    throw new IllegalArgumentException("controlled file version chain contains invalid version");
                }
                if (candidate.compareMajorIdentityTo(maximum) > 0) {
                    maximum = candidate;
                }
            }
        }
        return maximum.nextMajor();
    }

    private int majorIdentitySegmentCount() {
        Integer configured = properties.getMajorIdentitySegmentCount();
        if (configured == null || configured < 1) {
            throw new IllegalStateException("yudao.dcc.controlled-file.version-policy.major-identity-segment-count must be >= 1");
        }
        return configured;
    }

    public static final class VersionNumber implements Comparable<VersionNumber> {

        private final List<String> segments;
        private final int majorIdentitySegmentCount;

        private VersionNumber(List<String> segments, int majorIdentitySegmentCount) {
            this.segments = List.copyOf(segments);
            this.majorIdentitySegmentCount = majorIdentitySegmentCount;
        }

        public String display() {
            return String.join("/", segments);
        }

        public String majorIdentity() {
            return String.join("/", segments.subList(0, Math.min(majorIdentitySegmentCount, segments.size())));
        }

        public Integer iterationNo() {
            if (segments.size() <= majorIdentitySegmentCount) {
                return null;
            }
            return Integer.parseInt(segments.get(segments.size() - 1));
        }

        public boolean sameMajorIdentity(VersionNumber other) {
            return majorIdentity().equals(other.majorIdentity());
        }

        public boolean isFirstMinorIteration() {
            Integer iterationNo = iterationNo();
            return iterationNo != null && iterationNo == 1;
        }

        public VersionNumber nextMinor() {
            List<String> next = new ArrayList<>(segments);
            if (next.size() <= majorIdentitySegmentCount) {
                next.add("1");
            } else {
                int lastIndex = next.size() - 1;
                next.set(lastIndex, Integer.toString(Integer.parseInt(next.get(lastIndex)) + 1));
            }
            return new VersionNumber(next, majorIdentitySegmentCount);
        }

        public VersionNumber nextMajor() {
            List<String> next = new ArrayList<>(Math.max(segments.size(), majorIdentitySegmentCount + 1));
            next.add(DccWindchillVersionNumber.incrementRevision(segments.get(0)));
            while (next.size() < Math.max(segments.size(), majorIdentitySegmentCount + 1)) {
                next.add("1");
            }
            return new VersionNumber(next, majorIdentitySegmentCount);
        }

        public int compareMajorIdentityTo(VersionNumber other) {
            return compareSegments(majorSegments(), other.majorSegments());
        }

        @Override
        public int compareTo(VersionNumber other) {
            return compareSegments(segments, other.segments);
        }

        private List<String> majorSegments() {
            return segments.subList(0, Math.min(majorIdentitySegmentCount, segments.size()));
        }

        private static int compareSegments(List<String> left, List<String> right) {
            int max = Math.max(left.size(), right.size());
            for (int i = 0; i < max; i++) {
                String leftSegment = i < left.size() ? left.get(i) : "0";
                String rightSegment = i < right.size() ? right.get(i) : "0";
                int segmentCompare = compareSegment(leftSegment, rightSegment);
                if (segmentCompare != 0) {
                    return segmentCompare;
                }
            }
            return 0;
        }

        private static int compareSegment(String left, String right) {
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
}
