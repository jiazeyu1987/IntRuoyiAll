package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrDeviationHandlingSaveReqVO {

    @NotNull
    private Integer expectedContentVersion;
    @Size(max = 1000)
    private String revisionReason;
    private LocalDateTime investigationStartedAt;
    private LocalDateTime plannedCompletedAt;
    private LocalDateTime completedAt;
    @Size(max = 4000)
    private String investigationMembersJson;
    @Size(max = 4000)
    private String rootCauseAnalysis;
    @Size(max = 4000)
    private String impactScope;
    @Size(max = 4000)
    private String riskAssessment;
    @Size(max = 4000)
    private String productDisposition;
    @Size(max = 64)
    private String nonconformanceReviewCode;
    private Long correctiveOwnerId;
    @Size(max = 128)
    private String correctiveOwnerName;
    private LocalDateTime correctiveDueAt;
    private Boolean capaRequired;
    @Size(max = 64)
    private String capaCode;
    @Size(max = 4000)
    private String capaAttachmentsJson;
    @Size(max = 32)
    private String handlingConclusion;
    @Size(max = 32)
    private String verificationResult;
    @Size(max = 4000)
    private String verificationContent;
}
