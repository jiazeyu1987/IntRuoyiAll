package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - DCC 项目代码与产品目录联合新建申请 Response VO")
@Data
public class DccProjectProductCreateRespVO {

    private Long id;
    private String defaultAttributesJson;
    private String folderTemplateSnapshotJson;
    private String projectName;
    private String projectCode;
    private String projectLeader;
    private Long projectLeaderUserId;
    private Long folderTemplateId;
    private String productCode;
    private String productName;
    private String classification;
    private String remark;
    private String creationReason;
    private Integer writeAttemptNo;
    private String writeReason;
    private Long writeOperatorUserId;
    private String status;
    private Long applicantUserId;
    private Long reviewerUserId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long configuredReviewerUserId;
    private String configuredReviewerUsername;
    private String configuredReviewerNickname;
    private Long approverUserId;
    private String reviewReason;
    private String approvalReason;
    private String rejectReason;
    private String writeErrorCode;
    private String writeErrorMessage;
    private Long generatedProjectCodeId;
    private Long generatedProductCatalogId;
    private Long relationId;
    private Long previousRequestId;
    private Long resubmittedRequestId;
    private LocalDateTime submittedTime;
    private LocalDateTime reviewedTime;
    private LocalDateTime approvedTime;
    private LocalDateTime completedTime;
    private LocalDateTime failedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
