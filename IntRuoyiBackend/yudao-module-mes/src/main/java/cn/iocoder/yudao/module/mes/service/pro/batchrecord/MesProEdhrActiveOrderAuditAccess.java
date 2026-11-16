package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ACTIVE_ORDER_SOURCE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionErrorCodeConstants.*;

/** Batch audit access for the formal active-order chain, which has no independent form tasks. */
@Service
public class MesProEdhrActiveOrderAuditAccess {
    @Resource private PermissionApi permissionApi;
    @Resource private MesProEdhrBatchExecutionMapper batchMapper;
    @Resource private MesProEdhrBatchExecutionOriginMapper originMapper;
    @Resource private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Resource private MesProEdhrBatchExecutionVisibilityService visibilityService;

    public void requireAuditView(Long batchId) {
        Long actor = SecurityFrameworkUtils.getLoginUserId();
        Long tenant = TenantContextHolder.getRequiredTenantId();
        if (actor == null || batchId == null || batchId <= 0) {
            throw exception(PRO_EDHR_PERMISSION_CONTEXT_MISSING);
        }
        if (!permissionApi.hasAnyPermissions(actor, "mes:pro-edhr-operation-audit:query")
                || !permissionApi.hasAnyPermissions(actor, "mes:pro-edhr-batch-execution:query")) {
            throw exception(PRO_EDHR_OBJECT_PERMISSION_DENIED, "BATCH_EXECUTION:" + batchId + ":AUDIT_VIEW");
        }
        var batch = batchMapper.selectById(batchId);
        if (batch == null || Boolean.TRUE.equals(batch.getDeleted()) || !Objects.equals(tenant, batch.getTenantId())
                || batch.getWorkOrderId() == null || batch.getRouteId() == null || batch.getRouteVersionId() == null) {
            throw exception(PRO_EDHR_BATCH_ACTIVE_ORDER_SOURCE_INVALID);
        }
        var origins = originMapper.selectListByBatchExecutionId(batchId);
        if (origins.isEmpty()) {
            throw exception(PRO_EDHR_PERMISSION_SCOPE_REQUIRED, "BATCH_EXECUTION:" + batchId);
        }
        Long activeId = origins.get(0).getActiveOrderId();
        if (activeId == null || activeId <= 0 || origins.stream().anyMatch(origin ->
                Boolean.TRUE.equals(origin.getDeleted())
                        || !Objects.equals(tenant, origin.getTenantId())
                        || !Objects.equals(batchId, origin.getBatchExecutionId())
                        || !Objects.equals(batch.getWorkOrderId(), origin.getWorkOrderId())
                        || !Objects.equals(activeId, origin.getActiveOrderId())
                        || !MesProEdhrBatchTraceFormalSourceResolver.isActiveOrderEntryType(origin.getEntryType())
                        || !"CAPTURED".equals(origin.getRelationStatus()))) {
            throw exception(PRO_EDHR_BATCH_ACTIVE_ORDER_SOURCE_INVALID);
        }
        // Historical orders remain addressable by their persisted origin, never by a matching order code.
        var active = activeOrderMapper.selectByIdIgnoreDeleted(activeId);
        if (active == null || active.getLeaderUserId() == null
                || !Objects.equals(tenant, active.getTenantId())
                || !Objects.equals(batch.getWorkOrderId(), active.getWorkOrderId())
                || !Objects.equals(batch.getRouteId(), active.getRouteId())
                || !Objects.equals(batch.getRouteVersionId(), active.getRouteVersionId())) {
            throw exception(PRO_EDHR_BATCH_ACTIVE_ORDER_SOURCE_INVALID);
        }
        // Reuse batch visibility, including overview/holder/release candidates and revoked handovers.
        visibilityService.requireVisibleBatch(batch, actor);
    }
}
