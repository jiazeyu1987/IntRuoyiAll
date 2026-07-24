package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - DCC 受控文件上传历史名称选项 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileUploadNameOptionRespVO {

    @Schema(description = "历史文件名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "SOP-001")
    private String fileName;

    @Schema(description = "当前版本号", example = "V1.0")
    private String currentVersionNo;
}
