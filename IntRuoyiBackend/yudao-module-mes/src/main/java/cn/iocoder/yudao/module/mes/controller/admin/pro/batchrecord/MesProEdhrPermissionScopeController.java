package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPermissionEvaluateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPermissionEvaluateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPermissionRuleRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPermissionRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPermissionScopeDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPermissionScopeSaveReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionEvaluateCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionEvaluateResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionRuleCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionRuleResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeDetailResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeQueryCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeSaveCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES eDHR 对象级权限")
@RestController
@RequestMapping("/mes/pro/edhr-permission-scopes")
@Validated
public class MesProEdhrPermissionScopeController {

    @Resource
    private MesProEdhrPermissionScopeService permissionScopeService;

    @PostMapping("/save")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-permission-scope:save')")
    public CommonResult<MesProEdhrPermissionScopeDetailRespVO> save(
            @Valid @RequestBody MesProEdhrPermissionScopeSaveReqVO reqVO) {
        return success(toResp(permissionScopeService.saveRules(new MesProEdhrPermissionScopeSaveCommand()
                .setScopeId(reqVO.getScopeId())
                .setScopeName(reqVO.getScopeName())
                .setObjectType(reqVO.getObjectType())
                .setObjectId(reqVO.getObjectId())
                .setParentScopeId(reqVO.getParentScopeId())
                .setExpectedVersion(reqVO.getExpectedVersion())
                .setRules(toRuleCommands(reqVO.getRules())))));
    }

    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-permission-scope:query')")
    public CommonResult<MesProEdhrPermissionScopeDetailRespVO> get(@RequestParam(value = "scopeId", required = false) Long scopeId,
                                                                   @RequestParam(value = "objectType", required = false) String objectType,
                                                                   @RequestParam(value = "objectId", required = false) String objectId) {
        return success(toResp(permissionScopeService.getDetail(new MesProEdhrPermissionScopeQueryCommand()
                .setScopeId(scopeId)
                .setObjectType(objectType)
                .setObjectId(objectId))));
    }

    @PostMapping("/evaluate")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-permission-scope:evaluate')")
    public CommonResult<MesProEdhrPermissionEvaluateRespVO> evaluate(
            @Valid @RequestBody MesProEdhrPermissionEvaluateReqVO reqVO) {
        MesProEdhrPermissionEvaluateResult result = permissionScopeService.evaluate(
                new MesProEdhrPermissionEvaluateCommand()
                        .setScopeId(reqVO.getScopeId())
                        .setObjectType(reqVO.getObjectType())
                        .setObjectId(reqVO.getObjectId())
                        .setBatchExecutionId(reqVO.getBatchExecutionId())
                        .setExecutionId(reqVO.getExecutionId())
                        .setWorkTaskId(reqVO.getWorkTaskId())
                        .setRouteId(reqVO.getRouteId())
                        .setRouteProcessId(reqVO.getRouteProcessId())
                        .setReportId(reqVO.getReportId())
                        .setRecordCategory(reqVO.getRecordCategory())
                        .setAbilities(reqVO.getAbilities())
                        .setPermissionCode("mes:pro-edhr-permission-scope:evaluate")
                        .setActionName("评估 eDHR 对象级权限"));
        return success(new MesProEdhrPermissionEvaluateRespVO()
                .setScopeId(result.getScopeId())
                .setObjectType(result.getObjectType())
                .setObjectId(result.getObjectId())
                .setDecisions(result.getDecisions())
                .setMatchedRuleIds(result.getMatchedRuleIds())
                .setOperationAuditEventId(result.getOperationAuditEventId()));
    }

    private List<MesProEdhrPermissionRuleCommand> toRuleCommands(List<MesProEdhrPermissionRuleSaveReqVO> rules) {
        if (rules == null) {
            return List.of();
        }
        return rules.stream().map(rule -> new MesProEdhrPermissionRuleCommand()
                .setSubjectType(rule.getSubjectType())
                .setSubjectId(rule.getSubjectId())
                .setAbility(rule.getAbility())
                .setDecision(rule.getDecision())
                .setPriority(rule.getPriority())
                .setEffectiveFrom(rule.getEffectiveFrom())
                .setEffectiveTo(rule.getEffectiveTo())
                .setStatus(rule.getStatus())).toList();
    }

    private MesProEdhrPermissionScopeDetailRespVO toResp(MesProEdhrPermissionScopeDetailResult result) {
        return new MesProEdhrPermissionScopeDetailRespVO()
                .setScopeId(result.getScopeId())
                .setScopeName(result.getScopeName())
                .setObjectType(result.getObjectType())
                .setObjectId(result.getObjectId())
                .setParentScopeId(result.getParentScopeId())
                .setStatus(result.getStatus())
                .setVersion(result.getVersion())
                .setRules(result.getRules() == null ? List.of() : result.getRules().stream()
                        .map(this::toRuleResp).toList())
                .setOperationAuditEventId(result.getOperationAuditEventId());
    }

    private MesProEdhrPermissionRuleRespVO toRuleResp(MesProEdhrPermissionRuleResult rule) {
        return new MesProEdhrPermissionRuleRespVO()
                .setId(rule.getId())
                .setScopeId(rule.getScopeId())
                .setSubjectType(rule.getSubjectType())
                .setSubjectId(rule.getSubjectId())
                .setAbility(rule.getAbility())
                .setDecision(rule.getDecision())
                .setPriority(rule.getPriority())
                .setEffectiveFrom(rule.getEffectiveFrom())
                .setEffectiveTo(rule.getEffectiveTo())
                .setStatus(rule.getStatus())
                .setVersion(rule.getVersion());
    }
}
