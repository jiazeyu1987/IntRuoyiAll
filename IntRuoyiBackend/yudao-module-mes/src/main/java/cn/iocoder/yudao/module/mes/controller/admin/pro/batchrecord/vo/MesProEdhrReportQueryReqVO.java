package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - eDHR 报表查询 Request VO")
@Data
public class MesProEdhrReportQueryReqVO {

    @Schema(description = "报表定义编号", example = "1")
    private Long reportDefinitionId;

    @Schema(description = "报表编码", example = "PRODUCTION_TRACE")
    @NotBlank(message = "报表编码不能为空")
    private String reportCode;

    @Schema(description = "筛选快照JSON")
    private String filterSnapshotJson;
}
