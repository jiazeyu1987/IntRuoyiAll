package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.QuickFilter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EdhrBatchExecutionPageReqVO extends PageParam {

    /** Batch list projection; task capabilities and close/archive checks remain on detail. */
    private Boolean summaryOnly;

    private List<Long> batchExecutionIds;

    private String batchExecutionCode;

    private Long workOrderId;

    private String workOrderCode;

    private String batchCode;

    private String productCode;

    private String productName;

    private Long routeId;

    private String routeCode;

    private Integer status;

    private List<Integer> statuses;

    private List<Integer> excludeStatuses;

    private Boolean excludeReleased;

    private Boolean releasedOnly;

    private Boolean completedTraceOnly;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] releaseApprovedTime;

    private String tracePerson;
    private String traceEquipment;
    private String traceMaterial;
    private String traceMaterialLot;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] traceBusinessTime;

    @jakarta.validation.constraints.AssertTrue(message = "业务时间必须包含有效的开始和结束时间，且开始时间不得晚于结束时间")
    public boolean isTraceBusinessTimeValid() {
        return traceBusinessTime == null || (traceBusinessTime.length == 2
                && traceBusinessTime[0] != null && traceBusinessTime[1] != null
                && !traceBusinessTime[0].isAfter(traceBusinessTime[1]));
    }

    private QuickFilter quickFilter;
}
