package cn.iocoder.yudao.module.system.dal.dataobject.user;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 后台用户密码历史。
 */
@TableName("system_user_password_history")
@KeySequence("system_user_password_history_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserPasswordHistoryDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long userId;

    private String passwordHash;

    private LocalDateTime changedAt;

    private String sourceType;

}
