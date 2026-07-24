package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityEvidenceStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityReasonCode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO {

    private Long auditItemId;
    private Long auditBatchId;
    private Long fieldAuditRevision;
    private String oldValueJson;
    private String oldValueDisplay;
    private String oldValueHash;
    private String newValueJson;
    private String newValueDisplay;
    private String newValueHash;
    private String reasonCategory;
    private String reasonText;
    private Long actorId;
    private String actorName;
    private LocalDateTime changedAt;
    private Long signatureId;
    private String signatureActorUsernameSnapshot;
    private String signatureActorNicknameSnapshot;
    private LocalDateTime signatureDisplayAt;
    private String signatureProjectionHash;
    private String previousHash;
    private String auditHash;
    private MesProBatchRecordExecutionResponsibilityEvidenceStatus evidenceStatus;
    private List<MesProBatchRecordExecutionResponsibilityReasonCode> reasonCodes;
}
