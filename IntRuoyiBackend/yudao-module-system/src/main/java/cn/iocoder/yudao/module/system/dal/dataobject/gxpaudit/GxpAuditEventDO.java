package cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName(value = "gxp_audit_event", autoResultMap = true)
@KeySequence("gxp_audit_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class GxpAuditEventDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long ledgerSequence;
    private String operationId;
    private String domain;
    private String subjectType;
    private String subjectId;
    private String subjectVersion;
    private String action;
    private String reason;
    private Long actorId;
    private String actorUsername;
    private String actorDisplayName;
    private LocalDateTime serverOccurredAt;
    private String beforeState;
    private String beforeObjectVersion;
    private String beforeStateJson;
    private String afterState;
    private String afterObjectVersion;
    private String afterStateJson;
    private String policyVersion;
    private String idempotencyKey;
    private String requestId;
    private String signatureRecordId;
    private String signatureContentHash;
    private String idempotencyPayloadHash;
    private String canonicalEventJson;
    private String previousEventHash;
    private String eventHash;
    private String algorithm;

}
