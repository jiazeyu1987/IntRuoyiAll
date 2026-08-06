package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 生产工单异常标记并上报 Request VO")
@Data
@Accessors(chain = true)
public class MesWorkOrderAbnormalReportReqVO {

    @Schema(description = "生产工单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "30001")
    @NotNull
    private Long workOrderId;

    @Schema(description = "异常说明", requiredMode = Schema.RequiredMode.REQUIRED, example = "损耗异常")
    @NotBlank
    private String abnormalDescription;
}
