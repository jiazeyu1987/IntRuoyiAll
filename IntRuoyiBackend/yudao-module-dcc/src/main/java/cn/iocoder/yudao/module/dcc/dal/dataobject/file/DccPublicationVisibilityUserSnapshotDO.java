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

@TableName("dcc_publication_visibility_user_snapshot")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccPublicationVisibilityUserSnapshotDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long batchId;
    private Long ruleSnapshotId;
    private Long userId;
    private String userNameSnapshot;
    private Long deptIdSnapshot;
    private String deptNameSnapshot;
    private Integer userStatusSnapshot;
    private String resolutionReason;
    private String assignmentScopeResult;
}
