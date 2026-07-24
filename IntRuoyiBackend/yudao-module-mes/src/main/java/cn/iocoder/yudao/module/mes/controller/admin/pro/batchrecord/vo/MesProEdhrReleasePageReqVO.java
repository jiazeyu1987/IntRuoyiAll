package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
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
public class MesProEdhrReleasePageReqVO extends PageParam {

    private String batchExecutionCode;

    private String workOrderCode;

    private String batchCode;

    private String productCode;

    private List<Integer> batchExecutionStatuses;

    private List<Integer> excludeBatchExecutionStatuses;

    private Boolean completedTraceOnly;

    private String releaseStatus;

    private String dhrStatus;

    private String inspectionStatus;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;
}
