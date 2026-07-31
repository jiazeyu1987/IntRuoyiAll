package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DccControlledFileNasTransferReqVO {

    @NotEmpty(message = "selectedNasPaths is required")
    private List<String> selectedNasPaths;

    @NotNull(message = "templateCategoryId is required")
    private Long templateCategoryId;

    @NotNull(message = "dccProjectCodeId is required")
    private Long dccProjectCodeId;

    private Long productMasterId;

    @NotNull(message = "effectiveDate is required")
    private LocalDate effectiveDate;
}
