package cn.iocoder.yudao.module.system.dal.dataobject.permission;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("system_temporary_role_grant")
@KeySequence("system_temporary_role_grant_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class TemporaryRoleGrantDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long userId;

    private Long roleId;

    private String reason;

    private String status;

    private LocalDateTime applyTime;

    private Long applicantUserId;

    private String applicantUsername;

    private LocalDateTime approveTime;

    private Long approverUserId;

    private String approverUsername;

    private LocalDateTime effectiveTime;

    private LocalDateTime expireTime;

    private LocalDateTime revokeTime;

    private Long revokerUserId;

    private String revokerUsername;

    private String revokeReason;

}
