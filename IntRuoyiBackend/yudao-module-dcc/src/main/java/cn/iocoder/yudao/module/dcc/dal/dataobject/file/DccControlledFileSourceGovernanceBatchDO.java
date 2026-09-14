package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TableName("dcc_controlled_file_source_governance_batch")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileSourceGovernanceBatchDO extends BaseDO {

    @TableId
    private Long id;
    private String taskKey;
    private String tenantScopeJson;
    private String tenantScopeSha256;
    private Long snapshotMaxControlledFileId;
    private Long effectiveControlledFileCount;
    private String ruleVersion;
    private String schemaVersion;
    private String manifestSha256;
    private String requestSha256;
    private String batchStatus;
    private Long confirmedBy;
    private java.time.LocalDateTime confirmedTime;
    private Long completedCount;
    private Long blockedCount;
    private Long failedCount;
}
