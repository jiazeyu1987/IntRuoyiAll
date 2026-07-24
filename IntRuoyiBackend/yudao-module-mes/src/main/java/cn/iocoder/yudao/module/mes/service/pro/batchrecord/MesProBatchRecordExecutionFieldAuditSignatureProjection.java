package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditSignatureProjection {

    private Long id;

    private Long executionId;

    private String actionType;

    private Long actorId;

    private String actorName;

    private String signatureMode;

    private Boolean passwordVerified;

    private LocalDateTime signedAt;

    private LocalDateTime selectedSignedAt;

    private LocalDateTime signatureDisplayAt;

    private String signatureTimeMode;

    private String selectedTimeZone;

    private String selectedTimeReason;

    private String selectedTimePolicyVersion;

    private String selectedTimeAuditHash;

    private String reasonCategory;

    private String reasonText;

    private Long auditBatchId;

    private String signatureChallengeHash;

    private Long fieldAuditRevision;

    private String fieldAuditHeadHash;

    private String cellValuesHash;
}
