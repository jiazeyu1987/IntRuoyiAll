package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DCC controlled file revision.
 */
@TableName("dcc_controlled_file")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileDO extends BaseDO {

    @TableId
    private Long id;
    private Long tenantId;
    private Long masterId;
    private Long categoryId;
    private Long directoryId;
    private Long sourceFileId;
    private Long originalFileId;
    private Long drawingPdfFileId;
    private Long trainingRecordFileId;
    private Long publishedFileId;
    private Long stampedFileId;
    private String fileName;
    private String title;
    private String fileNumber;
    private Long fileOwnerUserId;
    private String fileOwnerUsernameSnapshot;
    private String fileOwnerNicknameSnapshot;
    private Long fileOwnerSignatureId;
    private String fileOwnerApprovalTaskId;
    private String fileOwnerProcessInstanceId;
    private LocalDateTime fileOwnerSelectedTime;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long productMasterId;
    private String productSource;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long productCatalogId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long productRelationId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long productCreateRequestId;
    private String productCode;
    private String productName;
    private Long dccProjectCodeId;
    private String projectCodeRecognitionType;
    private String projectCodeRecognitionText;
    private Long projectCodeRecognizedBy;
    private LocalDateTime projectCodeRecognizedTime;
    private Long fileTypeTaxonomyId;
    private String fileTypeLevel1;
    private String fileTypeLevel2;
    private String fileTypeLevel3;
    private String fileTypeLevel4;
    private String fileTypeLevel5;
    private Boolean needTraining;
    private String processType;
    private String changeType;
    private String versionNo;
    /** Actual formal submission intent; null on unclassified historical/working rows. */
    private String revisionChangeType;
    private Integer revisionAttemptNo;
    private Long reworkPredecessorControlledFileId;
    private Long revisionSourceControlledFileId;
    private String revisionSourceVersionNo;
    private Long selectedIterationControlledFileId;
    private String selectedIterationVersionNo;
    /** Exact original source filename, including case and extension. */
    private String sourceOriginalFileName;
    private String revisionCode;
    private Integer iterationNo;
    private Long predecessorControlledFileId;
    private Long revisionBaseActiveControlledFileId;
    private String sourceSha256;
    private String previousSourceSha256;
    private String changeDescription;
    private LocalDate effectiveDate;
    private String remark;
    private String status;
    private Long submitterId;
    private Long requesterId;
    private String processInstanceId;
    private String processDefinitionKey;
    private String creationIdempotencyKey;
    private String creationPayloadHash;
    private String submitIdempotencyKey;
    private String submitPayloadHash;
    private LocalDateTime submittedTime;
    private LocalDateTime approvedTime;
    private LocalDateTime publishedTime;
    private LocalDateTime controlledTime;
    private LocalDateTime activatedTime;
    private LocalDateTime distributedTime;
    private String distributionPayloadHash;
    private LocalDateTime rejectedTime;
    private LocalDateTime stampedTime;
    private Long obsoletedBy;
    private LocalDateTime obsoletedTime;
    private String obsoleteReason;
    private Long supersededByFileId;
    private String rejectReason;
    private String finalizationError;
    private Long checkedOutBy;
    private LocalDateTime checkedOutTime;
    private String checkedOutReason;

}
