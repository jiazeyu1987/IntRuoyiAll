package cn.iocoder.yudao.module.system.service.controlledcontent;

import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType;

/**
 * Exact active/open-candidate projection at one transaction boundary.
 */
public record ControlledContentProjectionSnapshot(
        Long tenantId,
        ControlledContentType contentType,
        String contentKey,
        Long activeNativeVersionId,
        Long openCandidateNativeVersionId) {

    public ControlledContentProjectionSnapshot {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null");
        }
        if (contentType == null) {
            throw new IllegalArgumentException("contentType must not be null");
        }
        if (contentKey == null || contentKey.trim().isEmpty()) {
            throw new IllegalArgumentException("contentKey must not be blank");
        }
        contentKey = contentKey.trim();
    }

    public static ControlledContentProjectionSnapshot of(ControlledContentKey key, Long activeNativeVersionId,
                                                         Long openCandidateNativeVersionId) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        return new ControlledContentProjectionSnapshot(key.getTenantId(), key.getContentType(), key.getContentKey(),
                activeNativeVersionId, openCandidateNativeVersionId);
    }

    public boolean isEmpty() {
        return activeNativeVersionId == null && openCandidateNativeVersionId == null;
    }

}
