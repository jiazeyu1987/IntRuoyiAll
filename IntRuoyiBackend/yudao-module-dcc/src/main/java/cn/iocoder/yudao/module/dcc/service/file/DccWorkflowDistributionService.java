package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccWorkflowDistributionReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccWorkflowDistributionRecipientRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.*;

/** Saves the actual per-file recipient selection, then performs the existing reliable delivery action. */
@Service
public class DccWorkflowDistributionService {
    @Resource private DccControlledFileMapper controlledFileMapper;
    @Resource private DccControlledFileDistributionMapper distributionMapper;
    @Resource private DccControlledFileDistributionRecipientMapper recipientMapper;
    @Resource private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Resource private AdminUserApi adminUserApi;
    @Resource private DeptApi deptApi;
    @Resource private DccControlledFileFinalizationService finalizationService;
    @Resource private PermissionApi permissionApi;
    @Resource private DccControlledFileMessageJobMapper messageJobMapper;
    @Resource private DccControlledFileMessageDeliveryService messageDeliveryService;

    /** Consumes an approved formal roster; never chooses snapshot timing or filters disabled accounts here. */
    @Transactional(rollbackFor = Exception.class)
    public void dispatchActivatedDistribution(DccControlledFileLifecycleEvent event, DccDistributionRosterSnapshot snapshot) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("自动下发必须在正式事务中保存义务和通知任务");
        if (event == null || !"ACTIVATED".equals(event.eventType()) || !Objects.equals(tenantId, event.tenantId())
                || event.controlledFileId() == null || event.controlledFileId() <= 0 || event.masterId() == null
                || event.versionNo() == null || event.versionNo().isBlank() || event.approvalProcessInstanceId() == null
                || event.approvalProcessInstanceId().isBlank() || event.occurredAt() == null || snapshot == null
                || !Objects.equals(tenantId, snapshot.tenantId())
                || !Objects.equals(event.controlledFileId(), snapshot.controlledFileId())
                || !Objects.equals(event.versionNo(), snapshot.versionNo())
                || !Objects.equals(event.approvalProcessInstanceId(), snapshot.approvalProcessInstanceId())
                || snapshot.snapshotAt() == null || snapshot.policyVersion() == null || snapshot.policyVersion().isBlank())
            throw new IllegalArgumentException("自动下发缺少本次生效事件及正式全员名单快照");
        var file = controlledFileMapper.selectByIdAndTenantForUpdate(tenantId, event.controlledFileId());
        if (file == null || !Objects.equals(file.getTenantId(), tenantId)
                || !Objects.equals(file.getMasterId(), event.masterId())
                || !Objects.equals(file.getVersionNo(), event.versionNo())
                || !Objects.equals(file.getProcessInstanceId(), event.approvalProcessInstanceId())
                || file.getControlledTime() == null || file.getActivatedTime() == null
                || !Objects.equals(file.getActivatedTime(), event.occurredAt())
                || !Set.of("ACTIVE", "OBSOLETE", "SUPERSEDED").contains(file.getStatus()))
            throw new IllegalStateException("自动下发版本尚未受控生效或事件身份不匹配");
        if (snapshot.departments() == null || snapshot.departments().isEmpty())
            throw new IllegalArgumentException("正式下发部门全员名单不能为空");
        List<Plan> plans = new ArrayList<>();
        Set<Long> departmentIds = new HashSet<>();
        for (var department : snapshot.departments()) {
            if (department == null || department.departmentId() == null || department.departmentId() <= 0
                    || !departmentIds.add(department.departmentId()) || department.userIds() == null
                    || department.userIds().isEmpty() || department.userIds().stream().anyMatch(id -> id == null || id <= 0)
                    || new HashSet<>(department.userIds()).size() != department.userIds().size())
                throw new IllegalArgumentException("正式下发部门或人员身份缺失、重复");
            plans.add(new Plan(department.departmentId(), "PUBLIC_FOLDER", department.userIds().stream().sorted().toList()));
        }
        plans.sort(Comparator.comparing(Plan::departmentId));
        String digest = DigestUtil.sha256Hex(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                List.of(tenantId, file.getId(), event.versionNo(), event.approvalProcessInstanceId(),
                        snapshot.snapshotAt(), snapshot.policyVersion(), plans)));
        if (file.getDistributionPayloadHash() != null && !digest.equals(file.getDistributionPayloadHash()))
            throw new IllegalArgumentException("自动下发重放的版本或正式名单与已保存证据不一致");
        var existing = distributionMapper.selectListByControlledFileIdForUpdate(tenantId, file.getId());
        if (existing == null) throw new IllegalStateException("正式下发范围读取失败");
        Map<Long, DccControlledFileDistributionDO> scopes = new LinkedHashMap<>();
        Map<Long, List<DccControlledFileDistributionRecipientDO>> scopeRecipients = new LinkedHashMap<>();
        // Prevalidate every existing scope before inserting any new job or obligation.
        for (var scope : existing) {
            if (scope == null || scope.getId() == null || !Objects.equals(file.getId(), scope.getControlledFileId())
                    || !"PUBLIC_FOLDER".equals(scope.getDistributionMedium())
                    || !Set.of("PENDING", "SENT", "READ", "ACKNOWLEDGED").contains(scope.getStatus())
                    || scopes.put(scope.getDepartmentId(), scope) != null)
                throw new IllegalStateException("既有下发义务身份异常，不能覆盖历史记录");
            var expected = plans.stream().filter(plan -> Objects.equals(plan.departmentId(), scope.getDepartmentId())).findFirst()
                    .orElseThrow(() -> new IllegalStateException("既有下发部门与正式快照冲突"));
            var rows = recipientMapper.selectListByDistributionIdForUpdate(tenantId, scope.getId());
            if (rows == null || rows.stream().anyMatch(row -> row == null || row.getId() == null || row.getUserId() == null)
                    || rows.stream().map(DccControlledFileDistributionRecipientDO::getUserId).distinct().count() != rows.size()
                    || !rows.stream().map(DccControlledFileDistributionRecipientDO::getUserId).sorted().toList().equals(expected.users()))
                throw new IllegalStateException("既有下发人员与正式快照冲突，不能重新选人");
            scopeRecipients.put(scope.getDepartmentId(), rows);
        }
        if (file.getDistributedTime() != null && (scopes.size() != plans.size() || file.getDistributionPayloadHash() == null))
            throw new IllegalStateException("既有下发事实缺少完整正式名单证据");
        List<DccControlledFileMessageJobDO> pendingJobs = new ArrayList<>();
        for (var plan : plans) {
            var scope = scopes.get(plan.departmentId());
            if (scope == null) {
                scope = DccControlledFileDistributionDO.builder().tenantId(tenantId).controlledFileId(file.getId())
                        .departmentId(plan.departmentId()).distributionMedium("PUBLIC_FOLDER").status("PENDING").build();
                int scopeRows = distributionMapper.insert(scope);
                if (scopeRows != 1 || scope.getId() == null)
                    throw new IllegalStateException("自动下发部门义务未持久化：affectedRows=" + scopeRows + ", generatedId=" + scope.getId());
                List<DccControlledFileDistributionRecipientDO> created = new ArrayList<>();
                for (Long userId : plan.users()) {
                    var row = DccControlledFileDistributionRecipientDO.builder().tenantId(tenantId).distributionId(scope.getId()).userId(userId).build();
                    if (recipientMapper.insert(row) != 1 || row.getId() == null)
                        throw new IllegalStateException("自动下发收件义务未持久化");
                    created.add(row);
                }
                scopeRecipients.put(plan.departmentId(), created);
            }
            for (var recipient : scopeRecipients.get(plan.departmentId())) {
                DccControlledFileMessageJobDO job;
                if (recipient.getMessageJobId() == null) {
                    job = DccControlledFileMessageJobDO.builder().tenantId(tenantId).businessType("DISTRIBUTION").businessId(scope.getId())
                            .recipientUserId(recipient.getUserId()).templateCode("dcc_distribution").status("PENDING").build();
                    if (messageJobMapper.insert(job) != 1 || job.getId() == null)
                        throw new IllegalStateException("自动下发通知任务未持久化");
                    if (recipientMapper.updateById(DccControlledFileDistributionRecipientDO.builder()
                            .id(recipient.getId()).messageJobId(job.getId()).build()) != 1)
                        throw new IllegalStateException("自动下发通知与收件义务未关联");
                } else {
                    job = messageJobMapper.selectById(recipient.getMessageJobId());
                    if (job == null || !"DISTRIBUTION".equals(job.getBusinessType())
                            || !Objects.equals(job.getBusinessId(), scope.getId())
                            || !Objects.equals(job.getRecipientUserId(), recipient.getUserId())
                            || !"dcc_distribution".equals(job.getTemplateCode()))
                        throw new IllegalStateException("既有通知与精确下发身份不匹配");
                }
                if (Set.of("PENDING", "FAILED").contains(job.getStatus())) pendingJobs.add(job);
                else if (!"SENT".equals(job.getStatus())) throw new IllegalStateException("自动下发通知状态异常");
            }
        }
        if (file.getDistributedTime() == null && controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(file.getId()).distributionPayloadHash(digest).distributedTime(java.time.LocalDateTime.now().withNano(0)).build()) != 1)
            throw new IllegalStateException("自动下发事实未持久化");
        for (var job : pendingJobs) messageDeliveryService.dispatchMessageJob(job, Map.of());
    }

    public List<DccWorkflowDistributionRecipientRespVO> recipientOptions(Long userId, Long fileId, Long departmentId) {
        var file=controlledFileMapper.selectById(fileId);
        if(userId==null || !permissionApi.hasAnyRoles(userId,"doc_control")
                || file==null || !Objects.equals(file.getTenantId(),TenantContextHolder.getRequiredTenantId())
                || file.getControlledTime()==null || !("ACTIVE".equals(file.getStatus()) || "CONTROLLED_PENDING_EFFECTIVE".equals(file.getStatus()))
                || !permissionSupport.hasCategoryPermission(file.getCategoryId(),userId,DccFileCategoryPermissionActionEnum.DISTRIBUTE))
            throw new IllegalArgumentException("当前文件或办理资格不允许读取下发接收人");
        if(departmentId==null || departmentId<=0) throw new IllegalArgumentException("请选择有效接收部门");
        deptApi.validateDeptList(List.of(departmentId));
        var users=adminUserApi.getUserListByDeptIds(List.of(departmentId));
        if(users==null) throw new IllegalStateException("下发接收人目录返回缺失");
        if(users.stream().anyMatch(Objects::isNull)) throw new IllegalStateException("下发接收人目录包含空账号");
        if(users.stream().map(cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO::getId).distinct().count()!=users.size())
            throw new IllegalStateException("下发接收人目录包含重复身份");
        return users.stream().filter(user->Integer.valueOf(0).equals(user.getStatus()))
                .map(user -> {
                    if(user.getId()==null || user.getId()<=0 || !Objects.equals(departmentId,user.getDeptId()))
                        throw new IllegalStateException("下发接收人目录账号与部门身份不匹配");
                    String name=user.getNickname();
                    if(name==null || name.isBlank()) name=user.getUsername();
                    if(name==null || name.isBlank()) throw new IllegalStateException("下发接收人姓名未维护");
                    return new DccWorkflowDistributionRecipientRespVO(user.getId(),name);
                }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void distribute(Long userId, Long fileId, DccWorkflowDistributionReqVO request) {
        var file=controlledFileMapper.selectByIdAndTenantForUpdate(TenantContextHolder.getRequiredTenantId(),fileId);
        if (file != null && file.getProcessInstanceId() != null && !file.getProcessInstanceId().isBlank())
            throw new IllegalArgumentException("本轮下发由生效事件按正式保存部门全员自动执行，不允许重新选择人员");
        if(userId==null || !permissionApi.hasAnyRoles(userId,"doc_control") || file==null || file.getControlledTime()==null
                || !permissionSupport.hasCategoryPermission(file.getCategoryId(),userId,DccFileCategoryPermissionActionEnum.DISTRIBUTE))
            throw new IllegalArgumentException("文件尚未受控或无下发资格");
        List<Plan> plans=validate(request,false);
        String digest=DigestUtil.sha256Hex(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(plans));
        if(file.getDistributedTime()!=null) {
            if(!digest.equals(file.getDistributionPayloadHash())) throw new IllegalArgumentException("已下发内容与重放请求不一致");
            return;
        }
        if (!"ACTIVE".equals(file.getStatus()) || file.getActivatedTime() == null)
            throw new IllegalArgumentException("当前文件状态不允许首次下发");
        validate(request,true);
        if(!distributionMapper.selectListByControlledFileId(fileId).isEmpty())
            throw new IllegalStateException("文件已有下发名单，不能覆盖既有下发记录");
        for(var plan:plans) {
            var distribution=DccControlledFileDistributionDO.builder().controlledFileId(fileId)
                    .departmentId(plan.departmentId()).distributionMedium(plan.medium()).status("PENDING").build();
            if(distributionMapper.insert(distribution)!=1 || distribution.getId()==null)
                throw new IllegalStateException("下发范围保存失败");
            for(Long user:plan.users()) {
                if(recipientMapper.insert(DccControlledFileDistributionRecipientDO.builder()
                        .distributionId(distribution.getId()).userId(user).build())!=1)
                    throw new IllegalStateException("下发接收人保存失败");
            }
        }
        if(controlledFileMapper.updateById(DccControlledFileDO.builder().id(fileId).distributionPayloadHash(digest).build())!=1)
            throw new IllegalStateException("下发请求证据保存失败");
        // The finalization command dispatches the saved electronic recipient rows and stamps distributedTime.
        finalizationService.releaseManualDistribution(userId,fileId);
    }

    private List<Plan> validate(DccWorkflowDistributionReqVO request, boolean checkDirectory) {
        if(request==null || request.getScopes()==null || request.getScopes().isEmpty())
            throw new IllegalArgumentException("请确认下发部门、人员和方式");
        List<Plan> plans=new ArrayList<>();Set<Long> departments=new HashSet<>();
        for(var scope:request.getScopes()) {
            if(scope==null || scope.getDepartmentId()==null || !departments.add(scope.getDepartmentId())
                    || !("PAPER".equals(scope.getDistributionMedium()) || "PUBLIC_FOLDER".equals(scope.getDistributionMedium())))
                throw new IllegalArgumentException("下发部门重复或方式无效");
            if(checkDirectory) deptApi.validateDeptList(List.of(scope.getDepartmentId()));
            List<Long> users=scope.getRecipientUserIds()==null ? List.of():scope.getRecipientUserIds();
            if(users.stream().anyMatch(Objects::isNull) || new HashSet<>(users).size()!=users.size()
                    || "PUBLIC_FOLDER".equals(scope.getDistributionMedium()) && users.isEmpty())
                throw new IllegalArgumentException("电子下发必须选择有效接收人");
            if(checkDirectory && !users.isEmpty()) {
                var directory=adminUserApi.getUserList(users);
                if(directory==null || directory.size()!=users.size() || directory.stream().anyMatch(user ->
                        user==null || !Integer.valueOf(0).equals(user.getStatus())
                                || !Objects.equals(user.getDeptId(),scope.getDepartmentId()) || !users.contains(user.getId())))
                    throw new IllegalArgumentException("下发接收人已失效或不属于所选部门");
                Set<Long> returnedUserIds=directory.stream().map(cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO::getId)
                        .collect(java.util.stream.Collectors.toSet());
                if(returnedUserIds.size()!=directory.size() || !returnedUserIds.equals(new HashSet<>(users)))
                    throw new IllegalArgumentException("接收人目录返回重复或缺失账号，不能保存下发名单");
            }
            plans.add(new Plan(scope.getDepartmentId(),scope.getDistributionMedium(),users.stream().sorted().toList()));
        }
        return plans.stream().sorted(Comparator.comparing(Plan::departmentId)).toList();
    }
    private record Plan(Long departmentId,String medium,List<Long> users) {}
}
