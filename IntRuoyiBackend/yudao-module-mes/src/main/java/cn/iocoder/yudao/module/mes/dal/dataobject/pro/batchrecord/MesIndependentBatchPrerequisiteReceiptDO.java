package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("mes_independent_batch_prerequisite_receipt")
@KeySequence("mes_independent_batch_prerequisite_receipt_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class MesIndependentBatchPrerequisiteReceiptDO extends BaseDO {
    @TableId private Long id;
    private String receiptId;
    private Long tenantId;
    private String entryType;
    private Long workOrderId;
    private String workOrderCode;
    private Long routeId;
    private Long routeVersionId;
    private String routeVersion;
    private String batchCode;
    private String sourceRelationId;
    private String sourceRelationVersion;
    private String sourceRelationSnapshotHash;
    private String sourceObjectType;
    private String sourceObjectId;
    private String materialSourceType;
    private String materialSourceId;
    private String sourceContextHash;
    private String sourceSnapshotHash;
    private String businessReason;
    private String issuerSystem;
    private Long issuerUserId;
    private String issuerUserRole;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private String revocationReason;
    private Long credentialVersion;
    private String status;
    private String canonicalPayload;
    private String sourceEvidenceJson;
    private String receiptHash;
    private String payloadHash;
    private String signature;
    private String auditEventId;
    private String idempotencyKey;
}
