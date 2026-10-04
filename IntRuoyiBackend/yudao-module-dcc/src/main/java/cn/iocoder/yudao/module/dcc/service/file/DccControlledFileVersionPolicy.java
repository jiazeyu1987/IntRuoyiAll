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

    private static final Pattern FIRST_SEGMENT_PATTERN = Pattern.compile("[A-Z]");
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
        if (parsed == null || parsed.isWorkingIteration() || !parsed.isFirstMinorIteration()) {
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
        String[] working = normalized.split("-", -1);
        if (working.length > 2) return null;
        int workingNo = 0;
        if (working.length == 2) {
            if (!NUMERIC_SEGMENT_PATTERN.matcher(working[1]).matches()) return null;
            try { workingNo = Integer.parseInt(working[1]); }
            catch (NumberFormatException ex) { return null; }
        }
        String[] split = working[0].toUpperCase(Locale.ROOT).split("/", -1);
        if (split.length != majorIdentitySegmentCount() + 1 || !FIRST_SEGMENT_PATTERN.matcher(split[0]).matches()) {
            return null;
        }
        List<String> segments = new ArrayList<>(split.length);
        segments.add(split[0]);
        for (int i = 1; i < split.length; i++) {
            if (!NUMERIC_SEGMENT_PATTERN.matcher(split[i]).matches()) {
                return null;
            }
            try {
                int value = Integer.parseInt(split[i]);
                if (majorIdentitySegmentCount() == 1 && split.length == 2 && value > 9) return null;
            } catch (NumberFormatException ex) { return null; }
            segments.add(split[i]);
        }
        return new VersionNumber(segments, majorIdentitySegmentCount(), workingNo);
    }

    public VersionNumber parseStored(DccControlledFileDO file) {
        if (file == null) {
            return null;
        }
        return parse(file.getVersionNo());
    }

    public VersionNumber parseStoredInitial(DccControlledFileDO file) {
        VersionNumber parsed = parseStored(file);
        if (parsed != null) {
            return parsed;
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
        if (previous == null || next == null || previous.isWorkingIteration() || next.isWorkingIteration()) {
            return false;
        }
        if ("Z".equals(previous.majorIdentity()) && (majorChange || previous.iterationNo() == 9)) return false;
        return (majorChange ? previous.nextMajor() : previous.nextMinor()).display().equals(next.display());
    }

    public VersionNumber formalTarget(DccControlledFileDO baseline, String actualIntent) {
        var type = DccControlledFileRevisionChangeType.requireRevision(actualIntent);
        var previous = requireStored(baseline);
        if (previous.isWorkingIteration()) throw new IllegalArgumentException("formal source must be a controlled version");
        return type == DccControlledFileRevisionChangeType.PARTIAL ? previous.nextMinor() : previous.nextMajor();
    }

    /** Includes A's approved IC-1 controlled-but-not-effective status; this does not select an execution version. */
    public boolean isControlledBaseline(DccControlledFileDO file) {
        return file != null && hasControlledBodyStatus(file.getStatus());
    }

    public static boolean hasControlledBodyStatus(String status) {
        return status != null && java.util.Set.of("ACTIVE", "SUPERSEDED", "CONTROLLED_PENDING_EFFECTIVE").contains(status);
    }

    public static boolean isCurrentControlledStatus(String status) {
        return "ACTIVE".equals(status) || "CONTROLLED_PENDING_EFFECTIVE".equals(status);
    }

    public static boolean hasRetainedControlledBodyStatus(String status) {
        return hasControlledBodyStatus(status) || "OBSOLETE".equals(status);
    }

    /** Historical read grammar is separate from write validation; it never assigns a modern identity. */
    public static HistoricalVersion parseHistorical(String raw) {
        if (raw == null) return null;
        String value = raw.trim().toUpperCase(Locale.ROOT);
        boolean slash = value.matches("[A-Z]+(?:/[1-9][0-9]*)+(?:-[1-9][0-9]*)?");
        if (!slash && !value.matches("V?[0-9]+(?:\\.[0-9]+)*")) return null;
        String[] working = slash ? value.split("-", -1) : new String[]{value};
        String core = slash ? working[0] : value.replaceFirst("^V", "");
        List<String> parts = List.of(core.split(slash ? "/" : "\\."));
        try {
            for (int i = slash ? 1 : 0; i < parts.size(); i++) Integer.parseInt(parts.get(i));
            int workingNo = working.length == 2 ? Integer.parseInt(working[1]) : 0;
            return new HistoricalVersion(parts, slash, workingNo);
        } catch (NumberFormatException invalidNumber) { return null; }
    }

    public static final class HistoricalVersion implements Comparable<HistoricalVersion> {
        private final List<String> parts;
        private final boolean slash;
        private final int workingNo;
        private HistoricalVersion(List<String> parts, boolean slash, int workingNo) {
            this.parts = parts; this.slash = slash; this.workingNo = workingNo;
        }
        @Override public int compareTo(HistoricalVersion other) {
            if (slash != other.slash) return slash ? 1 : -1;
            int formal = VersionNumber.compareSegments(parts, other.parts);
            return formal == 0 ? Integer.compare(workingNo, other.workingNo) : formal;
        }
    }

    static String incrementRevisionLetter(String revision) {
        String letter = Objects.requireNonNull(revision, "revision").trim().toUpperCase(Locale.ROOT);
        if (!letter.matches("[A-Y]")) throw new IllegalArgumentException("revision after Z is not defined");
        return Character.toString(letter.charAt(0) + 1);
    }

    public VersionNumber nextMinor(DccControlledFileDO file, Collection<DccControlledFileDO> chain) {
        VersionNumber current = requireStored(file).formal();
        VersionNumber maximum = current;
        if (chain != null) {
            for (DccControlledFileDO history : chain) {
                VersionNumber candidate = requireStored(history).formal();
                if (candidate.sameMajorIdentity(current) && candidate.compareTo(maximum) > 0) {
                    maximum = candidate;
                }
            }
        }
        return maximum.nextMinor();
    }

    public VersionNumber nextMajor(DccControlledFileDO file, Collection<DccControlledFileDO> chain) {
        VersionNumber maximum = requireStored(file).formal();
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

    /** Called with the master and chain locked in the caller's transaction. */
    public VersionNumber nextWorking(DccControlledFileDO file, Collection<DccControlledFileDO> chain) {
        VersionNumber base = requireStored(file).formal();
        int maximum = 0;
        for (DccControlledFileDO history : Objects.requireNonNull(chain, "version chain")) {
            VersionNumber candidate = requireStored(history);
            if (candidate.formal().display().equals(base.display())) maximum = Math.max(maximum, candidate.workingNo);
        }
        return new VersionNumber(base.segments, base.majorIdentitySegmentCount, Math.addExact(maximum, 1));
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
        private final int workingNo;

        private VersionNumber(List<String> segments, int majorIdentitySegmentCount) {
            this(segments, majorIdentitySegmentCount, 0);
        }

        private VersionNumber(List<String> segments, int majorIdentitySegmentCount, int workingNo) {
            this.segments = List.copyOf(segments);
            this.majorIdentitySegmentCount = majorIdentitySegmentCount;
            this.workingNo = workingNo;
        }

        public String display() {
            return String.join("/", segments) + (workingNo == 0 ? "" : "-" + workingNo);
        }

        public boolean isWorkingIteration() { return workingNo > 0; }
        public VersionNumber formal() { return new VersionNumber(segments, majorIdentitySegmentCount); }

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
            if (majorIdentitySegmentCount == 1 && segments.size() == 2 && iterationNo() == 9) return nextMajor();
            List<String> next = new ArrayList<>(segments);
            if (next.size() <= majorIdentitySegmentCount) {
                next.add("1");
            } else {
                int lastIndex = next.size() - 1;
                next.set(lastIndex, Integer.toString(Math.addExact(Integer.parseInt(next.get(lastIndex)), 1)));
            }
            return new VersionNumber(next, majorIdentitySegmentCount);
        }

        public VersionNumber nextMajor() {
            if (segments.get(0).length() != 1 || "Z".equals(segments.get(0))) {
                throw new IllegalArgumentException("revision after Z is not defined");
            }
            List<String> next = new ArrayList<>(Math.max(segments.size(), majorIdentitySegmentCount + 1));
            next.add(incrementRevisionLetter(segments.get(0)));
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
            int formalCompare = compareSegments(segments, other.segments);
            return formalCompare != 0 ? formalCompare : Integer.compare(workingNo, other.workingNo);
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
