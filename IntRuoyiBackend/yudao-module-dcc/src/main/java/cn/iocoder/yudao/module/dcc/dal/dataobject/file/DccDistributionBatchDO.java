package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@TableName("dcc_distribution_batch")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccDistributionBatchDO extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long sourceEventId;
    private String eventKey;
    private String eventType;
    private Long masterId;
    private Long controlledFileId;
    private String approvalProcessInstanceId;
    private String versionNo;
    private LocalDateTime sourceOccurredAt;
    private String departmentIdsJson;
    private LocalDateTime snapshotAt;
    private String snapshotPolicyVersion;
    private String snapshotPayloadHash;
    private String eventPayloadHash;
    private String status;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime failureAt;
    private LocalDateTime dispatchPreparedAt;
    private LocalDateTime sentAt;
    private Long rowVersion;
}
