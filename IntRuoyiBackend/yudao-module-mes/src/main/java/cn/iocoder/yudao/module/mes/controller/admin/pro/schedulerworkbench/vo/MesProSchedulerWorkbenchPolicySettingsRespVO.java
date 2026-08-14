package cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES 排产员工作台策略设置 Response VO")
@Data
public class MesProSchedulerWorkbenchPolicySettingsRespVO {

    @Schema(description = "ERP 工单同步时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "02:00")
    @NotBlank(message = "ERP工单同步时间不能为空")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "ERP工单同步时间必须为 HH:mm")
    private String erpWorkOrderSyncTime;

    @Schema(description = "自动重排时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "02:00")
    @NotBlank(message = "自动重排时间不能为空")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "自动重排时间必须为 HH:mm")
    private String nightlyReplanTime;

    @Schema(description = "排产优先级规则", requiredMode = Schema.RequiredMode.REQUIRED, example = "PROMISE_DATE")
    @NotBlank(message = "排产优先级规则不能为空")
    private String priorityRule;

    @Schema(description = "保护已报工任务", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "保护已报工任务不能为空")
    private Boolean protectReportedTasks;

    @Schema(description = "保护已完成任务", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "保护已完成任务不能为空")
    private Boolean protectCompletedTasks;

    @Schema(description = "保护手工锁定任务", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "保护手工锁定任务不能为空")
    private Boolean protectLockedTasks;

    @Schema(description = "默认智能排产用途启用状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "默认智能排产用途不能为空")
    private Boolean defaultScheduleUseEnabled;

    @Schema(description = "默认产能模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "RESOURCE_CALCULATED")
    @NotBlank(message = "默认产能模式不能为空")
    private String defaultScheduleCapacityMode;

    @Schema(description = "默认产能覆盖(产能/h)", example = "30")
    @DecimalMin(value = "0", inclusive = false, message = "默认产能覆盖必须大于 0")
    private BigDecimal defaultFiniteHourlyCapacity;

    @Schema(description = "默认无限产能公式数量系数 a(小时/件)", example = "0.02")
    @DecimalMin(value = "0", inclusive = false, message = "默认无限产能公式数量系数必须大于 0")
    private BigDecimal defaultInfiniteDurationQuantityFactorHours;

    @Schema(description = "默认无限产能公式固定值 b(小时)", example = "1")
    @DecimalMin(value = "0", message = "默认无限产能公式固定值不能小于 0")
    private BigDecimal defaultInfiniteDurationBaseHours;

    @Schema(description = "默认夜班启用状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "默认夜班设置不能为空")
    private Boolean defaultNightShiftEnabled;

    @Schema(description = "默认人工人数", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    @NotNull(message = "默认人工人数不能为空")
    @Min(value = 1, message = "默认人工人数必须大于 0")
    private Integer defaultWorkerQuantity;

    @Schema(description = "默认单人产能/h", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    @NotNull(message = "默认单人产能不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "默认单人产能必须大于 0")
    private BigDecimal defaultWorkerSingleHourlyCapacity;

    @Schema(description = "人工产能参数适用范围说明", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String workerCapacityApplicabilityText;

}
