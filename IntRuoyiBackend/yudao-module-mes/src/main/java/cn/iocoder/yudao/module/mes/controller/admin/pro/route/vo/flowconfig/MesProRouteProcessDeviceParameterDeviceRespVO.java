package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - MES 工艺路线工序设备参数设备 Response VO")
@Data
@Accessors(chain = true)
public class MesProRouteProcessDeviceParameterDeviceRespVO {

    @Schema(description = "设备编号")
    private Long deviceId;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "设备状态")
    private String deviceStatus;

    @Schema(description = "参数规则")
    private List<MesProRouteDeviceParameterRespVO> parameters;
}
