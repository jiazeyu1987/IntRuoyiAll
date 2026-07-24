package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class DccControlledFileLocalFolderImportBatchReqVO {

    @NotEmpty(message = "relativePaths is required")
    private List<String> relativePaths;

    @NotEmpty(message = "files is required")
    private MultipartFile[] files;
}
