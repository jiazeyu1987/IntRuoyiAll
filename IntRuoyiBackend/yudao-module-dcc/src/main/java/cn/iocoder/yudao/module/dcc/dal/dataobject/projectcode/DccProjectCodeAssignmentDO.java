package cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode;

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

@TableName("dcc_project_code_assignment")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccProjectCodeAssignmentDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String assignmentNo;
    private Long projectCodeId;
    private String scopeMode;
    private Long assigneeUserId;
    private Long assignedBy;
    private LocalDateTime assignedTime;
    private LocalDateTime expireTime;
    private String status;
    private String assignmentReason;
    private Integer fileCount;
    private Integer changedFileCount;
    private Integer changedFieldCount;
    private Long revokedBy;
    private LocalDateTime revokedTime;
    private String revokeReason;

}
