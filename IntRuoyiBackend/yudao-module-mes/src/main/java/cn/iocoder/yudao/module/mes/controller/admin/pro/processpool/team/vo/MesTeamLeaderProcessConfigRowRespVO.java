package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES 生产组长统一工序配置行 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderProcessConfigRowRespVO {

    private Long routeId;
    private String routeCode;
    private String routeName;
    private Long routeProcessId;
    private Long processId;
    private String processCode;
    private String processName;
    private Integer sort;
    private List<LossReason> lossReasons;
    private List<Device> devices;

    @Data
    @Accessors(chain = true)
    public static class LossReason {
        private Long id;
        private String reasonCode;
        private String reasonName;
        private Boolean enabled;
    }

    @Data
    @Accessors(chain = true)
    public static class Device {
        private Long bindingId;
        private Long deviceId;
        private String deviceCode;
        private String deviceName;
        private String deviceStatus;
        private Boolean mapped;
        private List<Parameter> parameters;
    }

    @Data
    @Accessors(chain = true)
    public static class Parameter {
        private Long ruleId;
        private String parameterCode;
        private String parameterName;
        private String unit;
        private String valueType;
        private BigDecimal lowerLimit;
        private BigDecimal targetValue;
        private BigDecimal upperLimit;
        private Boolean enabled;
        private BigDecimal actualAverage;
        private Integer sampleCount;
        private LocalDateTime statisticsStartTime;
        private LocalDateTime statisticsEndTime;
        private Integer statisticsWindowDays;
    }
}
