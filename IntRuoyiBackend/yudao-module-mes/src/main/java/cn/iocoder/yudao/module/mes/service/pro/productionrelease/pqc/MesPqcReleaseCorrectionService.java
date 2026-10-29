package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordChangeEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordChangeEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderDossierFileMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesWorkTaskAuxiliaryAudit;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowIdempotency;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrBatchLifecycleGuard;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseApplicationService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseApplyCommand;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** A rejection or market withdrawal is immutable. Its original applicant corrects the dossier and starts a new formal round. */
@Service
@RequiredArgsConstructor
public class MesPqcReleaseCorrectionService {
    public static final String TYPE = "PQC_RELEASE_CORRECTION";
    private static final String SCOPE = "RELEASE_APPLICATION";
    private static final Set<String> OPEN = Set.of("TODO", "DOING", "OVERDUE");
    private final MesProEdhrWorkTaskMapper tasks;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applications;
    private final MesProcessPoolActiveOrderMapper activeOrders;
    private final MesProcessPoolActiveOrderDossierFileMapper dossierFiles;
    private final MesProEdhrBatchExecutionMapper batches;
    private final MesProWorkOrderMapper workOrders;
    private final PermissionApi permissions;
    private final MesEdhrBatchLifecycleGuard lifecycle;
    private final MesProEdhrNonconformanceReviewService nonconformance;
    private final MesTeamLeaderActiveOrderReleaseApplicationService releaseApplications;
    private final GxpAuditService audit;
    @jakarta.annotation.Resource private NotifyMessageSendApi notifyApi;
    @jakarta.annotation.Resource private MesWorkTaskAuxiliaryAudit auxiliaryAudit;
    @jakarta.annotation.Resource private MesProEdhrNonconformanceReviewMapper reviewMapper;
    @jakarta.annotation.Resource private MesProEdhrRecordChangeEventMapper changeMapper;

    @jakarta.annotation.Resource private cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper releaseTransactions;
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrTaskDeadlineService deadlineService;

    public static boolean isCorrectable(MesProcessPoolActiveOrderReleaseApplicationDO app) {
        return app != null && ("PQC_RELEASE_REJECTED".equals(app.getApplicationStatus()) && "REJECT".equals(app.getPqcDecision())
                || Set.of("MARKET_RELEASE_WITHDRAWN", "MARKET_RELEASE_REJECTED").contains(app.getApplicationStatus() == null ? "" : app.getApplicationStatus()) && "APPROVE".equals(app.getPqcDecision()));
    }
    private static boolean marketCorrection(MesProcessPoolActiveOrderReleaseApplicationDO app) {
        return Set.of("MARKET_RELEASE_WITHDRAWN", "MARKET_RELEASE_REJECTED").contains(app.getApplicationStatus() == null ? "" : app.getApplicationStatus());
    }
    private static String sourceType(MesProcessPoolActiveOrderReleaseApplicationDO app) {
        return marketCorrection(app) ? ("MARKET_RELEASE_REJECTED".equals(app.getApplicationStatus()) ? "MARKET_REJECTION" : "MARKET_WITHDRAWAL") : "PQC_REJECTION";
    }
    private static Long sourceId(MesProcessPoolActiveOrderReleaseApplicationDO app) {
        return marketCorrection(app) ? app.getReleaseTransactionId() : app.getPqcReleaseWorkTaskId();
    }
    private String formalReason(MesProcessPoolActiveOrderReleaseApplicationDO app) {
        return formalReason(app, null);
    }
    private String formalReason(MesProcessPoolActiveOrderReleaseApplicationDO app, Long qualityReviewId) {
        if (!marketCorrection(app)) return app.getPqcRejectReason();
        var transaction = releaseTransactions.selectById(app.getReleaseTransactionId());
        boolean rejected = "MARKET_RELEASE_REJECTED".equals(app.getApplicationStatus());
        require(transaction != null && Objects.equals(transaction.getTenantId(), app.getTenantId())
                && Objects.equals(transaction.getBatchExecutionId(), app.getBatchExecutionId())
                && (rejected ? "REJECTED".equals(transaction.getReleaseStatus()) && transaction.getRejectedBy() != null
                    && transaction.getRejectedAt() != null && StrUtil.isNotBlank(transaction.getRejectReason())
                    : "WITHDRAWN".equals(transaction.getReleaseStatus()) && transaction.getWithdrawnBy() != null
                    && transaction.getWithdrawnAt() != null && StrUtil.isNotBlank(transaction.getWithdrawReason()))
                && app.getReleaseApprovalWorkTaskId() != null, "上市补正缺少原正式否决事务及原因");
        if (rejected) {
            var review = qualityReviewId == null ? reviewMapper.selectLatestBySource("ACTIVE_ORDER", app.getActiveOrderId())
                    : reviewMapper.selectById(qualityReviewId);
            require(isConcessionFor(app, review) && !review.getFrozenAt().isBefore(transaction.getRejectedAt()),
                    "上市不予放行尚未取得本轮QA让步决定，不能补正重提");
        }
        return rejected ? transaction.getRejectReason() : transaction.getWithdrawReason();
    }

    public static boolean isConcessionFor(MesProcessPoolActiveOrderReleaseApplicationDO app,
            MesProEdhrNonconformanceReviewDO review) {
        return app != null && review != null && Objects.equals(review.getTenantId(), app.getTenantId())
                && Objects.equals(review.getActiveOrderId(), app.getActiveOrderId())
                && Objects.equals(review.getBatchExecutionId(), app.getBatchExecutionId())
                && "ACTIVE_ORDER".equals(review.getSourceType()) && Objects.equals(review.getSourceId(), app.getActiveOrderId())
                && Objects.equals(review.getRemark(), "上市放行不予批准；原放行事务:" + app.getReleaseTransactionId())
                && "closed".equals(review.getReviewStatus()) && "concession_release".equals(review.getDisposition())
                && review.getQaUserId() != null && review.getFrozenAt() != null && review.getClosedAt() != null;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long receive(Long actor, Long applicationId) {
        require(actor != null && applicationId != null && applicationId > 0
                && permissions.hasAnyPermissions(actor, "mes:pro-edhr-work-task:query")
                && permissions.hasAnyPermissions(actor, "mes:pro-process-pool-team-leader:maintain"), "无接收补正任务权限");
        audit.acquireLedgerLock();
        var source = applications.selectById(applicationId);
        require(source != null, "正式放行申请不存在");
        var batch = batches.selectByTenantIdAndIdForUpdate(TenantContextHolder.getRequiredTenantId(), source.getBatchExecutionId());
        require(batch != null, "补正原申请缺少同租户正式批次");
        var active = activeOrders.selectByIdForUpdate(source.getActiveOrderId());
        var workOrder = workOrders.selectByIdForUpdate(source.getWorkOrderId());
        var app = applications.selectByIdForUpdate(applicationId);
        require(app != null && Objects.equals(app.getAppliedBy(), actor) && Objects.equals(app.getTenantId(), TenantContextHolder.getRequiredTenantId())
                && isCorrectable(app)
                && active != null && Objects.equals(active.getLeaderUserId(), actor) && "ACTIVE".equals(active.getActiveStatus())
                && workOrder != null && Objects.equals(active.getWorkOrderId(), app.getWorkOrderId())
                && Objects.equals(batch.getId(), app.getBatchExecutionId()), "当前账号不是该正式驳回申请的有效补正责任人");
        var latest = applications.selectLatestByActiveOrderId(app.getActiveOrderId());
        require(latest != null && Objects.equals(latest.getId(), app.getId()), "已存在更新放行轮次，不能接收历史补正任务");
        var existing = find(app.getId());
        if (existing != null) {
            requireSource(existing, actor);
            return existing.getId();
        }
        if (!marketCorrection(app)) lifecycle.requireReleaseAllowed(app.getBatchExecutionId());
        nonconformance.ensureWorkOrderNotFrozen(app.getWorkOrderId(), "接收PQC补正任务");
        nonconformance.ensureBatchNotFrozen(app.getBatchExecutionId(), "接收PQC补正任务");
        Long taskId = create(app, actor, formalReason(app));
        audit.append(GxpAuditCommand.builder().eventSchemaVersion(2).operationId("mes.pqc-release.correction-receive")
                .subjectId("PQC_RELEASE_CORRECTION:" + taskId).subjectVersion("1")
                .reason("原正式申请人主动接收放行补正任务").reasonCode("PQC_CORRECTION_RECEIVE").reasonSource("USER")
                .beforeState(GxpAuditStateEnvelope.builder().state("ABSENT").canonicalJson("{}").build())
                .afterState(GxpAuditStateEnvelope.builder().state("TODO").canonicalJson(JsonUtils.toJsonString(tasks.selectById(taskId))).build())
                .idempotencyKey("PQC_CORRECTION_RECEIVE:" + app.getId()).requestId("PQC_CORRECTION_RECEIVE:" + app.getId()).resultStatus("SUCCESS")
                .sourceType("SERVICE_METHOD").sourceLocator(MesPqcReleaseCorrectionService.class.getName() + "#receive")
                .links(List.of(new GxpAuditRelation("SOURCE", "RELEASE_APPLICATION", app.getId().toString(), String.valueOf(app.getVersion()), app.getSourceSnapshotHash())))
                .build());
        return taskId;
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public Long create(MesProcessPoolActiveOrderReleaseApplicationDO source, Long actor, String reason) {
        require(source != null && source.getId() != null && actor != null && actor > 0 && StrUtil.isNotBlank(reason),
                "补正任务缺少正式申请、操作人或原驳回原因");
        var app = applications.selectByIdForUpdate(source.getId());
        require(isCorrectable(app) && Objects.equals(app.getAppliedBy(), source.getAppliedBy()),
                "补正任务必须来自正式PQC驳回或上市撤回申请");
        require(Objects.equals(reason, formalReason(app)), "补正原因与原正式决定不一致");
        require(app.getAppliedBy() != null && app.getAppliedBy() > 0, "原申请人缺失，不能推断补正责任人");
        require(find(app.getId()) == null, "该驳回申请已有关联补正任务");
        var active = activeOrders.selectByIdForUpdate(app.getActiveOrderId());
        require(active != null && active.getDossierRevision() != null && active.getDossierRevision() >= 0,
                "正式资料版本缺失，不能建立补正基准");
        var frozen = new LinkedHashMap<String, Object>();
        frozen.put("applicationId", app.getId()); frozen.put("applicationVersion", app.getVersion());
        frozen.put("dossierRevision", active.getDossierRevision()); frozen.put("rejectedTaskId", app.getPqcReleaseWorkTaskId());
        frozen.put("correctionSourceType", sourceType(app)); frozen.put("correctionSourceId", sourceId(app));
        if (marketCorrection(app)) frozen.put("originalMarketTaskId", app.getReleaseApprovalWorkTaskId());
        if ("MARKET_RELEASE_REJECTED".equals(app.getApplicationStatus()))
            frozen.put("qualityReviewId", reviewMapper.selectLatestBySource("ACTIVE_ORDER", app.getActiveOrderId()).getId());
        frozen.put("dossierAttachmentIds", dossierFiles.selectListByActiveOrderId(app.getActiveOrderId()).stream()
                .map(row -> row.getId()).toList());
        var task = new MesProEdhrWorkTaskDO().setTaskCode("PQC-CORRECTION-" + app.getId()).setTaskType(TYPE)
                .setBusinessScopeType(SCOPE).setBusinessScopeId(app.getId()).setBatchExecutionId(app.getBatchExecutionId())
                .setWorkOrderId(app.getWorkOrderId()).setWorkOrderCode(app.getWorkOrderCode()).setBatchCode(app.getBatchCode())
                .setRouteId(app.getRouteId()).setProcessName(marketCorrection(app) ? "上市放行补正" : "PQC放行补正").setAssigneeUserId(app.getAppliedBy())
                .setCandidateSourceType("USER").setCandidateSourceId(app.getAppliedBy())
                .setCandidateUserSnapshot(app.getAppliedBy().toString()).setSourceUserId(actor)
                .setResponsibilitySourceType("APPLICANT").setResponsibilitySourceKey("RELEASE_APPLICATION:" + app.getId())
                .setResponsibilitySourceVersion(String.valueOf(app.getVersion())).setResponsibilitySourceDigest(app.getSourceSnapshotHash())
                .setResponsibilityScopeJson(JsonUtils.toJsonString(frozen)).setOwnershipLocked(true)
                .setReviewSourceType(sourceType(app)).setReviewSourceId(sourceId(app))
                .setReason(reason).setStatus("TODO").setRemark("由原申请人补正资料并重新提交PQC审查");
        if (marketCorrection(app)) require(correctionWritable(app.getAppliedBy(), task, app, active), "当前正式上市放行补正已不可派发");
        deadlineService.applyToWorkTask(task);
        require(tasks.insert(task) == 1 && task.getId() != null, "补正任务持久化失败");
        task.setActionUrl("/user/profile?tab=workbench&releaseCorrectionTaskId=" + task.getId());
        require(tasks.updateById(new MesProEdhrWorkTaskDO().setId(task.getId()).setActionUrl(task.getActionUrl())) == 1,
                "补正任务入口绑定失败");
        var notice = new NotifySendSingleToUserIdempotentReqDTO();
        notice.setUserId(task.getAssigneeUserId()); notice.setTemplateCode("MES_EDHR_PQC_CORRECTION_ASSIGNED");
        notice.setBusinessKey("MES_PQC_CORRECTION:" + task.getId() + ":USER:" + task.getAssigneeUserId());
        notice.setTemplateParams(java.util.Map.of("workOrderCode", task.getWorkOrderCode(), "batchCode", task.getBatchCode(),
                "reason", reason, "actionUrl", task.getActionUrl(), "workTaskId", task.getId().toString()));
        auxiliaryAudit.notifications(task, () -> {
            Long messageId = notifyApi.sendSingleMessageIdempotentlyToAdmin(notice);
            require(messageId != null && messageId > 0, "PQC补正通知未取得正式消息回执");
            return List.of(messageId);
        });
        return task.getId();
    }

    @Transactional(readOnly = true)
    public Detail get(Long actor, Long workTaskId) {
        var task = requireTask(actor, workTaskId);
        var app = requireSource(task, actor);
        var active = activeOrders.selectById(app.getActiveOrderId());
        require(active != null && active.getDossierRevision() != null, "正式资料版本缺失");
        var frozen = snapshot(task);
        boolean writable = correctionWritable(actor, task, app, active);
        return new Detail(task.getId(), app.getId(), app.getActiveOrderId(), app.getBatchExecutionId(),
                app.getWorkOrderCode(), app.getBatchCode(), task.getAssigneeUserId(), task.getReason(),
                task.getStatus(), app.getVersion(), frozen.getLong("dossierRevision"), active.getDossierRevision(),
                writable,
                frozen.getLong("nextApplicationId"));
    }

    private boolean correctionWritable(Long actor, MesProEdhrWorkTaskDO task,
            MesProcessPoolActiveOrderReleaseApplicationDO app,
            MesProcessPoolActiveOrderDO active) {
        if (!OPEN.contains(task.getStatus()) || !Objects.equals(active.getLeaderUserId(), actor)
                || !Objects.equals(active.getTenantId(), app.getTenantId())
                || !Objects.equals(active.getWorkOrderId(), app.getWorkOrderId())
                || !Objects.equals(task.getWorkOrderId(), app.getWorkOrderId())
                || !sourceType(app).equals(task.getReviewSourceType())
                || !"ACTIVE".equals(active.getActiveStatus())
                || active.getBusinessStatus() != null && Set.of("FROZEN", "CLOSED", "VOIDED", "RELEASED", "CANCELED").contains(active.getBusinessStatus())
                || !permissions.hasAnyPermissions(actor, "mes:pro-process-pool-team-leader:maintain")) return false;
        var latest = applications.selectLatestByActiveOrderId(app.getActiveOrderId());
        var batch = batches.selectById(app.getBatchExecutionId());
        var workOrder = workOrders.selectById(app.getWorkOrderId());
        if (latest == null || !Objects.equals(latest.getId(), app.getId()) || batch == null || workOrder == null
                || !Objects.equals(batch.getTenantId(), app.getTenantId()) || !Objects.equals(workOrder.getTenantId(), app.getTenantId())
                || !Objects.equals(batch.getWorkOrderId(), app.getWorkOrderId()) || batch.getStatus() == null
                || Set.of(15, 40, 50, 60).contains(batch.getStatus())
                || Boolean.TRUE.equals(workOrder.getTemporaryFrozen()) || Integer.valueOf(3).equals(workOrder.getStatus())) return false;
        var ncrCount = reviewMapper.selectCount(new LambdaQueryWrapperX<MesProEdhrNonconformanceReviewDO>()
                .and(scope -> scope.eq(MesProEdhrNonconformanceReviewDO::getWorkOrderId, app.getWorkOrderId())
                        .or().eq(MesProEdhrNonconformanceReviewDO::getBatchExecutionId, app.getBatchExecutionId())
                        .or().nested(order -> order.eq(MesProEdhrNonconformanceReviewDO::getActiveOrderId, active.getId())
                                .ne(MesProEdhrNonconformanceReviewDO::getSourceType, "PQC_RELEASE")))
                .and(state -> state.eq(MesProEdhrNonconformanceReviewDO::getReviewStatus, "pending_review")
                        .or().eq(MesProEdhrNonconformanceReviewDO::getDisposition, "void")));
        var voidCount = changeMapper.selectCount(new LambdaQueryWrapperX<MesProEdhrRecordChangeEventDO>()
                .eq(MesProEdhrRecordChangeEventDO::getBatchExecutionId, app.getBatchExecutionId())
                .eq(MesProEdhrRecordChangeEventDO::getTargetScope, "BATCH")
                .eq(MesProEdhrRecordChangeEventDO::getChangeType, "VOID")
                .in(MesProEdhrRecordChangeEventDO::getChangeStatus, Set.of("DRAFT", "SUBMITTED", "APPROVED")));
        require(ncrCount != null && voidCount != null, "补正生命周期读取未取得正式结果");
        return ncrCount == 0 && voidCount == 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public Receipt resubmit(Long actor, Resubmit command) {
        require(command != null && StrUtil.isNotBlank(command.correctionNote()) && command.correctionNote().trim().length() <= 500,
                "请填写1至500字的实际补正说明");
        String key = MesReleaseFlowIdempotency.requireKey(command.idempotencyKey());
        audit.acquireLedgerLock();
        var initial = requireTask(actor, command.workTaskId());
        var source = applications.selectById(initial.getBusinessScopeId());
        require(source != null, "补正原申请已不存在");
        // Same lock order as PQC decisions and dossier uploads: batch, active order, application, task.
        var batch = batches.selectByTenantIdAndIdForUpdate(TenantContextHolder.getRequiredTenantId(), source.getBatchExecutionId());
        require(batch != null, "补正申请正式批次已不存在");
        var active = activeOrders.selectByIdForUpdate(source.getActiveOrderId());
        require(workOrders.selectByIdForUpdate(source.getWorkOrderId()) != null, "补正申请正式工单已不存在");
        var app = applications.selectByIdForUpdate(source.getId());
        var task = tasks.selectOne(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>().eq(MesProEdhrWorkTaskDO::getId,
                command.workTaskId()).last("FOR UPDATE"));
        requireSource(task, actor);
        var frozen = snapshot(task);
        if ("DONE".equals(task.getStatus())) {
            require(Objects.equals(frozen.getString("resubmitKey"), key)
                    && Objects.equals(frozen.getString("correctionNote"), command.correctionNote().trim()),
                    "该补正任务已提交其他请求，请刷新查看原回执");
            var next = applications.selectById(frozen.getLong("nextApplicationId"));
            require(next != null && Objects.equals(next.getAppliedBy(), actor), "补正重提回执缺失");
            return new Receipt(task.getId(), next.getId(), next.getPqcReleaseWorkTaskId(), next.getApplicationStatus());
        }
        require(OPEN.contains(task.getStatus()) && Objects.equals(app.getVersion(), command.expectedVersion()),
                "补正任务或申请版本已变化，请刷新");
        var latest = applications.selectLatestByActiveOrderId(app.getActiveOrderId());
        require(latest != null && Objects.equals(latest.getId(), app.getId()), "已有更新的放行轮次，不能重提历史申请");
        require(active != null && "ACTIVE".equals(active.getActiveStatus()) && Objects.equals(active.getLeaderUserId(), actor)
                && permissions.hasAnyPermissions(actor, "mes:pro-process-pool-team-leader:maintain"),
                "补正责任人已不具备当前活跃订单申请资格，请先明确正式责任人");
        Long baseline = frozen.getLong("dossierRevision");
        require(baseline != null && active.getDossierRevision() != null && active.getDossierRevision() > baseline,
                "尚无原决定后的实际资料补正，请先通过正式资料入口上传补正文件");
        var originalAttachments = frozen.getJSONArray("dossierAttachmentIds");
        require(originalAttachments != null, "补正任务缺少原决定时正式资料清单");
        var originalIds = originalAttachments.toJavaList(Long.class);
        require(dossierFiles.selectListByActiveOrderId(app.getActiveOrderId()).stream()
                .anyMatch(row -> row.getId() != null && !originalIds.contains(row.getId())
                        && Objects.equals(row.getApplicationId(), app.getId())),
                "尚无原决定后的有效新增补正文件，删除文件不能单独完成补正");
        require(tasks.selectCount(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getTaskType, "DOSSIER_COMPLETE")
                .eq(MesProEdhrWorkTaskDO::getBusinessScopeType, "ACTIVE_ORDER_DOSSIER")
                .eq(MesProEdhrWorkTaskDO::getBusinessScopeId, app.getActiveOrderId())
                .in(MesProEdhrWorkTaskDO::getStatus, OPEN)) == 0, "仍有明确指派的资料补件未完成，请先补齐对应资料");
        if (marketCorrection(app)) require(correctionWritable(actor, task, app, active), "当前正式上市放行补正已不可办理");
        else lifecycle.requireReleaseAllowed(app.getBatchExecutionId());
        nonconformance.ensureWorkOrderNotFrozen(app.getWorkOrderId(), "补正重提PQC放行");
        nonconformance.ensureBatchNotFrozen(app.getBatchExecutionId(), "补正重提PQC放行");
        var next = releaseApplications.applyCorrection(actor, new MesTeamLeaderActiveOrderReleaseApplyCommand()
                .setActiveOrderId(app.getActiveOrderId()).setIdempotencyKey(key)
                .setApplyRemark(command.correctionNote().trim()), app.getId(), app.getBatchExecutionId());
        frozen.put("resubmitKey", key); frozen.put("correctionNote", command.correctionNote().trim());
        frozen.put("nextApplicationId", next.getApplicationId()); frozen.put("submittedDossierRevision", active.getDossierRevision());
        var at = LocalDateTime.now();
        require(tasks.update(new MesProEdhrWorkTaskDO().setStatus("DONE").setCompletedAt(at)
                        .setResponsibilityScopeJson(frozen.toJSONString()), new LambdaUpdateWrapper<MesProEdhrWorkTaskDO>()
                        .eq(MesProEdhrWorkTaskDO::getId, task.getId()).eq(MesProEdhrWorkTaskDO::getTaskType, TYPE)
                        .eq(MesProEdhrWorkTaskDO::getAssigneeUserId, actor).in(MesProEdhrWorkTaskDO::getStatus, OPEN)) == 1,
                "补正任务完成状态发生并发变化");
        audit.append(GxpAuditCommand.builder().eventSchemaVersion(2).operationId("mes.pqc-release.correction-resubmit")
                .subjectId("PQC_RELEASE_CORRECTION:" + task.getId()).subjectVersion("1")
                .reason(command.correctionNote().trim()).reasonCode("PQC_CORRECTION_RESUBMIT").reasonSource("USER")
                .beforeState(GxpAuditStateEnvelope.builder().state(task.getStatus()).canonicalJson(JsonUtils.toJsonString(task)).build())
                .afterState(GxpAuditStateEnvelope.builder().state("DONE").canonicalJson(frozen.toJSONString()).build())
                .idempotencyKey("PQC_CORRECTION_RESUBMIT:" + task.getId()).requestId(key).resultStatus("SUCCESS")
                .sourceType("SERVICE_METHOD").sourceLocator(MesPqcReleaseCorrectionService.class.getName() + "#resubmit")
                .links(List.of(new GxpAuditRelation("SOURCE", "RELEASE_APPLICATION", app.getId().toString(), String.valueOf(app.getVersion()), app.getSourceSnapshotHash()),
                        new GxpAuditRelation("RESULT", "RELEASE_APPLICATION", next.getApplicationId().toString(), String.valueOf(next.getVersion()), next.getSourceSnapshotHash())))
                .build());
        return new Receipt(task.getId(), next.getApplicationId(), next.getPqcReleaseWorkTaskId(), next.getStatus());
    }

    /** Called under the same ledger/row locks as dossier mutation; market authority is separate from correction duty. */
    public boolean requireMarketCorrectionMutation(Long actor, Long activeOrderId, Long applicationId) {
        var app = applications.selectLatestByActiveOrderId(activeOrderId);
        if (app == null || !marketCorrection(app)) return false;
        require(applicationId == null || Objects.equals(applicationId, app.getId()), "补正资料不属于当前正式撤回轮次");
        var task = find(app.getId());
        requireTask(actor, task == null ? null : task.getId());
        requireSource(task, actor);
        var active = activeOrders.selectById(activeOrderId);
        require(active != null && correctionWritable(actor, task, app, active), "当前正式上市放行补正已不可办理");
        return true;
    }

    public MesProEdhrWorkTaskDO find(Long applicationId) {
        return tasks.selectOne(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getTaskType, TYPE).eq(MesProEdhrWorkTaskDO::getBusinessScopeType, SCOPE)
                .eq(MesProEdhrWorkTaskDO::getBusinessScopeId, applicationId));
    }
    private MesProEdhrWorkTaskDO requireTask(Long actor, Long id) {
        require(actor != null && id != null && permissions.hasAnyPermissions(actor, "mes:pro-edhr-work-task:query"), "无补正任务查询权限");
        var task = tasks.selectById(id);
        require(task != null && TYPE.equals(task.getTaskType()) && SCOPE.equals(task.getBusinessScopeType())
                && Objects.equals(task.getAssigneeUserId(), actor) && Boolean.TRUE.equals(task.getOwnershipLocked()),
                "补正任务不属于当前责任人");
        return task;
    }
    private MesProcessPoolActiveOrderReleaseApplicationDO requireSource(MesProEdhrWorkTaskDO task, Long actor) {
        require(task != null && TYPE.equals(task.getTaskType()) && SCOPE.equals(task.getBusinessScopeType())
                && Objects.equals(task.getAssigneeUserId(), actor), "补正任务身份已变化");
        var app = applications.selectById(task.getBusinessScopeId());
        require(app != null && Objects.equals(app.getTenantId(), TenantContextHolder.getRequiredTenantId())
                && isCorrectable(app)
                && Objects.equals(app.getAppliedBy(), actor) && Objects.equals(app.getBatchExecutionId(), task.getBatchExecutionId())
                && Objects.equals(sourceId(app), task.getReviewSourceId())
                && Objects.equals(sourceType(app), task.getReviewSourceType()), "补正任务与原正式决定回执不一致");
        require(Objects.equals(task.getReason(), formalReason(app, snapshot(task).getLong("qualityReviewId"))), "补正任务与原正式决定原因不一致");
        return app;
    }
    private com.alibaba.fastjson.JSONObject snapshot(MesProEdhrWorkTaskDO task) {
        require(StrUtil.isNotBlank(task.getResponsibilityScopeJson()), "补正任务缺少正式基准快照");
        var snapshot = com.alibaba.fastjson.JSON.parseObject(task.getResponsibilityScopeJson());
        require(snapshot != null && Objects.equals(snapshot.getLong("applicationId"), task.getBusinessScopeId())
                && ("PQC_REJECTION".equals(task.getReviewSourceType())
                    && Objects.equals(snapshot.getLong("rejectedTaskId"), task.getReviewSourceId())
                    || Set.of("MARKET_WITHDRAWAL", "MARKET_REJECTION").contains(task.getReviewSourceType())
                    && Objects.equals(snapshot.getString("correctionSourceType"), task.getReviewSourceType())
                    && Objects.equals(snapshot.getLong("correctionSourceId"), task.getReviewSourceId())
                    && snapshot.getLong("originalMarketTaskId") != null), "补正基准快照与原正式申请不一致");
        return snapshot;
    }
    private static void require(boolean value, String message) {
        if (!value) throw ServiceExceptionUtil.invalidParamException(message);
    }
    public record Resubmit(Long workTaskId, Integer expectedVersion, String idempotencyKey, String correctionNote) { }
    public record Receipt(@JsonSerialize(using = ToStringSerializer.class) Long workTaskId,
                          @JsonSerialize(using = ToStringSerializer.class) Long applicationId,
                          @JsonSerialize(using = ToStringSerializer.class) Long pqcReleaseWorkTaskId, String status) { }
    public record Detail(@JsonSerialize(using = ToStringSerializer.class) Long workTaskId,
                         @JsonSerialize(using = ToStringSerializer.class) Long applicationId,
                         @JsonSerialize(using = ToStringSerializer.class) Long activeOrderId,
                         @JsonSerialize(using = ToStringSerializer.class) Long batchExecutionId,
                         String workOrderCode, String batchCode,
                         @JsonSerialize(using = ToStringSerializer.class) Long assigneeUserId,
                         String rejectReason, String taskStatus, Integer expectedVersion,
                         Long rejectedDossierRevision, Long currentDossierRevision, boolean writable,
                         @JsonSerialize(using = ToStringSerializer.class) Long nextApplicationId) { }
}
