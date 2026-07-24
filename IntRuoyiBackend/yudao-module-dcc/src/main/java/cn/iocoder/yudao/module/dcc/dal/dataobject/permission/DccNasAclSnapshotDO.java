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

@TableName("dcc_nas_acl_snapshot")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccNasAclSnapshotDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long transferTaskId;
    private String snapshotKey;
    private String server;
    private String share;
    private String rootPathsJson;
    private String status;
    private String normalizationVersion;
    private Long totalDirectoryCount;
    private Long snapshottedDirectoryCount;
    private Long failedDirectoryCount;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String failureCode;
    private String failureMessage;

}
