package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Data
public class DccControlledFileHistoricalImportReqVO {
    @NotNull @Positive private Long dccProjectCodeId;
    @NotNull @Positive private Long projectFolderId;
    @NotNull @Positive private Long categoryId;
    @NotNull @Positive private Long fileTypeTaxonomyId;
    @NotBlank @Size(max=128) private String fileNumber;
    @NotBlank @Size(max=64) private String versionNo;
    @NotBlank @Size(max=100) private String idempotencyKey;
    @NotNull private MultipartFile file;
    private List<@NotNull MultipartFile> attachments;
}
