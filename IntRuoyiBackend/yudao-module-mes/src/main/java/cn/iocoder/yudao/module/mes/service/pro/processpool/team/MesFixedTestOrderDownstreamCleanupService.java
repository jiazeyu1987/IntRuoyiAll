package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesFixedTestOrderDownstreamCleanupMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import jakarta.annotation.Resource;

/** Called only after the owning reset has locked and verified the exact fixed work order. */
@Service
public class MesFixedTestOrderDownstreamCleanupService {
    private final MesFixedTestOrderDownstreamCleanupMapper mapper;
    private final MesProEdhrWorkTaskService workTaskService;
    @Resource
    private MesFixedTestOrderBpmCancellationService bpmCancellationService;

    public MesFixedTestOrderDownstreamCleanupService(MesFixedTestOrderDownstreamCleanupMapper mapper,
            MesProEdhrWorkTaskService workTaskService) {
        this.mapper = mapper;
        this.workTaskService = workTaskService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void cleanup(Long tenantId, Long workOrderId, List<Long> activeOrderIds, List<Long> batchIds,
                        List<Long> executionIds, List<Long> releaseApplicationIds) {
        if (tenantId == null || tenantId <= 0 || !Objects.equals(tenantId, TenantContextHolder.getTenantId())
                || workOrderId == null || workOrderId <= 0) {
            throw new IllegalArgumentException("FIXED_TEST_RESET_DOWNSTREAM_SCOPE_INVALID");
        }
        requireIds(activeOrderIds);
        requireIds(batchIds);
        requireIds(executionIds);
        requireIds(releaseApplicationIds);
        if (mapper.countOwnershipConflicts(tenantId, workOrderId, activeOrderIds, batchIds,
                executionIds, releaseApplicationIds) != 0) {
            throw new IllegalStateException("FIXED_TEST_RESET_DOWNSTREAM_OWNERSHIP_CONFLICT");
        }
        // Formal BPM cancellation dispatches synchronous domain callbacks while their owners still exist.
        // Read tasks afterwards so callback-created or callback-cancelled tasks are included in revocation.
        bpmCancellationService.cancel(tenantId, batchIds, executionIds, SecurityFrameworkUtils.getLoginUserId());
        List<Long> taskIds = mapper.selectWorkTaskIdsForUpdate(tenantId, workOrderId, batchIds,
                executionIds, releaseApplicationIds);
        List<Long> deviationIds = mapper.selectDeviationIdsForUpdate(tenantId, workOrderId, batchIds);
        requireIds(taskIds);
        requireIds(deviationIds);
        // The formal service preserves terminal decisions and revokes each task's permission source.
        // A failure must escape before deletes and roll back the caller's entire reset transaction.
        if (!taskIds.isEmpty()) {
            workTaskService.cancelTasksForTestReset(taskIds, "固定测试订单重置：取消本轮待办并撤销任务权限");
        }
        if (!deviationIds.isEmpty()) {
            mapper.deleteDeviationHandling(tenantId, deviationIds);
            mapper.deleteDeviationRequests(tenantId, deviationIds);
            mapper.deleteDeviations(tenantId, deviationIds);
        }
        mapper.deleteNonconformanceReviewsForWorkOrder(tenantId, workOrderId);
        if (!taskIds.isEmpty()) {
            mapper.deleteWorkTasks(tenantId, taskIds);
        }
    }

    private static void requireIds(List<Long> ids) {
        if (ids == null || ids.stream().anyMatch(id -> id == null || id <= 0)
                || ids.stream().distinct().count() != ids.size()) {
            throw new IllegalArgumentException("FIXED_TEST_RESET_DOWNSTREAM_IDENTITIES_INVALID");
        }
    }
}
