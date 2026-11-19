package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.service.task.BpmApplicantCancellationGuard;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import org.flowable.engine.HistoryService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
public class DccApplicantCancellationGuard implements BpmApplicantCancellationGuard {
    static final Set<String> WITHDRAWABLE_STATUSES = Set.of(
            "PENDING_SUPERVISOR_REVIEW", "PENDING_MATRIX_REVIEW", "PENDING_MATRIX_APPROVAL");
    private static final Set<String> OBSOLETE_TASKS = Set.of(
            "SUPERVISOR_REVIEW", "SIGNOFF_ASSIGNMENT", "MATRIX_REVIEW", "MATRIX_APPROVAL");
    private final DccControlledFileMapper files;
    private final DccControlledFileMasterMapper masters;
    private final FormActionInstanceMapper forms;
    private final HistoryService history;

    public DccApplicantCancellationGuard(DccControlledFileMapper files, DccControlledFileMasterMapper masters,
            FormActionInstanceMapper forms, HistoryService history) {
        this.files = files; this.masters = masters; this.forms = forms; this.history = history;
    }

    @Override
    public void validate(Long actorId, ProcessInstance instance, String reason) {
        String key = instance.getProcessDefinitionKey();
        boolean obsolete = DccControlledFileProcessDefinitionKeys.FORM_CENTER_OBSOLETE_KEYS.contains(key);
        if (!obsolete && !DccControlledFileProcessDefinitionKeys.NATIVE_FINALIZATION_KEYS.contains(key)) return;
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("请填写撤回原因");
        Long tenant = TenantContextHolder.getRequiredTenantId();
        if (!Objects.equals(instance.getTenantId(), tenant.toString())
                || !Objects.equals(instance.getStartUserId(), String.valueOf(actorId))) denied();
        var ownership = obsolete ? forms.selectOwnershipByTenantAndProcessId(tenant, instance.getId()) : null;
        if (obsolete && (ownership == null || ownership.size() != 1)) denied();
        String fileKey = obsolete ? ownership.get(0).getObjectId() : instance.getBusinessKey();
        if (fileKey == null || !fileKey.matches("[1-9][0-9]*")) denied();
        Long fileId = Long.valueOf(fileKey);
        var identity = files.selectById(fileId);
        if (identity == null || identity.getMasterId() == null || !Objects.equals(identity.getTenantId(), tenant)) denied();
        // Approval and assignment lock Master then File; cancellation must serialize with both.
        var master = masters.selectByIdForUpdate(identity.getMasterId());
        var file = files.selectByIdAndTenantForUpdate(tenant, fileId);
        if (master == null || !Objects.equals(master.getTenantId(), tenant) || file == null
                || !Objects.equals(file.getMasterId(), master.getId()) || !Objects.equals(file.getTenantId(), tenant)) denied();
        if (obsolete) {
            var form = forms.selectByIdAndTenantForUpdate(tenant, ownership.get(0).getId());
            if (form == null || !Objects.equals(form.getBpmProcessInstanceId(), instance.getId())
                    || !Objects.equals(form.getApplicantUserId(), actorId) || !"IN_APPROVAL".equals(form.getStatus())
                    || !"DCC".equals(form.getSystemCode()) || !"CONTROLLED_FILE".equals(form.getObjectType())
                    || !"OBSOLETE".equals(form.getActionCode()) || !Objects.equals(form.getObjectId(), fileKey)
                    || !Objects.equals(form.getObjectVersion(), file.getVersionNo())
                    || !Set.of("ACTIVE", "CONTROLLED_PENDING_EFFECTIVE").contains(file.getStatus())) denied();
        } else {
            if (!Objects.equals(file.getRequesterId(), actorId) || !Objects.equals(file.getProcessInstanceId(), instance.getId())
                    || !Objects.equals(file.getProcessDefinitionKey(), key) || !WITHDRAWABLE_STATUSES.contains(file.getStatus())) denied();
            // Started history includes an unsigned live task and remains true if a later return occurs.
            if (hasEnteredDocControl(instance.getId())) {
                throw new IllegalStateException("本次申请已进入文控审核节点，不能撤回");
            }
        }
        var tasks = history.createHistoricTaskInstanceQuery().processInstanceId(instance.getId()).unfinished().list();
        if (tasks.isEmpty() || tasks.stream().anyMatch(task -> !OBSOLETE_TASKS.contains(task.getTaskDefinitionKey()))) denied();
    }

    public boolean hasEnteredDocControl(String processId) {
        return history.createHistoricActivityInstanceQuery().processInstanceId(processId)
                .activityId("DOC_CONTROL_REVIEW").count() > 0
                || history.createHistoricActivityInstanceQuery().processInstanceId(processId)
                .activityId("DOC_CONTROL_APPROVAL").count() > 0;
    }

    private static void denied() {
        throw new IllegalStateException("本次申请已进入不可撤回阶段，或申请人与申请轮次不一致，请刷新核对");
    }
}
