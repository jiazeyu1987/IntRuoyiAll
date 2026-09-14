package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "管理后台 - DCC 受控文件任务动作就绪度阻塞项 Response VO")
@Data
@AllArgsConstructor
public class DccControlledFileTaskReadinessBlockerRespVO {

    @Schema(description = "阻塞原因编码", example = "STAMPED_PDF_REQUIRED")
    private String reasonCode;

    @Schema(description = "阻塞原因说明", example = "请上传盖章 PDF")
    private String message;

}
