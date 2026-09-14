package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DccControlledFileMetadataUpdateReqVO {

    private Long assignmentId;

    private String changeReason;

    private Long productMasterId;

    private String productName;

    private Long dccProjectCodeId;

    private Boolean needTraining;

    private Long fileTypeTaxonomyId;

    private String fileTypeLevel1;

    private String fileTypeLevel2;

    private String fileTypeLevel3;

    private String fileTypeLevel4;

    private String fileTypeLevel5;

    @NotBlank(message = "fileName is required")
    private String fileName;

    private String productCode;

    private String fileNumber;

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    private Long directoryId;

}
