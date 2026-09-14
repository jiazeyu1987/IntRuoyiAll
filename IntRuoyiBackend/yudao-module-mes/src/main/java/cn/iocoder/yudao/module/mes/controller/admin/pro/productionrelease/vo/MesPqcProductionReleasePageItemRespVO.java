package cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES PQC 生产放行分页项 Response VO")
@Data
@Accessors(chain = true)
public class MesPqcProductionReleasePageItemRespVO {

    private Long applicationId;
    private Long pqcReleaseWorkTaskId;
    private Integer version;
    private String viewStatus;
    private String applicationStatus;
    private Long activeOrderId;
    private Long workOrderId;
    private String workOrderCode;
    private String batchCode;
    private Long productId;
    private Long batchExecutionId;
    private LocalDateTime appliedAt;
    private Long appliedBy;
    private LocalDateTime decidedAt;
    private Long decidedBy;
    private Boolean underReview;
    private Long nonconformanceReviewId;
    private String nonconformanceDisposition;
    private String nonconformanceReason;
    private LocalDateTime nonconformanceClosedAt;
    private Boolean approvalReady;
    private String approvalBlockerReason;
    private String approvalBlockerSuggestion;
}
