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

    @Schema(description = "班组活跃订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8101")
    @NotNull
    private Long activeOrderId;

    @Schema(description = "异常原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "设备停机，影响生产")
    @NotBlank
    private String abnormalDescription;
}
