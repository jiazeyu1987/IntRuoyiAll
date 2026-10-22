package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventRelationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventRelationMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GxpAuditQueryServiceImpl implements GxpAuditQueryService {

    @Resource
    private GxpAuditEventMapper auditEventMapper;
    @Resource
    private GxpAuditEventRelationMapper relationMapper;

    @Override
    public PageResult<GxpAuditEventDO> page(GxpAuditEventPageQuery query) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (query.getScopeType() != null && query.getScopeId() != null) {
            String scopeId = String.valueOf(query.getScopeId());
            List<Long> eventIds = relationMapper.selectEventIdsByTargets(
                    tenantId, List.of(query.getScopeType()), scopeId);
            List<String> subjectIds = List.of(
                    query.getScopeType() + ":" + scopeId,
                    "MES_" + query.getScopeType() + ":" + scopeId);
            return auditEventMapper.selectPageByScope(tenantId, query, eventIds, subjectIds);
        }
        return auditEventMapper.selectPage(tenantId, query);
    }

    @Override
    public GxpAuditEventDO get(Long eventId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return get(tenantId, eventId);
    }

    public GxpAuditEventDO get(Long tenantId, Long eventId) {
        Long currentTenantId = TenantContextHolder.getRequiredTenantId();
        if (!currentTenantId.equals(tenantId)) {
            return null;
        }
        return auditEventMapper.selectByTenantIdAndId(currentTenantId, eventId);
    }

    @Override
    public List<GxpAuditEventRelationDO> listRelations(Long eventId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return relationMapper.selectListByTenantIdAndEventId(tenantId, eventId);
    }
}
