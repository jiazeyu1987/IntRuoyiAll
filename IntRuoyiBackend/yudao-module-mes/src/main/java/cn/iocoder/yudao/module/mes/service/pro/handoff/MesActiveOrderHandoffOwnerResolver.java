package cn.iocoder.yudao.module.mes.service.pro.handoff;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesSubmissionSignatureIdentityReader;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffContract.*;

/** Resolves system identities only from formal tenant-scoped responsibility records. */
@Service
public class MesActiveOrderHandoffOwnerResolver {
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService verifiedSignatureFacts;
    private cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesSignatureContentProtocol.VerifiedMesSignatureContent verifiedContent(Long id) {
        return cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesSignatureContentProtocol.parseVerifiedContent(verifiedSignatureFacts.requireVerifiedFact(id), cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId());
    }

    @Resource private AdminUserMapper users;
    @Resource private cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderScopeService leaderScopes;
    @Resource private PermissionApi permissions;
    @Resource private MesProcessPoolTeamLeaderScopeMapper scopes;
    @Resource private MesProcessPoolTeamEmployeeProfileMapper profiles;
    @Resource private MesSubmissionSignatureIdentityReader signatureIdentities;
    @Resource private MesPqcInspectionTaskMapper pqcTasks;
    @Resource private cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProductionSignatureEvidenceService productionEvidence;
    @Resource private cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService signatureQuery;
    @Resource private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper orders;
    @Resource private MesActiveOrderHandoffTaskMapper handoffTasks;

    public void user(Long id,String permission) {
        identityUser(id);
        require(permissions.hasAnyPermissions(id,permission),
                "交接负责人缺少岗位办理权限："+permission);
    }

    public void qualifiedPqcSubmitter(Long id) {
        identityUser(id);
        require(permissions.hasAnyPermissions(id,"mes:pro-feedback:create")
                ||permissions.hasAnyRoles(id,"pqc_permission","pqc_leader_permission"),"PQC本人提交缺少正式岗位办理权限");
    }

    private void identityUser(Long id) {
        require(id!=null&&id>0,"交接负责人缺少正式系统账号");
        var user=users.selectById(id);
        require(user!=null&&Objects.equals(user.getTenantId(),tenant())&&CommonStatusEnum.isEnable(user.getStatus()),
                "交接负责人系统账号不存在、非本租户或已停用");
    }

    public record SubmissionIdentity(String domain,Long signerId,Long operatorId) {
        public boolean isSystemUser() { return "SYSTEM_USER".equals(domain); }
    }
    /** Domain is frozen by the verified signature, never inferred from colliding numeric IDs. */
    public SubmissionIdentity submissionIdentity(MesProProcessPoolEventDO event) {
        require(event!=null&&Objects.equals(event.getTenantId(),tenant())&&event.getSignatureId()!=null
                &&event.getSignatureId()>0&&Objects.equals(event.getActualEmployeeId(),event.getSignatureUserId()),
                "提交记录缺少正式签名身份");
        var raw=JsonUtils.parseTree(event.getRawPayload());
        if("PRODUCTION_SUBMIT".equals(event.getEventType())) {
            require(!Boolean.TRUE.equals(event.getSimulated())&&productionEvidence.isValidForEvent(event),"交接禁止使用模拟或无法核验的生产提交签名");
            var projection = verifiedContent(event.getSignatureId());
            var identity = projection.identity();
            String domain = identity.domain();
            require(Set.of("SYSTEM_USER", "MES_EMPLOYEE_PROFILE").contains(domain)
                    && domain.equals(raw.path("signatureIdentityDomain").asText())
                    && Objects.equals(identity.tenantId(), tenant())
                    && Objects.equals(identity.signerId(), event.getActualEmployeeId())
                    && Objects.equals(identity.operatorId(), event.getDeviceAccountId()),
                    "生产原签名身份域、签名人与正式操作者不一致");
            if("SYSTEM_USER".equals(domain)) identityUser(event.getActualEmployeeId());
            // Existing PROFILE signatures have no system account. Preserve that formal business domain;
            // notification recipients remain the real system leader, never the device or a guessed user.
            return new SubmissionIdentity(domain,event.getActualEmployeeId(),event.getDeviceAccountId());
        }
        require("PQC_INSPECTION".equals(event.getEventType()),"不支持的正式提交签名类型");
        var snapshot=JsonUtils.parseTree(event.getSignatureSnapshot());
        require("SYSTEM_USER".equals(snapshot.path("performedBy").path("actorType").asText())
                &&Objects.equals(String.valueOf(event.getActualEmployeeId()),snapshot.path("performedBy").path("actorId").asText()),
                "PQC提交缺少冻结的系统账号签名身份");
        var task=pqcTask(event);
        var signature=signatureIdentities.read(event.getSignatureId(),event.getId(),task.getActiveOrderId(),"PQC_SUBMIT");
        require(signature!=null&&Objects.equals(signature.getSignatureId(),event.getSignatureId()),"交接禁止使用无法核验的PQC提交签名");
        identityUser(event.getActualEmployeeId());
        return new SubmissionIdentity("SYSTEM_USER",event.getActualEmployeeId(),event.getDeviceAccountId());
    }
    /** Freezes the actual signer for a submission, and the actual system actor for other commands. */
    public Map<String,Object> initiatorSnapshot(Long initiator,MesProProcessPoolEventDO submission) {
        var result=new LinkedHashMap<String,Object>();
        if(submission!=null) {
            var identity=submissionIdentity(submission);var signature=signatureQuery.getById(submission.getSignatureId());
            require(signature!=null&&Objects.equals(signature.actorId(),identity.signerId())
                    &&signature.actorDisplayName()!=null&&!signature.actorDisplayName().isBlank(),"原提交签名缺少真实冻结姓名");
            result.put("domain",identity.domain());result.put("id",identity.signerId());result.put("name",signature.actorDisplayName());
            result.put("operatorId",identity.operatorId());result.put("signatureId",submission.getSignatureId());
        } else {
            var actor=users.selectById(initiator);require(actor!=null&&Objects.equals(actor.getTenantId(),tenant())
                    &&CommonStatusEnum.isEnable(actor.getStatus())&&actor.getNickname()!=null&&!actor.getNickname().isBlank(),"交接实际发起账号或姓名无效");
            result.put("domain","SYSTEM_USER");result.put("id",initiator);result.put("name",actor.getNickname());
        }
        return Collections.unmodifiableMap(result);
    }

    public Long originalActor(MesProProcessPoolEventDO event) {
        var identity=submissionIdentity(event);
        require(identity.isSystemUser(),"原签名为人员档案域；该域沿用正式组长更正链路，没有个人系统待办合同");
        return identity.signerId();
    }

    public Long profileProductionLeader(MesProProcessPoolEventDO event) {
        var identity = submissionIdentity(event);
        require(!identity.isSystemUser() && "PRODUCTION_SUBMIT".equals(event.getEventType()), "该提交不是临时人员生产签名域");
        var profile = profiles.selectById(identity.signerId());
        var activeId = JsonUtils.parseTree(event.getRawPayload()).path("activeOrderId");
        require(activeId.isIntegralNumber() && activeId.longValue() > 0, "临时人员提交原周期缺失");
        var order = orders.selectById(activeId.longValue());
        require(profile != null && Objects.equals(tenant(), profile.getTenantId()) && Boolean.TRUE.equals(profile.getEnabled())
                && "TEMPORARY".equals(profile.getEmployeeType()) && profile.getSystemUserId() == null
                && order != null && Objects.equals(tenant(), order.getTenantId())
                && Objects.equals(order.getWorkOrderId(), event.getWorkOrderId())
                && Objects.equals(order.getLeaderUserId(), profile.getLeaderUserId()), "临时签名人与原周期正式生产组长责任不一致");
        return productionLeader(order);
    }

    public void assertPqcLeaderCorrector(MesProProcessPoolEventDO event,Long actor) {
        require("PQC_INSPECTION".equals(event.getEventType()),"组长PQC补正必须来自正式PQC事件");
        user(actor,"mes:pro-process-pool:event-revision:update");
        var pqc=pqcTask(event);
        var original=handoffTasks.byIdentity(pqc.getActiveOrderId(),"PQC_REVIEW","PROCESS_POOL_EVENT",event.getId(),event.getId());
        require(frozenPqcReviewers(event,original).contains(actor),"当前账号不是原PQC复核任务冻结的责任组长");
    }

    public Long productionLeader(MesProcessPoolActiveOrderDO order) {
        require(order!=null&&Objects.equals(order.getTenantId(),tenant()),"活跃订单正式责任来源缺失");
        user(order.getLeaderUserId(),"mes:pro-process-pool-team-leader:review");
        return order.getLeaderUserId();
    }

    /** PQC events carry QA identity; their MES stage belongs exclusively to the formal inspection task. */
    public MesPqcInspectionTaskDO pqcTask(MesProProcessPoolEventDO event) {
        validatePqcEventIdentity(event);
        return validatePqcTask(event,pqcTasks.selectById(event.getFeedbackSourceId()));
    }

    private void validatePqcEventIdentity(MesProProcessPoolEventDO event) {
        require(event!=null&&Objects.equals(event.getTenantId(),tenant())&&event.getId()!=null&&event.getId()>0
                &&"PQC_INSPECTION".equals(event.getEventType())&&event.getRouteProcessId()==null&&event.getProcessId()==null
                &&"MES_PQC_INSPECTION_TASK".equals(event.getFeedbackSourceType())
                &&event.getFeedbackSourceId()!=null&&event.getFeedbackSourceId()>0,
                "PQC交接必须使用保留QA身份的正式检验事件");
    }

    private MesPqcInspectionTaskDO validatePqcTask(MesProProcessPoolEventDO event,MesPqcInspectionTaskDO task) {
        validatePqcEventIdentity(event);
        require(task!=null&&Objects.equals(task.getId(),event.getFeedbackSourceId())&&Objects.equals(task.getTenantId(),tenant())
                &&task.getActiveOrderId()!=null&&task.getActiveOrderId()>0
                &&task.getRouteProcessId()==null&&task.getProcessId()==null&&task.getRegulationVersionId()!=null
                &&task.getWorkOrderId()!=null&&task.getWorkOrderId()>0&&Objects.equals(task.getWorkOrderId(),event.getWorkOrderId())
                &&task.getRouteId()!=null&&task.getRouteId()>0&&Objects.equals(task.getRouteId(),event.getRouteId())
                &&task.getQaProcessId()!=null&&task.getQaProcessId()>0&&Objects.equals(task.getQaProcessId(),event.getQaProcessId())
                &&Objects.equals(task.getSubmittedEventId(),event.getId())
                &&("SUBMITTED".equals(task.getTaskStatus())||"CONFIRMED".equals(task.getTaskStatus())),
                "PQC正式任务与原事件的租户、工单、路线或提交关联不一致");
        return task;
    }

    public Long pqcLeader(MesProProcessPoolEventDO event) {
        pqcTask(event);
        return pqcLeaderFor(event.getActualEmployeeId(),pqcEmployeeScopes());
    }

    public List<Long> frozenPqcReviewers(MesProProcessPoolEventDO event,MesActiveOrderHandoffTaskDO task) {
        var pqc=pqcTask(event);
        return validateFrozenPqcReviewers(event,task,pqc,id->user(id,"mes:pro-process-pool-team-leader:review"));
    }

    private List<Long> validateFrozenPqcReviewers(MesProProcessPoolEventDO event,MesActiveOrderHandoffTaskDO task,
            MesPqcInspectionTaskDO pqc,java.util.function.Consumer<Long> validateLeader) {
        require(task!=null&&Objects.equals(task.getTenantId(),tenant())&&"PQC_REVIEW".equals(task.getTaskType())
                &&Objects.equals(task.getActiveOrderId(),pqc.getActiveOrderId())&&Objects.equals(task.getWorkOrderId(),pqc.getWorkOrderId())
                &&Objects.equals(task.getRouteProcessId(),pqc.getRouteProcessId())&&Objects.equals(task.getSourceId(),event.getId())
                &&Set.of("PROCESS_POOL_EVENT",EVENT_SIGNED_REVISION).contains(task.getSourceType())
                &&Set.of("TODO","DONE").contains(task.getStatus()),"PQC复核任务与原事件不一致");
        var snapshot=JsonUtils.parseTree(task.getResponsibilitySnapshotJson());var handlers=candidates(task.getCandidateUserSnapshot());
        require(snapshot!=null&&snapshot.isObject(),"PQC复核冻结责任快照缺失");
        require("SYSTEM_USER".equals(snapshot.path("identityDomain").asText())
                &&sameId(snapshot,"activeOrderId",pqc.getActiveOrderId())&&sameId(snapshot,"workOrderId",pqc.getWorkOrderId())
                &&!snapshot.hasNonNull("routeProcessId")&&!snapshot.hasNonNull("processId")
                &&sameId(snapshot,"pqcTaskId",pqc.getId())&&sameId(snapshot,"qaProcessId",pqc.getQaProcessId())
                &&sameId(snapshot,"routeVersionId",pqc.getRouteVersionId())&&sameId(snapshot,"regulationVersionId",pqc.getRegulationVersionId())
                &&sameId(snapshot,"sourceId",event.getId())&&sameId(snapshot,"roundId",task.getRoundId())
                &&Objects.equals(snapshot.path("sourceType").asText(),task.getSourceType())
                &&frozenIds(snapshot.path("userIds")).equals(handlers)
                &&(!"PROCESS_POOL_EVENT".equals(task.getSourceType())
                    ||Objects.equals(task.getRoundId(),event.getId())),
                "PQC复核冻结办理责任与原任务冲突");
        handlers.forEach(validateLeader);
        return handlers;
    }

    /** Request-local bulk reads retain every member's original frozen responsibility checks. */
    public Set<Long> frozenPqcReviewEventIds(List<MesActiveOrderHandoffTaskDO> reviewTasks,
            List<MesProProcessPoolEventDO> eventRows,Long actor) {
        require(reviewTasks!=null&&eventRows!=null,"PQC冻结复核批量来源缺失");
        if(reviewTasks.isEmpty())return Set.of();
        var eventById=new HashMap<Long,MesProProcessPoolEventDO>();
        for(var event:eventRows) {
            validatePqcEventIdentity(event);
            require(eventById.putIfAbsent(event.getId(),event)==null,"PQC冻结复核事件来源重复");
        }
        var expectedEventIds=reviewTasks.stream().map(MesActiveOrderHandoffTaskDO::getSourceId).collect(Collectors.toSet());
        require(eventById.keySet().equals(expectedEventIds),"PQC冻结复核事件来源缺失或不一致");
        var taskIds=eventRows.stream().map(MesProProcessPoolEventDO::getFeedbackSourceId).collect(Collectors.toSet());
        var pqcById=new HashMap<Long,MesPqcInspectionTaskDO>();
        var pqcRows=pqcTasks.selectBatchIds(taskIds);
        require(pqcRows!=null,"PQC正式任务批量读取失败");
        for(var task:pqcRows)require(task!=null&&pqcById.putIfAbsent(task.getId(),task)==null,"PQC正式任务来源重复");
        var orderIds=pqcRows.stream().map(MesPqcInspectionTaskDO::getActiveOrderId).collect(Collectors.toSet());
        require(!orderIds.isEmpty()&&!orderIds.contains(null),"PQC原活跃工单来源缺失");
        var orderById=new HashMap<Long,MesProcessPoolActiveOrderDO>();
        var orderRows=orders.selectBatchIds(orderIds);
        require(orderRows!=null,"PQC原活跃工单批量读取失败");
        for(var order:orderRows)require(order!=null&&orderById.putIfAbsent(order.getId(),order)==null,"PQC原活跃工单来源重复");
        var checkedLeaders=new HashSet<Long>();
        java.util.function.Consumer<Long> validateLeader=id->{
            if(checkedLeaders.add(id))user(id,"mes:pro-process-pool-team-leader:review");
        };
        var result=new LinkedHashSet<Long>();
        for(var review:reviewTasks) {
            access(review,actor);validate(review);
            var event=eventById.get(review.getSourceId());
            var pqc=validatePqcTask(event,event==null?null:pqcById.get(event.getFeedbackSourceId()));
            var order=orderById.get(pqc.getActiveOrderId());
            require(order!=null&&Objects.equals(order.getTenantId(),tenant())
                    &&Objects.equals(order.getWorkOrderId(),pqc.getWorkOrderId())
                    &&Objects.equals(order.getRouteId(),pqc.getRouteId()),"PQC复核不属于原正式工单与路线");
            require(validateFrozenPqcReviewers(event,review,pqc,validateLeader).contains(actor),"PQC本人复核冻结责任不一致");
            result.add(event.getId());
        }
        return Collections.unmodifiableSet(result);
    }

    private static boolean sameId(JsonNode node,String field,Long expected) {
        return expected!=null&&node.path(field).isIntegralNumber()&&Objects.equals(node.path(field).longValue(),expected);
    }
    private static List<Long> frozenIds(JsonNode node) {
        require(node.isArray()&&!node.isEmpty(),"PQC冻结办理责任集合缺失");var ids=new TreeSet<Long>();
        for(var value:node)require(value.isIntegralNumber()&&value.longValue()>0&&ids.add(value.longValue()),"PQC冻结办理责任身份无效或重复");
        return List.copyOf(ids);
    }

    public String productionEmployees(MesProcessPoolActiveOrderDO order) {
        var rows=scopes.selectActiveScopesByLeader(order.getLeaderUserId(),"PRODUCTION");
        var bindings=profiles.selectList(new LambdaQueryWrapperX<MesProcessPoolTeamEmployeeProfileDO>()
                .eq(MesProcessPoolTeamEmployeeProfileDO::getLeaderUserId,order.getLeaderUserId())
                .eq(MesProcessPoolTeamEmployeeProfileDO::getEnabled,true));
        require(rows!=null&&bindings!=null,"生产人员责任查询失败");
        Set<Long> ids=new TreeSet<>();
        for(var row:rows) if("EMPLOYEE".equals(row.getScopeType())&&Objects.equals(tenant(),row.getTenantId())) {
            require(Boolean.TRUE.equals(row.getEnabled())&&"PRODUCTION".equals(row.getLeaderType())
                    &&Objects.equals(order.getLeaderUserId(),row.getLeaderUserId()),"生产人员责任与班组不一致");
            var matches=bindings.stream().filter(p->Objects.equals(tenant(),p.getTenantId())
                    &&Objects.equals(order.getLeaderUserId(),p.getLeaderUserId())&&Boolean.TRUE.equals(p.getEnabled())
                    &&(("FORMAL".equals(p.getEmployeeType())&&Objects.equals(row.getEmployeeUserId(),p.getSystemUserId()))
                    ||("TEMPORARY".equals(p.getEmployeeType())&&p.getSystemUserId()==null
                    &&Objects.equals(row.getEmployeeUserId(),p.getId())))).toList();
            require(matches.size()==1,"生产人员责任缺少唯一启用的身份绑定，或正式账号与临时工身份发生冲突");
            var binding=matches.get(0);
            if("FORMAL".equals(binding.getEmployeeType())) {
                user(binding.getSystemUserId(),"mes:pro-feedback:create");ids.add(binding.getSystemUserId());
            }
        }
        require(!ids.isEmpty(),"生产接手缺少正式人员责任配置"); return snapshot(ids);
    }

    private List<MesProcessPoolTeamLeaderScopeDO> pqcEmployeeScopes() {
        var rows=scopes.selectActiveScopesByLeaderType("PQC");require(rows!=null,"PQC正式人员责任查询失败");
        require(rows.stream().noneMatch(Objects::isNull),"PQC正式人员责任包含无效记录");
        return rows.stream().filter(row->Objects.equals(tenant(),row.getTenantId())
                &&Boolean.TRUE.equals(row.getEnabled())&&"PQC".equals(row.getLeaderType())&&"EMPLOYEE".equals(row.getScopeType()))
                .toList();
    }

    private Long pqcLeaderFor(Long actor,List<MesProcessPoolTeamLeaderScopeDO> rows) {
        require(actor!=null&&actor>0,"PQC原提交人员缺少正式系统身份");
        var ids=rows.stream().filter(row->Objects.equals(actor,row.getEmployeeUserId())||Objects.equals(actor,row.getLeaderUserId()))
                .peek(row->require(row.getLeaderUserId()!=null&&row.getLeaderUserId()>0
                        &&row.getEmployeeUserId()!=null&&row.getEmployeeUserId()>0,"PQC所选人员责任缺少有效系统身份"))
                .map(MesProcessPoolTeamLeaderScopeDO::getLeaderUserId).distinct().toList();
        require(ids.size()==1,"PQC原提交人员必须具有唯一启用的正式组长责任配置");
        user(ids.get(0),"mes:pro-process-pool-team-leader:review");return ids.get(0);
    }

    public static String snapshot(Collection<Long> ids) {
        return ids.stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
    }
}
