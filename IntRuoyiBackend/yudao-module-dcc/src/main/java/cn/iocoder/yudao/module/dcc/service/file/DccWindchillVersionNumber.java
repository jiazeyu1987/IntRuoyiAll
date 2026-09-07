package cn.iocoder.yudao.module.dcc.service.file;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server-owned Windchill style revision/iteration number.
 *
 * <p>The initial number is deliberately independent of the client payload. Later
 * phases can use the parser and increment helpers without reintroducing a
 * client-controlled version string.</p>
 */
public record DccWindchillVersionNumber(String revisionCode, int iterationNo) {

    private static final Pattern PATTERN = Pattern.compile("([A-Z]+)/([1-9][0-9]*)");

    public DccWindchillVersionNumber {
        revisionCode = normalizeRevision(revisionCode);
        if (iterationNo < 1) {
            throw new IllegalArgumentException("iterationNo must be positive");
        }
    }

    public static String initialForNewFile(String ignoredClientVersion) {
        return "A/1";
    }

    public static DccWindchillVersionNumber initial() {
        return new DccWindchillVersionNumber("A", 1);
    }

    public static DccWindchillVersionNumber parse(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher matcher = PATTERN.matcher(raw.trim().toUpperCase(Locale.ROOT));
        if (!matcher.matches()) {
            return null;
        }
        try {
            return new DccWindchillVersionNumber(matcher.group(1), Integer.parseInt(matcher.group(2)));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public DccWindchillVersionNumber nextIteration() {
        if (iterationNo == Integer.MAX_VALUE) {
            throw new IllegalStateException("iterationNo overflow");
        }
        return new DccWindchillVersionNumber(revisionCode, iterationNo + 1);
    }

    public DccWindchillVersionNumber nextRevision() {
        return new DccWindchillVersionNumber(incrementRevision(revisionCode), 1);
    }

    public int compareRevisionTo(DccWindchillVersionNumber other) {
        int lengthCompare = Integer.compare(revisionCode.length(), other.revisionCode.length());
        return lengthCompare != 0 ? lengthCompare : revisionCode.compareTo(other.revisionCode);
    }

    public String display() {
        return revisionCode + "/" + iterationNo;
    }

    public static String incrementRevision(String revision) {
        String normalized = normalizeRevision(revision);
        char[] chars = normalized.toCharArray();
        int index = chars.length - 1;
        while (index >= 0 && chars[index] == 'Z') {
            chars[index] = 'A';
            index--;
        }
        if (index < 0) {
            return "A" + new String(chars);
        }
        chars[index]++;
        return new String(chars);
    }

    private static String normalizeRevision(String value) {
        String normalized = Objects.requireNonNull(value, "revisionCode").trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]+")) {
            throw new IllegalArgumentException("revisionCode must contain only A-Z");
        }
        return normalized;
    }

}
