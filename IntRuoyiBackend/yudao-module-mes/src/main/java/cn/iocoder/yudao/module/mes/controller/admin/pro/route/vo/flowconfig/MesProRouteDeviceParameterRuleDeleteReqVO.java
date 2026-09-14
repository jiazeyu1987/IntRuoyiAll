package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES 候选路线工序设备参数规则删除 Request VO")
@Data
public class MesProRouteDeviceParameterRuleDeleteReqVO {

    @NotNull
    private Long routeVersionId;
    @NotBlank
    private String expectedRouteSnapshotSha256;
    @NotNull
    private Long routeProcessId;
    @NotNull
    private Long deviceId;
    @NotBlank
    private String parameterCode;
}
