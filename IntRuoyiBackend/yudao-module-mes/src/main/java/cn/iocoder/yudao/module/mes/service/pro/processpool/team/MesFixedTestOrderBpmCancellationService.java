package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCancelReqVO;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesFixedTestOrderDownstreamCleanupMapper;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/** Internal fixed-test reset operation, called before deleting the business rows used by synchronous callbacks. */
@Service
public class MesFixedTestOrderBpmCancellationService {
    private final MesFixedTestOrderDownstreamCleanupMapper mapper;
    private final BpmProcessInstanceService processInstanceService;

    public MesFixedTestOrderBpmCancellationService(MesFixedTestOrderDownstreamCleanupMapper mapper,
            BpmProcessInstanceService processInstanceService) {
        this.mapper = mapper;
        this.processInstanceService = processInstanceService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long tenantId, List<Long> batchIds, List<Long> executionIds, Long actorUserId) {
        if (tenantId == null || tenantId <= 0 || !Objects.equals(tenantId, TenantContextHolder.getTenantId())
                || batchIds == null || executionIds == null
                || batchIds.stream().anyMatch(id -> id == null || id <= 0)
                || executionIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("FIXED_TEST_RESET_BPM_SCOPE_INVALID");
        }
        List<String> processIds = mapper.selectProcessInstanceIds(tenantId, batchIds, executionIds);
        if (processIds == null || processIds.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new IllegalStateException("FIXED_TEST_RESET_BPM_QUERY_INVALID");
        }
        for (String processId : processIds) {
            ProcessInstance running = processInstanceService.getProcessInstance(processId);
            if (running == null) continue; // Formal completed history remains intact; no runtime work exists.
            if (!String.valueOf(tenantId).equals(running.getTenantId())) {
                throw new IllegalStateException("FIXED_TEST_RESET_BPM_TENANT_MISMATCH");
            }
            if (actorUserId == null || actorUserId <= 0) {
                throw new IllegalStateException("FIXED_TEST_RESET_BPM_AUTHENTICATED_ACTOR_REQUIRED");
            }
            BpmProcessInstanceCancelReqVO request = new BpmProcessInstanceCancelReqVO();
            request.setId(processId);
            request.setReason("固定测试订单重置：取消本轮审批流程");
            processInstanceService.cancelProcessInstanceByAdmin(actorUserId, request);
            if (processInstanceService.getProcessInstance(processId) != null) {
                throw new IllegalStateException("FIXED_TEST_RESET_BPM_CANCEL_NOT_COMPLETED");
            }
        }
    }
}
