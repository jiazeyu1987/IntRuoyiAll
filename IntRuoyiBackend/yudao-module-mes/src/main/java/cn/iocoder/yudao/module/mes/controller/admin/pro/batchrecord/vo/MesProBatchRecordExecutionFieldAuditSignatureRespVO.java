package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditSignatureRespVO {

    private Long signatureId;
    private String actionType;
    private String signatureMode;
    private Long actorId;
    private String actorName;
    private LocalDateTime signedAt;
    private Boolean passwordVerified;
    private String signatureChallengeHash;
    private Long fieldAuditRevision;
    private String fieldAuditHeadHash;
    private String cellValuesHash;
}
