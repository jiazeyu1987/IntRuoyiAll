package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - MES 工序池提交事件时间轴分页 Request VO")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProcessPoolTimelinePageReqVO extends PageParam {

    @Schema(description = "提交日期；不传时查询全部提交时间", example = "2026-07-30")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate submitDate;

    @Schema(description = "实际填写员工编号", example = "2001")
    private Long employeeUserId;

    @Schema(description = "实际填写员工编号集合", hidden = true)
    private Set<Long> employeeUserIds;

    @Schema(description = "工序编号", example = "6001")
    private Long processId;

    @Schema(description = "工序编号集合", hidden = true)
    private Set<Long> processIds;

    @Schema(description = "设备编号", example = "9001")
    private Long deviceId;

    @Schema(description = "模板类型", example = "PRODUCTION")
    private String templateType;

    @Schema(description = "工序池事件类型", hidden = true)
    private String eventType;

    @Schema(description = "是否只查询完成数量为正数的事件", hidden = true)
    private Boolean requirePositiveOutputQuantity;

    public String getEventType() {
        return eventType;
    }

    public ProcessPoolTimelinePageReqVO setEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    @Schema(description = "生产工单编号", example = "30001")
    private Long workOrderId;

    @Schema(description = "生产工单编码", example = "WO-20260730001")
    private String workOrderCode;

    @Schema(description = "产品编号", example = "91001")
    private Long productId;

    @Schema(description = "产品编码或名称关键字", example = "PP-88")
    private String productKeyword;

    @Schema(description = "PQC 检验类型", example = "PATROL")
    private String inspectionType;

    @Schema(description = "PQC 检验轮次", example = "2")
    private Integer roundNo;

    @Schema(description = "提交复核状态", example = "REJECTED")
    private String submissionReviewStatus;

    @Schema(description = "生产报工分配视图：WORKBENCH-待处理，HISTORY-全部历史", hidden = true)
    private String allocationView;

    @Schema(description = "提交时间起点", hidden = true)
    private LocalDateTime submittedAtStart;

    @Schema(description = "提交时间终点", hidden = true)
    private LocalDateTime submittedAtEnd;

}
