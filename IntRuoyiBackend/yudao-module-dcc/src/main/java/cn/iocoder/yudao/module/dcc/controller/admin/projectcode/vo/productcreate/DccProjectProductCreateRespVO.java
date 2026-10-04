package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - DCC 项目代码与产品目录联合新建申请 Response VO")
@Data
public class DccProjectProductCreateRespVO {

    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long id;
    private String defaultAttributesJson;
    private String folderTemplateSnapshotJson;
    private String projectName;
    private String projectCode;
    private String projectLeader;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long projectLeaderUserId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long folderTemplateId;
    private String productCode;
    private String productName;
    private String classification;
    private String remark;
    private String creationReason;
    private Integer writeAttemptNo;
    private String writeReason;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long writeOperatorUserId;
    private String status;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long applicantUserId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long reviewerUserId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long configuredReviewerUserId;
    private String configuredReviewerUsername;
    private String configuredReviewerNickname;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long approverUserId;
    private String reviewReason;
    private String approvalReason;
    private String rejectReason;
    private String writeErrorCode;
    private String writeErrorMessage;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long generatedProjectCodeId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long generatedProductCatalogId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long relationId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long previousRequestId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long resubmittedRequestId;
    private LocalDateTime submittedTime;
    private LocalDateTime reviewedTime;
    private LocalDateTime approvedTime;
    private LocalDateTime completedTime;
    private LocalDateTime failedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
