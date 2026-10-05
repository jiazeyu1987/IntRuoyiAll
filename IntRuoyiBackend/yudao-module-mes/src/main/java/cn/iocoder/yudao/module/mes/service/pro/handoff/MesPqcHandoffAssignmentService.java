package cn.iocoder.yudao.module.mes.service.pro.handoff;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffContract.*;

/** Explicit route dispatch; employee scopes validate selected responsibility and never select a tenant-wide pool. */
@Service
public class MesPqcHandoffAssignmentService {
    public static final String TASK_TYPE="PQC_HANDOFF";
    public static final String CREATE_PERMISSION="mes:pro-feedback:create";
    @Resource private MesProEdhrWorkTaskAssignmentRuleMapper rules;
    @Resource private MesProRouteMapper routes;
    @Resource private MesActiveOrderHandoffOwnerResolver owners;
    @Resource private AdminUserApi users;
    @Resource private RoleService roles;
    @Resource private PermissionApi permissions;
    @Resource private MesProEdhrOperationAuditService operationAudit;
    @Resource private MesActiveOrderHandoffAudit audit;

    public record Option(Long id,String label) { }
    public record RuleView(Long id,String candidateSourceType,Long candidateSourceId,String candidateLabel,
            Boolean enabled,String remark,String candidateUserSnapshot,List<Long> handlerLeaderUserIds) { }
    public record ResolvedAssignment(Long ruleId,Long routeId,String candidateSourceType,Long candidateSourceId,
            String candidateUserSnapshot,List<Long> handlerLeaderUserIds) {
        public Map<String,Object> snapshot() {
            return Map.of("ruleId",ruleId,"scopeType","ROUTE","scopeId",routeId,"taskType",TASK_TYPE,
                    "candidateSourceType",candidateSourceType,"candidateSourceId",candidateSourceId,
                    "candidateUserSnapshot",candidateUserSnapshot,"handlerLeaderUserIds",handlerLeaderUserIds);
        }
    }
    public List<Option> routeOptions() {
        return routes.selectList(new LambdaQueryWrapperX<MesProRouteDO>().eq(MesProRouteDO::getStatus,0)
                .orderByAsc(MesProRouteDO::getCode)).stream().map(r->new Option(r.getId(),r.getCode()+" / "+r.getName())).toList();
    }
    public List<Option> userOptions(String keyword) {
        require(keyword!=null&&!keyword.isBlank()&&keyword.length()<=100,"请输入PQC检验员姓名以查询正式账号");
        return users.getUserListByNickname(keyword.trim()).stream().filter(u->CommonStatusEnum.isEnable(u.getStatus()))
                .filter(u->permissions.hasAnyPermissions(u.getId(),CREATE_PERMISSION))
                .map(u->new Option(u.getId(),u.getNickname()+"（"+u.getUsername()+"）")).toList();
    }
    public List<Option> roleOptions() {
        return roles.getRoleListByStatus(List.of(0)).stream()
                .filter(r->permissions.hasAnyPermissionsInRoles(Set.of(r.getId()),CREATE_PERMISSION))
                .map(r->new Option(r.getId(),r.getName())).toList();
    }
    public RuleView view(Long routeId) {
        var rule=get(routeId);if(rule==null)return null;
        var resolved=resolveRule(rule);
        String label="USER".equals(rule.getCandidateSourceType())
                ? users.getUser(rule.getCandidateSourceId()).getNickname()+"（"+users.getUser(rule.getCandidateSourceId()).getUsername()+"）"
                : roles.getRole(rule.getCandidateSourceId()).getName();
        return new RuleView(rule.getId(),rule.getCandidateSourceType(),rule.getCandidateSourceId(),label,
                rule.getEnabled(),rule.getRemark(),resolved.candidateUserSnapshot(),resolved.handlerLeaderUserIds());
    }
    public MesProEdhrWorkTaskAssignmentRuleDO get(Long routeId) {
        route(routeId,false);
        var all=rules.selectListByScopeAndType("ROUTE",routeId,TASK_TYPE);
        require(all.size()<=1,"该路线存在重复PQC检验接手规则，请先处理重复配置");
        if(all.isEmpty())return null;
        var rule=all.get(0);
        require(rule!=null&&rule.getId()!=null&&rule.getId()>0&&"ROUTE".equals(rule.getScopeType())
                &&Objects.equals(routeId,rule.getScopeId())&&TASK_TYPE.equals(rule.getTaskType())&&rule.getRouteProcessId()==null,
                "PQC接手规则与正式路线作用域不一致");
        return rule;
    }
    public ResolvedAssignment resolve(Long routeId) {
        var rule=get(routeId);
        require(rule!=null&&Boolean.TRUE.equals(rule.getEnabled()),
                "当前路线未配置启用的PQC检验接手负责人，请管理员在PQC接手负责人配置中完成设置");
        return resolveRule(rule);
    }
    private ResolvedAssignment resolveRule(MesProEdhrWorkTaskAssignmentRuleDO rule) {
        require(rule.getCandidateSourceType()!=null&&Set.of("USER","ROLE_GROUP").contains(rule.getCandidateSourceType())
                &&rule.getCandidateSourceId()!=null&&rule.getCandidateSourceId()>0,"PQC接手仅支持明确正式用户或角色候选责任源");
        Set<Long> selected;
        if("USER".equals(rule.getCandidateSourceType()))selected=Set.of(rule.getCandidateSourceId());
        else {
            var role=roles.getRole(rule.getCandidateSourceId());
            require(role!=null&&Objects.equals(role.getId(),rule.getCandidateSourceId())
                    &&Objects.equals(role.getTenantId(),tenant())&&CommonStatusEnum.isEnable(role.getStatus()),"PQC责任角色不存在、不属本租户或已停用");
            require(permissions.hasAnyPermissionsInRoles(Set.of(role.getId()),CREATE_PERMISSION),"PQC责任角色缺少正式提交权限");
            selected=permissions.getUserRoleIdListByRoleIds(Set.of(role.getId()));
        }
        require(selected!=null&&!selected.isEmpty()&&selected.stream().allMatch(id->id!=null&&id>0),"PQC正式责任候选为空或存在无效身份");
        String snapshot=MesActiveOrderHandoffOwnerResolver.snapshot(selected);
        var handlers=owners.validatePqcHandoffCandidates(snapshot);
        require(handlers!=null&&!handlers.isEmpty(),"PQC正式责任缺少有效复核组长");
        return new ResolvedAssignment(rule.getId(),rule.getScopeId(),rule.getCandidateSourceType(),rule.getCandidateSourceId(),snapshot,List.copyOf(handlers));
    }
    @Transactional(rollbackFor=Exception.class)
    public MesProEdhrWorkTaskAssignmentRuleDO save(Long routeId,String sourceType,Long sourceId,Boolean enabled,String reason,Long expectedRuleId) {
        require(reason!=null&&!reason.isBlank()&&reason.length()<=1000,"请填写PQC接手责任配置变更原因");
        require(SecurityFrameworkUtils.getLoginUserId()!=null,"请先登录后配置PQC接手负责人");
        require(enabled!=null,"请选择规则启用状态");audit.lock();route(routeId,true);
        var before=get(routeId);
        require(Objects.equals(expectedRuleId,before==null?null:before.getId()),"PQC接手责任规则已变化，请刷新后重试");
        var rule=new MesProEdhrWorkTaskAssignmentRuleDO().setId(before==null?null:before.getId())
                .setScopeType("ROUTE").setScopeId(routeId).setTaskType(TASK_TYPE)
                .setCandidateSourceType(sourceType).setCandidateSourceId(sourceId)
                .setAssigneeUserId("USER".equals(sourceType)?sourceId:null).setEnabled(enabled).setRemark(reason.trim());
        resolveRule(rule);
        require((before==null?rules.insert(rule):rules.updateById(rule))==1&&rule.getId()!=null,"PQC接手责任规则保存失败");
        var after=get(routeId);
        require(after!=null&&Objects.equals(after.getId(),rule.getId()),"PQC接手责任规则保存回读不一致");
        var metadata=new LinkedHashMap<String,Object>();metadata.put("requestSource","PQC_HANDOFF_ASSIGNMENT_CONFIG");
        metadata.put("routeId",routeId);metadata.put("taskType",TASK_TYPE);metadata.put("reason",reason.trim());
        metadata.put("beforeRules",before==null?List.of():List.of(before));metadata.put("afterRules",List.of(after));
        metadata.put("associatedSignatureId","NOT_APPLICABLE");metadata.put("permissionDecision","ALLOW");metadata.put("resultStatus","SUCCESS");
        operationAudit.recordInCallerTransaction(new MesProEdhrOperationAuditCommand().setRequestId("PQC-HANDOFF-RULE-"+UUID.randomUUID())
                .setObjectType("WORK_TASK_ASSIGNMENT_RULE").setObjectId(rule.getId().toString()).setRouteId(routeId)
                .setOperationType("WORK_TASK_RULE_SAVE").setActionName("保存PQC检验接手负责人规则")
                .setActorUserId(SecurityFrameworkUtils.getLoginUserId()).setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode("mes:pro-edhr-work-task-rule:update").setPermissionDecision("ALLOW").setResultStatus("SUCCESS")
                .setBeforeSummaryHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(JsonUtils.toJsonString(before==null?Map.of():before)))
                .setAfterSummaryHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(JsonUtils.toJsonString(after)))
                .setMetadataJson(JsonUtils.toJsonString(metadata)));
        return after;
    }
    private void route(Long id,boolean lock) {
        require(id!=null&&id>0,"请选择正式路线");
        var route=routes.selectOne(new LambdaQueryWrapperX<MesProRouteDO>().eq(MesProRouteDO::getId,id).last(lock?"FOR UPDATE":""));
        require(route!=null&&Objects.equals(route.getId(),id)&&CommonStatusEnum.isEnable(route.getStatus()),"请选择本租户启用的正式路线");
    }
}
