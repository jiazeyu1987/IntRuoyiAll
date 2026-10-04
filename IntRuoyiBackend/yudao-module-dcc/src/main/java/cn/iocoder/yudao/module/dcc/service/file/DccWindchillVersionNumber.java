package cn.iocoder.yudao.module.dcc.service.file;

/** Formal version facade for existing callers. All grammar and arithmetic delegate to the policy. */
public record DccWindchillVersionNumber(String revisionCode, int iterationNo) {
    public DccWindchillVersionNumber {
        var value = DccControlledFileVersionPolicy.defaultPolicy().parse(revisionCode + "/" + iterationNo);
        if (value == null || value.isWorkingIteration()) throw new IllegalArgumentException("invalid formal version");
        revisionCode = value.majorIdentity();
    }
    public static DccWindchillVersionNumber parse(String raw) {
        var value = DccControlledFileVersionPolicy.defaultPolicy().parse(raw);
        return value == null || value.isWorkingIteration() ? null : new DccWindchillVersionNumber(value.majorIdentity(), value.iterationNo());
    }
    public static DccWindchillVersionNumber initial() { return parse(DccControlledFileVersionPolicy.defaultPolicy().initial().display()); }
    public static String initialForNewFile(String raw) { return DccControlledFileVersionPolicy.defaultPolicy().initialForNewFile(raw); }
    public static DccWindchillVersionNumber parseStoredInitial(String raw) { return parse(raw); }
    private DccControlledFileVersionPolicy.VersionNumber value() { return DccControlledFileVersionPolicy.defaultPolicy().parse(display()); }
    public DccWindchillVersionNumber nextIteration() { return parse(value().nextMinor().display()); }
    public DccWindchillVersionNumber nextRevision() { return parse(value().nextMajor().display()); }
    public int compareRevisionTo(DccWindchillVersionNumber other) { return value().compareMajorIdentityTo(other.value()); }
    public String display() { return revisionCode + "/" + iterationNo; }
    public static String incrementRevision(String revision) { return DccControlledFileVersionPolicy.incrementRevisionLetter(revision); }
}
