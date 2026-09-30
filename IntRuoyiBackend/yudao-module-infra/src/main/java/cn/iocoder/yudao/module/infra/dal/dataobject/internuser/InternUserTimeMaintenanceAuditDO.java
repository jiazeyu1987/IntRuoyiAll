package cn.iocoder.yudao.module.infra.dal.dataobject.internuser;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("intern_user_time_maintenance_audit")
@Data
@EqualsAndHashCode(callSuper = true)
public class InternUserTimeMaintenanceAuditDO extends BaseDO {

    @TableId
    private Long id;
    private Long tenantId;
    private String targetType;
    private Long targetId;
    private String targetName;
    private String fieldName;
    private LocalDateTime oldTime;
    private LocalDateTime newTime;
    private Long operatorUserId;

}
