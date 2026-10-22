package cn.iocoder.yudao.module.system.dal.dataobject.permission;

import lombok.Data;
import java.time.LocalDateTime;

/** Append-only command fact; deliberately does not inherit mutable BaseDO fields. */
@Data
public class PermissionCommandReceiptDO {
    private Long id;
    private Long tenantId;
    private String operationId;
    private String subjectId;
    private byte[] sourceKey;
    private byte[] sourceKeySha256;
    private byte[] sourcePayloadHash;
    private String identityJson;
    private String sourceJson;
    private Long auditEventId;
    private byte[] auditEventHash;
    private String resultJson;
    private LocalDateTime createdAtUtc;
}
