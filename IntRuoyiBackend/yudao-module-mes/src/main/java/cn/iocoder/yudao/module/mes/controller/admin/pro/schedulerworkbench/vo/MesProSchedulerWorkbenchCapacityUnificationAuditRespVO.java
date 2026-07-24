package cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - MES 产能统一审计 Response VO")
@Data
public class MesProSchedulerWorkbenchCapacityUnificationAuditRespVO {

    @Schema(description = "审计开关是否开启")
    private Boolean enabled = true;

    @Schema(description = "历史小时产能配置数量")
    private Long legacyFiniteHourlyConfigCount = 0L;

    @Schema(description = "产能覆盖与资源计算不一致数量")
    private Long manualOverrideDiffCount = 0L;

    @Schema(description = "资源缺失数量")
    private Long resourceMissingCount = 0L;

    @Schema(description = "设备工序产能缺失数量")
    private Long machineryProcessCapacityMissingCount = 0L;

    @Schema(description = "审计问题总数")
    private Long totalIssueCount = 0L;

    @Schema(description = "审计问题列表")
    private List<Issue> issues = new ArrayList<>();

    @Schema(description = "产能统一审计问题")
    @Data
    public static class Issue {

        private String code;

        private String message;

        private Long routeScheduleConfigId;

        private Long routeProcessId;

        private String capacityMode;

        private BigDecimal manualHourlyCapacity;

        private BigDecimal resourceCapacityHourly;

        private String capacitySource;

        private Long workstationId;

        private String workstationCode;

        private Long machineryId;

        private String machineryCode;
    }
}
