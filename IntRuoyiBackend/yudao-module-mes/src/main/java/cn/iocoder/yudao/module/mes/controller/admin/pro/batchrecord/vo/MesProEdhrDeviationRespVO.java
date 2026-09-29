package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrDeviationRespVO {

    private Long id;
    private String deviationCode;
    private Long batchExecutionId;
    private String batchExecutionCode;
    private String batchCode;
    private String level;
    private String status;
    private String description;
    private String discoveryDepartmentName;
    private String discovererName;
    private LocalDateTime discoveredAt;
    private String discoveryLocation;
    private String productName;
    private String productSpecification;
    private String equipmentOrSystem;
    private LocalDateTime reportedAt;
    private String receiverName;
    private String categoryCodesJson;
    private String emergencyAction;
    private String levelBasis;
    private String closeReason;
    private LocalDateTime closedAt;
    private Long nonconformanceReviewId;
    private String nonconformanceReviewCode;
    private String nonconformanceReviewStatus;
    private String nonconformanceDisposition;
    private LocalDateTime nonconformanceClosedAt;
    private LocalDateTime initiatedAt;
    private Long initiatorSignatureId;
    private String initiatorContentHash;
}
