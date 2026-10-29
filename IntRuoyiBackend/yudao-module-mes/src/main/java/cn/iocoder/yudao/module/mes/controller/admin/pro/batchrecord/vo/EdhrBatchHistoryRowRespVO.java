package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import java.time.LocalDateTime;

/** 历史追溯列表投影；任务、权限动作和表单证据由详情接口按需读取。 */
@Data
public class EdhrBatchHistoryRowRespVO {
    private Long id;
    private String batchExecutionCode;
    private String workOrderCode;
    private String batchCode;
    private String productName;
    private Integer status;
    private LocalDateTime startedAt;
    private LocalDateTime pqcReleasedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long releaseTransactionId;
    private String releaseStatus;
    private LocalDateTime releaseApprovedAt;
}
