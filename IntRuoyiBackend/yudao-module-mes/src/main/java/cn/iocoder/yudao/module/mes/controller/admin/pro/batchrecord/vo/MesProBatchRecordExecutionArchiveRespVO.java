package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - MES 批记录执行归档 Response VO")
@Data
public class MesProBatchRecordExecutionArchiveRespVO {

    private Long id;
    private Long executionId;
    private String archiveCode;
    private Integer archiveVersion;
    private String artifactType;
    private String archiveStatus;
    private Long fileId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String sha256;
    private String renderSourceVersion;
    private String executionSnapshotHash;
    private String cellValuesHash;
    private Long fieldAuditRevision;
    private String fieldAuditHeadHash;
    private String signatureHash;
    private Long approvalSnapshotId;
    private String approvalSnapshotHash;
    private Long generatedBy;
    private LocalDateTime generatedAt;
    private Long sealedBy;
    private LocalDateTime sealedAt;
    private Long sealSignatureId;
    private String failureReason;
    private String remark;
    private Map<String, Object> formSlotManifest;
    private Integer attachmentManifestCount;
    private String attachmentManifestHeadHash;
    private List<AttachmentManifestItem> attachmentManifest;
    private Boolean created;

    @Data
    public static class AttachmentManifestItem {
        private Long id;
        private String fieldKey;
        private String attachmentType;
        private String attachmentGroupKey;
        private String fileName;
        private String contentType;
        private Long fileSize;
        private String sha256;
        private String attachmentHash;
    }
}
