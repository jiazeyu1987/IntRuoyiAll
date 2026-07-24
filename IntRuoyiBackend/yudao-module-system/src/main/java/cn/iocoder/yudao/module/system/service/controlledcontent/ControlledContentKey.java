package cn.iocoder.yudao.module.system.service.controlledcontent;

import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType;

import java.util.Objects;

/**
 * Stable platform key for one controlled business object.
 */
public final class ControlledContentKey {

    private final Long tenantId;
    private final ControlledContentType contentType;
    private final String contentKey;

    private ControlledContentKey(Long tenantId, ControlledContentType contentType, String contentKey) {
        this.tenantId = tenantId;
        this.contentType = contentType;
        this.contentKey = contentKey;
    }

    public static ControlledContentKey of(Long tenantId, ControlledContentType contentType, String contentKey) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null");
        }
        if (contentType == null) {
            throw new IllegalArgumentException("contentType must not be null");
        }
        if (contentKey == null || contentKey.trim().isEmpty()) {
            throw new IllegalArgumentException("contentKey must not be blank");
        }
        return new ControlledContentKey(tenantId, contentType, contentKey.trim());
    }

    public Long getTenantId() {
        return tenantId;
    }

    public ControlledContentType getContentType() {
        return contentType;
    }

    public String getContentKey() {
        return contentKey;
    }

    public String toUniqueKey() {
        return tenantId + ":" + contentType.name() + ":" + contentKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ControlledContentKey that)) {
            return false;
        }
        return Objects.equals(tenantId, that.tenantId)
                && contentType == that.contentType
                && Objects.equals(contentKey, that.contentKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, contentType, contentKey);
    }

    @Override
    public String toString() {
        return toUniqueKey();
    }

}
