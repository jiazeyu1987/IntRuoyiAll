package cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("dcc_project_product_create_request")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccProjectProductCreateRequestDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String projectName;
    private String projectCode;
    private String projectLeader;
    private Long projectLeaderUserId;
    private String defaultAttributesJson;
    private Long folderTemplateId;
    private String folderTemplateSnapshotJson;
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
    private Long configuredReviewerUserId;
    private String configuredReviewerUsername;
    private String configuredReviewerNickname;
    private Long approverUserId;
    private String reviewReason;
    private String approvalReason;
    private String rejectReason;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String writeErrorCode;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String writeErrorMessage;
    private Long generatedProjectCodeId;
    private Long generatedProductCatalogId;
    private Long relationId;
    private Long previousRequestId;
    @TableField(exist = false)
    private Long resubmittedRequestId;
    private LocalDateTime submittedTime;
    private LocalDateTime reviewedTime;
    private LocalDateTime approvedTime;
    private LocalDateTime completedTime;
    private LocalDateTime failedTime;
}
