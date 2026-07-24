package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 第三方报工待归属 Response VO")
@Data
public class MesProFeedbackImportRecordRespVO {

    private Long id;
    private String attributionStatus;
    private String sourceFileName;
    private String sheetName;
    private Integer rowNo;
    private String taskCode;
    private String workOrderCode;
    private String itemCode;
    private String itemName;
    private String specification;
    private String processCode;
    private String processName;
    private BigDecimal feedbackQuantity;
    private LocalDateTime feedbackTime;
    private String feedbackUserCode;
    private String feedbackUserName;
    private String approverName;
    private Long feedbackUserId;
    private String feedbackUserNickname;
    private Long approveUserId;
    private String approveUserNickname;
    private Long scheduleOrderId;
    private Long scheduleOrderProcessId;
    private String attributionTargetType;
    private Long feedbackId;
    private LocalDateTime attributionTime;
    private Integer candidateCount;
    private BigDecimal surplusPoolQuantity;
    private Boolean canModifyAttribution;
    private String modifyBlockedReason;
    private Integer linkedFeedbackCount;
    private Boolean generatedFeedbackDraft;
    private Integer linkedFeedbackStatus;
    private String remark;
}
