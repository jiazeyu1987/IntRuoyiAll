package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Data
public class DccControlledFileLocalFolderImportReqVO {

    @NotNull(message = "templateCategoryId is required")
    private Long templateCategoryId;

    private Long productMasterId;

    @NotNull(message = "effectiveDate is required")
    private LocalDate effectiveDate;

    @NotBlank(message = "rootDirectoryName is required")
    private String rootDirectoryName;

    @NotEmpty(message = "relativePaths is required")
    private List<String> relativePaths;

    @NotEmpty(message = "files is required")
    private MultipartFile[] files;
}
