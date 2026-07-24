package cn.iocoder.yudao.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - NAS 目录树读取 Request VO")
@Data
public class FileNasDirectoryTreeReqVO {

    @Schema(description = "服务器可访问的本地目录或 UNC/NAS 路径", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "\\\\nas\\share\\quality")
    @NotBlank(message = "目录路径不能为空")
    private String path;
}
