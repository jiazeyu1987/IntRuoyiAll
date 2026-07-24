package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrLocalStateSampleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrLocalStateSampleRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionArchiveDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseCheckItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionArchiveMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseCheckItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_LOCAL_STATE_SAMPLE_CONTEXT_FORBIDDEN;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_LOCAL_STATE_SAMPLE_PROFILE_FORBIDDEN;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_LOCAL_STATE_SAMPLE_STATE_INVALID;

@Service
public class MesProEdhrLocalStateSampleServiceImpl implements MesProEdhrLocalStateSampleService {

    private static final Long YUDAO_SOURCE_TENANT_ID = 1L;
    private static final String LOCAL_STATE_SAMPLE_MARK = "[LOCAL_STATE_SAMPLE]";
    private static final String DETAIL_PATH = "/mes/pro/feedback/edhr-batch-execution/detail";
    private static final DateTimeFormatter CODE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final Set<String> SUPPORTED_STATES = Set.of(
            "CLOSE", "PRECHECK", "RELEASE_APPROVAL", "ARCHIVE", "ARCHIVED", "QUALITY_TERMINAL");

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Resource
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Resource
    private MesProEdhrReleaseCheckItemMapper releaseCheckItemMapper;
    @Resource
    private MesProEdhrBatchExecutionArchiveMapper archiveMapper;
    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private MesProEdhrOperationAuditService operationAuditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrLocalStateSampleRespVO createLocalStateSample(EdhrLocalStateSampleReqVO reqVO) {
        String state = normalizeState(reqVO == null ? null : reqVO.getState());
        validateLocalProfile();
        LoginUser loginUser = validateAdminTenantContext();

        LocalDateTime now = LocalDateTime.now();
        String code = "EDHR-UI-SAMPLE-" + state + "-" + now.format(CODE_TIME_FORMATTER);
        MesProEdhrBatchExecutionDO batch = buildBatch(state, code, now, loginUser.getId());
        batchExecutionMapper.insert(batch);

        MesProEdhrBatchExecutionTaskDO batchTask = buildBatchTask(batch, state, now, loginUser.getId());
        batchTaskMapper.insert(batchTask);

        MesProEdhrReleaseTransactionDO releaseTransaction = buildReleaseTransaction(batch, state, now, loginUser.getId());
        List<MesProEdhrReleaseCheckItemDO> releaseCheckItems = List.of();
        if (releaseTransaction != null) {
            releaseTransactionMapper.insert(releaseTransaction);
            releaseCheckItems = insertReleaseCheckItems(releaseTransaction, batch, state, now);
        }
        List<MesProEdhrWorkTaskDO> workTasks =
                insertStageWorkTask(batch, batchTask, releaseTransaction, state, now, loginUser.getId());
        MesProEdhrBatchExecutionArchiveDO archive = null;
        if ("ARCHIVED".equals(state)) {
            archive = buildSealedArchive(batch, code, now, loginUser.getId());
            archiveMapper.insert(archive);
        }
        recordLocalStateSampleCreateAudit(batch, batchTask, releaseTransaction, releaseCheckItems, workTasks, archive,
                state, code, now, loginUser.getId());

        return new EdhrLocalStateSampleRespVO()
                .setBatchExecutionId(batch.getId())
                .setBatchExecutionCode(batch.getBatchExecutionCode())
                .setSampleState(state)
                .setDetailPath(DETAIL_PATH)
                .setRouteQuery(Map.of(
                        "id", String.valueOf(batch.getId()),
                        "release", "1",
                        "sampleState", state));
    }

    private String normalizeState(String rawState) {
        String state = rawState == null ? "" : rawState.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_STATES.contains(state)) {
            throw exception(PRO_EDHR_LOCAL_STATE_SAMPLE_STATE_INVALID);
        }
        return state;
    }

    private void validateLocalProfile() {
        boolean localProfile = Arrays.stream(String.valueOf(activeProfiles).split(","))
                .map(String::trim)
                .anyMatch("local"::equals);
        if (!localProfile) {
            throw exception(PRO_EDHR_LOCAL_STATE_SAMPLE_PROFILE_FORBIDDEN);
        }
    }

    private LoginUser validateAdminTenantContext() {
        Long currentTenantId = TenantContextHolder.getTenantId();
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (!YUDAO_SOURCE_TENANT_ID.equals(currentTenantId)
                || loginUser == null
                || loginUserId == null
                || !YUDAO_SOURCE_TENANT_ID.equals(loginUser.getTenantId())) {
            throw exception(PRO_EDHR_LOCAL_STATE_SAMPLE_CONTEXT_FORBIDDEN);
        }
        AdminUserRespDTO adminUser = adminUserApi.getUser(loginUserId);
        if (adminUser == null || !"admin".equals(adminUser.getUsername())) {
            throw exception(PRO_EDHR_LOCAL_STATE_SAMPLE_CONTEXT_FORBIDDEN);
        }
        return loginUser;
    }

    private MesProEdhrBatchExecutionDO buildBatch(String state, String code, LocalDateTime now, Long actorUserId) {
        int batchStatus = switch (state) {
            case "CLOSE" -> MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_READY_TO_CLOSE;
            case "ARCHIVED" -> MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_ARCHIVED;
            case "QUALITY_TERMINAL" -> MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REJECTED;
            default -> MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED;
        };
        MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO()
                .setBatchExecutionCode(code)
                .setWorkOrderId(900_000_001L)
                .setWorkOrderCode("WO-" + code)
                .setBatchCode("BATCH-" + state)
                .setActiveContextKey(code)
                .setProductId(900_000_002L)
                .setProductCode("EDHR-SAMPLE")
                .setProductName("eDHR 放行 UI 本地样本")
                .setRouteId(900_000_003L)
                .setRouteVersionId(900_000_004L)
                .setRouteVersionNo("LOCAL")
                .setRouteSnapshotJson(JSON.toJSONString(Map.of("marker", LOCAL_STATE_SAMPLE_MARK, "state", state)))
                .setRouteCode("EDHR-SAMPLE-ROUTE")
                .setRouteName("eDHR 放行 UI 本地样本路线")
                .setStatus(batchStatus)
                .setTaskTotal(1)
                .setTaskApprovedCount(1)
                .setBlockedCount("PRECHECK".equals(state) ? 1 : 0)
                .setAggregateHash("LOCAL_STATE_SAMPLE_" + state)
                .setRemark(LOCAL_STATE_SAMPLE_MARK + " " + state);
        if (!"CLOSE".equals(state)) {
            batch.setClosedBy(actorUserId).setClosedAt(now.minusMinutes(20));
        }
        if ("QUALITY_TERMINAL".equals(state)) {
            batch.setRejectedBy(actorUserId)
                    .setRejectedAt(now.minusMinutes(5))
                    .setRejectReason(LOCAL_STATE_SAMPLE_MARK + " 质量拒收样本");
        }
        return batch;
    }

    private MesProEdhrBatchExecutionTaskDO buildBatchTask(MesProEdhrBatchExecutionDO batch, String state,
                                                          LocalDateTime now, Long actorUserId) {
        return new MesProEdhrBatchExecutionTaskDO()
                .setBatchExecutionId(batch.getId())
                .setNodeType(MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM)
                .setRouteProcessId(900_000_101L)
                .setRootProcessFlag(true)
                .setRouteProcessSort(1)
                .setProcessId(900_000_102L)
                .setProcessCode("EDHR-SAMPLE-P01")
                .setProcessName("本地状态样本工序")
                .setBatchRecordReportId("LOCAL_STATE_SAMPLE")
                .setBatchRecordReportName("eDHR 放行 UI 本地样本记录")
                .setBatchRecordDefinitionId(900_000_103L)
                .setBatchRecordVersionId(900_000_104L)
                .setBatchRecordSort(1)
                .setExecutionMode("SEQUENTIAL")
                .setFormSlotType("MAIN")
                .setRecordCategory("BATCH_RECORD")
                .setValidationProfile("CONTROLLED_BATCH")
                .setRequiredPolicy("REQUIRED")
                .setOwnerRoleKey("PRODUCTION")
                .setArchiveVisibility("FINAL_DHR")
                .setSlotConfigSnapshotHash("LOCAL_STATE_SAMPLE")
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)
                .setRequiredFlag(true)
                .setOpenedBy(actorUserId)
                .setOpenedAt(now.minusHours(2))
                .setSubmittedAt(now.minusHours(1))
                .setApprovedAt(now.minusMinutes(45))
                .setSpecialPayloadJson(JSON.toJSONString(Map.of("marker", LOCAL_STATE_SAMPLE_MARK, "state", state)));
    }

    private MesProEdhrReleaseTransactionDO buildReleaseTransaction(MesProEdhrBatchExecutionDO batch, String state,
                                                                   LocalDateTime now, Long actorUserId) {
        if ("CLOSE".equals(state)) {
            return null;
        }
        String releaseStatus = switch (state) {
            case "PRECHECK" -> MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_FAILED;
            case "RELEASE_APPROVAL" -> MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL;
            case "ARCHIVE", "ARCHIVED" -> MesProEdhrReleaseServiceImpl.STATUS_RELEASED;
            case "QUALITY_TERMINAL" -> MesProEdhrReleaseServiceImpl.STATUS_REJECTED;
            default -> throw exception(PRO_EDHR_LOCAL_STATE_SAMPLE_STATE_INVALID);
        };
        int failedCount = "PRECHECK".equals(state) || "QUALITY_TERMINAL".equals(state) ? 1 : 0;
        int blockingCount = "PRECHECK".equals(state) ? 1 : 0;
        MesProEdhrReleaseTransactionDO transaction = new MesProEdhrReleaseTransactionDO()
                .setReleaseCode("EDHR-REL-SAMPLE-" + batch.getId())
                .setBatchExecutionId(batch.getId())
                .setBatchExecutionCode(batch.getBatchExecutionCode())
                .setWorkOrderId(batch.getWorkOrderId())
                .setWorkOrderCode(batch.getWorkOrderCode())
                .setBatchCode(batch.getBatchCode())
                .setProductId(batch.getProductId())
                .setProductCode(batch.getProductCode())
                .setProductName(batch.getProductName())
                .setRouteId(batch.getRouteId())
                .setRouteCode(batch.getRouteCode())
                .setRouteName(batch.getRouteName())
                .setDhrStatus(failedCount == 0 ? MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS : MesProEdhrReleaseServiceImpl.CHECK_RESULT_FAIL)
                .setInspectionStatus(failedCount == 0 ? MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS : MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER)
                .setDeviationStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .setReworkStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .setScrapStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .setInventoryStatus(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS)
                .setReleaseStatus(releaseStatus)
                .setRequiredCheckCount(2)
                .setFailedCheckCount(failedCount)
                .setBlockingCheckCount(blockingCount)
                .setLastPrecheckAt(now.minusMinutes(15))
                .setPrecheckSnapshotJson(JSON.toJSONString(Map.of("marker", LOCAL_STATE_SAMPLE_MARK, "state", state)))
                .setVersion(1)
                .setRemark(LOCAL_STATE_SAMPLE_MARK + " " + state);
        if ("RELEASE_APPROVAL".equals(state) || "ARCHIVE".equals(state) || "ARCHIVED".equals(state)) {
            transaction.setSubmittedBy(actorUserId)
                    .setSubmittedAt(now.minusMinutes(10))
                    .setSubmitIdempotencyKey("LOCAL_STATE_SAMPLE_SUBMIT_" + batch.getId());
        }
        if ("ARCHIVE".equals(state) || "ARCHIVED".equals(state)) {
            transaction.setApprovedBy(actorUserId)
                    .setApprovedAt(now.minusMinutes(5))
                    .setApprovalIdempotencyKey("LOCAL_STATE_SAMPLE_APPROVE_" + batch.getId())
                    .setApprovalSignoffEvidenceHash("LOCAL_STATE_SAMPLE_SIGNOFF")
                    .setApprovalOpinion("LOCAL_STATE_SAMPLE 已放行样本");
        }
        if ("QUALITY_TERMINAL".equals(state)) {
            transaction.setRejectedBy(actorUserId)
                    .setRejectedAt(now.minusMinutes(5))
                    .setRejectReason(LOCAL_STATE_SAMPLE_MARK + " 质量拒收样本");
        }
        return transaction;
    }

    private List<MesProEdhrReleaseCheckItemDO> insertReleaseCheckItems(MesProEdhrReleaseTransactionDO transaction,
                                                                       MesProEdhrBatchExecutionDO batch,
                                                                       String state,
                                                                       LocalDateTime now) {
        boolean failed = "PRECHECK".equals(state) || "QUALITY_TERMINAL".equals(state);
        MesProEdhrReleaseCheckItemDO dhrItem = buildCheckItem(transaction.getId(), batch, now,
                MesProEdhrReleaseServiceImpl.CHECK_DHR_COMPLETENESS,
                "DHR", "DHR 完整性检查",
                failed ? MesProEdhrReleaseServiceImpl.CHECK_RESULT_FAIL : MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS,
                failed ? "BLOCKER" : "INFO",
                failed ? "LOCAL_STATE_SAMPLE 阻塞项" : "LOCAL_STATE_SAMPLE 通过项");
        MesProEdhrReleaseCheckItemDO inspectionItem = buildCheckItem(transaction.getId(), batch, now,
                MesProEdhrReleaseServiceImpl.CHECK_INSPECTION_RESULT,
                "INSPECTION", "检验结果检查",
                "PRECHECK".equals(state) ? MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER : MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS,
                "PRECHECK".equals(state) ? "BLOCKER" : "INFO",
                "PRECHECK".equals(state) ? "LOCAL_STATE_SAMPLE 检验未放行" : "LOCAL_STATE_SAMPLE 检验已放行");
        releaseCheckItemMapper.insert(dhrItem);
        releaseCheckItemMapper.insert(inspectionItem);
        return List.of(dhrItem, inspectionItem);
    }

    private MesProEdhrReleaseCheckItemDO buildCheckItem(Long releaseTransactionId, MesProEdhrBatchExecutionDO batch,
                                                        LocalDateTime now, String code, String category, String name,
                                                        String result, String severity, String reason) {
        return new MesProEdhrReleaseCheckItemDO()
                .setReleaseTransactionId(releaseTransactionId)
                .setCheckCode(code)
                .setCheckCategory(category)
                .setCheckName(name)
                .setCheckResult(result)
                .setItemStatus(MesProEdhrReleaseServiceImpl.ITEM_STATUS_OPEN)
                .setSeverity(severity)
                .setResponsibilityModule("EDHR")
                .setSourceObjectType("EDHR_BATCH_EXECUTION")
                .setSourceObjectId(String.valueOf(batch.getId()))
                .setSourceObjectCode(batch.getBatchExecutionCode())
                .setSourceRecordUrl(DETAIL_PATH + "?id=" + batch.getId() + "&release=1")
                .setFailureReason(reason)
                .setRemediationSuggestion("请联系当前阶段负责人处理本地状态样本验证。")
                .setImpactScopeJson(JSON.toJSONString(Map.of("marker", LOCAL_STATE_SAMPLE_MARK)))
                .setEvidenceHash("LOCAL_STATE_SAMPLE")
                .setCheckedAt(now);
    }

    private List<MesProEdhrWorkTaskDO> insertStageWorkTask(MesProEdhrBatchExecutionDO batch,
                                                           MesProEdhrBatchExecutionTaskDO batchTask,
                                                           MesProEdhrReleaseTransactionDO transaction,
                                                           String state,
                                                           LocalDateTime now,
                                                           Long actorUserId) {
        List<MesProEdhrWorkTaskDO> insertedTasks = new ArrayList<>();
        if ("CLOSE".equals(state)) {
            MesProEdhrWorkTaskDO workTask = buildWorkTask(batch, batchTask.getId(), "BATCH_CLOSE", batch.getId(),
                    MesProEdhrWorkTaskService.TASK_TYPE_CLOSE, "收尾关闭", now, actorUserId);
            workTaskMapper.insert(workTask);
            insertedTasks.add(workTask);
            return insertedTasks;
        }
        if ("RELEASE_APPROVAL".equals(state) && transaction != null) {
            MesProEdhrWorkTaskDO workTask = buildWorkTask(batch, null, "RELEASE_TRANSACTION", transaction.getId(),
                    MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE, "最终放行审批", now, actorUserId);
            workTaskMapper.insert(workTask);
            insertedTasks.add(workTask);
            return insertedTasks;
        }
        if ("ARCHIVE".equals(state)) {
            MesProEdhrWorkTaskDO workTask = buildWorkTask(batch, null, "BATCH_ARCHIVE", batch.getId(),
                    MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE, "最终归档", now, actorUserId);
            workTaskMapper.insert(workTask);
            insertedTasks.add(workTask);
        }
        return insertedTasks;
    }

    private MesProEdhrWorkTaskDO buildWorkTask(MesProEdhrBatchExecutionDO batch, Long batchTaskId,
                                               String businessScopeType, Long businessScopeId,
                                               String taskType, String processName,
                                               LocalDateTime now, Long actorUserId) {
        return new MesProEdhrWorkTaskDO()
                .setTaskCode("EDHR-WORK-SAMPLE-" + taskType + "-" + batch.getId())
                .setTaskType(taskType)
                .setBatchExecutionId(batch.getId())
                .setBatchTaskId(batchTaskId)
                .setBusinessScopeType(businessScopeType)
                .setBusinessScopeId(businessScopeId)
                .setWorkOrderId(batch.getWorkOrderId())
                .setWorkOrderCode(batch.getWorkOrderCode())
                .setBatchCode(batch.getBatchCode())
                .setRouteId(batch.getRouteId())
                .setRouteProcessId(900_000_101L)
                .setProcessId(900_000_102L)
                .setProcessName(processName)
                .setAssigneeUserId(actorUserId)
                .setCandidateSourceType("USER")
                .setCandidateSourceId(actorUserId)
                .setCandidateUserSnapshot(String.valueOf(actorUserId))
                .setSourceUserId(actorUserId)
                .setResponsibilitySourceType("LOCAL_STATE_SAMPLE")
                .setResponsibilitySourceKey(taskType + "|" + batch.getId())
                .setResponsibilitySourceVersion(String.valueOf(batch.getId()))
                .setResponsibilitySourceDigest("LOCAL_STATE_SAMPLE|" + taskType)
                .setOwnershipLocked(false)
                .setStatus(MesProEdhrWorkTaskStatus.TODO)
                .setDueTime(now.plusDays(1))
                .setActionUrl(DETAIL_PATH + "?id=" + batch.getId() + "&release=1")
                .setRemark(LOCAL_STATE_SAMPLE_MARK + " " + taskType);
    }

    private void recordLocalStateSampleCreateAudit(MesProEdhrBatchExecutionDO batch,
                                                   MesProEdhrBatchExecutionTaskDO batchTask,
                                                   MesProEdhrReleaseTransactionDO releaseTransaction,
                                                   List<MesProEdhrReleaseCheckItemDO> releaseCheckItems,
                                                   List<MesProEdhrWorkTaskDO> workTasks,
                                                   MesProEdhrBatchExecutionArchiveDO archive,
                                                   String state,
                                                   String code,
                                                   LocalDateTime occurredAt,
                                                   Long actorUserId) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("requestSource", "BATCH_EXECUTION_LIST_LOCAL_STATE_SAMPLE");
        metadata.put("idempotencyKey", code);
        metadata.put("associatedSignatureId", "NOT_APPLICABLE");
        metadata.put("reason", "本地状态样本创建");
        metadata.put("permissionDecision", "ALLOW");
        metadata.put("resultStatus", "SUCCESS");
        metadata.put("sampleState", state);
        metadata.put("batchExecutionId", batch.getId());
        metadata.put("batchExecutionCode", batch.getBatchExecutionCode());
        metadata.put("batchTaskId", batchTask.getId());
        metadata.put("releaseTransactionId", releaseTransaction == null ? null : releaseTransaction.getId());
        metadata.put("releaseCheckItemIds", releaseCheckItems.stream()
                .map(MesProEdhrReleaseCheckItemDO::getId)
                .toList());
        metadata.put("workTaskIds", workTasks.stream()
                .map(MesProEdhrWorkTaskDO::getId)
                .toList());
        metadata.put("archiveId", archive == null ? null : archive.getId());
        metadata.put("createdRecords", toLocalStateCreatedRecordsPayload(batch, batchTask, releaseTransaction,
                releaseCheckItems, workTasks, archive));
        String afterHash = hashLocalStateAuditPayload(metadata);
        operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId("EDHR-LOCAL-STATE-SAMPLE-" + code)
                .setObjectType("BATCH_EXECUTION")
                .setObjectId(String.valueOf(batch.getId()))
                .setBatchExecutionId(batch.getId())
                .setRouteId(batch.getRouteId())
                .setOperationType("LOCAL_STATE_SAMPLE_CREATE")
                .setActionName("创建 eDHR 本地状态样本")
                .setActorUserId(actorUserId)
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode("mes:pro-edhr-batch-execution:create")
                .setPermissionDecision("ALLOW")
                .setResultStatus("SUCCESS")
                .setBeforeSummaryHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                .setAfterSummaryHash(afterHash)
                .setMetadataJson(JSON.toJSONString(metadata))
                .setOccurredAt(occurredAt));
    }

    private Map<String, Object> toLocalStateCreatedRecordsPayload(MesProEdhrBatchExecutionDO batch,
                                                                  MesProEdhrBatchExecutionTaskDO batchTask,
                                                                  MesProEdhrReleaseTransactionDO releaseTransaction,
                                                                  List<MesProEdhrReleaseCheckItemDO> releaseCheckItems,
                                                                  List<MesProEdhrWorkTaskDO> workTasks,
                                                                  MesProEdhrBatchExecutionArchiveDO archive) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("batchExecution", Map.of(
                "id", batch.getId(),
                "code", batch.getBatchExecutionCode(),
                "status", batch.getStatus(),
                "aggregateHash", batch.getAggregateHash()));
        payload.put("batchTask", Map.of(
                "id", batchTask.getId(),
                "status", batchTask.getStatus(),
                "batchRecordReportId", batchTask.getBatchRecordReportId()));
        if (releaseTransaction != null) {
            Map<String, Object> releasePayload = new LinkedHashMap<>();
            releasePayload.put("id", releaseTransaction.getId());
            releasePayload.put("releaseStatus", releaseTransaction.getReleaseStatus());
            releasePayload.put("failedCheckCount", releaseTransaction.getFailedCheckCount());
            releasePayload.put("blockingCheckCount", releaseTransaction.getBlockingCheckCount());
            releasePayload.put("precheckSnapshotJson", releaseTransaction.getPrecheckSnapshotJson());
            payload.put("releaseTransaction", releasePayload);
        }
        payload.put("releaseCheckItems", releaseCheckItems.stream()
                .map(item -> {
                    Map<String, Object> itemPayload = new LinkedHashMap<>();
                    itemPayload.put("id", item.getId());
                    itemPayload.put("checkCode", item.getCheckCode());
                    itemPayload.put("checkResult", item.getCheckResult());
                    itemPayload.put("severity", item.getSeverity());
                    itemPayload.put("evidenceHash", item.getEvidenceHash());
                    return itemPayload;
                })
                .toList());
        payload.put("workTasks", workTasks.stream()
                .map(task -> {
                    Map<String, Object> taskPayload = new LinkedHashMap<>();
                    taskPayload.put("id", task.getId());
                    taskPayload.put("taskType", task.getTaskType());
                    taskPayload.put("businessScopeType", task.getBusinessScopeType());
                    taskPayload.put("businessScopeId", task.getBusinessScopeId());
                    taskPayload.put("assigneeUserId", task.getAssigneeUserId());
                    return taskPayload;
                })
                .toList());
        if (archive != null) {
            Map<String, Object> archivePayload = new LinkedHashMap<>();
            archivePayload.put("id", archive.getId());
            archivePayload.put("archiveStatus", archive.getArchiveStatus());
            archivePayload.put("contentHash", archive.getContentHash());
            archivePayload.put("sealedSignatureId", archive.getSealedSignatureId());
            payload.put("archive", archivePayload);
        }
        return payload;
    }

    private String hashLocalStateAuditPayload(Object payload) {
        return MesProBatchRecordExecutionFieldAuditHasher.sha256(JSON.toJSONString(payload));
    }

    private MesProEdhrBatchExecutionArchiveDO buildSealedArchive(MesProEdhrBatchExecutionDO batch, String code,
                                                                 LocalDateTime now, Long actorUserId) {
        return new MesProEdhrBatchExecutionArchiveDO()
                .setBatchExecutionId(batch.getId())
                .setArtifactType("BATCH_FINAL_PDF")
                .setArchiveVersion(1)
                .setArchiveStatus("SEALED")
                .setFileName(code + ".pdf")
                .setContentType("application/pdf")
                .setFileSize(0L)
                .setFilePath("/local-state-sample/" + code + ".pdf")
                .setContentHash("LOCAL_STATE_SAMPLE")
                .setSourceManifestJson(JSON.toJSONString(Map.of("marker", LOCAL_STATE_SAMPLE_MARK, "state", "ARCHIVED")))
                .setGeneratedBy(actorUserId)
                .setGeneratedAt(now.minusMinutes(3))
                .setSealedSignatureId(900_000_201L)
                .setArchiveValidFlag(true)
                .setArchiveValidStatus("VALID");
    }
}
