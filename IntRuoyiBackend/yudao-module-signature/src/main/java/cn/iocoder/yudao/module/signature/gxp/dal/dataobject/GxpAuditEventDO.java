package cn.iocoder.yudao.module.signature.gxp.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("gxp_audit_event")
@KeySequence("gxp_audit_event_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GxpAuditEventDO {

    @TableId
    private Long id;
    private Long tenantId;
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
    private String canonicalEventJson;
    private String previousEventHash;
    private String eventHash;
    private String algorithm;
    private LocalDateTime createTime;

}
