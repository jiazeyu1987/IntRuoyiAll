package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesPqcProductionReleasePageItem {

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
}
