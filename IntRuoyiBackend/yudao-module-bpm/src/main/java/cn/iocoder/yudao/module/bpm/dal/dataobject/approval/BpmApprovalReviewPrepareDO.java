package cn.iocoder.yudao.module.bpm.dal.dataobject.approval;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.time.LocalDateTime;

/** Append-only facts: deliberately no logical-delete or mutable base record fields. */
@Data
@TableName("bpm_approval_review_prepare")
public class BpmApprovalReviewPrepareDO {
    @TableId private Long id;
    private Long tenantId;
    private Long actorId;
    private String prepareId;
    private String ownerCode;
    private String sourceTaskType;
    private String sourceTaskId;
    private String processInstanceId;
    private String subjectType;
    private String subjectId;
    private String contentPolicyVersion;
    private String subjectVersion;
    private String previewContentHash;
    private String scopeCode;
    private String canonicalBusinessContentJson;
    private String prepareRecordHash;
    private LocalDateTime preparedAt;
    private LocalDateTime expiresAt;
}
