package cn.iocoder.yudao.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - NAS 连接测试 Response VO")
@Data
@Accessors(chain = true)
public class FileNasConfigTestRespVO {

    @Schema(description = "NAS 共享根 UNC 路径", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "\\\\172.30.30.4\\it共享")
    private String rootPath = "";

    @Schema(description = "根目录条目数", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    private Integer itemCount = 0;

    @Schema(description = "测试结果提示", requiredMode = Schema.RequiredMode.REQUIRED, example = "连接成功")
    private String message = "";
}
