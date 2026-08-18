package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.QuickFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - MES 排产工单待同步差异分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProScheduleOrderAdmissionDiffPageReqVO extends PageParam {

    @Schema(description = "生产工单编码", example = "MO-001")
    private String workOrderCode;

    @Schema(description = "产品编码", example = "ITEM-001")
    private String productCode;

    @Schema(description = "产品名称", example = "球囊扩张管")
    private String productName;

    @Schema(description = "规格型号", example = "S012010-4")
    private String productSpecification;

    @Schema(description = "生产数量区间")
    private BigDecimal[] quantity;

    @Schema(description = "生产工单状态", example = "1")
    private Integer status;

    @Schema(description = "差异状态", example = "READY_TO_ADMIT")
    private String admissionStatus;

    @Schema(description = "原因码", example = "BLOCKED_MISSING_ROUTE")
    private String reasonCode;

    @Schema(description = "不可排原因关键字", example = "产品未绑定")
    private String message;

    @Schema(description = "建议处理角色", example = "工艺维护")
    private String ownerRole;

    @Schema(description = "需求日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] requestDate;

    @Schema(description = "快速过滤")
    private QuickFilter quickFilter;

}
