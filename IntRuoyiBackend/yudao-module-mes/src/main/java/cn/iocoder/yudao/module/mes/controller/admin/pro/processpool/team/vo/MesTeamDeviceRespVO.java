package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 生产组长班组设备 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamDeviceRespVO {

    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private String deviceStatus;
    private Boolean enabled;
}
