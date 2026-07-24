package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - MES 批记录执行归档分页 Request VO")
@Data
public class MesProBatchRecordExecutionArchivePageReqVO extends PageParam {

    @Schema(description = "执行记录编号", example = "9")
    private Long executionId;

    @Schema(description = "生产工单编号", example = "1001")
    private Long workOrderId;

    @Schema(description = "生产工单编码", example = "MO001")
    private String workOrderCode;

    @Schema(description = "批次号", example = "BATCH001")
    private String batchCode;

    @Schema(description = "归档类型：PDF、EXCEL", example = "PDF")
    private String artifactType;

    @Schema(description = "归档状态：GENERATING、SEALED、FAILED", example = "SEALED")
    private String archiveStatus;

    @Schema(description = "生成开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime generatedTimeStart;

    @Schema(description = "生成结束时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime generatedTimeEnd;
}
