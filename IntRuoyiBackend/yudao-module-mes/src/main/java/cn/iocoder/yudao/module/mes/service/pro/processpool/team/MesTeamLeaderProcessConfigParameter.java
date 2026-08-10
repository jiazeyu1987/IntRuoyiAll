package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesTeamLeaderProcessConfigParameter {

    private Long ruleId;
    private String parameterCode;
    private String parameterName;
    private String unit;
    private String valueType;
    private String standardText;
    private BigDecimal lowerLimit;
    private BigDecimal targetValue;
    private BigDecimal upperLimit;
    private List<String> optionValues;
    private String defaultText;
    private Integer decimalScale;
    private Boolean enabled;
    private BigDecimal actualAverage;
    private Integer sampleCount;
    private LocalDateTime statisticsStartTime;
    private LocalDateTime statisticsEndTime;
    private Integer statisticsWindowDays;
}
