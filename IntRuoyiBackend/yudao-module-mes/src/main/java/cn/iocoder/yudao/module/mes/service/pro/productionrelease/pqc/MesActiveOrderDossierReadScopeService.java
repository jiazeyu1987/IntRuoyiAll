package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDossierFileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionVisibilityService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_DOSSIER_FILE_BLOCKED;

/** Read authorization only. Upload/delete retain their separate owner and lifecycle guards. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MesActiveOrderDossierReadScopeService {
    private static final String BATCH_QUERY = "mes:pro-edhr-batch-execution:query";
    private static final String PQC_QUERY = "mes:pro-production-release:query";
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProEdhrBatchExecutionMapper batchExecutionMapper;
    private final MesProEdhrBatchExecutionVisibilityService batchVisibilityService;
    private final MesProEdhrBatchExecutionOriginMapper batchOriginMapper;
    private final MesPqcProductionReleaseService pqcReleaseService;
    private final MesProEdhrWorkTaskMapper workTaskMapper;
    private final PermissionApi permissionApi;

    public Context requireBatch(Long actor, Long batchExecutionId, Long activeOrderId) {
        Long tenant = requireActor(actor);
        require((batchExecutionId == null) != (activeOrderId == null), "批次资料查询必须且只能提供一个正式范围。");
        require(permissionApi.hasAnyPermissions(actor, BATCH_QUERY), "当前用户无批次资料查询权限。");
        if (batchExecutionId != null) {
            require(positive(batchExecutionId), "正式批次编号无效。");
            MesProEdhrBatchExecutionDO batch = requireVisibleBatch(actor, tenant, batchExecutionId);
            MesProcessPoolActiveOrderReleaseApplicationDO app = applicationMapper.selectByBatchExecutionId(batchExecutionId);
            MesProcessPoolActiveOrderDO active = requireActive(app == null ? null : app.getActiveOrderId(), tenant);
            requireApplication(app, active, tenant);
            requireBatchIdentity(batch, app, active);
            return new Context(active, app);
        }
        require(positive(activeOrderId), "活跃订单编号无效。");
        MesProcessPoolActiveOrderDO active = requireActive(activeOrderId, tenant);
        MesProcessPoolActiveOrderReleaseApplicationDO app = applicationMapper.selectLatestByActiveOrderId(activeOrderId);
        requireApplication(app, active, tenant);
        requireFormalBatch(actor, tenant, app, active);
        return new Context(active, app);
    }

    /** Upload authorization is independent of caller-supplied application IDs. */
    public Context requireMutation(Long actor, Long activeId, Long requestedApplicationId, boolean deleting) {
        Long tenant = requireActor(actor);
        var active = requireActive(activeId, tenant);
        var app = applicationMapper.selectLatestByActiveOrderId(activeId);
        if (app != null && positive(app.getBatchExecutionId())) {
            var applications = applicationMapper.selectList(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                    .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getBatchExecutionId, app.getBatchExecutionId())
                    .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getTenantId, tenant));
            require(applications != null && applications.size() == 1
                    && Objects.equals(applications.get(0).getId(), app.getId()), "正式批次放行申请归属不唯一，不能写入资料。");
        }
        if (app != null) requireApplication(app, active, tenant);
        require(requestedApplicationId == null || app != null && Objects.equals(requestedApplicationId, app.getId()),
                "上传申请与正式活跃订单关联不一致。");
        if (Objects.equals(active.getLeaderUserId(), actor)
                && permissionApi.hasAnyPermissions(actor, "mes:pro-process-pool-team-leader:maintain")) {
            return new Context(active, app);
        }
        requireApplication(app, active, tenant);
        // Choose the current formal responsibility before invoking an authorization path.
        if (permissionApi.hasAnyPermissions(actor, "mes:pro-production-release:pqc-approve")
                && isFrozenPqcCandidate(actor, app) && isPqcDossierWritable(app)) {
            var batch = batchExecutionMapper.selectById(app.getBatchExecutionId());
            require(batch != null && Objects.equals(batch.getTenantId(), tenant), "PQC申请正式批次租户不一致。");
            requireBatchIdentity(batch, app, active);
            return new Context(active, app);
        }
        require(!deleting && permissionApi.hasAnyPermissions(actor, "mes:pro-edhr-batch-execution:upload"),
                "当前用户无该资料的写入权限。");
        requireFormalBatch(actor, tenant, app, active);
        return new Context(active, app);
    }

    public void requirePreview(Long actor, MesProcessPoolActiveOrderDossierFileDO row) {
        Long tenant = requireActor(actor);
        require(row != null && positive(row.getId()) && positive(row.getFileId())
                && positive(row.getActiveOrderId()) && Objects.equals(tenant, row.getTenantId()),
                "资料文件身份或租户不一致。");
        MesProcessPoolActiveOrderDO active = requireActive(row.getActiveOrderId(), tenant);
        MesProcessPoolActiveOrderReleaseApplicationDO app = null;
        if (row.getApplicationId() != null) {
            require(positive(row.getApplicationId()), "资料文件申请编号无效。");
            app = applicationMapper.selectById(row.getApplicationId());
            requireApplication(app, active, tenant);
            require(Objects.equals(row.getApplicationId(), app.getId()), "资料文件与申请身份不一致。");
        }
        if (Objects.equals(active.getLeaderUserId(), actor)) {
            return;
        }
        if (row.getApplicationId() == null) {
            app = applicationMapper.selectLatestByActiveOrderId(active.getId());
            requireApplication(app, active, tenant);
        }
        // Select an authorized business scope before invoking its reader; errors never switch scope.
        if (permissionApi.hasAnyPermissions(actor, PQC_QUERY) && isFrozenPqcCandidate(actor, app)) {
            var decision = pqcReleaseService.get(actor, app.getId());
            require(decision != null && Objects.equals(decision.getApplicationId(), app.getId())
                    && Objects.equals(decision.getPqcReleaseWorkTaskId(), app.getPqcReleaseWorkTaskId())
                    && Objects.equals(decision.getBatchExecutionId(), app.getBatchExecutionId()),
                    "PQC正式资料读取回执与申请不一致。");
            return;
        }
        require(permissionApi.hasAnyPermissions(actor, BATCH_QUERY), "当前用户不在该资料的正式读取范围内。");
        requireFormalBatch(actor, tenant, app, active);
    }

    private Long requireActor(Long actor) {
        var login = SecurityFrameworkUtils.getLoginUser();
        Long tenant = TenantContextHolder.getTenantId();
        require(positive(actor) && login != null && Objects.equals(actor, login.getId())
                && positive(tenant) && Objects.equals(tenant, login.getTenantId()), "资料读取身份与当前登录用户或租户不一致。");
        return tenant;
    }

    private MesProcessPoolActiveOrderDO requireActive(Long id, Long tenant) {
        require(positive(id), "资料缺少正式活跃订单身份。");
        var active = activeOrderMapper.selectById(id);
        require(active != null && Objects.equals(id, active.getId()) && positive(active.getWorkOrderId())
                && Objects.equals(tenant, active.getTenantId()), "资料活跃订单不存在或归属不一致。");
        return active;
    }

    private void requireApplication(MesProcessPoolActiveOrderReleaseApplicationDO app,
                                    MesProcessPoolActiveOrderDO active, Long tenant) {
        require(app != null && positive(app.getId()) && Objects.equals(tenant, app.getTenantId())
                && Objects.equals(active.getId(), app.getActiveOrderId())
                && Objects.equals(active.getWorkOrderId(), app.getWorkOrderId()), "资料申请与活跃订单、工单或租户不一致。");
    }

    private void requireFormalBatch(Long actor, Long tenant, MesProcessPoolActiveOrderReleaseApplicationDO app,
                                    MesProcessPoolActiveOrderDO active) {
        require(positive(app.getBatchExecutionId()), "资料申请缺少正式批次身份。");
        requireBatchIdentity(requireVisibleBatch(actor, tenant, app.getBatchExecutionId()), app, active);
    }

    private MesProEdhrBatchExecutionDO requireVisibleBatch(Long actor, Long tenant, Long batchId) {
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(batchId);
        require(batch != null && Objects.equals(batchId, batch.getId()) && Objects.equals(tenant, batch.getTenantId()),
                "正式批次不存在或编号、租户不一致。");
        // Authorization must not synchronize tasks or recover configuration through the detail reader.
        batchVisibilityService.requireVisibleBatch(batch, actor);
        return batch;
    }

    private void requireBatchIdentity(MesProEdhrBatchExecutionDO batch,
                                      MesProcessPoolActiveOrderReleaseApplicationDO app,
                                      MesProcessPoolActiveOrderDO active) {
        require(batch != null && positive(batch.getId()) && Objects.equals(batch.getId(), app.getBatchExecutionId())
                && Objects.equals(batch.getWorkOrderId(), active.getWorkOrderId()), "正式批次与资料申请、活跃订单或工单不一致。");
        var origins = batchOriginMapper.selectListByBatchExecutionId(batch.getId());
        require(origins != null && !origins.isEmpty() && origins.stream().allMatch(origin -> origin != null
                && positive(origin.getId()) && Objects.equals(origin.getTenantId(), active.getTenantId())
                && Objects.equals(origin.getBatchExecutionId(), batch.getId())
                && Objects.equals(origin.getActiveOrderId(), active.getId())
                && Objects.equals(origin.getWorkOrderId(), active.getWorkOrderId())),
                "正式批次来源与资料活跃订单、工单或租户不一致。");
    }

    private boolean isPqcDossierWritable(MesProcessPoolActiveOrderReleaseApplicationDO app) {
        var task = workTaskMapper.selectById(app.getPqcReleaseWorkTaskId());
        if (task == null || task.getStatus() == null) return false;
        if ("PQC_RELEASE_PENDING".equals(app.getApplicationStatus())) {
            return Set.of("TODO", "DOING", "OVERDUE").contains(task.getStatus());
        }
        return "DONE".equals(task.getStatus()) && "APPROVE".equals(task.getReason())
                && Set.of("REPORT_UPLOAD_PENDING", "MANAGER_RELEASE_PENDING").contains(
                        app.getApplicationStatus() == null ? "" : app.getApplicationStatus());
    }

    private boolean isFrozenPqcCandidate(Long actor, MesProcessPoolActiveOrderReleaseApplicationDO app) {
        if (!positive(app.getPqcReleaseWorkTaskId())) return false;
        MesProEdhrWorkTaskDO task = workTaskMapper.selectById(app.getPqcReleaseWorkTaskId());
        return task != null && Objects.equals(task.getId(), app.getPqcReleaseWorkTaskId())
                && "PQC_PRODUCTION_RELEASE".equals(task.getTaskType())
                && "RELEASE_APPLICATION".equals(task.getBusinessScopeType())
                && Objects.equals(task.getBusinessScopeId(), app.getId())
                && Boolean.TRUE.equals(task.getOwnershipLocked())
                && task.getCandidateUserSnapshot() != null
                && Arrays.stream(task.getCandidateUserSnapshot().split(","))
                    .map(String::trim).anyMatch(String.valueOf(actor)::equals);
    }

    private static boolean positive(Long id) { return id != null && id > 0; }
    private static void require(boolean condition, String message) {
        if (!condition) throw ServiceExceptionUtil.exception(PRO_PROCESS_POOL_ACTIVE_ORDER_DOSSIER_FILE_BLOCKED, message);
    }
    public record Context(MesProcessPoolActiveOrderDO activeOrder, MesProcessPoolActiveOrderReleaseApplicationDO application) { }
}
