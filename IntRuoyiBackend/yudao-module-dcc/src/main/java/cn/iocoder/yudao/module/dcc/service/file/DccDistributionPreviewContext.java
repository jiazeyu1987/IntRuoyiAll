package cn.iocoder.yudao.module.dcc.service.file;

/** Exact notification read identity, bound into the signed preview purpose; never a permanent grant. */
public record DccDistributionPreviewContext(Long userId, Long controlledFileId, Long distributionId, Long recipientId) {
    public static final String PURPOSE_PREFIX = "DIST_PREVIEW:";

    public DccDistributionPreviewContext {
        if (userId == null || userId <= 0 || controlledFileId == null || controlledFileId <= 0
                || distributionId == null || distributionId <= 0 || recipientId == null || recipientId <= 0) {
            throw new IllegalArgumentException("exact distribution preview identities are required");
        }
    }

    public String purpose() { return PURPOSE_PREFIX + distributionId + ":" + recipientId; }

    public static DccDistributionPreviewContext fromPurpose(Long userId, Long fileId, String purpose) {
        if (purpose == null || !purpose.startsWith(PURPOSE_PREFIX)) {
            throw new IllegalArgumentException("exact distribution preview purpose is required");
        }
        String[] ids = purpose.substring(PURPOSE_PREFIX.length()).split(":", -1);
        if (ids.length != 2 || !ids[0].matches("[1-9][0-9]*") || !ids[1].matches("[1-9][0-9]*")) {
            throw new IllegalArgumentException("distribution preview purpose identities are invalid");
        }
        return new DccDistributionPreviewContext(userId, fileId, Long.valueOf(ids[0]), Long.valueOf(ids[1]));
    }
}
