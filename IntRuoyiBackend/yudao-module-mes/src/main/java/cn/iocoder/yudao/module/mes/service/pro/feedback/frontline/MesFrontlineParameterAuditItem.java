package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class MesFrontlineParameterAuditItem {

    private Integer readingIndex;
    private Long deviceId;
    private String parameterCode;
    private String parameterName;
    private String unit;
    private BigDecimal value;
    private String textValue;
    private BigDecimal lowerLimit;
    private BigDecimal upperLimit;
    private String parameterStatus;
    private String resolutionStatus;
    private String reasonCode;
    private String snapshotSource;
}
