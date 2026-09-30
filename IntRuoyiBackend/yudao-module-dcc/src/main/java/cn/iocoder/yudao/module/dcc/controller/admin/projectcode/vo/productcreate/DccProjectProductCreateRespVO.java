package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - DCC 项目代码与产品目录联合新建申请 Response VO")
@Data
public class DccProjectProductCreateRespVO {

    private Long id;
    private String projectName;
    private String projectCode;
    private String projectLeader;
    private String productCode;
    private String productName;
    private String classification;
    private String remark;
    private String status;
    private Long applicantUserId;
    private Long reviewerUserId;
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
    private LocalDateTime submittedTime;
    private LocalDateTime reviewedTime;
    private LocalDateTime approvedTime;
    private LocalDateTime completedTime;
    private LocalDateTime failedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
