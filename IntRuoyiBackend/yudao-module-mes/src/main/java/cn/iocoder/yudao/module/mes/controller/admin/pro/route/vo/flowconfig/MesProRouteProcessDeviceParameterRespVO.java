package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - MES 工艺路线工序设备参数配置 Response VO")
@Data
@Accessors(chain = true)
public class MesProRouteProcessDeviceParameterRespVO {

    @Schema(description = "路线工序编号")
    private Long routeProcessId;

    @Schema(description = "工序编号")
    private Long processId;

    @Schema(description = "工序名称")
    private String processName;

    @Schema(description = "设备列表")
    private List<MesProRouteProcessDeviceParameterDeviceRespVO> devices;
}
