package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DccControlledFileLocalFolderImportSessionCreateReqVO {

    @NotNull(message = "templateCategoryId is required")
    private Long templateCategoryId;

    @NotNull(message = "dccProjectCodeId is required")
    private Long dccProjectCodeId;

    private Long productMasterId;

    @NotNull(message = "effectiveDate is required")
    private LocalDate effectiveDate;

    @NotBlank(message = "rootDirectoryName is required")
    private String rootDirectoryName;

    @NotNull(message = "expectedFileCount is required")
    @Positive(message = "expectedFileCount must be positive")
    private Long expectedFileCount;

    @NotNull(message = "expectedTotalBytes is required")
    @PositiveOrZero(message = "expectedTotalBytes must be zero or positive")
    private Long expectedTotalBytes;
}
