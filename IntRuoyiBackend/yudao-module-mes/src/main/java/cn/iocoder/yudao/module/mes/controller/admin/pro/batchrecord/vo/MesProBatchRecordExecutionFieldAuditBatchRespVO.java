package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditBatchRespVO {

    private Long id;
    private Long executionId;
    private String idempotencyKey;
    private String requestHash;
    private String actionType;
    private String reasonCategory;
    private String reasonText;
    private Integer fieldCount;
    private Long actorId;
    private String actorName;
    private Long signatureId;
    private String signatureChallengeHash;
    private String signatureProjectionHash;
    private String baseCellValuesHash;
    private String beforeCellValuesHash;
    private String afterCellValuesHash;
    private Long baseFieldAuditRevision;
    private Long beforeFieldAuditRevision;
    private Long afterFieldAuditRevision;
    private String baseFieldAuditHeadHash;
    private String previousHeadHash;
    private String newHeadHash;
    private LocalDateTime changedAt;
}
