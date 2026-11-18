package cn.iocoder.yudao.module.bpm.api.task;

import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCancelReqVO;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalRequestStore;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

/**
 * Flowable 流程实例 Api 实现类
 *
 * @author 瑛泰源码
 * @author jason
 */
@Service
@Validated
public class BpmProcessInstanceApiImpl implements BpmProcessInstanceApi {

    @Resource
    private BpmProcessInstanceService processInstanceService;

    @Resource
    private BusinessApprovalRequestStore businessApprovalRequestStore;

    @Override
    public String createProcessInstance(Long userId, @Valid BpmProcessInstanceCreateReqDTO reqDTO) {
        return processInstanceService.createProcessInstance(userId, reqDTO);
    }

    @Override
    public void cancelProcessInstance(Long userId, String processInstanceId, String reason) {
        BpmProcessInstanceCancelReqVO cancelReqVO = new BpmProcessInstanceCancelReqVO();
        cancelReqVO.setId(processInstanceId);
        cancelReqVO.setReason(reason);
        processInstanceService.cancelProcessInstanceByStartUser(userId, cancelReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelBusinessApprovalProcess(Long actorUserId, String processInstanceId,
                                             String expectedExecutorCode, String expectedObjectId, String reason) {
        if (actorUserId == null || actorUserId <= 0 || processInstanceId == null || processInstanceId.isBlank()
                || expectedExecutorCode == null || expectedExecutorCode.isBlank()
                || expectedObjectId == null || expectedObjectId.isBlank() || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("业务审批作废缺少操作者、流程归属或原因");
        }
        var request = businessApprovalRequestStore.findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new IllegalStateException("业务审批作废请求不存在"));
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (request.getRequestId() == null || request.getRequestId() <= 0 || request.getContext() == null
                || !Objects.equals(tenantId, request.getTenantId())
                || !Objects.equals(tenantId, request.getContext().getTenantId())
                || !Objects.equals(processInstanceId, request.getProcessInstanceId())
                || !Objects.equals(expectedExecutorCode, request.getEffectExecutorCode())
                || !Objects.equals(expectedObjectId, request.getContext().getObjectId())
                || request.getStatus() != BusinessApprovalRequestStatus.PENDING_BPM) {
            throw new IllegalStateException("业务审批作废请求归属或状态不一致");
        }
        processInstanceService.cancelProcessInstanceByBusiness(actorUserId, processInstanceId,
                "BUSINESS_APPROVAL:" + request.getRequestId(), request.getProcessDefinitionKey(), reason);
    }

}
