package cn.iocoder.yudao.module.mes.service.pro.handoff;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffContract.*;

@Service
public class MesQaHandoffAssignmentService {
    public static final String DISPOSE_PERMISSION="mes:pro-edhr-nonconformance-review:dispose";
    @Resource private MesProEdhrWorkTaskAssignmentRuleMapper rules;
    @Resource private MesProRouteMapper routes;
    @Resource private MesProEdhrCandidateResolver candidates;
    @Resource private MesActiveOrderHandoffOwnerResolver owners;
    @Resource private AdminUserApi users;
    @Resource private RoleService roles;
    @Resource private PermissionApi permissions;
    @Resource private MesProEdhrOperationAuditService operationAudit;
    @Resource private MesActiveOrderHandoffAudit audit;

    public record Option(Long id,String label) { }
    public record RuleView(Long id,String candidateSourceType,Long candidateSourceId,String candidateLabel,Boolean enabled,String remark) { }
    public RuleView view(Long routeId) {
        var rule=get(routeId);if(rule==null)return null;resolveRule(rule);
        String label="USER".equals(rule.getCandidateSourceType())
                ? users.getUser(rule.getCandidateSourceId()).getNickname()+"（"+users.getUser(rule.getCandidateSourceId()).getUsername()+"）"
                : roles.getRole(rule.getCandidateSourceId()).getName();
        return new RuleView(rule.getId(),rule.getCandidateSourceType(),rule.getCandidateSourceId(),label,rule.getEnabled(),rule.getRemark());
    }
    public List<Option> routeOptions() {
        return routes.selectList(new LambdaQueryWrapperX<MesProRouteDO>().eq(MesProRouteDO::getStatus,0)
                .orderByAsc(MesProRouteDO::getCode)).stream().map(r->new Option(r.getId(),r.getCode()+" / "+r.getName())).toList();
    }
    public List<Option> userOptions(String keyword) {
        require(keyword!=null&&!keyword.isBlank()&&keyword.length()<=100,"请输入负责人姓名以查询正式账号");
        return users.getUserListByNickname(keyword.trim()).stream().filter(u->CommonStatusEnum.isEnable(u.getStatus()))
                .filter(u->permissions.hasAnyPermissions(u.getId(),DISPOSE_PERMISSION))
                .map(u->new Option(u.getId(),u.getNickname()+"（"+u.getUsername()+"）")).toList();
    }
    public List<Option> roleOptions() {
        return roles.getRoleListByStatus(List.of(0)).stream()
                .filter(r->permissions.hasAnyPermissionsInRoles(Set.of(r.getId()),DISPOSE_PERMISSION))
                .map(r->new Option(r.getId(),r.getName())).toList();
    }
    public MesProEdhrWorkTaskAssignmentRuleDO get(Long routeId) {
        route(routeId,false); var all=rules.selectListByScopeAndType("ROUTE",routeId,"QA_REVIEW");
        require(all.size()<=1,"该路线存在重复QA评审责任规则，请先处理重复配置");return all.isEmpty()?null:all.get(0);
    }
    public String resolve(Long routeId) {
        var rule=get(routeId);require(rule!=null&&Boolean.TRUE.equals(rule.getEnabled()),
                "当前路线未配置启用的QA评审负责人，请管理员在QA负责人配置中完成设置");return resolveRule(rule);
    }
    private String resolveRule(MesProEdhrWorkTaskAssignmentRuleDO rule) {
        require(Set.of("USER","ROLE_GROUP").contains(rule.getCandidateSourceType())&&rule.getCandidateSourceId()!=null,
                "QA评审仅支持明确正式用户或角色候选责任源");
        if("ROLE_GROUP".equals(rule.getCandidateSourceType())) {
            var role=roles.getRole(rule.getCandidateSourceId());require(role!=null&&CommonStatusEnum.isEnable(role.getStatus()),"QA责任角色不存在或已停用");
            require(permissions.hasAnyPermissionsInRoles(Set.of(role.getId()),DISPOSE_PERMISSION),"QA责任角色缺少正式处置权限");
        }
        String snapshot=candidates.resolveAssignmentRule(rule).userSnapshot();
        for(Long id:candidates(snapshot))owners.user(id,DISPOSE_PERMISSION);
        return snapshot;
    }
    @Transactional(rollbackFor=Exception.class)
    public MesProEdhrWorkTaskAssignmentRuleDO save(Long routeId,String sourceType,Long sourceId,Boolean enabled,String reason,Long expectedRuleId) {
        require(reason!=null&&!reason.isBlank()&&reason.length()<=1000,"请填写QA责任配置变更原因");
        require(enabled!=null,"请选择规则启用状态"); audit.lock();route(routeId,true);
        var before=get(routeId);require(Objects.equals(expectedRuleId,before==null?null:before.getId()),"QA责任规则已变化，请刷新后重试");
        var rule=new MesProEdhrWorkTaskAssignmentRuleDO().setId(before==null?null:before.getId())
                .setScopeType("ROUTE").setScopeId(routeId).setTaskType("QA_REVIEW")
                .setCandidateSourceType(sourceType).setCandidateSourceId(sourceId)
                .setAssigneeUserId("USER".equals(sourceType)?sourceId:null).setEnabled(enabled).setRemark(reason.trim());
        resolveRule(rule);
        require((before==null?rules.insert(rule):rules.updateById(rule))==1&&rule.getId()!=null,"QA责任规则保存失败");
        var after=get(routeId);
        var metadata=new LinkedHashMap<String,Object>();metadata.put("requestSource","WORK_TASK_RULE_CONFIG");
        metadata.put("routeId",routeId);metadata.put("taskType","QA_REVIEW");metadata.put("reason",reason.trim());
        metadata.put("beforeRules",before==null?List.of():List.of(before));metadata.put("afterRules",List.of(after));
        metadata.put("associatedSignatureId","NOT_APPLICABLE");metadata.put("permissionDecision","ALLOW");metadata.put("resultStatus","SUCCESS");
        operationAudit.record(new MesProEdhrOperationAuditCommand().setRequestId("QA-RULE-"+UUID.randomUUID())
                .setObjectType("WORK_TASK_ASSIGNMENT_RULE").setObjectId(rule.getId().toString()).setRouteId(routeId)
                .setOperationType("WORK_TASK_RULE_SAVE").setActionName("保存QA评审负责人规则")
                .setActorUserId(SecurityFrameworkUtils.getLoginUserId()).setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode("mes:pro-edhr-work-task-rule:update").setPermissionDecision("ALLOW").setResultStatus("SUCCESS")
                .setBeforeSummaryHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(JsonUtils.toJsonString(before==null?Map.of():before)))
                .setAfterSummaryHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(JsonUtils.toJsonString(after)))
                .setMetadataJson(JsonUtils.toJsonString(metadata)));
        return after;
    }
    private void route(Long id,boolean lock) {
        var route=routes.selectOne(new LambdaQueryWrapperX<MesProRouteDO>().eq(MesProRouteDO::getId,id).last(lock?"FOR UPDATE":""));
        require(route!=null&&CommonStatusEnum.isEnable(route.getStatus()),"请选择本租户启用的正式路线");
    }
}
