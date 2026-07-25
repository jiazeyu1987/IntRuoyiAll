package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmTaskCompletedReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmTaskCreatedReqVO;
import org.flowable.task.api.Task;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FormCenterBpmEventBridge {

    private final FormCenterRuntimeService runtimeService;

    public FormCenterBpmEventBridge(@Lazy FormCenterRuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    public void onTaskCreated(Task task) {
        Long assignee = parseAssignee(task);
        if (assignee == null) {
            return;
        }
        FormBpmTaskCreatedReqVO reqVO = new FormBpmTaskCreatedReqVO();
        reqVO.setProcessInstanceId(task.getProcessInstanceId());
        reqVO.setTaskId(task.getId());
        reqVO.setHandlerUserIds(List.of(assignee));
        runtimeService.onBpmTaskCreated(reqVO);
    }

    public void onTaskAssigned(Task task) {
        onTaskCreated(task);
    }

    public void onTaskCompleted(Task task) {
        FormBpmTaskCompletedReqVO reqVO = new FormBpmTaskCompletedReqVO();
        reqVO.setProcessInstanceId(task.getProcessInstanceId());
        reqVO.setTaskId(task.getId());
        runtimeService.onBpmTaskCompleted(reqVO);
    }

    private Long parseAssignee(Task task) {
        if (task.getAssignee() == null || task.getAssignee().isBlank()) {
            return null;
        }
        return Long.valueOf(task.getAssignee());
    }

}
