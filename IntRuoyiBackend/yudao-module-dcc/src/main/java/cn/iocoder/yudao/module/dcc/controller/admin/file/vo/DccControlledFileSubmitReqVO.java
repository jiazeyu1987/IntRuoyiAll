package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DccControlledFileSubmitReqVO {
    private cn.iocoder.yudao.module.dcc.service.file.DccApplicationCustomMatrix applicationCustomMatrix;
    private String expectedApplicationCustomMatrixDigest;
    private String applicationCustomMatrixToken;
    private String applicationCustomMatrixPhase;

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    private String sessionId;

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;

    @JsonIgnore
    @Schema(hidden = true)
    private String submitPayloadHash;

    @JsonIgnore
    @Schema(hidden = true)
    private String creationPayloadHash;

    /** Required PDF used for online read-only browsing. */
    private String readOnlyUploadTicket;

    /** Optional editable source used only by the editable download endpoint. */
    private String editableUploadTicket;

    private String originalUploadTicket;

    private String sourceUploadTicket;

    private String drawingPdfUploadTicket;

    private List<DccControlledFileAttachmentUploadTicketReqVO> attachmentUploadTickets;

    @JsonIgnore
    @Schema(hidden = true)
    private Long originalFileId;

    @JsonIgnore
    @Schema(hidden = true)
    private Long readOnlyFileId;

    @JsonIgnore
    @Schema(hidden = true)
    private Long editableFileId;

    @JsonIgnore
    @Schema(hidden = true)
    private Long sourceFileId;

    private String sourceFileName;

    @JsonIgnore
    @Schema(hidden = true)
    private Long drawingPdfFileId;

    private Long productMasterId;

    private String productCode;

    private Long dccProjectCodeId;

    @Schema(description = "Selected project logical folder; independent of directoryId storage identity")
    private Long projectFolderId;

    @Schema(description = "User reason for placing this file in the selected project folder")
    private String projectFolderChangeReason;

    private Long fileTypeTaxonomyId;

    private Long revisionTargetControlledFileId;

    /** Explicit A/2 (or another iteration) selected as the source for a new major revision. */
    private Long revisionSourceControlledFileId;

    private String revisionSourceReason;

    private List<Long> relatedControlledFileIds;

    private Boolean needTraining;

    private String processType;

    @NotBlank(message = "changeType is required")
    private String changeType;
    private String revisionChangeType;

    private List<Long> selectedSignoffUserIds;
    /** This application's department list; the workflow validates and freezes it. */
    private List<Long> selectedSignoffDepartmentIds;
    private List<Long> selectedDistributionDepartmentIds;
    /** User-selected actual attributes; the default source is read from the project by the server. */
    private cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes projectAttributes;

    @NotBlank(message = "fileName is required")
    private String fileName;

    @NotBlank(message = "fileNumber is required")
    private String fileNumber;

    private Long directoryId;
    @JsonIgnore
    @Schema(hidden=true)
    private boolean directoryIdProvided;
    public void setDirectoryId(Long directoryId) { this.directoryId=directoryId;this.directoryIdProvided=true; }

    @NotBlank(message = "versionNo is required")
    private String versionNo;

    private LocalDate effectiveDate;
    private String effectiveMode;
    private Long expectedApprovalRouteId;
    private Integer expectedApprovalRouteVersionNo;
    private String expectedApprovalMatrixDigest;
    private String approvalMatrixBaselineToken;
    private String approvalMatrixConfirmationToken;
    private Boolean matrixChangeConfirmed;
    private String applicationContextKey;
    private String executionContract;
    private String expectedProcessDefinitionId;
    private String expectedDefinitionDigest;
    private String expectedSupervisorSourceDigest;
    private String supervisorSourceToken;

    @JsonIgnore
    @AssertTrue(message="主管来源期待摘要和凭证须完整同给，或全部不提供")
    public boolean isSupervisorSourceExpectationComplete() {
        if (expectedSupervisorSourceDigest == null && supervisorSourceToken == null) return true;
        return expectedSupervisorSourceDigest != null && expectedSupervisorSourceDigest.matches("[a-f0-9]{64}")
                && supervisorSourceToken != null && !supervisorSourceToken.isEmpty() && supervisorSourceToken.length() <= 131072
                && supervisorSourceToken.chars().noneMatch(value -> value <= 0x20 || value == 0x7f);
    }

    @JsonIgnore
    @AssertTrue(message="请明确生效方式；按预设日期生效须填写日期，标准化后生效不填写预设日期", groups=DccSubmissionValidationGroup.class)
    public boolean isEffectiveSelectionValid() {
        if ("EXTERNAL_REVIEW".equals(processType)) return effectiveDate != null;
        return cn.iocoder.yudao.module.dcc.service.file.DccApplicationEffectiveSelection.isComplete(effectiveMode,effectiveDate);
    }

    @JsonIgnore
    @AssertTrue(message="外来文件评审须填写生效日期")
    public boolean isExternalReviewDateValid() {
        return !"EXTERNAL_REVIEW".equals(processType) || effectiveDate != null;
    }

    private String remark;
}
