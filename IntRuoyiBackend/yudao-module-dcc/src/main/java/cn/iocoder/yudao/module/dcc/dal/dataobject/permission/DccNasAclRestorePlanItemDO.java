package cn.iocoder.yudao.module.dcc.dal.dataobject.permission;

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

@TableName("dcc_nas_acl_restore_plan_item")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccNasAclRestorePlanItemDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long planId;
    private Long directorySnapshotId;
    private Long transferTaskItemId;
    private Long dccDirectoryId;
    private Long dccCategoryId;
    private Long sourceDescriptorId;
    private String plannedOperationsHash;
    private String plannedOperationsJson;
    private String status;
    private String blockReason;
    private String expectedAfterHash;
    private String actualAfterHash;
    private LocalDateTime verifiedAt;

}
