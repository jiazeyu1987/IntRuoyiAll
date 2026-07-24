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

@TableName("dcc_nas_acl_directory_snapshot")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccNasAclDirectorySnapshotDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long snapshotId;
    private Long transferTaskId;
    private Long transferTaskItemId;
    private Long dccDirectoryId;
    private Long parentSnapshotId;
    private Integer depth;
    private String nasPath;
    private String pathHash;
    private String itemName;
    private Long descriptorId;
    private String collectStatus;
    private String failureCode;
    private String failureMessage;

}
