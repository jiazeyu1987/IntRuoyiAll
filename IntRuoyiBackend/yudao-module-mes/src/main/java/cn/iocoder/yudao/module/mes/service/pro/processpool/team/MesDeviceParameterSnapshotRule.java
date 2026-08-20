package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesDeviceParameterSnapshotRule {

    private Long routeProcessId;
    private Long processId;
    private Long deviceId;
    private String parameterCode;
    private String parameterName;
    private String unit;
    private BigDecimal lowerLimit;
    private BigDecimal upperLimit;
    private BigDecimal defaultValue;
    private String valueType;
    private String standardText;
    private String optionValuesJson;
    private String defaultText;
    private Integer decimalScale;
}
