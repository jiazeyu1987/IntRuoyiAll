package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - MES 生产组长路线工序设备参数标准保存 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamDeviceParameterRuleSaveReqVO {

    @Schema(description = "路线工序编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "5001")
    @NotNull
    private Long routeProcessId;

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

    @Schema(description = "下限；文本标准为空", example = "20")
    private BigDecimal lowerLimit;

    @Schema(description = "上限；文本标准为空", example = "40")
    private BigDecimal upperLimit;

    @Schema(description = "目标值；范围标准和文本标准可为空", example = "30")
    private BigDecimal targetValue;

    @Schema(description = "值类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "DECIMAL")
    @NotBlank
    private String valueType;

    @Schema(description = "参数标准原文", requiredMode = Schema.RequiredMode.REQUIRED, example = "20-30%")
    @NotBlank
    @Size(max = 1000)
    private String standardText;

    @Schema(description = "下拉选项；值类型为 SELECT 时必填", example = "[\"自来水\", \"纯净水\"]")
    private List<String> optionValues;

    @Schema(description = "文本默认值；值类型为 SELECT 时用于默认选项", example = "自来水")
    private String defaultText;

    @Schema(description = "小数位数；DECIMAL 可配置", example = "1")
    private Integer decimalScale;
}
