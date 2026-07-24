package cn.iocoder.yudao.module.bpm.service.task;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import jakarta.annotation.Resource;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.TASK_APPROVAL_REQUIRES_DCC_SIGNATURE;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.TASK_APPROVAL_REQUIRES_EDHR_SIGNATURE;

@Component
public class BpmTaskExternalSignatureGuard {

    static final String DCC_CONTROLLED_FILE_PROCESS_DEFINITION_KEY = "dcc-controlled-file-approval";
    static final String EDHR_BATCH_RECORD_PROCESS_DEFINITION_KEY = "mes-edhr-approval-v1";

    @Resource
    private BpmProcessDefinitionService processDefinitionService;
    @Resource
    private FormActionInstanceMapper formActionInstanceMapper;

    public void assertGenericApproveOrRejectAllowed(Task task) {
        assertGenericTaskMutationAllowed(task);
    }

    public void assertGenericTaskMutationAllowed(Task task) {
        if (task == null || StrUtil.isBlank(task.getProcessDefinitionId())) {
            return;
        }
        ProcessDefinition definition = processDefinitionService.getProcessDefinition(task.getProcessDefinitionId());
        if (definition == null) {
            return;
        }
        if (StrUtil.equals(definition.getKey(), DCC_CONTROLLED_FILE_PROCESS_DEFINITION_KEY)
                && !isFormCenterOwnedProcess(task)) {
            throw exception(TASK_APPROVAL_REQUIRES_DCC_SIGNATURE);
        }
        if (StrUtil.equals(definition.getKey(), EDHR_BATCH_RECORD_PROCESS_DEFINITION_KEY)
                && !isFormCenterOwnedProcess(task)) {
            throw exception(TASK_APPROVAL_REQUIRES_EDHR_SIGNATURE);
        }
    }

    private boolean isFormCenterOwnedProcess(Task task) {
        if (task == null || StrUtil.isBlank(task.getProcessInstanceId())) {
            return false;
        }
        Long tenantId = resolveTenantId(task);
        if (tenantId == null) {
            return false;
        }
        return formActionInstanceMapper.selectByProcessInstanceId(tenantId, task.getProcessInstanceId()) != null;
    }

    private Long resolveTenantId(Task task) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null || task == null || StrUtil.isBlank(task.getTenantId())) {
            return tenantId;
        }
        try {
            return Long.valueOf(task.getTenantId());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
