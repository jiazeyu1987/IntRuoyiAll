package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmProcessApprovedReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmProcessCancelledReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmProcessRejectedReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmTaskCompletedReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmTaskCreatedReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import org.flowable.task.api.Task;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class FormCenterBpmEventBridge implements ApplicationListener<BpmProcessInstanceStatusEvent> {

    private static final String FORM_ACTION_BUSINESS_KEY_PREFIX = "FORM_ACTION:";

    private final FormActionInstanceMapper actionInstanceMapper;
    private final FormCenterRuntimeService runtimeService;

    public FormCenterBpmEventBridge(FormActionInstanceMapper actionInstanceMapper,
            FormCenterRuntimeService runtimeService) {
        this.actionInstanceMapper = actionInstanceMapper;
        this.runtimeService = runtimeService;
    }

    public void onTaskCreated(Task task) {
        dispatchTaskCreated(task, false);
    }

    public void onTaskAssigned(Task task) {
        dispatchTaskCreated(task, true);
    }

    public void onTaskCompleted(Task task) {
        if (!hasFormActionInstance(task.getProcessInstanceId())) {
            return;
        }
        FormBpmTaskCompletedReqVO reqVO = new FormBpmTaskCompletedReqVO();
        reqVO.setProcessInstanceId(task.getProcessInstanceId());
        reqVO.setTaskId(task.getId());
        runtimeService.onBpmTaskCompleted(reqVO);
    }

    @Override
    public void onApplicationEvent(BpmProcessInstanceStatusEvent event) {
        onProcessInstanceStatusChanged(event);
    }

    public void onProcessInstanceStatusChanged(BpmProcessInstanceStatusEvent event) {
        if (!hasFormActionInstance(event.getId())) {
            return;
        }
        if (BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(event.getStatus())) {
            FormBpmProcessApprovedReqVO reqVO = new FormBpmProcessApprovedReqVO();
            reqVO.setProcessInstanceId(event.getId());
            runtimeService.onBpmProcessApproved(reqVO);
            return;
        }
        if (BpmProcessInstanceStatusEnum.REJECT.getStatus().equals(event.getStatus())) {
            FormBpmProcessRejectedReqVO reqVO = new FormBpmProcessRejectedReqVO();
            reqVO.setProcessInstanceId(event.getId());
            reqVO.setReason(event.getReason());
            runtimeService.onBpmProcessRejected(reqVO);
            return;
        }
        if (BpmProcessInstanceStatusEnum.CANCEL.getStatus().equals(event.getStatus())) {
            FormBpmProcessCancelledReqVO reqVO = new FormBpmProcessCancelledReqVO();
            reqVO.setProcessInstanceId(event.getId());
            reqVO.setReason(event.getReason());
            runtimeService.onBpmProcessCancelled(reqVO);
        }
    }

    private void dispatchTaskCreated(Task task, boolean requireHandler) {
        if (!hasFormActionInstance(task.getProcessInstanceId())) {
            return;
        }
        List<Long> handlerUserIds = resolveHandlerUserIds(task);
        if (handlerUserIds.isEmpty() && !requireHandler) {
            return;
        }
        FormBpmTaskCreatedReqVO reqVO = new FormBpmTaskCreatedReqVO();
        reqVO.setProcessInstanceId(task.getProcessInstanceId());
        reqVO.setTaskId(task.getId());
        reqVO.setHandlerUserIds(handlerUserIds);
        runtimeService.onBpmTaskCreated(reqVO);
    }

    private boolean hasFormActionInstance(String processInstanceId) {
        if (StrUtil.isBlank(processInstanceId)) {
            return false;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return false;
        }
        FormActionInstanceDO instance = actionInstanceMapper.selectByProcessInstanceId(tenantId, processInstanceId);
        return instance != null;
    }

    private List<Long> resolveHandlerUserIds(Task task) {
        Set<Long> userIds = new LinkedHashSet<>();
        addUserId(userIds, task.getAssignee());
        addUserId(userIds, task.getOwner());
        return List.copyOf(userIds);
    }

    private void addUserId(Set<Long> userIds, String rawUserId) {
        if (StrUtil.isBlank(rawUserId)) {
            return;
        }
        try {
            userIds.add(Long.valueOf(rawUserId));
        } catch (NumberFormatException ex) {
            throw new FormCenterException(FormCenterErrorCode.BPM_ACTIVE_TASK_ASSIGNEE_MISSING,
                    "BPM active task handler is not a numeric user id: " + rawUserId);
        }
    }
}
