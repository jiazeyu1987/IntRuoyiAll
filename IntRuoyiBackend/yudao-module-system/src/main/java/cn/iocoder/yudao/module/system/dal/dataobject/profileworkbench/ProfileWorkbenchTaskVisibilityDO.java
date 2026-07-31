package cn.iocoder.yudao.module.system.dal.dataobject.profileworkbench;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("system_profile_workbench_task_visibility")
@KeySequence("system_profile_workbench_task_visibility_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ProfileWorkbenchTaskVisibilityDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long userId;

    private String taskKey;

    private String taskType;

    private String source;

    private String businessId;

    private String detail;

    private LocalDateTime hiddenAt;
}
