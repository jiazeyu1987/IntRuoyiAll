package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.QuickFilter;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderCompletionFilterEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - MES 排产工单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProScheduleOrderPageReqVO extends PageParam {

    @Schema(description = "排产工单编码", example = "SCH-20260610-0001")
    private String code;

    @Schema(description = "ERP 工单编码", example = "881MO090880")
    private String erpWorkOrderCode;

    @Schema(description = "生产工单编号", example = "100")
    private Long workOrderId;

    @Schema(description = "产品编号", example = "200")
    private Long productId;

    @Schema(description = "当前工序编号", example = "300")
    private Long currentProcessId;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "完成筛选", example = "INCOMPLETE")
    @InEnum(value = MesProScheduleOrderCompletionFilterEnum.class, message = "完成筛选必须是 {value}")
    private String completionFilter;

    @Schema(description = "差异状态", example = "0")
    private Integer diffStatus;

    @Schema(description = "承诺交期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] promiseDate;

    @Schema(description = "导出列")
    private List<String> exportColumns;

    @Schema(description = "排序字段", example = "priorityNo")
    @Pattern(regexp = "priorityNo", message = "排序字段必须是 priorityNo")
    private String sortField;

    @Schema(description = "排序方向", example = "asc")
    @Pattern(regexp = "asc|desc", message = "排序方向必须是 asc 或 desc")
    private String sortOrder;

    @Schema(description = "快速过滤")
    private QuickFilter quickFilter;

}
