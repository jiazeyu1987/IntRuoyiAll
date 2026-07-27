package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPermissionScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrPermissionScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteFlowContextMatcher;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordCellRuleSupport;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordJimuReportGateway;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleMenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyTemplateMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.MenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import jakarta.annotation.Resource;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class MesProEdhrRehearsalReadinessServiceImpl implements MesProEdhrRehearsalReadinessService {

    private static final String BPM_PROCESS_KEY = "mes-edhr-approval-v1";
    private static final Long EDHR_PARENT_MENU_ID = 900220L;
    private static final String RULE_SCOPE_TYPE_ROUTE = "ROUTE";
    private static final String FORM_SLOT_MAIN = "MAIN";
    private static final List<String> PERMISSIONS_EXECUTOR = List.of(
            "mes:pro-work-order:query",
            "mes:pro-edhr-batch-execution:query",
            "mes:pro-edhr-batch-execution:create",
            "mes:pro-edhr-batch-execution:update",
            "mes:pro-edhr-batch-execution:close",
            "mes:pro-edhr-work-task:query",
            "mes:pro-batch-record-execution:track",
            "mes:pro-batch-record-execution:domain-trace-query");
    private static final List<String> PERMISSIONS_APPROVER = List.of(
            "mes:pro-edhr-batch-execution:query",
            "mes:pro-edhr-work-task:query",
            "mes:pro-batch-record-execution:approve",
            "mes:pro-batch-record-execution:track",
            "mes:pro-batch-record-execution:domain-trace-query");
    private static final List<String> PERMISSIONS_ARCHIVER = List.of(
            "mes:pro-edhr-batch-execution:query",
            "mes:pro-edhr-work-task:query",
            "mes:pro-edhr-batch-execution-archive:create",
            "mes:pro-edhr-batch-execution-archive:query",
            "mes:pro-edhr-batch-execution-archive:download",
            "mes:pro-batch-record-execution:track",
            "mes:pro-batch-record-execution:domain-trace-query");
    private static final List<String> BPM_NOTIFY_TEMPLATE_CODES = List.of(
            "MES_EDHR_BPM_TASK_ASSIGNED",
            "MES_EDHR_BPM_APPROVED",
            "MES_EDHR_BPM_REJECTED",
            "MES_EDHR_BPM_TASK_TIMEOUT");
    private static final List<String> REQUIRED_EXECUTOR_RECORD_ABILITIES = List.of("VIEW", "FILL", "SIGN");
    private static final List<String> REQUIRED_APPROVER_RECORD_ABILITIES = List.of("VIEW", "APPROVE");

    @Resource
    private MenuMapper menuMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Resource
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Resource
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesProEdhrPermissionScopeMapper permissionScopeMapper;
    @Resource
    private MesProEdhrWorkTaskAssignmentRuleMapper assignmentRuleMapper;
    @Resource
    private MesProEdhrProcessFormPermissionRuleMapper processFormPermissionRuleMapper;
    @Resource
    private MesProEdhrPermissionScopeService permissionScopeService;
    @Resource
    private MesProBatchRecordReportMapper reportMapper;
    @Resource
    private MesProBatchRecordVersionMapper batchRecordVersionMapper;
    @Resource
    private MesProBatchRecordJimuReportGateway jimuReportGateway;
    @Resource
    private DccElectronicSignatureAuthorizationService signatureAuthorizationService;
    @Resource
    private BpmProcessDefinitionService bpmProcessDefinitionService;
    @Resource
    private NotifyTemplateMapper notifyTemplateMapper;

    @Override
    public MesProEdhrRehearsalReadinessResult check(MesProEdhrRehearsalReadinessCommand command) {
        MesProEdhrRehearsalReadinessResult result = new MesProEdhrRehearsalReadinessResult();
        checkRoleMenus(result, "executor", command.getExecutorUserId(), PERMISSIONS_EXECUTOR);
        checkRoleMenus(result, "approver", command.getApproverUserId(), PERMISSIONS_APPROVER);
        checkRoleMenus(result, "archiver", command.getArchiverUserId(), PERMISSIONS_ARCHIVER);
        checkSignatureAuthorizations(result, command);
        checkBpmStartEligibility(result, command.getExecutorUserId());
        checkBpmNotifyTemplates(result);
        checkRouteArchiveRule(result, command.getRouteId(), command.getArchiverUserId());
        checkRouteBatchRecordConfig(result, command.getRouteId(), command.getExecutorUserId(),
                command.getApproverUserId());
        result.setOverallStatus(hasBlocker(result)
                ? MesProEdhrRehearsalReadinessResult.STATUS_BLOCKED
                : MesProEdhrRehearsalReadinessResult.STATUS_PASS);
        return result;
    }

    private void checkRoleMenus(MesProEdhrRehearsalReadinessResult result,
                                String roleKey,
                                Long userId,
                                List<String> requiredPermissions) {
        Set<Long> menuIds = getUserMenuIds(userId);
        boolean parentMissing = false;
        if (!menuIds.contains(EDHR_PARENT_MENU_ID)) {
            parentMissing = true;
            addBlocker(result, "MENU_PARENT_MISSING", roleKey, userId,
                    "用户缺少 eDHR 动态菜单父节点，页面可能进入 404。",
                    "为该角色绑定 eDHR批记录 父菜单后刷新权限缓存。");
        }
        List<String> missingPermissions = new ArrayList<>();
        for (String requiredPermission : requiredPermissions) {
            List<MenuDO> menus = menuMapper.selectListByPermission(requiredPermission);
            boolean hasPermissionMenu = menus.stream().anyMatch(menu -> menuIds.contains(menu.getId()));
            if (!hasPermissionMenu) {
                missingPermissions.add(requiredPermission);
            }
        }
        if (CollUtil.isNotEmpty(missingPermissions)) {
            addBlocker(result, "MENU_MISSING", roleKey, userId,
                    "用户缺少演练角色所需的 eDHR 菜单权限：" + String.join("、", missingPermissions),
                    "按角色模板补齐菜单权限，并确认登录后的 get-permission-info 返回该路由。");
            return;
        }
        if (parentMissing) {
            return;
        }
        addPass(result, "MENU_READY", roleKey, userId,
                "用户具备演练角色所需菜单权限：" + String.join("、", requiredPermissions));
    }

    private Set<Long> getUserMenuIds(Long userId) {
        List<UserRoleDO> userRoles = userRoleMapper.selectListByUserId(userId);
        if (CollUtil.isEmpty(userRoles)) {
            return Set.of();
        }
        Set<Long> roleIds = new HashSet<>();
        userRoles.forEach(row -> roleIds.add(row.getRoleId()));
        return roleMenuMapper.selectListByRoleId(roleIds).stream()
                .map(RoleMenuDO::getMenuId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }

    private void checkSignatureAuthorizations(MesProEdhrRehearsalReadinessResult result,
                                              MesProEdhrRehearsalReadinessCommand command) {
        List<Long> userIds = List.of(command.getExecutorUserId(), command.getApproverUserId(), command.getArchiverUserId());
        Map<Long, Boolean> authorizationMap = signatureAuthorizationService.getAuthorizationMap(userIds);
        checkSignatureAuthorization(result, authorizationMap, "executor", command.getExecutorUserId());
        checkSignatureAuthorization(result, authorizationMap, "approver", command.getApproverUserId());
        checkSignatureAuthorization(result, authorizationMap, "archiver", command.getArchiverUserId());
    }

    private void checkSignatureAuthorization(MesProEdhrRehearsalReadinessResult result,
                                             Map<Long, Boolean> authorizationMap,
                                             String roleKey,
                                             Long userId) {
        if (!Boolean.TRUE.equals(authorizationMap.get(userId))) {
            addBlocker(result, "SIGNATURE_AUTH_MISSING", roleKey, userId,
                    "用户未启用电子签名授权，提交、审批或归档签名会被阻断。",
                    "在电子签名授权管理中启用该用户，并记录授权依据。");
            return;
        }
        addPass(result, "SIGNATURE_AUTH_READY", roleKey, userId, "用户电子签名授权已启用。");
    }

    private void checkBpmStartEligibility(MesProEdhrRehearsalReadinessResult result, Long executorUserId) {
        ProcessDefinition definition = bpmProcessDefinitionService.getActiveProcessDefinition(BPM_PROCESS_KEY);
        if (definition == null || StrUtil.isBlank(definition.getId())) {
            addBlocker(result, "BPM_DEFINITION_MISSING", "executor", executorUserId,
                    "未找到 eDHR 审批流程的激活定义。",
                    "发布并启用 mes-edhr-approval-v1 流程定义。");
            return;
        }
        List<BpmProcessDefinitionInfoDO> definitionInfos =
                bpmProcessDefinitionService.getProcessDefinitionInfoList(List.of(definition.getId()));
        if (CollUtil.isEmpty(definitionInfos)) {
            addBlocker(result, "BPM_DEFINITION_INFO_MISSING", "executor", executorUserId,
                    "eDHR BPM 流程定义缺少扩展信息。",
                    "补齐 bpm_process_definition_info，并确认没有重复定义信息。");
            return;
        }
        if (definitionInfos.size() != 1) {
            addBlocker(result, "BPM_DEFINITION_INFO_MISMATCH", "executor", executorUserId,
                    "eDHR BPM 流程定义扩展信息数量异常：" + definitionInfos.size(),
                    "清理重复的 bpm_process_definition_info 记录后再预检。");
            return;
        }
        BpmProcessDefinitionInfoDO definitionInfo = definitionInfos.get(0);
        if (!bpmProcessDefinitionService.canUserStartProcessDefinition(definitionInfo, executorUserId)) {
            addBlocker(result, "BPM_START_USER_DENIED", "executor", executorUserId,
                    "执行人不在 eDHR BPM 可发起范围内。",
                    "将执行人加入流程定义 startUserIds，或清空 startUserIds 表示全员可发起。");
            return;
        }
        addPass(result, "BPM_START_READY", "executor", executorUserId, "执行人具备 eDHR BPM 发起资格。");
    }

    private void checkBpmNotifyTemplates(MesProEdhrRehearsalReadinessResult result) {
        List<String> missingCodes = new ArrayList<>();
        List<String> disabledCodes = new ArrayList<>();
        for (String templateCode : BPM_NOTIFY_TEMPLATE_CODES) {
            NotifyTemplateDO template = notifyTemplateMapper.selectByCode(templateCode);
            if (template == null) {
                missingCodes.add(templateCode);
                continue;
            }
            if (!Objects.equals(template.getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
                disabledCodes.add(templateCode);
            }
        }
        if (CollUtil.isNotEmpty(missingCodes)) {
            addBlocker(result, "BPM_NOTIFY_TEMPLATE_MISSING", "bpm", 0L,
                    "eDHR BPM 站内信模板缺失：" + String.join("、", missingCodes),
                    "执行正式种子脚本 ruoyi-vue-pro/sql/mysql/20260622_mes_edhr_bpm_notify_to_inbox.sql，或通过站内信模板正式页面补齐后再演练。");
            return;
        }
        if (CollUtil.isNotEmpty(disabledCodes)) {
            addBlocker(result, "BPM_NOTIFY_TEMPLATE_DISABLED", "bpm", 0L,
                    "eDHR BPM 站内信模板被禁用：" + String.join("、", disabledCodes),
                    "在站内信模板正式页面启用上述模板，确保审批通过、驳回、待办和超时通知不会阻断 BPM。");
            return;
        }
        addPass(result, "BPM_NOTIFY_TEMPLATE_READY", "bpm", 0L,
                "eDHR BPM 站内信模板已配置并启用。");
    }

    private void checkRouteArchiveRule(MesProEdhrRehearsalReadinessResult result, Long routeId, Long archiverUserId) {
        MesProEdhrWorkTaskAssignmentRuleDO rule = assignmentRuleMapper.selectEnabledByScopeAndType(
                RULE_SCOPE_TYPE_ROUTE, routeId, MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE);
        if (rule == null) {
            addBlocker(result, "ARCHIVE_RULE_MISSING", "archiver", archiverUserId,
                    "工艺路线未配置启用的最终归档责任规则，批次关闭后无法生成可处理的归档待办。",
                    "在 eDHR 工作任务看板的归档规则中，为该工艺路线配置启用的最终归档责任人。");
            return;
        }
        Long expectedArchiverId = rule.getCandidateSourceId() != null ? rule.getCandidateSourceId() : rule.getAssigneeUserId();
        if (!Objects.equals(expectedArchiverId, archiverUserId)) {
            addBlocker(result, "ARCHIVE_RULE_ASSIGNEE_MISMATCH", "archiver", archiverUserId,
                    "所选归档人不是该工艺路线最终归档规则的责任人：当前选择=" + archiverUserId + "，规则责任人=" + expectedArchiverId + "。",
                    "选择路线归档规则中的责任人重新预检，或通过归档规则正式入口调整责任人后再演练。");
            return;
        }
        addPass(result, "ARCHIVE_RULE_READY", "archiver", archiverUserId,
                "所选归档人与工艺路线最终归档责任规则一致。");
    }

    private void checkRouteBatchRecordConfig(MesProEdhrRehearsalReadinessResult result,
                                             Long routeId,
                                             Long executorUserId,
                                             Long approverUserId) {
        String batchUseType = MesProRouteFlowConfigTypeEnum.BATCH.getType();
        MesProRouteFlowConfigDO flowConfig = routeFlowConfigMapper.selectByRouteIdAndUseType(routeId, batchUseType);
        if (flowConfig != null && !MesProRouteFlowContextMatcher.isFlowContext(flowConfig, routeId, batchUseType)) {
            addBlocker(result, "ROUTE_BATCH_FLOW_DISABLED", "route", routeId,
                    "工艺路线未启用 eDHR 批记录工艺流程配置。",
                    "在工艺流程中启用 BATCH 用途后再运行演练预检。");
            return;
        }
        Map<Long, MesProRouteFlowProcessConfigDO> enabledConfigMap = new LinkedHashMap<>();
        for (MesProRouteFlowProcessConfigDO config :
                routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(routeId, batchUseType)) {
            if (config.getId() != null
                    && config.getRouteProcessId() != null) {
                enabledConfigMap.put(config.getId(), config);
            }
        }
        List<MesProRouteFlowProcessBatchRecordDO> records =
                routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(routeId, batchUseType).stream()
                        .filter(record -> isOwnedByEnabledProcessConfig(record, enabledConfigMap, batchUseType))
                        .toList();
        if (CollUtil.isEmpty(records)) {
            addBlocker(result, "ROUTE_BATCH_RECORD_MISSING", "route", routeId,
                    "工艺路线未配置 eDHR 批记录表单。",
                    "在工艺路线批记录配置中绑定可执行表单和任务规则。");
            return;
        }
        boolean hasPermissionRuleGap = false;
        boolean hasProcessFormRuleGap = false;
        for (MesProRouteFlowProcessBatchRecordDO record : records) {
            Long currentRouteProcessId =
                    resolveFrozenRouteProcessId(routeId, record.getRouteProcessId());
            ReadinessFormBinding formBinding = resolveReadinessFormBinding(result, record);
            if (formBinding == null) {
                return;
            }
            MesProEdhrPermissionScopeDO scope = record.getPermissionScopeId() == null
                    ? null : permissionScopeMapper.selectById(record.getPermissionScopeId());
            if (!checkTemplateReadiness(result, record, formBinding)) {
                return;
            }
            if (!checkProcessFormFillRule(result, record, formBinding, currentRouteProcessId, executorUserId)) {
                hasProcessFormRuleGap = true;
            }
            if (scope != null) {
                if (!checkRecordScopeAbilities(result, routeId, record, currentRouteProcessId,
                        scope, executorUserId, "executor",
                        REQUIRED_EXECUTOR_RECORD_ABILITIES)) {
                    hasPermissionRuleGap = true;
                }
                if (!checkRecordScopeAbilities(result, routeId, record, currentRouteProcessId,
                        scope, approverUserId, "approver",
                        REQUIRED_APPROVER_RECORD_ABILITIES)) {
                    hasPermissionRuleGap = true;
                }
            }
        }
        if (hasPermissionRuleGap) {
            return;
        }
        if (hasProcessFormRuleGap) {
            return;
        }
        addPass(result, "ROUTE_BATCH_RECORD_READY", "route", routeId, "工艺路线批记录配置和填写规则已存在。");
    }

    private boolean isOwnedByEnabledProcessConfig(
            MesProRouteFlowProcessBatchRecordDO record,
            Map<Long, MesProRouteFlowProcessConfigDO> enabledConfigMap,
            String batchUseType) {
        MesProRouteFlowProcessConfigDO processConfig =
                record == null ? null : enabledConfigMap.get(record.getRouteFlowProcessConfigId());
        return processConfig != null
                && Objects.equals(batchUseType, record.getUseType())
                && Objects.equals(batchUseType, processConfig.getUseType())
                && Objects.equals(record.getRouteId(), processConfig.getRouteId())
                && Objects.equals(record.getRouteProcessId(), processConfig.getRouteProcessId());
    }

    private boolean checkProcessFormFillRule(MesProEdhrRehearsalReadinessResult result,
                                             MesProRouteFlowProcessBatchRecordDO record,
                                             ReadinessFormBinding formBinding,
                                             Long currentRouteProcessId,
                                             Long executorUserId) {
        String reportId = formBinding.reportId();
        Long batchRecordVersionId = formBinding.batchRecordVersionId();
        List<MesProEdhrProcessFormPermissionRuleDO> fillRules =
                processFormPermissionRuleMapper.selectEnabledFillRulesForRouteOrReport(
                        currentRouteProcessId, reportId, batchRecordVersionId);
        if (fillRules.isEmpty() && !Objects.equals(currentRouteProcessId, record.getRouteProcessId())) {
            fillRules = processFormPermissionRuleMapper.selectEnabledFillRulesForRouteOrReport(
                    record.getRouteProcessId(), reportId, batchRecordVersionId);
        }
        if (fillRules.isEmpty()) {
            addBlocker(result, "PROCESS_FORM_FILL_RULE_MISSING", "executor", executorUserId,
                    "工序表单未配置启用的填写权限规则：routeProcessId=" + currentRouteProcessId
                            + "，boundReportId=" + record.getBatchRecordReportId()
                            + "，resolvedReportId=" + reportId
                            + "，batchRecordVersionId=" + batchRecordVersionId
                            + "。批次开始后无法生成可填写待办。",
                    "在工艺路线电子批记录页签为该工序表单配置填写权限/派工规则后再演练。");
            return false;
        }
        if (fillRules.stream().anyMatch(fillRule ->
                StrUtil.isBlank(fillRule.getCandidateSourceType())
                        || StrUtil.isBlank(fillRule.getCandidateSourceIds()))) {
            addBlocker(result, "PROCESS_FORM_FILL_RULE_CANDIDATE_EMPTY", "executor", executorUserId,
                    "工序表单填写权限规则缺少候选来源或候选对象：routeProcessId=" + currentRouteProcessId
                            + "，reportId=" + reportId + "。",
                    "在工艺路线电子批记录页签补齐填写权限的候选来源和候选对象后再演练。");
            return false;
        }
        return true;
    }

    private boolean checkRecordScopeAbilities(MesProEdhrRehearsalReadinessResult result,
                                              Long routeId,
                                              MesProRouteFlowProcessBatchRecordDO record,
                                              Long currentRouteProcessId,
                                              MesProEdhrPermissionScopeDO scope,
                                              Long userId,
                                              String roleKey,
                                              List<String> requiredAbilities) {
        MesProEdhrPermissionEvaluateResult evaluation = permissionScopeService.evaluate(
                new MesProEdhrPermissionEvaluateCommand()
                        .setScopeId(scope.getId())
                        .setObjectType(scope.getObjectType())
                        .setObjectId(scope.getObjectId())
                        .setRouteId(routeId)
                        .setRouteProcessId(currentRouteProcessId)
                        .setReportId(record.getBatchRecordReportId())
                        .setRecordCategory("BATCH_RECORD")
                        .setAbilities(requiredAbilities)
                        .setActorUserId(userId)
                        .setPermissionCode("mes:pro-edhr-rehearsal-readiness:check")
                        .setActionName("eDHR 演练预检-批记录表单对象权限"));
        Map<String, String> decisions = evaluation.getDecisions() == null ? Map.of() : evaluation.getDecisions();
        List<String> deniedAbilities = requiredAbilities.stream()
                .filter(ability -> !"ALLOW".equals(decisions.get(ability)))
                .toList();
        if (CollUtil.isEmpty(deniedAbilities)) {
            return true;
        }
        addBlocker(result, "PERMISSION_RULE_MISSING", roleKey, userId,
                "批记录表单对象权限缺少 ALLOW 规则：scopeId=" + scope.getId()
                        + "，reportId=" + record.getBatchRecordReportId()
                        + "，缺少能力=" + String.join("、", deniedAbilities) + "。",
                "在 eDHR 权限矩阵页面为该权限范围补齐 " + roleKey
                        + " 用户的 " + String.join("、", deniedAbilities) + " ALLOW 规则后再演练。");
        return false;
    }

    private ReadinessFormBinding resolveReadinessFormBinding(MesProEdhrRehearsalReadinessResult result,
                                                             MesProRouteFlowProcessBatchRecordDO record) {
        String reportId = StrUtil.trim(record.getBatchRecordReportId());
        if (StrUtil.isBlank(reportId)) {
            addBlocker(result, "TEMPLATE_REPORT_MISSING", "template", record.getId(),
                    "工艺路线批记录未绑定报表。",
                    "为该批记录补齐 batchRecordReportId 后再预检。");
            return null;
        }
        MesProBatchRecordReportDO report = reportMapper.selectByReportId(reportId);
        if (report == null) {
            addBlocker(result, "TEMPLATE_REPORT_MISSING", "template", record.getId(),
                    "找不到批记录报表元数据：" + reportId,
                    "先完成报表生成或恢复报表元数据。");
            return null;
        }
        Long definitionId = record.getBatchRecordDefinitionId() != null
                ? record.getBatchRecordDefinitionId()
                : report.getBatchRecordDefinitionId();
        Long versionId = record.getBatchRecordVersionId() != null
                ? record.getBatchRecordVersionId()
                : report.getBatchRecordVersionId();
        if (definitionId == null) {
            addBlocker(result, "TEMPLATE_STABLE_IDENTITY_MISSING", "template", record.getId(),
                    "工艺路线批记录绑定缺少稳定批记录定义身份，无法自动解析最新已发布版本：boundReportId=" + reportId + "。",
                    "重新绑定该表单到批记录定义的最新已发布版本，或修复报表元数据中的 batchRecordDefinitionId 后再预检。");
            return null;
        }
        MesProBatchRecordVersionDO latestVersion =
                batchRecordVersionMapper.selectLatestApprovedByDefinitionId(definitionId);
        if (latestVersion == null || latestVersion.getId() == null) {
            addBlocker(result, "TEMPLATE_LATEST_APPROVED_VERSION_MISSING", "template", record.getId(),
                    "批记录定义没有最新已发布版本：definitionId=" + definitionId
                            + "，boundReportId=" + reportId + "。",
                    "先完成该批记录定义的版本发布，再运行演练预检。");
            return null;
        }
        if (report.getSourceTableIndex() == null) {
            addBlocker(result, "TEMPLATE_LATEST_REPORT_MISSING", "template", record.getId(),
                    "旧绑定报表缺少 sourceTableIndex，无法映射最新已发布版本成员表：boundReportId=" + reportId + "。",
                    "修复报表元数据后再运行演练预检。");
            return null;
        }
        String expectedFormSlotType = normalizeFormSlotType(StrUtil.blankToDefault(
                record.getFormSlotType(), report.getFormSlotType()));
        List<MesProBatchRecordReportDO> matches = reportMapper
                .selectListByDefinitionIdAndVersionId(definitionId, latestVersion.getId()).stream()
                .filter(candidate -> Objects.equals(report.getSourceTableIndex(), candidate.getSourceTableIndex()))
                .filter(candidate -> Objects.equals(expectedFormSlotType,
                        normalizeFormSlotType(candidate.getFormSlotType())))
                .toList();
        if (matches.size() != 1) {
            addBlocker(result, "TEMPLATE_LATEST_REPORT_MISSING", "template", record.getId(),
                    "无法在最新已发布版本中唯一定位成员报表：definitionId=" + definitionId
                            + "，latestVersionId=" + latestVersion.getId()
                            + "，sourceTableIndex=" + report.getSourceTableIndex()
                            + "，formSlotType=" + expectedFormSlotType
                            + "，matches=" + matches.size() + "。",
                    "修复最新版本报表成员元数据后再运行演练预检。");
            return null;
        }
        MesProBatchRecordReportDO latestReport = matches.get(0);
        return new ReadinessFormBinding(latestReport.getReportId(), latestVersion.getId(), latestReport);
    }

    private String normalizeFormSlotType(String formSlotType) {
        return StrUtil.blankToDefault(formSlotType, FORM_SLOT_MAIN);
    }

    private boolean checkTemplateReadiness(MesProEdhrRehearsalReadinessResult result,
                                           MesProRouteFlowProcessBatchRecordDO record,
                                           ReadinessFormBinding formBinding) {
        String reportId = formBinding.reportId();
        if (StrUtil.isBlank(reportId) || formBinding.report() == null) {
            addBlocker(result, "TEMPLATE_REPORT_MISSING", "template", record.getId(),
                    "找不到可预检的批记录报表元数据：" + record.getBatchRecordReportId(),
                    "先完成报表生成或恢复报表元数据。");
            return false;
        }
        String reportJson = jimuReportGateway.getReportJson(reportId);
        if (StrUtil.isBlank(reportJson)) {
            addBlocker(result, "TEMPLATE_JSON_MISSING", "template", record.getId(),
                    "批记录报表 JSON 为空：" + reportId,
                    "恢复报表 JSON 后再预检。");
            return false;
        }
        JSONObject root = JSON.parseObject(reportJson);
        int unreviewedFillableCellCount = MesProBatchRecordCellRuleSupport.countUnreviewedFillableCells(root);
        if (unreviewedFillableCellCount > 0) {
            addBlocker(result, "TEMPLATE_CELL_RULE_UNREVIEWED", "template", record.getId(),
                    "批记录报表存在未确认填写规则单元格：" + reportId + "，数量=" + unreviewedFillableCellCount,
                    "先完成该报表的填写规则确认，再运行真实演练。");
            return false;
        }
        return true;
    }

    private record ReadinessFormBinding(String reportId,
                                        Long batchRecordVersionId,
                                        MesProBatchRecordReportDO report) {
    }

    private Long resolveFrozenRouteProcessId(Long routeId, Long snapshotRouteProcessId) {
        return routeProcessService.resolveFrozenRouteProcess(snapshotRouteProcessId, routeId, null).getId();
    }

    private boolean hasBlocker(MesProEdhrRehearsalReadinessResult result) {
        return result.getItems().stream().anyMatch(item ->
                MesProEdhrRehearsalReadinessResult.ITEM_STATUS_BLOCKER.equals(item.getStatus()));
    }

    private void addPass(MesProEdhrRehearsalReadinessResult result,
                         String code,
                         String roleKey,
                         Long subjectId,
                         String message) {
        result.getItems().add(new MesProEdhrRehearsalReadinessResult.Item()
                .setCode(code)
                .setStatus(MesProEdhrRehearsalReadinessResult.ITEM_STATUS_PASS)
                .setSeverity(MesProEdhrRehearsalReadinessResult.SEVERITY_INFO)
                .setRoleKey(roleKey)
                .setSubjectId(subjectId)
                .setMessage(message));
    }

    private void addBlocker(MesProEdhrRehearsalReadinessResult result,
                            String code,
                            String roleKey,
                            Long subjectId,
                            String message,
                            String suggestion) {
        result.getItems().add(new MesProEdhrRehearsalReadinessResult.Item()
                .setCode(code)
                .setStatus(MesProEdhrRehearsalReadinessResult.ITEM_STATUS_BLOCKER)
                .setSeverity(MesProEdhrRehearsalReadinessResult.SEVERITY_BLOCKER)
                .setRoleKey(roleKey)
                .setSubjectId(subjectId)
                .setMessage(message)
                .setSuggestion(suggestion));
    }
}
