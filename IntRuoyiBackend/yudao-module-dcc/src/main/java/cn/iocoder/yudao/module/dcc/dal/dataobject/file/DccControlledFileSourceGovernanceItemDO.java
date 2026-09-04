package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("dcc_controlled_file_source_governance_item")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileSourceGovernanceItemDO extends BaseDO {

    @TableId
    private Long id;
    private Long batchId;
    private Long tenantId;
    private Long controlledFileId;
    private Long legacySourceFileId;
    private Long isolatedSourceFileId;
    private Long originSourceFileId;
    private Long snapshotSourceFileId;
    private String snapshotSourceSha256;
    private String snapshotLocationHash;
    private Long snapshotSourceConfigId;
    private String snapshotSourcePath;
    private Boolean snapshotSourceDeleted;
    private String sourceSha256;
    private String sharedGroupKey;
    private String governanceAction;
    private String itemStatus;
    private String blockerReasonCode;
    private String blockerDetail;
    private String lastError;
    private Long processedBy;
    private LocalDateTime processedTime;
}
