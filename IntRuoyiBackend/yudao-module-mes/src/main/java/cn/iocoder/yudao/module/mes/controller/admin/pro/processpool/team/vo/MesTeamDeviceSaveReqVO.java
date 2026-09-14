package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 班组设备保存 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamDeviceSaveReqVO {

    @Schema(description = "设备编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "D-001")
    @NotBlank
    private String deviceCode;

    @Schema(description = "设备名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "压力泵")
    @NotBlank
    private String deviceName;

    @Schema(description = "设备状态：ENABLED/REPAIRING/DISABLED", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "ENABLED")
    @NotBlank
    private String deviceStatus;
}
