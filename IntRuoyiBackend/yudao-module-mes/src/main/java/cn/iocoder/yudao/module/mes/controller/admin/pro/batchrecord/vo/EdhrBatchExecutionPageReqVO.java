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

    private String batchExecutionCode;

    private Long workOrderId;

    private String workOrderCode;

    private String batchCode;

    private String productCode;

    private Long routeId;

    private String routeCode;

    private Integer status;

    private List<Integer> statuses;

    private List<Integer> excludeStatuses;

    private Boolean excludeReleased;

    private Boolean completedTraceOnly;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    private QuickFilter quickFilter;
}
