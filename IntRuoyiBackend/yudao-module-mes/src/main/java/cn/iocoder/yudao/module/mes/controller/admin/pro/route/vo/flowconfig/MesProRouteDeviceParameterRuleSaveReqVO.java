package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - MES 工艺路线工序设备参数规则保存 Request VO")
@Data
@Accessors(chain = true)
public class MesProRouteDeviceParameterRuleSaveReqVO {

    @Schema(description = "路线工序编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long routeProcessId;

    @Schema(description = "设备编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long deviceId;

    @Schema(description = "参数编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String parameterCode;

    @Schema(description = "参数名称")
    private String parameterName;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "参数标准原文", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String standardText;

    @Schema(description = "下限")
    private BigDecimal lowerLimit;

    @Schema(description = "上限")
    private BigDecimal upperLimit;

    @Schema(description = "目标值")
    private BigDecimal targetValue;

    @Schema(description = "值类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String valueType;

    @Schema(description = "下拉选项")
    private List<String> optionValues;

    @Schema(description = "默认文本")
    private String defaultText;

    @Schema(description = "小数位数")
    private Integer decimalScale;
}
