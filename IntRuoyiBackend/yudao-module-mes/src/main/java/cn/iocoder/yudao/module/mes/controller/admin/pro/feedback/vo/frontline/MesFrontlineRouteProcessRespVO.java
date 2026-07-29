package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 一线设备账号可切换工序 Response VO")
@Data
public class MesFrontlineRouteProcessRespVO {

    @Schema(description = "工艺路线编号")
    private Long routeId;
    @Schema(description = "工艺路线编码")
    private String routeCode;
    @Schema(description = "工艺路线名称")
    private String routeName;
    @Schema(description = "工艺路线工序编号")
    private Long routeProcessId;
    @Schema(description = "工序编号")
    private Long processId;
    @Schema(description = "工序编码")
    private String processCode;
    @Schema(description = "工序名称")
    private String processName;
    @Schema(description = "工艺路线工序序号")
    private Integer sort;
    @Schema(description = "设备编号")
    private Long deviceId;
    @Schema(description = "设备编码")
    private String deviceCode;
    @Schema(description = "设备名称")
    private String deviceName;
    @Schema(description = "工作站编号")
    private Long workstationId;
    @Schema(description = "工作站编码")
    private String workstationCode;
    @Schema(description = "工作站名称")
    private String workstationName;

}
