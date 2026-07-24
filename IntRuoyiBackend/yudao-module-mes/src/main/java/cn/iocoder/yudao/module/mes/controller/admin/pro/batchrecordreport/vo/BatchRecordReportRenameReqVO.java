package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 电子批记录报表重命名 Request VO")
@Data
public class BatchRecordReportRenameReqVO {

    @Schema(description = "积木报表 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "reportId 不能为空")
    private String reportId;

    @Schema(description = "新的报表名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "reportName 不能为空")
    private String reportName;
}
