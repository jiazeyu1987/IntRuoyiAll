package cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("dcc_project_access_rule")
@Data
@EqualsAndHashCode(callSuper = true)
public class DccProjectAccessRuleDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long dccProjectCodeId;
    private String subjectType;
    private Long subjectId;
    private String accessLevel;
    private Boolean active;
    private LocalDateTime validFrom;
    private LocalDateTime expireTime;
    private String changeReason;

}
