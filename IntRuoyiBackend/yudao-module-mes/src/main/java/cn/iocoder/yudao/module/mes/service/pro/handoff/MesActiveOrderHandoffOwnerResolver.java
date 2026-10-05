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
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffContract.*;

/** Resolves system identities only from formal tenant-scoped responsibility records. */
@Service
public class MesActiveOrderHandoffOwnerResolver {
    @Resource private AdminUserMapper users;
    @Resource private cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderScopeService leaderScopes;
    @Resource private PermissionApi permissions;
    @Resource private MesProcessPoolTeamLeaderScopeMapper scopes;
    @Resource private MesProcessPoolTeamEmployeeProfileMapper profiles;
    @Resource private MesSubmissionSignatureIdentityReader signatureIdentities;
    @Resource private MesPqcInspectionTaskMapper pqcTasks;
    @Resource private cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProductionSignatureEvidenceService productionEvidence;
    @Resource private cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService signatureQuery;

    public void user(Long id,String permission) {
        require(id!=null&&id>0,"交接负责人缺少正式系统账号");
        var user=users.selectById(id);
        require(user!=null&&Objects.equals(user.getTenantId(),tenant())&&CommonStatusEnum.isEnable(user.getStatus()),
                "交接负责人系统账号不存在、非本租户或已停用");
        var roles=permissions.getUserRoleIdListByUserId(id);
        require(roles!=null&&!roles.isEmpty()&&permissions.hasAnyPermissionsInRoles(roles,permission),
                "交接负责人缺少岗位办理权限："+permission);
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
            var signature=signatureQuery.getById(event.getSignatureId());require(signature!=null,"原生产统一签名不存在");
            var identity=JsonUtils.parseTree(signature.canonicalContentJson()).path("signatureIdentity");
            String domain=identity.path("domain").asText();
            require(Set.of("SYSTEM_USER","MES_EMPLOYEE_PROFILE").contains(domain)
                    &&domain.equals(raw.path("signatureIdentityDomain").asText())
                    &&identity.path("tenantId").isIntegralNumber()&&Objects.equals(identity.path("tenantId").longValue(),tenant())
                    &&identity.path("signerId").isIntegralNumber()&&Objects.equals(identity.path("signerId").longValue(),event.getActualEmployeeId())
                    &&identity.path("operatorId").isIntegralNumber()&&Objects.equals(identity.path("operatorId").longValue(),event.getDeviceAccountId()),
                    "生产原签名身份域、签名人与正式操作者不一致");
            if("SYSTEM_USER".equals(domain)) user(event.getActualEmployeeId(),"mes:pro-feedback:create");
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
        user(event.getActualEmployeeId(),"mes:pro-feedback:create");
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

    public void assertPqcLeaderCorrector(MesProProcessPoolEventDO event,Long actor) {
        require("PQC_INSPECTION".equals(event.getEventType()),"组长PQC补正必须来自正式PQC事件");
        user(actor,"mes:pro-process-pool:event-revision:update");
        leaderScopes.assertCanAccessEmployee(actor,"PQC",event.getActualEmployeeId());
    }

    public Long productionLeader(MesProcessPoolActiveOrderDO order) {
        require(order!=null&&Objects.equals(order.getTenantId(),tenant()),"活跃订单正式责任来源缺失");
        user(order.getLeaderUserId(),"mes:pro-process-pool-team-leader:review");
        return order.getLeaderUserId();
    }

    /** PQC events carry QA identity; their MES stage belongs exclusively to the formal inspection task. */
    public MesPqcInspectionTaskDO pqcTask(MesProProcessPoolEventDO event) {
        require(event!=null&&Objects.equals(event.getTenantId(),tenant())&&event.getId()!=null&&event.getId()>0
                &&"PQC_INSPECTION".equals(event.getEventType())&&event.getRouteProcessId()==null&&event.getProcessId()==null
                &&"MES_PQC_INSPECTION_TASK".equals(event.getFeedbackSourceType())
                &&event.getFeedbackSourceId()!=null&&event.getFeedbackSourceId()>0,
                "PQC交接必须使用保留QA身份的正式检验事件");
        var task=pqcTasks.selectById(event.getFeedbackSourceId());
        require(task!=null&&Objects.equals(task.getId(),event.getFeedbackSourceId())&&Objects.equals(task.getTenantId(),tenant())
                &&task.getActiveOrderId()!=null&&task.getActiveOrderId()>0
                &&task.getRouteProcessId()!=null&&task.getRouteProcessId()>0&&task.getProcessId()!=null&&task.getProcessId()>0
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

    public String productionEmployees(MesProcessPoolActiveOrderDO order) {
        var rows=scopes.selectActiveScopesByLeader(order.getLeaderUserId(),"PRODUCTION");
        Set<Long> ids=new TreeSet<>();
        for(var row:rows) if("EMPLOYEE".equals(row.getScopeType())&&Objects.equals(tenant(),row.getTenantId())) {
            var formal=profiles.selectList(new LambdaQueryWrapperX<MesProcessPoolTeamEmployeeProfileDO>()
                    .eq(MesProcessPoolTeamEmployeeProfileDO::getLeaderUserId,order.getLeaderUserId())
                    .eq(MesProcessPoolTeamEmployeeProfileDO::getSystemUserId,row.getEmployeeUserId())
                    .eq(MesProcessPoolTeamEmployeeProfileDO::getEmployeeType,"FORMAL")
                    .eq(MesProcessPoolTeamEmployeeProfileDO::getEnabled,true));
            require(formal.size()==1,"生产接手人员缺少唯一启用的正式账号绑定");
            user(row.getEmployeeUserId(),"mes:pro-feedback:create"); ids.add(row.getEmployeeUserId());
        }
        require(!ids.isEmpty(),"生产接手缺少正式人员责任配置"); return snapshot(ids);
    }

    /** Validates every explicitly selected inspector; unrelated tenant personnel are not dispatch candidates. */
    public List<Long> validatePqcHandoffCandidates(String employeeSnapshot) {
        var rows=pqcEmployeeScopes();var ids=new TreeSet<Long>();
        for(Long employee:candidates(employeeSnapshot)) {
            user(employee,"mes:pro-feedback:create");
            require(rows.stream().anyMatch(row->Objects.equals(employee,row.getEmployeeUserId())),"PQC接手人员不属于正式员工责任来源");
            ids.add(pqcLeaderFor(employee,rows));
        }
        require(!ids.isEmpty(),"PQC接手缺少冻结的正式组长"); return List.copyOf(ids);
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
