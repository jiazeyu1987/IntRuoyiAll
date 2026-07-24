package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES eDHR 主数据追溯详情 Response VO")
@Data
@Accessors(chain = true)
public class MesProBatchRecordDomainTraceDetailRespVO {

    private Long executionId;
    private String executionCode;
    private String status;
    private Long domainTraceSnapshotId;
    private String domainTraceHash;
    private LocalDateTime verifiedAt;
    private Integer attachmentCount;
    private String attachmentChainStatus;
    private String attachmentChainHeadHash;
    private List<AttachmentSummary> attachmentSummaries;
    private List<Blocker> blockers;
    private List<Item> items;

    @Data
    @Accessors(chain = true)
    public static class Blocker {

        private String itemType;
        private String itemKey;
        private String blockerCode;
        private String blockerMessage;
    }

    @Data
    @Accessors(chain = true)
    public static class Item {

        private String itemType;
        private String itemKey;
        private String itemName;
        private Long sourceId;
        private String sourceCode;
        private String sourceVersion;
        private String snapshotJson;
        private String snapshotHash;
        private String status;
        private String blockerReason;
    }

    @Data
    @Accessors(chain = true)
    public static class AttachmentSummary {

        private Long id;
        private Long auditBatchId;
        private Long signatureId;
        private Long workTaskId;
        private Integer rowIndex;
        private Integer columnIndex;
        private String fieldKey;
        private String fieldPath;
        private String fieldLabel;
        private String attachmentType;
        private String attachmentGroupKey;
        private String attachmentAction;
        private Integer versionNo;
        private Long fileId;
        private String fileName;
        private String contentType;
        private Long fileSize;
        private String sha256;
        private String storageRetentionHash;
        private String previousAttachmentHash;
        private String attachmentHash;
        private Long operatorId;
        private String operatorName;
        private LocalDateTime operatedAt;
        private String reasonCategory;
        private String reasonText;
    }
}
