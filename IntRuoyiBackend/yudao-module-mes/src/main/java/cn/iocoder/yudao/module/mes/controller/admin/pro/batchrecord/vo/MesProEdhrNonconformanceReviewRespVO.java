package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrNonconformanceReviewRespVO {

    private Long id;

    private String reviewCode;

    private String sourceType;

    private Long sourceId;

    private Long batchExecutionId;

    private String batchExecutionCode;

    private Long workOrderId;

    private String workOrderCode;

    private String batchCode;

    private Integer previousBatchStatus;

    private String reviewStatus;

    private String nonconformanceReason;

    private String reviewMaterialUrl;

    private String reviewOpinion;

    private String qaSignature;

    private Long qaUserId;

    private LocalDateTime frozenAt;

    private LocalDateTime closedAt;

    private LocalDateTime unfrozenAt;

    private LocalDateTime voidedAt;

    private String disposition;

    private String traceSnapshotJson;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
