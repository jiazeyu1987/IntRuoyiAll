package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormControlledActionApprovalOutcome;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectExecutor;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectPrecheck;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectResult;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormControlledActionLifecycleAdapter;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class MesProEdhrRouteFormFillEffectExecutor
        implements FormBusinessEffectExecutor, FormControlledActionLifecycleAdapter {

    public static final String EXECUTOR_CODE = "MES_EDHR_ROUTE_FORM_FILL";
    private static final String CONTEXT_ERROR =
            "MES_EDHR_ROUTE_FORM_FILL only accepts MES EDHR_ROUTE_FORM ACTIVE actions";

    private final MesProEdhrBatchExecutionTaskMapper batchTaskMapper;

    public MesProEdhrRouteFormFillEffectExecutor(MesProEdhrBatchExecutionTaskMapper batchTaskMapper) {
        this.batchTaskMapper = batchTaskMapper;
    }

    @Override
    public String getExecutorCode() {
        return EXECUTOR_CODE;
    }

    @Override
    public FormBusinessEffectResult execute(FormActionInstance instance, String idempotencyKey) {
        if (!supports(instance)) {
            return FormBusinessEffectResult.failure(CONTEXT_ERROR);
        }
        try {
            MesProEdhrBatchExecutionTaskDO task = requireWritableTask(instance);
            LocalDateTime now = LocalDateTime.now();
            task.setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)
                    .setSubmittedAt(now)
                    .setApprovedAt(now)
                    .setBlockerCode(null)
                    .setBlockerMessage(null);
            if (task.getOpenedAt() == null) {
                task.setOpenedAt(now);
            }
            if (task.getOpenedBy() == null) {
                task.setOpenedBy(instance.getApplicantUserId());
            }
            batchTaskMapper.updateById(task);
            return FormBusinessEffectResult.success(String.valueOf(task.getId()));
        } catch (RuntimeException ex) {
            return FormBusinessEffectResult.failure(ex.getMessage());
        }
    }

    @Override
    public boolean supports(FormActionInstance instance) {
        BusinessActionContext context = instance == null ? null : instance.getBusinessContext();
        return context != null
                && "MES".equals(context.getSystemCode())
                && "EDHR_ROUTE_FORM".equals(context.getObjectType())
                && StrUtil.startWith(context.getActionCode(), "EDHR_RF_")
                && "ACTIVE".equals(context.getObjectState());
    }

    @Override
    public FormBusinessEffectPrecheck preflight(FormActionInstance instance) {
        if (!supports(instance)) {
            return FormBusinessEffectPrecheck.fail(CONTEXT_ERROR);
        }
        try {
            requireWritableTask(instance);
            return FormBusinessEffectPrecheck.pass();
        } catch (RuntimeException ex) {
            return FormBusinessEffectPrecheck.fail(ex.getMessage());
        }
    }

    @Override
    public void onPendingApprovalStarted(FormActionInstance instance) {
        throw new IllegalStateException("MES_EDHR_ROUTE_FORM_FILL uses DIRECT approval only");
    }

    @Override
    public void onPendingApprovalClosed(FormActionInstance instance, FormControlledActionApprovalOutcome outcome,
                                        String reason) {
        if (!supports(instance)) {
            throw new IllegalArgumentException(CONTEXT_ERROR);
        }
    }

    private MesProEdhrBatchExecutionTaskDO requireWritableTask(FormActionInstance instance) {
        Long taskId = requiredTaskId(instance);
        MesProEdhrBatchExecutionTaskDO task = batchTaskMapper.selectByIdForUpdate(taskId);
        if (task == null || !MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM.equals(task.getNodeType())) {
            throw new IllegalArgumentException("eDHR route form task not found: " + taskId);
        }
        if (task.getFormCenterInstanceId() == null
                || task.getFormTemplateId() == null
                || task.getFormTemplateVersionId() == null) {
            throw new IllegalArgumentException("eDHR route form task misses form center snapshot: " + taskId);
        }
        Integer status = task.getStatus();
        if (!Objects.equals(status, MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING)
                && !Objects.equals(status, MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_DRAFT)
                && !Objects.equals(status, MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_REWORK_REQUIRED)) {
            throw new IllegalStateException("eDHR route form task is not writable: " + taskId);
        }
        return task;
    }

    private Long requiredTaskId(FormActionInstance instance) {
        BusinessActionContext context = instance.getBusinessContext();
        String objectId = context == null ? null : context.getObjectId();
        if (StrUtil.isBlank(objectId)) {
            throw new IllegalArgumentException("Missing eDHR route form task id");
        }
        return Long.valueOf(objectId);
    }

}
