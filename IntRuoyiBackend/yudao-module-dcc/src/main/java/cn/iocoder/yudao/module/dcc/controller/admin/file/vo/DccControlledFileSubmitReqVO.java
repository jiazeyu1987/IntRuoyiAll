package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DccControlledFileSubmitReqVO {

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

    private String originalUploadTicket;

    private String sourceUploadTicket;

    private String drawingPdfUploadTicket;

    private List<DccControlledFileAttachmentUploadTicketReqVO> attachmentUploadTickets;

    @JsonIgnore
    @Schema(hidden = true)
    private Long originalFileId;

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

    @NotNull(message = "effectiveDate is required")
    private LocalDate effectiveDate;

    private String remark;
}
