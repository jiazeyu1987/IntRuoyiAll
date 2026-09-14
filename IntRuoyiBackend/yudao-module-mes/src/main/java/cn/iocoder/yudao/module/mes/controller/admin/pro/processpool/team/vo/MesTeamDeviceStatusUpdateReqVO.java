package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 班组设备状态更新 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamDeviceStatusUpdateReqVO {

    @Schema(description = "班组设备编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "7001")
    @NotNull
    private Long deviceId;

    @Schema(description = "设备状态：ENABLED/REPAIRING/DISABLED", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "REPAIRING")
    @NotBlank
    private String deviceStatus;
}
