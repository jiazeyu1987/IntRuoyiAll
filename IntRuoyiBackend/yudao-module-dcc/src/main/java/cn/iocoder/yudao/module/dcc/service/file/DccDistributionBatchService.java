package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DccDistributionBatchService {
    @Resource private DccControlledFileMapper controlledFileMapper;
    @Resource private DccDistributionBatchMapper batchMapper;
    @Resource private DccControlledFileDistributionMapper distributionMapper;
    @Resource private DccControlledFileDistributionRecipientMapper recipientMapper;
    @Resource private DccControlledFileMessageJobMapper messageJobMapper;
    @Resource private JdbcTemplate jdbcTemplate;
    @Resource private GxpAuditService gxpAuditService;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource private DccWorkflowDistributionService distributionService;
    @Resource private org.springframework.beans.factory.ObjectProvider<DccDistributionRosterSource> rosterSources;
    @Resource private DccControlledFileMessageDeliveryService messageDeliveryService;
    @Resource private cn.iocoder.yudao.module.system.api.user.AdminUserApi adminUserApi;

    /** Production automatic entry: all roster and policy facts come from a unique trusted server source. */
    public DccDistributionBatchResult processAutomaticBatch(Long batchId) {
        return processFromFormalSource(batchId,null);
    }

    /** Called only after TaskService verifies the real human, exact batch and every distribution boundary. */
    public DccDistributionBatchResult retryAutomaticBatch(Long batchId,Long actorId) {
        var actor=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser();
        if(actorId==null || actor==null || !Objects.equals(actorId,actor.getId())
                || !Objects.equals(actor.getTenantId(),TenantContextHolder.getRequiredTenantId()))
            throw new IllegalArgumentException("下发重试需要本租户真实登录操作者");
        return processFromFormalSource(batchId,actorId);
    }

    private DccDistributionBatchResult processFromFormalSource(Long batchId,Long retryActor) {
        var identity=requireBatchIdentity(batchId);
        try {
            transaction().executeWithoutResult(ignored->{
                var file=controlledFileMapper.selectByIdAndTenantForUpdate(identity.getTenantId(),identity.getControlledFileId());
                var batch=batchMapper.lock(identity.getTenantId(),batchId);
                requireProcessingIdentity(file,batch);
                var before=DccDistributionAuditSupport.snapshot(batch);
                DccDistributionBatchResult result;
                if(batch.getSnapshotPayloadHash()!=null) {
                    var frozen=rebuildFrozenSnapshot(batch);
                    if("SENT".equals(batch.getStatus()))return;
                    requireEnabledFrozenAccounts(batch,frozen);
                    result=prepareLockedBatch(batch,frozen,Map.of("source","PERSISTED_BATCH_RECIPIENTS"));
                    for(var job:messageJobMapper.selectBatchJobsForUpdate(batch.getTenantId(),batchId))
                        if(Set.of("PENDING","FAILED").contains(job.getStatus()))messageDeliveryService.dispatchMessageJob(job,Map.of());
                } else {
                    var installed=rosterSources.orderedStream().toList();
                    if(installed.isEmpty())throw new RosterNotReady("ROSTER_SOURCE_MISSING","未安装正式下发名册来源，未生成下发义务");
                    if(installed.size()!=1)throw new RosterNotReady("ROSTER_SOURCE_AMBIGUOUS","正式下发名册来源注册不唯一");
                    if(batch.getDepartmentIdsJson()==null)throw new RosterNotReady("DISTRIBUTION_DEPARTMENTS_MISSING","本版本未保存独立下发部门");
                    var departments=JsonUtils.parseArray(normalizeDepartments(batch.getDepartmentIdsJson()),String.class);
                    if(departments.isEmpty())throw new RosterNotReady("EMPTY_SCOPE_UNDECIDED","空下发部门是否免下发尚未确定");
                    var request=new DccDistributionRosterSource.Request(batch.getTenantId(),batchId,batch.getSourceEventId(),batch.getEventKey(),batch.getMasterId(),
                            batch.getControlledFileId(),batch.getVersionNo(),batch.getApprovalProcessInstanceId(),batch.getSourceOccurredAt(),List.copyOf(departments));
                    DccDistributionRosterSource.Resolution resolution;
                    try {resolution=installed.get(0).resolve(request);}catch(RuntimeException failure){throw new RosterSourceFailure(failure);}
                    if(resolution instanceof DccDistributionRosterSource.NotReady notReady) {
                        if(notReady.errorCode()==null || !notReady.errorCode().matches("ROSTER_[A-Z0-9_]{1,56}") || notReady.reason()==null || notReady.reason().isBlank())
                            throw new RosterNotReady("ROSTER_SOURCE_INVALID","正式名册来源返回的未就绪事实不完整");
                        throw new RosterNotReady(notReady.errorCode(),notReady.reason());
                    }
                    if(!(resolution instanceof DccDistributionRosterSource.Ready ready) || ready.sourceIdentity()==null || ready.sourceIdentity().isBlank()
                            || ready.policyEvidence()==null || ready.policyEvidence().isBlank())
                        throw new RosterNotReady("ROSTER_SOURCE_INVALID","正式名册来源缺少来源身份或实际政策依据");
                    var snapshot=validateFormalSnapshot(batch,ready.snapshot());
                    result=prepareLockedBatch(batch,snapshot,Map.of("sourceIdentity",ready.sourceIdentity(),"policyEvidence",ready.policyEvidence()));
                }
                if(retryActor!=null)appendHumanRetry(retryActor,batch,before,result);
            });
        } catch(RosterNotReady notReady) {
            return persistUnfrozenFailure(batchId,notReady.code,notReady,retryActor);
        } catch(RosterSourceFailure technical) {
            var actual=(RuntimeException)technical.getCause();
            try {persistUnfrozenFailure(batchId,"ROSTER_SOURCE_FAILED",actual,retryActor);}catch(RuntimeException persistence){actual.addSuppressed(persistence);}
            throw actual;
        } catch(RuntimeException failure) {
            try {recordFailure(batchId,failure instanceof FrozenAccountsInvalid ? "ROSTER_FROZEN_ACCOUNTS_INVALID" : "DISPATCH_PREPARE_FAILED",failure);}catch(RuntimeException persistence){failure.addSuppressed(persistence);}
            throw failure;
        }
        return result(requireBatchIdentity(batchId));
    }

    private DccDistributionBatchDO requireBatchIdentity(Long batchId) {
        if(batchId==null || batchId<=0)throw new IllegalArgumentException("下发批次身份无效");
        var batch=batchMapper.selectById(batchId);
        if(batch==null || !Objects.equals(batch.getTenantId(),TenantContextHolder.getRequiredTenantId()))throw new IllegalArgumentException("下发批次不存在或租户不匹配");
        return batch;
    }
    private void requireProcessingIdentity(DccControlledFileDO file,DccDistributionBatchDO batch) {
        if(batch==null)throw new IllegalStateException("锁定下发批次不存在");
        requireFile(file,new DccControlledFileLifecycleEvent(batch.getEventKey(),batch.getEventType(),batch.getTenantId(),batch.getMasterId(),batch.getControlledFileId(),null,
                batch.getVersionNo(),batch.getApprovalProcessInstanceId(),batch.getSourceOccurredAt()));
        if(!"ACTIVATED".equals(batch.getEventType()) || !Objects.equals(normalizeDepartments(file.getDistributionDepartmentIdsJson()),batch.getDepartmentIdsJson())
                || !Set.of("PENDING","FAILED","DISPATCH_READY","SENT").contains(batch.getStatus()))throw new IllegalStateException("下发批次实际版本/原部门/状态不一致");
        if("SENT".equals(batch.getStatus()) && batch.getSnapshotPayloadHash()==null)throw new IllegalStateException("已送达批次缺少原冻结名册证据");
    }
    private DccDistributionRosterSnapshot validateFormalSnapshot(DccDistributionBatchDO batch,DccDistributionRosterSnapshot snapshot) {
        if(snapshot==null || !Objects.equals(snapshot.tenantId(),batch.getTenantId()) || !Objects.equals(snapshot.controlledFileId(),batch.getControlledFileId())
                || !Objects.equals(snapshot.versionNo(),batch.getVersionNo()) || !Objects.equals(snapshot.approvalProcessInstanceId(),batch.getApprovalProcessInstanceId())
                || snapshot.snapshotAt()==null || snapshot.policyVersion()==null || snapshot.policyVersion().isBlank() || snapshot.policyVersion().length()>128
                || snapshot.departments()==null || snapshot.departments().isEmpty())throw new RosterNotReady("ROSTER_SNAPSHOT_INVALID","正式下发名册版本、轮次或政策事实不完整");
        Set<Long> scopes=new HashSet<>();var departments=new ArrayList<DccDistributionRosterSnapshot.Department>();
        for(var scope:snapshot.departments()) {
            if(scope==null || scope.departmentId()==null || scope.departmentId()<=0 || !scopes.add(scope.departmentId()) || scope.userIds()==null || scope.userIds().isEmpty()
                    || scope.userIds().stream().anyMatch(id->id==null || id<=0) || scope.userIds().stream().distinct().count()!=scope.userIds().size())
                throw new RosterNotReady("ROSTER_SNAPSHOT_INVALID","正式下发名册部门或人员身份无效/重复");
            departments.add(new DccDistributionRosterSnapshot.Department(scope.departmentId(),scope.userIds().stream().sorted().toList()));
        }
        departments.sort(Comparator.comparing(DccDistributionRosterSnapshot.Department::departmentId));
        if(!departments.stream().map(scope->scope.departmentId().toString()).toList().equals(JsonUtils.parseArray(batch.getDepartmentIdsJson(),String.class)))
            throw new RosterNotReady("ROSTER_SNAPSHOT_INVALID","正式名册不属于本版本已保存下发部门");
        return new DccDistributionRosterSnapshot(snapshot.tenantId(),snapshot.controlledFileId(),snapshot.versionNo(),snapshot.approvalProcessInstanceId(),snapshot.snapshotAt(),snapshot.policyVersion(),List.copyOf(departments));
    }
    private DccDistributionRosterSnapshot rebuildFrozenSnapshot(DccDistributionBatchDO batch) {
        var departments=new ArrayList<DccDistributionRosterSnapshot.Department>();
        for(var scope:distributionMapper.selectListByControlledFileIdForUpdate(batch.getTenantId(),batch.getControlledFileId())) {
            if(!"PUBLIC_FOLDER".equals(scope.getDistributionMedium()) || !Objects.equals(scope.getControlledFileId(),batch.getControlledFileId()))throw new IllegalStateException("冻结下发部门身份异常");
            var rows=recipientMapper.selectListByDistributionIdForUpdate(batch.getTenantId(),scope.getId());
            if(rows==null || rows.isEmpty())throw new IllegalStateException("冻结逐人下发义务缺失");
            for(var recipient:rows) {
                var job=messageJobMapper.selectByIdAndTenantForUpdate(batch.getTenantId(),recipient.getMessageJobId());
                if(job==null || !Objects.equals(job.getDistributionBatchId(),batch.getId()) || !"DISTRIBUTION".equals(job.getBusinessType())
                        || !Objects.equals(job.getBusinessId(),scope.getId()) || !Objects.equals(job.getRecipientUserId(),recipient.getUserId()))throw new IllegalStateException("冻结下发义务与通知批次不一致");
            }
            departments.add(new DccDistributionRosterSnapshot.Department(scope.getDepartmentId(),rows.stream().map(DccControlledFileDistributionRecipientDO::getUserId).sorted().toList()));
        }
        var snapshot=validateFormalSnapshot(batch,new DccDistributionRosterSnapshot(batch.getTenantId(),batch.getControlledFileId(),batch.getVersionNo(),batch.getApprovalProcessInstanceId(),
                batch.getSnapshotAt(),batch.getSnapshotPolicyVersion(),departments));
        if(!batch.getSnapshotPayloadHash().equals(DccDistributionAuditSupport.digest(snapshot)))throw new IllegalStateException("冻结名册完整性哈希与持久义务不一致");
        return snapshot;
    }
    private static final class RosterNotReady extends RuntimeException {final String code;RosterNotReady(String code,String reason){super(reason);this.code=code;}}
    private static final class RosterSourceFailure extends RuntimeException {RosterSourceFailure(RuntimeException error){super(error);}}

    /** Validate all original obligations before dispatching any retry; current department membership cannot replace them. */
    private void requireEnabledFrozenAccounts(DccDistributionBatchDO batch,DccDistributionRosterSnapshot snapshot) {
        if(TenantContextHolder.isIgnore() || !Objects.equals(batch.getTenantId(),TenantContextHolder.getRequiredTenantId()))
            throw new IllegalStateException("冻结下发账号核验必须保留本租户边界");
        var expected=snapshot.departments().stream().flatMap(scope->scope.userIds().stream()).distinct().sorted().toList();
        var accounts=cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils.executeIgnore(()->adminUserApi.getUserList(expected));
        if(accounts==null)throw new IllegalStateException("系统目录未返回冻结下发账号核验结果："+expected);
        var returned=new HashMap<Long,cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO>();
        for(var account:accounts) {
            if(account==null || !expected.contains(account.getId()) || returned.put(account.getId(),account)!=null)
                throw new IllegalStateException("系统目录返回的冻结下发账号身份无效或重复");
        }
        var invalid=expected.stream().filter(id->{var account=returned.get(id);
            return account==null || !Objects.equals(account.getTenantId(),batch.getTenantId())
                    || !Objects.equals(account.getStatus(),cn.iocoder.yudao.framework.common.enums.CommonStatusEnum.ENABLE.getStatus());}).toList();
        if(!invalid.isEmpty())throw new FrozenAccountsInvalid("冻结下发账号不存在、停用或租户不匹配，保留原义务并阻断全部重试通知："+invalid);
    }
    private static final class FrozenAccountsInvalid extends RuntimeException {FrozenAccountsInvalid(String message){super(message);}}

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    @GxpWriteOperation(operationId="dcc.distribution.batch.accept")
    public Long acceptActivatedEvent(DccControlledFileLifecycleEvent event) {
        if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("自动下发事件必须在生效事务中接收");
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(event==null || !"ACTIVATED".equals(event.eventType()) || !Objects.equals(event.tenantId(),tenant)
                || event.controlledFileId()==null || event.eventKey()==null || event.eventKey().isBlank())
            throw new IllegalArgumentException("自动下发只接收本租户真实ACTIVATED版本事件");
        var file=controlledFileMapper.selectByIdAndTenantForUpdate(tenant,event.controlledFileId());
        requireFile(file,event);
        var persisted=jdbcTemplate.query("SELECT * FROM dcc_workflow_lifecycle_event WHERE tenant_id=? AND event_key=? FOR UPDATE",(rs,n)-> {
            var result=new org.springframework.jdbc.core.ColumnMapRowMapper().mapRow(rs,n);
            result.put("occurred_at",rs.getObject("occurred_at",LocalDateTime.class));return result;
        },tenant,event.eventKey());
        if(persisted.size()!=1)throw new IllegalStateException("自动下发缺少唯一持久生命周期事件");
        var source=persisted.get(0);
        if(!Objects.equals(source.get("event_type"),event.eventType()) || !Objects.equals(((Number)source.get("master_id")).longValue(),event.masterId())
                || !Objects.equals(((Number)source.get("controlled_file_id")).longValue(),event.controlledFileId())
                || !Objects.equals(source.get("previous_active_file_id")==null?null:((Number)source.get("previous_active_file_id")).longValue(),event.previousActiveFileId())
                || !Objects.equals(source.get("version_no"),event.versionNo()) || !Objects.equals(source.get("approval_process_instance_id"),event.approvalProcessInstanceId())
                || !Objects.equals(source.get("occurred_at"),event.occurredAt()))
            throw new IllegalStateException("自动下发持久事件载荷与真实版本不一致");
        String departments=normalizeDepartments(file.getDistributionDepartmentIdsJson());
        String hash=DccDistributionAuditSupport.digest(Arrays.asList(event,departments));
        var existing=batchMapper.lockVersion(tenant,file.getId(),file.getProcessInstanceId());
        if(existing!=null)return requireReplay(existing,event,hash);
        var now=LocalDateTime.now();
        var batch=DccDistributionBatchDO.builder().tenantId(tenant).sourceEventId(((Number)source.get("id")).longValue())
                .eventKey(event.eventKey()).eventType(event.eventType()).masterId(file.getMasterId()).controlledFileId(file.getId())
                .approvalProcessInstanceId(file.getProcessInstanceId()).versionNo(file.getVersionNo()).sourceOccurredAt(event.occurredAt())
                .departmentIdsJson(departments).eventPayloadHash(hash).status("PENDING").rowVersion(0L).build();
        batch.setCreateTime(now);batch.setUpdateTime(now);
        try {if(batchMapper.insert(batch)!=1 || batch.getId()==null)throw new IllegalStateException("自动下发批次接收未持久化");}
        catch(DuplicateKeyException concurrent) {
            var winner=batchMapper.lockVersion(tenant,file.getId(),file.getProcessInstanceId());
            if(winner==null)throw concurrent;return requireReplay(winner,event,hash);
        }
        DccDistributionAuditSupport.append(gxpAuditService,"dcc.distribution.batch.accept",batch.getId(),0L,"NO_BATCH","PENDING",
                Map.of(),batch,"受控文件已生效，持久接收自动下发版本事件",event.eventKey()+":ACCEPT");
        return batch.getId();
    }
    private Long requireReplay(DccDistributionBatchDO batch,DccControlledFileLifecycleEvent event,String hash){
        if(!Objects.equals(batch.getEventKey(),event.eventKey()) || !Objects.equals(batch.getEventPayloadHash(),hash))
            throw new IllegalArgumentException("同版本下发事件重放载荷冲突");return batch.getId();
    }
    static String normalizeDepartments(String json) {
        if(json==null)return null;
        var node=JsonUtils.parseObject(json,com.fasterxml.jackson.databind.JsonNode.class);
        if(node==null || !node.isArray())throw new IllegalArgumentException("已保存独立下发部门必须为精确字符串数组");
        List<String> ids=new ArrayList<>();
        for(var value:node) {
            if(!value.isTextual() || !value.textValue().matches("[1-9][0-9]*"))throw new IllegalArgumentException("独立下发部门不是精确十进制字符串");
            String id=value.textValue();try{Long.parseLong(id);}catch(NumberFormatException overflow){throw new IllegalArgumentException("独立下发部门超出Long范围",overflow);}ids.add(id);
        }
        if(ids.stream().distinct().count()!=ids.size())
            throw new IllegalArgumentException("已保存独立下发部门身份缺失或重复");
        var sorted=ids.stream().sorted(Comparator.comparingLong(Long::parseLong)).toList();
        String normalized=JsonUtils.toJsonString(sorted);
        if(!normalized.equals(json))throw new IllegalArgumentException("独立下发部门不是正式规范化原字符串");
        return json;
    }
    static void requireFile(DccControlledFileDO file,DccControlledFileLifecycleEvent event){
        if(file==null || !Objects.equals(file.getTenantId(),event.tenantId()) || !Objects.equals(file.getMasterId(),event.masterId())
                || !Objects.equals(file.getVersionNo(),event.versionNo()) || !Objects.equals(file.getProcessInstanceId(),event.approvalProcessInstanceId())
                || file.getControlledTime()==null || file.getActivatedTime()==null || !Objects.equals(file.getActivatedTime(),event.occurredAt())
                || !Set.of("ACTIVE","OBSOLETE","SUPERSEDED").contains(file.getStatus()))
            throw new IllegalStateException("自动下发缺少同版本受控生效事实");
    }
    private TransactionTemplate transaction(){var template=new TransactionTemplate(transactionManager);template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);return template;}
    /** Internal explicit-snapshot primitive. New automatic and HTTP callers use the server-owned source entry. */
    @GxpWriteOperation(operationId="dcc.distribution.batch.prepare")
    public DccDistributionBatchResult prepareAndDispatchBatch(Long batchId,DccDistributionRosterSnapshot snapshot,boolean rosterPolicyDecided) {
        var identity=requireBatchIdentity(batchId);
        String code=identity.getDepartmentIdsJson()==null?"DISTRIBUTION_DEPARTMENTS_MISSING":identity.getDepartmentIdsJson().equals("[]")?"EMPTY_SCOPE_UNDECIDED"
                :!rosterPolicyDecided?"ROSTER_POLICY_UNDECIDED":snapshot==null?"ROSTER_SNAPSHOT_MISSING":null;
        if(code!=null)return recordFailure(batchId,code,new IllegalStateException("下发未执行："+code));
        try {
            return transaction().execute(ignored->{
                controlledFileMapper.selectByIdAndTenantForUpdate(identity.getTenantId(),identity.getControlledFileId());
                var batch=batchMapper.lock(identity.getTenantId(),batchId);
                return prepareLockedBatch(batch,snapshot,Map.of("source","EXPLICIT_INTERNAL_SNAPSHOT"));
            });
        } catch(RuntimeException failure) {
            try {recordFailure(batchId,"DISPATCH_PREPARE_FAILED",failure);}catch(RuntimeException writeFailure){failure.addSuppressed(writeFailure);}
            throw failure;
        }
    }
    private DccDistributionBatchResult prepareLockedBatch(DccDistributionBatchDO batch,DccDistributionRosterSnapshot snapshot,Map<String,String> sourceEvidence) {
        if(batch==null)throw new IllegalStateException("锁定下发批次不存在");
        var current=controlledFileMapper.selectByIdAndTenantForUpdate(batch.getTenantId(),batch.getControlledFileId());
        if(current==null || !Objects.equals(normalizeDepartments(current.getDistributionDepartmentIdsJson()),batch.getDepartmentIdsJson()))
            throw new IllegalArgumentException("批次独立部门与当前正式原版本实际值不一致");
        var expected=JsonUtils.parseArray(batch.getDepartmentIdsJson(),String.class);
        if(snapshot.departments()==null || !snapshot.departments().stream().map(DccDistributionRosterSnapshot.Department::departmentId).sorted().map(String::valueOf).toList().equals(expected))
            throw new IllegalArgumentException("正式全员快照部门与本次原独立部门不一致");
        String snapshotHash=DccDistributionAuditSupport.digest(snapshot);
        if(batch.getSnapshotPayloadHash()!=null && !batch.getSnapshotPayloadHash().equals(snapshotHash))throw new IllegalArgumentException("下发冻结快照不能变更");
        if("SENT".equals(batch.getStatus()))return result(batch);
        if(batch.getSnapshotPayloadHash()!=null) {
            if(!"DISPATCH_READY".equals(batch.getStatus())) {
                var before=DccDistributionAuditSupport.snapshot(batch);String old=batch.getStatus();
                batch.setStatus("DISPATCH_READY");batch.setErrorCode("");batch.setErrorMessage("");batch.setRowVersion(batch.getRowVersion()+1);
                if(batchMapper.updateById(batch)!=1)throw new IllegalStateException("冻结下发批次重试状态未持久化");
                DccDistributionAuditSupport.append(gxpAuditService,"dcc.distribution.batch.prepare",batch.getId(),batch.getRowVersion(),old,batch.getStatus(),before,
                        DccDistributionAuditSupport.snapshot(batch),"按本批原冻结名册重试通知，不重取今日人员",batch.getEventKey()+":RETRY_PREPARE:"+batch.getRowVersion());
            }
            return result(batch);
        }
        if(!distributionMapper.selectListByControlledFileIdForUpdate(batch.getTenantId(),batch.getControlledFileId()).isEmpty())
            throw new IllegalStateException("新下发批次不能接管旧原手工或提前创建的下发记录");
        var before=DccDistributionAuditSupport.snapshot(batch);String previousStatus=batch.getStatus();
        var event=new DccControlledFileLifecycleEvent(batch.getEventKey(),batch.getEventType(),batch.getTenantId(),batch.getMasterId(),batch.getControlledFileId(),null,
                batch.getVersionNo(),batch.getApprovalProcessInstanceId(),batch.getSourceOccurredAt());
        distributionService.dispatchActivatedDistribution(event,snapshot);
        for(var scope:distributionMapper.selectListByControlledFileIdForUpdate(batch.getTenantId(),batch.getControlledFileId()))
            for(var recipient:recipientMapper.selectListByDistributionIdForUpdate(batch.getTenantId(),scope.getId())) {
                var job=messageJobMapper.selectByIdAndTenantForUpdate(batch.getTenantId(),recipient.getMessageJobId());
                if(job==null || job.getDistributionBatchId()!=null && !Objects.equals(job.getDistributionBatchId(),batch.getId()))throw new IllegalStateException("通知任务批次身份冲突");
                if(job.getDistributionBatchId()==null && messageJobMapper.updateById(DccControlledFileMessageJobDO.builder().id(job.getId()).distributionBatchId(batch.getId()).build())!=1)
                    throw new IllegalStateException("通知任务批次关联未持久化");
            }
        batch.setStatus("DISPATCH_READY");batch.setSnapshotAt(snapshot.snapshotAt());batch.setSnapshotPolicyVersion(snapshot.policyVersion());batch.setSnapshotPayloadHash(snapshotHash);
        batch.setDispatchPreparedAt(LocalDateTime.now());batch.setErrorCode("");batch.setErrorMessage("");batch.setRowVersion(batch.getRowVersion()+1);
        if(batchMapper.updateById(batch)!=1)throw new IllegalStateException("下发准备状态未持久化");
        var after=new TreeMap<String,Object>();after.put("batch",DccDistributionAuditSupport.snapshot(batch));after.put("formalRosterSource",sourceEvidence);
        DccDistributionAuditSupport.append(gxpAuditService,"dcc.distribution.batch.prepare",batch.getId(),batch.getRowVersion(),previousStatus,batch.getStatus(),before,after,
                "按正式冻结名单建立下发义务和通知任务",batch.getEventKey()+":PREPARE:"+snapshotHash+":"+batch.getRowVersion());
        return result(batch);
    }

    private DccDistributionBatchResult persistUnfrozenFailure(Long batchId,String code,RuntimeException failure,Long retryActor) {
        String message=DccDistributionAuditSupport.failure(failure);
        return transaction().execute(ignored->{
            var batch=batchMapper.lock(TenantContextHolder.getRequiredTenantId(),batchId);
            if(batch==null)throw new IllegalStateException("下发来源失败批次不存在");
            // A source failure from an earlier attempt cannot undo a concurrently frozen or sent batch.
            if(batch.getSnapshotPayloadHash()!=null || "SENT".equals(batch.getStatus()))return result(batch);
            var before=DccDistributionAuditSupport.snapshot(batch);
            var result=recordFailureLocked(batch,code,message);
            if(retryActor!=null)appendHumanRetry(retryActor,batch,before,result);
            return result;
        });
    }
    private void appendHumanRetry(Long actorId,DccDistributionBatchDO batch,Object before,DccDistributionBatchResult result) {
        var actor=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser();
        if(actor==null || !Objects.equals(actor.getId(),actorId) || !Objects.equals(actor.getTenantId(),batch.getTenantId()))
            throw new IllegalArgumentException("重试审计缺少真实本租户操作者");
        gxpAuditService.append(GxpAuditCommand.builder().operationId("dcc.distribution.batch.retry")
                .subjectId("DCC_DISTRIBUTION_BATCH:"+batch.getTenantId()+":"+batch.getId()).subjectVersion(String.valueOf(batch.getRowVersion()))
                .reason("文控按精确批次重试自动下发")
                .beforeState(GxpAuditStateEnvelope.builder().state("RETRY_REQUESTED").objectVersion(String.valueOf(batch.getRowVersion())).canonicalJson(JsonUtils.toJsonString(before)).build())
                .afterState(GxpAuditStateEnvelope.builder().state(result.status()).objectVersion(String.valueOf(batch.getRowVersion())).canonicalJson(JsonUtils.toJsonString(DccDistributionAuditSupport.snapshot(batch))).build())
                .idempotencyKey("DCC_BATCH_RETRY:"+DccDistributionAuditSupport.digest(Arrays.asList(batch.getId(),batch.getRowVersion(),actorId)))
                .source("DccDistributionTaskServiceImpl.retryAutomaticDistribution").build());
    }
    @GxpWriteOperation(operationId="dcc.distribution.batch.fail")
    public DccDistributionBatchResult recordFailure(Long batchId,String code,RuntimeException failure) {
        if(batchId==null || batchId<=0 || code==null || !code.matches("[A-Z][A-Z0-9_]{0,63}") || failure==null)
            throw new IllegalArgumentException("真实下发失败需要批次、明确分类及实际异常");
        String message=DccDistributionAuditSupport.failure(failure);
        return transaction().execute(ignored->{
            var batch=batchMapper.lock(TenantContextHolder.getRequiredTenantId(),batchId);if(batch==null)throw new IllegalStateException("失败下发批次不存在");
            if("SENT".equals(batch.getStatus()))return result(batch);
            return recordFailureLocked(batch,code,message);
        });
    }
    private DccDistributionBatchResult recordFailureLocked(DccDistributionBatchDO batch,String code,String message) {
        var before=DccDistributionAuditSupport.snapshot(batch);String oldStatus=batch.getStatus();
        batch.setStatus("FAILED");batch.setErrorCode(code);batch.setErrorMessage(message);batch.setFailureAt(LocalDateTime.now());batch.setRowVersion(batch.getRowVersion()+1);
        if(batchMapper.updateById(batch)!=1)throw new IllegalStateException("下发失败事实未持久化");
        DccDistributionAuditSupport.append(gxpAuditService,"dcc.distribution.batch.fail",batch.getId(),batch.getRowVersion(),oldStatus,"FAILED",before,batch,
                "真实下发失败分类："+code,batch.getEventKey()+":FAIL:"+batch.getRowVersion());
        return result(batch);
    }
    static DccDistributionBatchResult result(DccDistributionBatchDO batch){return new DccDistributionBatchResult(batch.getId(),batch.getStatus(),batch.getErrorCode(),batch.getErrorMessage());}
}
