package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES 班组工序设备参数上下限保存 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamDeviceParameterRuleSaveReqVO {

    @Schema(description = "路线工序编号", example = "5001")
    private Long routeProcessId;

    @Schema(description = "工序编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "6001")
    @NotNull
    private Long processId;

    @Schema(description = "设备编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "7001")
    @NotNull
    private Long deviceId;

    @Schema(description = "参数编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "pressure")
    @NotBlank
    private String parameterCode;

    @Schema(description = "参数名称", example = "压力")
    private String parameterName;

    @Schema(description = "参数单位", example = "MPa")
    private String unit;

    @Schema(description = "下限", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    @NotNull
    private BigDecimal lowerLimit;

    @Schema(description = "上限", requiredMode = Schema.RequiredMode.REQUIRED, example = "40")
    @NotNull
    private BigDecimal upperLimit;

    @Schema(description = "默认值", example = "30")
    private BigDecimal defaultValue;

    @Schema(description = "值类型", example = "DECIMAL")
    private String valueType;
}
