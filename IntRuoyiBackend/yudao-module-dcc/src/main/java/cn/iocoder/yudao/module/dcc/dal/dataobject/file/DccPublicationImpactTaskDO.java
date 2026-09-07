package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("dcc_publication_impact_task")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccPublicationImpactTaskDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long batchId;
    private Long publicationRelationSnapshotId;
    private Long publishedControlledFileId;
    private Long relatedMasterId;
    private Long relatedActiveControlledFileId;
    private String relatedFileNumberSnapshot;
    private String relatedFileNameSnapshot;
    private String relatedVersionNoSnapshot;
    private Long assigneeUserId;
    private String assigneeUserNameSnapshot;
    private String taskStatus;
    private String decision;
    private String decisionReason;
    private Long decidedBy;
    private LocalDateTime decidedAt;
    private String revisionTrackingStatus;
    private Long linkedRevisionControlledFileId;
    private String linkedRevisionVersionSnapshot;
    private LocalDateTime resolvedAt;
    private Integer rowVersion;
    private String creationToken;
}
