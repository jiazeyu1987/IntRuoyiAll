package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 生产组长路线工序设备绑定保存 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamProcessDeviceBindingSaveReqVO {

    @Schema(description = "路线工序编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "5001")
    @NotNull
    private Long routeProcessId;

    @Schema(description = "班组设备编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "7001")
    @NotNull
    private Long deviceId;
}
