package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Builder
@Accessors(chain = true)
public class MesProcessDeviceParameterRuleSaveReqBO {

    private Long leaderUserId;
    private Long routeProcessId;
    private Long processId;
    private Long deviceId;
    private String parameterCode;
    private String parameterName;
    private BigDecimal lowerLimit;
    private BigDecimal upperLimit;
    private String valueType;
}
