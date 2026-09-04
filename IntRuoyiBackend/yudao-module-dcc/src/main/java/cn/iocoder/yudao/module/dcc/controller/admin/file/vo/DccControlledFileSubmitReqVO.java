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

    private String originalUploadTicket;

    private String sourceUploadTicket;

    private String drawingPdfUploadTicket;

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

    private Long fileTypeTaxonomyId;

    private Long revisionTargetControlledFileId;

    private List<Long> relatedControlledFileIds;

    private Boolean needTraining;

    private String processType;

    @NotBlank(message = "changeType is required")
    private String changeType;

    private List<Long> selectedSignoffUserIds;

    @NotBlank(message = "fileName is required")
    private String fileName;

    @NotBlank(message = "fileNumber is required")
    private String fileNumber;

    @NotNull(message = "directoryId is required")
    private Long directoryId;

    @NotBlank(message = "versionNo is required")
    private String versionNo;

    @NotNull(message = "effectiveDate is required")
    private LocalDate effectiveDate;

    private String remark;
}
