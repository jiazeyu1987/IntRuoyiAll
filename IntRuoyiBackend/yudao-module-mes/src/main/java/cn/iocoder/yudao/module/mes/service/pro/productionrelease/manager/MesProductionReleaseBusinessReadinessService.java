package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormInstanceStatus;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesOrderReleaseCompletenessCheck;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesOrderReleaseCompletenessService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrApprovalStatusMapping;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImpl;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class MesProductionReleaseBusinessReadinessService {

    private static final String SEVERITY_INFO = "INFO";
    private static final String SEVERITY_BLOCKER = "BLOCKER";
    private static final String MODULE_EDHR = "EDHR";
    private static final String CATEGORY_DHR = "DHR";
    private static final String REQUIRED_POLICY_CONDITIONAL_REQUIRED = "CONDITIONAL_REQUIRED";
    private static final String REQUIRED_POLICY_SKIPPABLE_CONTROLLED = "SKIPPABLE_CONTROLLED";
    private static final String FORM_SLOT_TYPE_LOSS_REPORT = "LOSS_REPORT";
    private static final String CONDITION_TYPE_HAS_ACTUAL_LOSS = "HAS_ACTUAL_LOSS";
    private static final Set<String> KNOWN_RESULTS = Set.of(
            MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS,
            MesProEdhrReleaseServiceImpl.CHECK_RESULT_FAIL,
            MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER,
            MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE,
            MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_REQUIRED);

    private final MesProEdhrBatchExecutionTaskMapper batchExecutionTaskMapper;
    private final MesProEdhrBatchExecutionOriginMapper batchExecutionOriginMapper;
    private final MesProBatchRecordExecutionMapper executionMapper;
    private final MesProBatchRecordExecutionSignatureMapper executionSignatureMapper;
    private final FormActionInstanceMapper formActionInstanceMapper;
    private final MesOrderReleaseCompletenessService releaseCompletenessService;

    public MesProductionReleaseBusinessReadinessService(
            MesProEdhrBatchExecutionTaskMapper batchExecutionTaskMapper,
            MesProEdhrBatchExecutionOriginMapper batchExecutionOriginMapper,
            MesProBatchRecordExecutionMapper executionMapper,
            MesProBatchRecordExecutionSignatureMapper executionSignatureMapper,
            MesProBatchRecordExecutionAttachmentMapper ignoredAttachmentMapper,
            FormActionInstanceMapper formActionInstanceMapper,
            MesOrderReleaseCompletenessService releaseCompletenessService) {
        this.batchExecutionTaskMapper = batchExecutionTaskMapper;
        this.batchExecutionOriginMapper = batchExecutionOriginMapper;
        this.executionMapper = executionMapper;
        this.executionSignatureMapper = executionSignatureMapper;
        this.formActionInstanceMapper = formActionInstanceMapper;
        this.releaseCompletenessService = releaseCompletenessService;
    }

    public MesProductionReleaseBusinessReadiness resolveBusinessReadinessChecks(MesProEdhrBatchExecutionDO batch) {
        if (batch == null || batch.getId() == null) {
            throw new IllegalArgumentException("batch execution is required for release business readiness");
        }
        List<MesOrderReleaseCompletenessCheck> checks = List.of(
                buildDhrCompletenessCheck(batch),
                releaseCompletenessService.evaluateInspectionResult(batch),
                releaseCompletenessService.evaluateDeviationClosed(batch),
                releaseCompletenessService.evaluateReworkClosed(batch),
                releaseCompletenessService.evaluateScrapRecorded(batch),
                releaseCompletenessService.evaluateInventoryConsistency(batch));
        int failedCount = (int) checks.stream().filter(this::isFailedCheck).count();
        int blockingCount = (int) checks.stream().filter(this::isBlockingCheck).count();
        String snapshotJson = buildSnapshotJson(batch, checks, LocalDateTime.now());
        return new MesProductionReleaseBusinessReadiness(
                resultOf(checks, MesProEdhrReleaseServiceImpl.CHECK_DHR_COMPLETENESS),
                resultOf(checks, MesProEdhrReleaseServiceImpl.CHECK_INSPECTION_RESULT),
                resultOf(checks, MesProEdhrReleaseServiceImpl.CHECK_DEVIATION_CLOSED),
                resultOf(checks, MesProEdhrReleaseServiceImpl.CHECK_REWORK_CLOSED),
                resultOf(checks, MesProEdhrReleaseServiceImpl.CHECK_SCRAP_RECORDED),
                resultOf(checks, MesProEdhrReleaseServiceImpl.CHECK_INVENTORY_CONSISTENCY),
                checks.size(),
                failedCount,
                blockingCount,
                snapshotJson,
                DigestUtil.sha256Hex(snapshotJson),
                checks);
    }

    private MesOrderReleaseCompletenessCheck buildDhrCompletenessCheck(MesProEdhrBatchExecutionDO batch) {
        boolean pass = ordinaryProcessFillEvidenceComplete(batch.getId());
        String failureReason = pass ? "普通工序已填写完成并提交电子签名证据"
                : "普通工序未填写完成或缺少提交电子签名证据";
        String suggestion = pass ? "无需处理" : "完成普通工序填写并提交电子签名后重新预检";
        return new MesOrderReleaseCompletenessCheck(
                MesProEdhrReleaseServiceImpl.CHECK_DHR_COMPLETENESS,
                "DHR 完整性检查",
                CATEGORY_DHR,
                pass ? MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS : MesProEdhrReleaseServiceImpl.CHECK_RESULT_FAIL,
                pass ? SEVERITY_INFO : SEVERITY_BLOCKER,
                MODULE_EDHR,
                "EDHR_BATCH_EXECUTION",
                String.valueOf(batch.getId()),
                batch.getBatchExecutionCode(),
                failureReason,
                suggestion);
    }

    private boolean ordinaryProcessFillEvidenceComplete(Long batchExecutionId) {
        List<MesProEdhrBatchExecutionTaskDO> tasks =
                batchExecutionTaskMapper.selectListByBatchExecutionId(batchExecutionId);
        List<MesProEdhrBatchExecutionOriginDO> origins = hasConditionalRequiredRouteForm(tasks)
                ? batchExecutionOriginMapper.selectListByBatchExecutionId(batchExecutionId) : List.of();
        List<MesProEdhrBatchExecutionTaskDO> ordinaryTasks = tasks.stream()
                .filter(task -> Objects.equals(MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM,
                        task.getNodeType()))
                .filter(task -> Boolean.TRUE.equals(task.getRequiredFlag()))
                .filter(task -> !REQUIRED_POLICY_SKIPPABLE_CONTROLLED.equals(task.getRequiredPolicy()))
                .filter(task -> routeFormRequiredForReadiness(task, origins))
                .toList();
        if (ordinaryTasks.isEmpty()) {
            return false;
        }
        return ordinaryTasks.stream().allMatch(this::ordinaryTaskFillEvidenceComplete);
    }

    private boolean hasConditionalRequiredRouteForm(List<MesProEdhrBatchExecutionTaskDO> tasks) {
        return tasks.stream()
                .filter(task -> Objects.equals(MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM,
                        task.getNodeType()))
                .filter(task -> Boolean.TRUE.equals(task.getRequiredFlag()))
                .anyMatch(task -> REQUIRED_POLICY_CONDITIONAL_REQUIRED.equals(task.getRequiredPolicy()));
    }

    private boolean routeFormRequiredForReadiness(MesProEdhrBatchExecutionTaskDO task,
                                                  List<MesProEdhrBatchExecutionOriginDO> origins) {
        return cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrConditionalLossRequirement.required(task,
                cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrConditionalLossRequirement.formalDecision(origins));
    }

    private boolean ordinaryTaskFillEvidenceComplete(MesProEdhrBatchExecutionTaskDO task) {
        if (!Objects.equals(task.getStatus(), MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)) {
            return false;
        }
        if (hasFormCenterRouteEvidenceContext(task)) {
            return formCenterRouteTaskFillEvidenceComplete(task);
        }
        if (task.getExecutionId() == null) {
            return false;
        }
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(task.getExecutionId());
        if (execution == null || !Objects.equals(execution.getStatus(),
                MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_FILL_COMPLETED)
                || execution.getSubmittedAt() == null
                || execution.getClosedAt() == null
                || StrUtil.isBlank(execution.getCellValuesHash())
                || execution.getFieldAuditRevision() == null
                || StrUtil.isBlank(execution.getFieldAuditHeadHash())) {
            return false;
        }
        return executionSignatureMapper.selectCount(new LambdaQueryWrapperX<MesProBatchRecordExecutionSignatureDO>()
                .eq(MesProBatchRecordExecutionSignatureDO::getExecutionId, execution.getId())
                .eq(MesProBatchRecordExecutionSignatureDO::getActionType,
                        MesProBatchRecordExecutionSignatureService.ACTION_SUBMIT)
                .eq(MesProBatchRecordExecutionSignatureDO::getPasswordVerified, Boolean.TRUE)
                .eq(MesProBatchRecordExecutionSignatureDO::getFieldAuditRevision, execution.getFieldAuditRevision())
                .eq(MesProBatchRecordExecutionSignatureDO::getFieldAuditHeadHash, execution.getFieldAuditHeadHash())
                .eq(MesProBatchRecordExecutionSignatureDO::getCellValuesHash, execution.getCellValuesHash())) > 0;
    }

    private boolean hasFormCenterRouteEvidenceContext(MesProEdhrBatchExecutionTaskDO task) {
        return task != null && (task.getFormCenterInstanceId() != null
                || task.getFormTemplateId() != null
                || task.getFormTemplateVersionId() != null
                || StrUtil.isNotBlank(task.getFormBindingKey()));
    }

    private boolean formCenterRouteTaskFillEvidenceComplete(MesProEdhrBatchExecutionTaskDO task) {
        if (!hasCompleteFormCenterRouteEvidenceContext(task)) {
            return false;
        }
        FormActionInstanceDO instance = formActionInstanceMapper.selectById(task.getFormCenterInstanceId());
        if (instance == null
                || !Objects.equals(FormInstanceStatus.EFFECTIVE.name(), instance.getStatus())
                || !Objects.equals("MES", instance.getSystemCode())
                || !Objects.equals("EDHR_ROUTE_FORM", instance.getObjectType())
                || !Objects.equals("ACTIVE", instance.getObjectState())) {
            return false;
        }
        return formCenterInstanceBelongsToRouteTask(task, instance);
    }

    private boolean hasCompleteFormCenterRouteEvidenceContext(MesProEdhrBatchExecutionTaskDO task) {
        return task != null
                && task.getFormCenterInstanceId() != null
                && task.getFormTemplateId() != null
                && task.getFormTemplateVersionId() != null
                && StrUtil.isNotBlank(task.getFormBindingKey());
    }

    private boolean formCenterInstanceBelongsToRouteTask(MesProEdhrBatchExecutionTaskDO task,
                                                         FormActionInstanceDO instance) {
        Long instanceObjectTaskId = parsePositiveLongOrNull(instance.getObjectId());
        if (instanceObjectTaskId == null) {
            return false;
        }
        if (Objects.equals(task.getId(), instanceObjectTaskId)) {
            return true;
        }
        if (!Objects.equals("BATCH_SHARED", task.getInstanceScope())) {
            return false;
        }
        MesProEdhrBatchExecutionTaskDO representativeTask =
                batchExecutionTaskMapper.selectById(instanceObjectTaskId);
        return representativeTask != null
                && Objects.equals(task.getBatchExecutionId(), representativeTask.getBatchExecutionId())
                && Objects.equals(MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM,
                representativeTask.getNodeType())
                && Objects.equals(task.getFormCenterInstanceId(), representativeTask.getFormCenterInstanceId())
                && Objects.equals(task.getFormTemplateId(), representativeTask.getFormTemplateId())
                && Objects.equals(task.getFormTemplateVersionId(), representativeTask.getFormTemplateVersionId())
                && Objects.equals(task.getSharedFormKey(), representativeTask.getSharedFormKey());
    }

    private Long parsePositiveLongOrNull(String value) {
        if (StrUtil.isBlank(value) || value.chars().anyMatch(ch -> !Character.isDigit(ch))) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isFailedCheck(MesOrderReleaseCompletenessCheck item) {
        validateCheckItem(item);
        return !Objects.equals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, item.checkResult())
                && !Objects.equals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE, item.checkResult());
    }

    private boolean isBlockingCheck(MesOrderReleaseCompletenessCheck item) {
        validateCheckItem(item);
        return Objects.equals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER, item.checkResult())
                || Objects.equals(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_REQUIRED, item.checkResult())
                || Objects.equals(SEVERITY_BLOCKER, item.severity());
    }

    private String resultOf(List<MesOrderReleaseCompletenessCheck> checkItems, String checkCode) {
        return checkItems.stream()
                .filter(item -> Objects.equals(item.checkCode(), checkCode))
                .map(MesOrderReleaseCompletenessCheck::checkResult)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("missing business readiness check result: " + checkCode));
    }

    private void validateCheckItem(MesOrderReleaseCompletenessCheck item) {
        if (item == null || StrUtil.isBlank(item.checkCode()) || StrUtil.isBlank(item.checkResult())) {
            throw new IllegalStateException("business readiness check item is incomplete");
        }
        if (!KNOWN_RESULTS.contains(item.checkResult())) {
            throw new IllegalStateException("unknown business readiness check result: " + item.checkResult());
        }
    }

    private String buildSnapshotJson(MesProEdhrBatchExecutionDO batch,
                                     List<MesOrderReleaseCompletenessCheck> checks,
                                     LocalDateTime checkedAt) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("batchExecutionId", batch.getId());
        snapshot.put("batchExecutionCode", batch.getBatchExecutionCode());
        snapshot.put("workOrderCode", batch.getWorkOrderCode());
        snapshot.put("batchCode", batch.getBatchCode());
        snapshot.put("productCode", batch.getProductCode());
        snapshot.put("checkedAt", checkedAt);
        snapshot.put("items", checks.stream().map(this::toSnapshotItem).toList());
        return JSON.toJSONString(snapshot);
    }

    private Map<String, Object> toSnapshotItem(MesOrderReleaseCompletenessCheck item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkCode", item.checkCode());
        result.put("checkName", item.checkName());
        result.put("checkCategory", item.checkCategory());
        result.put("checkResult", item.checkResult());
        result.put("severity", item.severity());
        result.put("responsibilityModule", item.responsibilityModule());
        result.put("sourceObjectType", item.sourceObjectType());
        result.put("sourceObjectId", item.sourceObjectId());
        result.put("sourceObjectCode", item.sourceObjectCode());
        result.put("failureReason", item.failureReason());
        result.put("remediationSuggestion", item.remediationSuggestion());
        return result;
    }
}
