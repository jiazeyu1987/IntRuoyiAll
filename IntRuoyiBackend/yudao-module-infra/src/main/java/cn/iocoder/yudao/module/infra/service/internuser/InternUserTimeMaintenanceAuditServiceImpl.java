package cn.iocoder.yudao.module.infra.service.internuser;

import cn.hutool.core.lang.Assert;
import cn.iocoder.yudao.module.infra.dal.dataobject.internuser.InternUserTimeMaintenanceAuditDO;
import cn.iocoder.yudao.module.infra.dal.mysql.internuser.InternUserTimeMaintenanceAuditMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InternUserTimeMaintenanceAuditServiceImpl implements InternUserTimeMaintenanceAuditService {

    @Resource
    private InternUserTimeMaintenanceAuditMapper auditMapper;

    @Override
    public void recordTimeChange(Long tenantId, String targetType, Long targetId, String targetName,
                                 String fieldName, LocalDateTime oldTime, LocalDateTime newTime,
                                 Long operatorUserId) {
        Assert.notNull(targetType, "审计对象类型不能为空");
        Assert.notNull(targetId, "审计对象编号不能为空");
        Assert.notNull(fieldName, "审计字段不能为空");
        Assert.notNull(newTime, "审计新时间不能为空");

        InternUserTimeMaintenanceAuditDO audit = new InternUserTimeMaintenanceAuditDO();
        audit.setTenantId(tenantId);
        audit.setTargetType(targetType);
        audit.setTargetId(targetId);
        audit.setTargetName(targetName);
        audit.setFieldName(fieldName);
        audit.setOldTime(oldTime);
        audit.setNewTime(newTime);
        audit.setOperatorUserId(operatorUserId);
        auditMapper.insert(audit);
    }

    @Override
    public List<InternUserTimeMaintenanceAuditDO> getAuditList(String targetType, Long targetId) {
        Assert.notNull(targetType, "审计对象类型不能为空");
        Assert.notNull(targetId, "审计对象编号不能为空");
        return auditMapper.selectListByTarget(targetType, targetId);
    }

}
