package cn.iocoder.yudao.module.dcc.service.file;

/** Facts from explicit formal submission intent; never inferred from a version number. */
public enum DccControlledFileRevisionChangeType {
    INITIAL, PARTIAL, REPLACEMENT;

    public static DccControlledFileRevisionChangeType requireRevision(String value) {
        if (!"PARTIAL".equals(value) && !"REPLACEMENT".equals(value)) {
            throw new IllegalArgumentException("formal revision intent must be PARTIAL or REPLACEMENT");
        }
        return valueOf(value);
    }
}
