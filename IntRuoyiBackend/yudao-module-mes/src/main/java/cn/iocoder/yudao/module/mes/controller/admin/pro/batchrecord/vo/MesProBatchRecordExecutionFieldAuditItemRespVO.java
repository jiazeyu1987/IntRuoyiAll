package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditItemRespVO {

    private Long id;
    private Long auditBatchId;
    private Long executionId;
    private String executionCode;
    private Long fieldAuditRevision;
    private String fieldPath;
    private String fieldKey;
    private String fieldLabel;
    private Integer rowIndex;
    private Integer columnIndex;
    private String component;
    private String valueType;
    private Object oldValueJson;
    private String oldValueDisplay;
    private String oldValueHash;
    private Object newValueJson;
    private String newValueDisplay;
    private String newValueHash;
    private String reasonCategory;
    private String reasonText;
    private Long actorId;
    private String actorName;
    private Long signatureId;
    private String previousHash;
    private String auditHash;
    private LocalDateTime changedAt;
    private MesProBatchRecordExecutionFieldAuditHashVerificationRespVO hashVerification;
}
