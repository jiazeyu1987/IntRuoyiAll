package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("mes_edhr_release_submission_material_association")
public class MesEdhrReleaseSubmissionMaterialAssociationDO {
    @TableId private Long id;
    private Long tenantId;
    private String associationId;
    private String associationProtocol;
    private Integer associationVersion;
    private Long releaseTransactionId;
    private Long batchExecutionId;
    private Long submissionEventId;
    private Integer submissionTransactionVersion;
    private Long approvalWorkTaskId;
    private String sourceMethod;
    private String submitIdempotencyKey;
    private String submissionKeyDigest;
    private Long submittedBy;
    private String submissionEvidenceHash;
    private String precheckSnapshotHash;
    private Long materialSourceRowId;
    private String materialReceiptId;
    private Integer materialVersion;
    private String materialReceiptHash;
    private String materialManifestHash;
    private String sourceSnapshotHash;
    private Long materialCreationEventId;
    private String materialCreationEventHash;
    private String associationPayloadJson;
    private String associationHash;
    private Long submissionAuditEventId;
    private String submissionAuditEventHash;
}
