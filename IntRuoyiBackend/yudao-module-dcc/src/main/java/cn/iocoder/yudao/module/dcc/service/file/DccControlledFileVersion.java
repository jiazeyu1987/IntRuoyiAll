package cn.iocoder.yudao.module.dcc.service.file;

/** Existing historical comparator facade. Parsing and comparison are owned by the version policy. */
final class DccControlledFileVersion implements Comparable<DccControlledFileVersion> {
    private final DccControlledFileVersionPolicy.HistoricalVersion value;
    private DccControlledFileVersion(DccControlledFileVersionPolicy.HistoricalVersion value) { this.value = value; }
    static DccControlledFileVersion parse(String raw) {
        var value = DccControlledFileVersionPolicy.parseHistorical(raw);
        return value == null ? null : new DccControlledFileVersion(value);
    }
    @Override public int compareTo(DccControlledFileVersion other) { return value.compareTo(other.value); }
}
