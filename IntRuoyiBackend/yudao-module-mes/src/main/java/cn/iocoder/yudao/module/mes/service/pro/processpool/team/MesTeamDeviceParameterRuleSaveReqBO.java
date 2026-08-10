package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@Accessors(chain = true)
public class MesTeamDeviceParameterRuleSaveReqBO {

    private Long leaderUserId;
    private Long routeProcessId;
    private Long deviceId;
    private String parameterCode;
    private String parameterName;
    private String unit;
    private BigDecimal lowerLimit;
    private BigDecimal upperLimit;
    private BigDecimal targetValue;
    private String valueType;
    private String standardText;
    private List<String> optionValues;
    private String defaultText;
    private Integer decimalScale;
}
