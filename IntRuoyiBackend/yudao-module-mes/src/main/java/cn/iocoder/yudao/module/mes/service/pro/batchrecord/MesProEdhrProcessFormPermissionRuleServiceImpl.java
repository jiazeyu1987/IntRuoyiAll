package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportAssistRowVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchRecordFormPermissionRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrProcessFormPermissionRuleRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrProcessFormPermissionRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordCellRuleSupport;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordJimuReportGateway;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteFlowContextMatcher;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.dto.SystemEntitlementSyncReqDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserNickname;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_CANDIDATE_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_COMPLETION_POLICY_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_ROUTE_BINDING_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_SIGNATURE_ROLE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_SOURCE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_VERSION_REQUIRED;
import static cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper.FORM_LEVEL_ROUTE_PROCESS_ID;

@Service
public class MesProEdhrProcessFormPermissionRuleServiceImpl implements MesProEdhrProcessFormPermissionRuleService {

    private static final String RULE_TYPE_FILL = "FILL";
    private static final String RULE_TYPE_SIGNATURE = "SIGNATURE";
    private static final String ASSIST_SCOPE_ALL = "ALL";
    private static final String SOURCE_TYPE_USER = "USER";
    private static final String SOURCE_TYPE_USERS = "USERS";
    private static final String SOURCE_TYPE_ROLE = "ROLE";
    private static final Set<String> SUPPORTED_SOURCE_TYPES = Set.of(
            SOURCE_TYPE_USER, SOURCE_TYPE_USERS, SOURCE_TYPE_ROLE);
    private static final Set<String> SUPPORTED_COMPLETION_POLICIES = Set.of("ANY_ONE", "ALL");
    private static final Set<String> SUPPORTED_SIGNATURE_ROLES = Set.of("APPROVAL", "APPROVE", "REVIEW");
    private static final String STATUS_NOT_CONFIGURED = "NOT_CONFIGURED";
    private static final String STATUS_CONFIGURED = "CONFIGURED";
    private static final String OBJECT_TYPE_ROUTE_PROCESS_BATCH_RECORD = "ROUTE_PROCESS_BATCH_RECORD";
    private static final String DECISION_ALLOW = "ALLOW";
    private static final String STATUS_ENABLED = "ENABLED";
    private static final int UNLIMITED_DUE_MINUTES = Integer.MAX_VALUE;
    private static final String ENTITLEMENT_SOURCE_TYPE_FILLER = "EDHR_PROCESS_FORM_FILLER";
    private static final String ENTITLEMENT_POLICY_FILLER_MINIMAL = "MES_EDHR_FILLER_MINIMAL";
    private static final int MAX_ENTITLEMENT_SOURCE_DIGEST_LENGTH = 128;

    @Resource
    private MesProEdhrProcessFormPermissionRuleMapper processFormPermissionRuleMapper;
    @Resource
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Resource
    private MesProBatchRecordReportMapper batchRecordReportMapper;
    @Resource
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Resource
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesProEdhrPermissionScopeService permissionScopeService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private MesProEdhrWorkTaskService workTaskService;
    @Resource
    private MesProBatchRecordJimuReportGateway jimuReportGateway;

    @Override
    public MesProEdhrProcessFormPermissionRuleRespVO getRule(Long routeProcessId, String batchRecordReportId) {
        MesProRouteFlowProcessBatchRecordDO routeBatchRecord = findRouteBatchRecord(routeProcessId, batchRecordReportId);
        Long batchRecordVersionId = resolveReportBatchRecordVersionId(batchRecordReportId,
                routeBatchRecord == null ? List.of() : List.of(routeBatchRecord));
        List<MesProEdhrProcessFormPermissionRuleDO> rules =
                selectRulesByVersionScope(routeProcessId, batchRecordReportId, batchRecordVersionId);
        if (rules.isEmpty() && routeBatchRecord != null
                && !Objects.equals(routeProcessId, routeBatchRecord.getRouteProcessId())) {
            rules = selectRulesByVersionScope(
                    routeBatchRecord.getRouteProcessId(), batchRecordReportId, batchRecordVersionId);
        }
        if (rules.isEmpty()) {
            rules = selectRulesByVersionScope(
                    FORM_LEVEL_ROUTE_PROCESS_ID, batchRecordReportId, batchRecordVersionId);
        }
        MesProEdhrProcessFormPermissionRuleRespVO.CandidateRule fillRule = extractFillRule(rules);
        List<MesProEdhrProcessFormPermissionRuleRespVO.SignatureRule> signatureRules = extractSignatureRules(rules);
        return new MesProEdhrProcessFormPermissionRuleRespVO()
                .setRouteProcessId(routeProcessId)
                .setBatchRecordReportId(batchRecordReportId)
                .setFillRuleStatus(fillRule == null ? STATUS_NOT_CONFIGURED : STATUS_CONFIGURED)
                .setSignatureRuleStatus(signatureRules.isEmpty() ? STATUS_NOT_CONFIGURED : STATUS_CONFIGURED)
                .setPermissionScopeId(routeBatchRecord == null ? null : routeBatchRecord.getPermissionScopeId())
                .setFillRule(fillRule)
                .setSignatureRules(signatureRules)
                .setAffectedRouteBindingCount(routeBatchRecord == null ? 0 : 1);
    }

    private List<MesProEdhrProcessFormPermissionRuleDO> selectRulesByVersionScope(
            Long routeProcessId, String batchRecordReportId, Long batchRecordVersionId) {
        List<MesProEdhrProcessFormPermissionRuleDO> exactRules =
                processFormPermissionRuleMapper.selectListByRouteProcessReportAndVersion(
                        routeProcessId, batchRecordReportId, batchRecordVersionId);
        return exactRules;
    }

    @Override
    public MesProEdhrProcessFormPermissionRuleRespVO getRuleByReport(String batchRecordReportId) {
        String reportId = StrUtil.trim(batchRecordReportId);
        List<MesProRouteFlowProcessBatchRecordDO> routeBatchRecords = findRouteBatchRecordsByReport(reportId);
        Long batchRecordVersionId = resolveReportBatchRecordVersionId(reportId, routeBatchRecords);
        Set<Long> enabledRouteProcessIds = routeBatchRecords.stream()
                .map(MesProRouteFlowProcessBatchRecordDO::getRouteProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MesProEdhrProcessFormPermissionRuleDO> formLevelFillRules =
                processFormPermissionRuleMapper.selectEnabledFillRules(
                        FORM_LEVEL_ROUTE_PROCESS_ID, reportId, batchRecordVersionId)
                        .stream()
                        .filter(rule -> RULE_TYPE_FILL.equals(rule.getRuleType()))
                        .toList();
        List<MesProEdhrProcessFormPermissionRuleRespVO.FillAssignment> fillAssignments =
                formLevelFillRules.stream()
                        .filter(this::isAssistScopeRule)
                        .map(this::toFillAssignmentResp)
                        .toList();
        MesProRouteFlowProcessBatchRecordDO firstBinding = routeBatchRecords.isEmpty() ? null : routeBatchRecords.get(0);
        if (!fillAssignments.isEmpty()) {
            return new MesProEdhrProcessFormPermissionRuleRespVO()
                    .setRouteProcessId(FORM_LEVEL_ROUTE_PROCESS_ID)
                    .setBatchRecordReportId(reportId)
                    .setFillRuleStatus(STATUS_CONFIGURED)
                    .setSignatureRuleStatus(STATUS_NOT_CONFIGURED)
                    .setPermissionScopeId(firstBinding == null ? null : firstBinding.getPermissionScopeId())
                    .setFillRule(null)
                    .setFillAssignments(fillAssignments)
                    .setSignatureRules(List.of())
                    .setAffectedRouteBindingCount(routeBatchRecords.size());
        }
        MesProEdhrProcessFormPermissionRuleDO fillRuleDO =
                selectFormLevelFillRuleForReportList(reportId, routeBatchRecords);
        if (fillRuleDO == null) {
            fillRuleDO = processFormPermissionRuleMapper.selectEnabledFillRulesByReportId(reportId)
                        .stream()
                        .filter(rule -> enabledRouteProcessIds.contains(rule.getRouteProcessId()))
                        .findFirst()
                        .orElse(null);
        }
        MesProEdhrProcessFormPermissionRuleRespVO.CandidateRule fillRule =
                fillRuleDO == null ? null : toCandidateResp(fillRuleDO);
        return new MesProEdhrProcessFormPermissionRuleRespVO()
                .setRouteProcessId(fillRuleDO == null ? null : fillRuleDO.getRouteProcessId())
                .setBatchRecordReportId(reportId)
                .setFillRuleStatus(fillRule == null ? STATUS_NOT_CONFIGURED : STATUS_CONFIGURED)
                .setSignatureRuleStatus(STATUS_NOT_CONFIGURED)
                .setPermissionScopeId(firstBinding == null ? null : firstBinding.getPermissionScopeId())
                .setFillRule(fillRule)
                .setFillAssignments(List.of())
                .setSignatureRules(List.of())
                .setAffectedRouteBindingCount(routeBatchRecords.size());
    }

    private MesProEdhrProcessFormPermissionRuleDO selectFormLevelFillRuleForReportList(
            String reportId,
            List<MesProRouteFlowProcessBatchRecordDO> routeBatchRecords) {
        Long batchRecordVersionId = routeBatchRecords.stream()
                .map(MesProRouteFlowProcessBatchRecordDO::getBatchRecordVersionId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> {
                    MesProBatchRecordReportDO report = batchRecordReportMapper.selectByReportId(reportId);
                    return report == null ? null : report.getBatchRecordVersionId();
                });
        if (batchRecordVersionId != null) {
            return processFormPermissionRuleMapper.selectEnabledFillRule(
                    FORM_LEVEL_ROUTE_PROCESS_ID, reportId, batchRecordVersionId);
        }
        return processFormPermissionRuleMapper.selectEnabledFillRule(FORM_LEVEL_ROUTE_PROCESS_ID, reportId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrProcessFormPermissionRuleRespVO saveRule(MesProEdhrProcessFormPermissionRuleSaveReqVO reqVO) {
        normalizeCandidateRuleDueMinutes(reqVO.getFillRule());
        normalizeSignatureRuleDueMinutes(reqVO.getSignatureRules());
        validateCandidateRule(reqVO.getFillRule());
        validateSignatureRules(reqVO.getSignatureRules());
        MesProRouteFlowProcessBatchRecordDO routeBatchRecord =
                findRouteBatchRecord(reqVO.getRouteProcessId(), reqVO.getBatchRecordReportId());
        if (routeBatchRecord == null || routeBatchRecord.getId() == null) {
            throw exception(PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_ROUTE_BINDING_MISSING,
                    reqVO.getRouteProcessId(), reqVO.getBatchRecordReportId());
        }
        return saveRuleForRouteBinding(reqVO, routeBatchRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrProcessFormPermissionRuleRespVO saveRuleByReport(
            MesProEdhrBatchRecordFormPermissionRuleSaveReqVO reqVO) {
        String reportId = StrUtil.trim(reqVO.getBatchRecordReportId());
        List<MesProRouteFlowProcessBatchRecordDO> routeBatchRecords = findRouteBatchRecordsByReport(reportId);
        if (CollUtil.isNotEmpty(reqVO.getFillAssignments())) {
            if (reqVO.getFillRule() != null) {
                throw new IllegalArgumentException("fillRule and fillAssignments cannot both be submitted");
            }
            normalizeFillAssignmentDueMinutes(reqVO.getFillAssignments());
            Map<String, BatchRecordReportAssistRowVO> assistRows = requireAssistRowsByKey(reportId);
            validateFillAssignments(reqVO.getFillAssignments(), assistRows);
            List<MesProEdhrProcessFormPermissionRuleDO> savedRules =
                    saveReportLevelAssignments(reportId, reqVO.getFillAssignments(), assistRows, routeBatchRecords);
            MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule permissionRule =
                    aggregateAssignmentPermissionRule(reqVO.getFillAssignments());
            for (MesProRouteFlowProcessBatchRecordDO routeBatchRecord : routeBatchRecords) {
                bindPermissionScope(new MesProEdhrProcessFormPermissionRuleSaveReqVO()
                        .setRouteProcessId(routeBatchRecord.getRouteProcessId())
                        .setBatchRecordReportId(reportId)
                        .setFillRule(permissionRule)
                        .setSignatureRules(List.of()), routeBatchRecord);
            }
            String sourceKey = syncFormLevelFillerEntitlement(
                    reportId, reqVO.getFillAssignments(), routeBatchRecords);
            workTaskService.reconcileProcessFormFillTaskOwnership(sourceKey, savedRules.get(0), "填写人配置变更");
            return getRuleByReport(reportId).setAffectedRouteBindingCount(routeBatchRecords.size());
        }
        if (reqVO.getFillRule() == null) {
            throw new IllegalArgumentException("fillRule or fillAssignments is required");
        }
        normalizeCandidateRuleDueMinutes(reqVO.getFillRule());
        validateCandidateRule(reqVO.getFillRule());
        MesProEdhrProcessFormPermissionRuleDO savedRule =
                saveReportLevelRule(reportId, reqVO.getFillRule(), routeBatchRecords);
        for (MesProRouteFlowProcessBatchRecordDO routeBatchRecord : routeBatchRecords) {
            bindPermissionScope(new MesProEdhrProcessFormPermissionRuleSaveReqVO()
                    .setRouteProcessId(routeBatchRecord.getRouteProcessId())
                    .setBatchRecordReportId(reportId)
                    .setFillRule(reqVO.getFillRule())
                    .setSignatureRules(List.of()), routeBatchRecord);
        }
        String sourceKey = syncFormLevelFillerEntitlement(reportId, reqVO.getFillRule(), routeBatchRecords);
        workTaskService.reconcileProcessFormFillTaskOwnership(sourceKey, savedRule, "填写人配置变更");
        return getRuleByReport(reportId).setAffectedRouteBindingCount(routeBatchRecords.size());
    }

    private MesProEdhrProcessFormPermissionRuleDO saveReportLevelRule(
            String reportId,
            MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule fillRule,
            List<MesProRouteFlowProcessBatchRecordDO> routeBatchRecords) {
        Long batchRecordVersionId = requireBatchRecordVersionId(
                resolveReportBatchRecordVersionId(reportId, routeBatchRecords),
                FORM_LEVEL_ROUTE_PROCESS_ID, reportId);
        processFormPermissionRuleMapper.physicalDeleteByRouteProcessAndReport(
                FORM_LEVEL_ROUTE_PROCESS_ID, reportId);
        MesProRouteFlowProcessBatchRecordDO firstBinding =
                routeBatchRecords.isEmpty() ? null : routeBatchRecords.get(0);
        MesProEdhrProcessFormPermissionRuleDO rule = toDO(new MesProEdhrProcessFormPermissionRuleSaveReqVO()
                        .setRouteProcessId(FORM_LEVEL_ROUTE_PROCESS_ID)
                        .setBatchRecordReportId(reportId)
                        .setFillRule(fillRule)
                        .setSignatureRules(List.of()),
                firstBinding, RULE_TYPE_FILL, "", null, fillRule)
                .setRouteProcessId(FORM_LEVEL_ROUTE_PROCESS_ID)
                .setBatchRecordVersionId(batchRecordVersionId);
        processFormPermissionRuleMapper.insert(rule);
        return rule;
    }

    private List<MesProEdhrProcessFormPermissionRuleDO> saveReportLevelAssignments(
            String reportId,
            List<MesProEdhrBatchRecordFormPermissionRuleSaveReqVO.FillAssignment> fillAssignments,
            Map<String, BatchRecordReportAssistRowVO> assistRows,
            List<MesProRouteFlowProcessBatchRecordDO> routeBatchRecords) {
        Long batchRecordVersionId = requireBatchRecordVersionId(
                resolveReportBatchRecordVersionId(reportId, routeBatchRecords),
                FORM_LEVEL_ROUTE_PROCESS_ID, reportId);
        processFormPermissionRuleMapper.physicalDeleteByRouteProcessAndReport(
                FORM_LEVEL_ROUTE_PROCESS_ID, reportId);
        MesProRouteFlowProcessBatchRecordDO firstBinding =
                routeBatchRecords.isEmpty() ? null : routeBatchRecords.get(0);
        List<MesProEdhrProcessFormPermissionRuleDO> savedRules = new ArrayList<>();
        for (MesProEdhrBatchRecordFormPermissionRuleSaveReqVO.FillAssignment assignment : fillAssignments) {
            BatchRecordReportAssistRowVO assistRow = assistRows.get(StrUtil.trim(assignment.getScopeKey()));
            MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule candidateRule =
                    toCandidateRule(assignment);
            MesProEdhrProcessFormPermissionRuleDO rule = toDO(new MesProEdhrProcessFormPermissionRuleSaveReqVO()
                            .setRouteProcessId(FORM_LEVEL_ROUTE_PROCESS_ID)
                            .setBatchRecordReportId(reportId)
                            .setFillRule(candidateRule)
                            .setSignatureRules(List.of()),
                    firstBinding, RULE_TYPE_FILL, "", null, candidateRule)
                    .setRouteProcessId(FORM_LEVEL_ROUTE_PROCESS_ID)
                    .setBatchRecordVersionId(batchRecordVersionId)
                    .setScopeKey(StrUtil.trim(assignment.getScopeKey()))
                    .setFillableScopeJson(buildAssistRowFillableScopeJson(assistRow));
            processFormPermissionRuleMapper.insert(rule);
            savedRules.add(rule);
        }
        return savedRules;
    }

    private MesProEdhrProcessFormPermissionRuleRespVO saveRuleForRouteBinding(
            MesProEdhrProcessFormPermissionRuleSaveReqVO reqVO,
            MesProRouteFlowProcessBatchRecordDO routeBatchRecord) {
        requireBatchRecordVersionId(routeBatchRecord.getBatchRecordVersionId(),
                reqVO.getRouteProcessId(), reqVO.getBatchRecordReportId());
        processFormPermissionRuleMapper.physicalDeleteByRouteProcessReportAndVersion(
                reqVO.getRouteProcessId(), reqVO.getBatchRecordReportId(),
                routeBatchRecord.getBatchRecordVersionId());
        MesProEdhrProcessFormPermissionRuleDO savedRule =
                toDO(reqVO, routeBatchRecord, RULE_TYPE_FILL, "", null, reqVO.getFillRule());
        processFormPermissionRuleMapper.insert(savedRule);
        if (CollUtil.isNotEmpty(reqVO.getSignatureRules())) {
            for (MesProEdhrProcessFormPermissionRuleSaveReqVO.SignatureRule signatureRule : reqVO.getSignatureRules()) {
                processFormPermissionRuleMapper.insert(toDO(reqVO, routeBatchRecord, RULE_TYPE_SIGNATURE,
                        signatureRule.getSignatureCellKey(), signatureRule.getSignatureRole(),
                        signatureRule.getRule()));
            }
        }
        bindPermissionScope(reqVO, routeBatchRecord);
        String sourceKey = syncRouteLevelFillerEntitlement(reqVO, routeBatchRecord);
        workTaskService.reconcileProcessFormFillTaskOwnership(sourceKey, savedRule, "填写人配置变更");
        return getRule(reqVO.getRouteProcessId(), reqVO.getBatchRecordReportId());
    }

    private String syncRouteLevelFillerEntitlement(
            MesProEdhrProcessFormPermissionRuleSaveReqVO reqVO,
            MesProRouteFlowProcessBatchRecordDO routeBatchRecord) {
        Long batchRecordVersionId = requireBatchRecordVersionId(
                routeBatchRecord == null ? null : routeBatchRecord.getBatchRecordVersionId(),
                reqVO.getRouteProcessId(), reqVO.getBatchRecordReportId());
        String sourceKey = "ROUTE|" + reqVO.getRouteProcessId() + "|"
                + StrUtil.trim(reqVO.getBatchRecordReportId()) + "|"
                + batchRecordVersionId;
        syncFillerEntitlement(sourceKey, batchRecordVersionId, reqVO.getFillRule());
        return sourceKey;
    }

    private String syncFormLevelFillerEntitlement(
            String reportId,
            MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule fillRule,
            List<MesProRouteFlowProcessBatchRecordDO> routeBatchRecords) {
        Long batchRecordVersionId = requireBatchRecordVersionId(
                resolveReportBatchRecordVersionId(reportId, routeBatchRecords),
                FORM_LEVEL_ROUTE_PROCESS_ID, reportId);
        String sourceKey = "FORM|" + reportId + "|" + batchRecordVersionId;
        syncFillerEntitlement(sourceKey, batchRecordVersionId, fillRule);
        return sourceKey;
    }

    private String syncFormLevelFillerEntitlement(
            String reportId,
            List<MesProEdhrBatchRecordFormPermissionRuleSaveReqVO.FillAssignment> fillAssignments,
            List<MesProRouteFlowProcessBatchRecordDO> routeBatchRecords) {
        Long batchRecordVersionId = requireBatchRecordVersionId(
                resolveReportBatchRecordVersionId(reportId, routeBatchRecords),
                FORM_LEVEL_ROUTE_PROCESS_ID, reportId);
        String sourceKey = "FORM|" + reportId + "|" + batchRecordVersionId;
        Set<Long> resolvedUserIds = collectResolvedAssignmentUserIds(fillAssignments);
        if (resolvedUserIds.isEmpty()) {
            throw exception(PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_CANDIDATE_EMPTY);
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("tenantId is required to sync eDHR filler entitlement");
        }
        permissionApi.syncEntitlementClaims(SystemEntitlementSyncReqDTO.builder()
                .tenantId(tenantId)
                .sourceType(ENTITLEMENT_SOURCE_TYPE_FILLER)
                .sourceKey(sourceKey)
                .sourceVersion(String.valueOf(batchRecordVersionId))
                .sourceDigest(buildAssignmentEntitlementSourceDigest(fillAssignments))
                .policyCode(ENTITLEMENT_POLICY_FILLER_MINIMAL)
                .resolvedUserIds(resolvedUserIds)
                .operatorUserId(getLoginUserId())
                .operatorUsername(getLoginUserNickname())
                .build());
        return sourceKey;
    }

    private void syncFillerEntitlement(String sourceKey, Long batchRecordVersionId,
                                       MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule fillRule) {
        Set<Long> resolvedUserIds = resolveEnabledUsers(fillRule.getCandidateSourceType(),
                normalizeIds(fillRule.getCandidateSourceIds()))
                .stream()
                .map(AdminUserRespDTO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (resolvedUserIds.isEmpty()) {
            throw exception(PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_CANDIDATE_EMPTY);
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("tenantId is required to sync eDHR filler entitlement");
        }
        Long requiredVersionId = requireBatchRecordVersionId(batchRecordVersionId, null, sourceKey);
        permissionApi.syncEntitlementClaims(SystemEntitlementSyncReqDTO.builder()
                .tenantId(tenantId)
                .sourceType(ENTITLEMENT_SOURCE_TYPE_FILLER)
                .sourceKey(sourceKey)
                .sourceVersion(String.valueOf(requiredVersionId))
                .sourceDigest(buildFillerEntitlementSourceDigest(fillRule))
                .policyCode(ENTITLEMENT_POLICY_FILLER_MINIMAL)
                .resolvedUserIds(resolvedUserIds)
                .operatorUserId(getLoginUserId())
                .operatorUsername(getLoginUserNickname())
                .build());
    }

    private Long resolveReportBatchRecordVersionId(
            String reportId,
            List<MesProRouteFlowProcessBatchRecordDO> routeBatchRecords) {
        return routeBatchRecords.stream()
                .map(MesProRouteFlowProcessBatchRecordDO::getBatchRecordVersionId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> {
                    MesProBatchRecordReportDO report = batchRecordReportMapper.selectByReportId(reportId);
                    return report == null ? null : report.getBatchRecordVersionId();
                });
    }

    private Long requireBatchRecordVersionId(Long batchRecordVersionId, Long routeProcessId, String reportId) {
        if (batchRecordVersionId == null) {
            throw exception(PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_VERSION_REQUIRED,
                    routeProcessId, StrUtil.trim(reportId));
        }
        return batchRecordVersionId;
    }

    private String buildFillerEntitlementSourceDigest(
            MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule fillRule) {
        List<Long> candidateSourceIds = normalizeIds(fillRule.getCandidateSourceIds()).stream()
                .sorted()
                .toList();
        return "candidateSourceType=" + fillRule.getCandidateSourceType()
                + ";candidateSourceIds=" + candidateSourceIds
                + ";completionPolicy=" + fillRule.getCompletionPolicy()
                + ";dueMinutes=" + fillRule.getDueMinutes()
                + ";enabled=" + Boolean.TRUE.equals(fillRule.getEnabled());
    }

    private String buildAssignmentEntitlementSourceDigest(
            List<MesProEdhrBatchRecordFormPermissionRuleSaveReqVO.FillAssignment> fillAssignments) {
        String rawDigest = fillAssignments.stream()
                .map(assignment -> StrUtil.trim(assignment.getScopeKey())
                        + ":" + assignment.getCandidateSourceType()
                        + ":" + normalizeIds(assignment.getCandidateSourceIds()).stream().sorted().toList()
                        + ":" + assignment.getCompletionPolicy()
                        + ":" + assignment.getDueMinutes()
                        + ":" + Boolean.TRUE.equals(assignment.getEnabled()))
                .collect(Collectors.joining("|"));
        if (rawDigest.length() <= MAX_ENTITLEMENT_SOURCE_DIGEST_LENGTH) {
            return rawDigest;
        }
        return "assignmentCount=" + fillAssignments.size() + ";sha256=" + DigestUtil.sha256Hex(rawDigest);
    }

    private void normalizeCandidateRuleDueMinutes(MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule rule) {
        if (rule != null) {
            rule.setDueMinutes(UNLIMITED_DUE_MINUTES);
        }
    }

    private void normalizeSignatureRuleDueMinutes(
            List<MesProEdhrProcessFormPermissionRuleSaveReqVO.SignatureRule> signatureRules) {
        if (CollUtil.isEmpty(signatureRules)) {
            return;
        }
        for (MesProEdhrProcessFormPermissionRuleSaveReqVO.SignatureRule signatureRule : signatureRules) {
            normalizeCandidateRuleDueMinutes(signatureRule == null ? null : signatureRule.getRule());
        }
    }

    private void normalizeFillAssignmentDueMinutes(
            List<MesProEdhrBatchRecordFormPermissionRuleSaveReqVO.FillAssignment> fillAssignments) {
        for (MesProEdhrBatchRecordFormPermissionRuleSaveReqVO.FillAssignment assignment : fillAssignments) {
            if (assignment != null) {
                assignment.setDueMinutes(UNLIMITED_DUE_MINUTES);
            }
        }
    }

    private MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule toCandidateRule(
            MesProEdhrBatchRecordFormPermissionRuleSaveReqVO.FillAssignment assignment) {
        return new MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule()
                .setCandidateSourceType(assignment.getCandidateSourceType())
                .setCandidateSourceIds(assignment.getCandidateSourceIds())
                .setCompletionPolicy(assignment.getCompletionPolicy())
                .setDueMinutes(assignment.getDueMinutes())
                .setEnabled(assignment.getEnabled())
                .setRemark(assignment.getRemark());
    }

    private MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule aggregateAssignmentPermissionRule(
            List<MesProEdhrBatchRecordFormPermissionRuleSaveReqVO.FillAssignment> fillAssignments) {
        return new MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule()
                .setCandidateSourceType(SOURCE_TYPE_USERS)
                .setCandidateSourceIds(new ArrayList<>(collectResolvedAssignmentUserIds(fillAssignments)))
                .setCompletionPolicy("ANY_ONE")
                .setDueMinutes(UNLIMITED_DUE_MINUTES)
                .setEnabled(true)
                .setRemark("辅助行填写人合并授权");
    }

    private Set<Long> collectResolvedAssignmentUserIds(
            List<MesProEdhrBatchRecordFormPermissionRuleSaveReqVO.FillAssignment> fillAssignments) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (MesProEdhrBatchRecordFormPermissionRuleSaveReqVO.FillAssignment assignment : fillAssignments) {
            userIds.addAll(resolveEnabledUsers(assignment.getCandidateSourceType(),
                            normalizeIds(assignment.getCandidateSourceIds()))
                    .stream()
                    .map(AdminUserRespDTO::getId)
                    .toList());
        }
        return userIds;
    }

    private MesProEdhrProcessFormPermissionRuleRespVO.CandidateRule toResp(
            MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule rule) {
        return new MesProEdhrProcessFormPermissionRuleRespVO.CandidateRule()
                .setCandidateSourceType(rule.getCandidateSourceType())
                .setCandidateSourceIds(rule.getCandidateSourceIds())
                .setCompletionPolicy(rule.getCompletionPolicy())
                .setDueMinutes(rule.getDueMinutes())
                .setEnabled(rule.getEnabled())
                .setRemark(rule.getRemark())
                .setCandidateUsers(List.of());
    }

    private void bindPermissionScope(MesProEdhrProcessFormPermissionRuleSaveReqVO reqVO,
                                     MesProRouteFlowProcessBatchRecordDO routeBatchRecord) {
        MesProEdhrPermissionScopeDetailResult scope = permissionScopeService.saveRules(
                new MesProEdhrPermissionScopeSaveCommand()
                        .setScopeId(routeBatchRecord.getPermissionScopeId())
                        .setScopeName("route-process-batch-record-" + reqVO.getRouteProcessId()
                                + "-" + reqVO.getBatchRecordReportId())
                        .setObjectType(OBJECT_TYPE_ROUTE_PROCESS_BATCH_RECORD)
                        .setObjectId(buildScopeObjectId(reqVO.getRouteProcessId(), reqVO.getBatchRecordReportId()))
                        .setActorUserId(getLoginUserId())
                        .setActorUsername(getLoginUserNickname())
                        .setRules(buildPermissionRules(reqVO)));
        if (!Objects.equals(routeBatchRecord.getPermissionScopeId(), scope.getScopeId())) {
            routeFlowProcessBatchRecordMapper.updateById(MesProRouteFlowProcessBatchRecordDO.builder()
                    .id(routeBatchRecord.getId())
                    .permissionScopeId(scope.getScopeId())
                    .build());
        }
    }

    private List<MesProRouteFlowProcessBatchRecordDO> findRouteBatchRecordsByReport(
            String batchRecordReportId) {
        if (StrUtil.isBlank(batchRecordReportId)) {
            return List.of();
        }
        String batchUseType = MesProRouteFlowConfigTypeEnum.BATCH.getType();
        return routeFlowProcessBatchRecordMapper.selectListByBatchRecordReportIdAndUseType(
                StrUtil.trim(batchRecordReportId), batchUseType);
    }

    private MesProRouteFlowProcessBatchRecordDO findRouteBatchRecord(Long routeProcessId, String batchRecordReportId) {
        String batchUseType = MesProRouteFlowConfigTypeEnum.BATCH.getType();
        List<MesProRouteFlowProcessBatchRecordDO> directCandidates =
                routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(
                        List.of(routeProcessId), batchUseType);
        MesProRouteFlowProcessBatchRecordDO directCandidate = directCandidates.stream()
                .filter(record -> Objects.equals(routeProcessId, record.getRouteProcessId())
                        && Objects.equals(StrUtil.trim(batchRecordReportId),
                        StrUtil.trim(record.getBatchRecordReportId())))
                .findFirst()
                .orElse(null);
        if (directCandidate != null) {
            return isEnabledBatchFlowRecord(directCandidate, batchUseType) ? directCandidate : null;
        }
        return null;
    }

    private boolean isEnabledBatchFlowRecord(MesProRouteFlowProcessBatchRecordDO record, String batchUseType) {
        MesProRouteFlowConfigDO flowConfig =
                routeFlowConfigMapper.selectByRouteIdAndUseType(record.getRouteId(), batchUseType);
        if (!MesProRouteFlowContextMatcher.isEnabledFlowContext(flowConfig, record.getRouteId(), batchUseType)) {
            return false;
        }
        List<MesProRouteFlowProcessConfigDO> processConfigs =
                routeFlowProcessConfigMapper.selectListByRouteProcessIdsAndUseType(
                        List.of(record.getRouteProcessId()), batchUseType);
        return processConfigs.stream().anyMatch(config ->
                Objects.equals(config.getId(), record.getRouteFlowProcessConfigId())
                        && Objects.equals(config.getRouteId(), record.getRouteId())
                        && Objects.equals(config.getRouteProcessId(), record.getRouteProcessId()));
    }

    private String buildScopeObjectId(Long routeProcessId, String batchRecordReportId) {
        return routeProcessId + "|" + StrUtil.trim(batchRecordReportId);
    }

    private List<MesProEdhrPermissionRuleCommand> buildPermissionRules(
            MesProEdhrProcessFormPermissionRuleSaveReqVO reqVO) {
        List<MesProEdhrPermissionRuleCommand> rules = new ArrayList<>();
        addRuleCommands(rules, reqVO.getFillRule(), List.of("VIEW", "FILL"), 10);
        return rules;
    }

    private void addRuleCommands(List<MesProEdhrPermissionRuleCommand> target,
                                 MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule candidateRule,
                                 List<String> abilities,
                                 int priority) {
        if (candidateRule == null) {
            return;
        }
        for (Map.Entry<String, List<Long>> subjectEntry : resolvePermissionSubjects(candidateRule).entrySet()) {
            for (Long subjectId : subjectEntry.getValue()) {
                for (String ability : abilities) {
                    target.add(new MesProEdhrPermissionRuleCommand()
                            .setSubjectType(subjectEntry.getKey())
                            .setSubjectId(subjectId)
                            .setAbility(ability)
                            .setDecision(DECISION_ALLOW)
                            .setPriority(priority)
                            .setStatus(STATUS_ENABLED));
                }
            }
        }
    }

    private Map<String, List<Long>> resolvePermissionSubjects(
            MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule candidateRule) {
        List<Long> sourceIds = normalizeIds(candidateRule.getCandidateSourceIds());
        if (SOURCE_TYPE_USER.equals(candidateRule.getCandidateSourceType())
                || SOURCE_TYPE_USERS.equals(candidateRule.getCandidateSourceType())) {
            return Map.of("USER", sourceIds);
        }
        if (SOURCE_TYPE_ROLE.equals(candidateRule.getCandidateSourceType())) {
            return Map.of("ROLE", sourceIds);
        }
        throw exception(PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_SOURCE_INVALID,
                candidateRule.getCandidateSourceType());
    }

    private MesProEdhrProcessFormPermissionRuleDO toDO(MesProEdhrProcessFormPermissionRuleSaveReqVO reqVO,
                                                       MesProRouteFlowProcessBatchRecordDO routeBatchRecord,
                                                       String ruleType,
                                                       String signatureCellKey,
                                                       String signatureRole,
                                                       MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule rule) {
        return new MesProEdhrProcessFormPermissionRuleDO()
                .setRouteProcessId(reqVO.getRouteProcessId())
                .setBatchRecordReportId(reqVO.getBatchRecordReportId())
                .setBatchRecordDefinitionId(routeBatchRecord == null ? null : routeBatchRecord.getBatchRecordDefinitionId())
                .setBatchRecordVersionId(routeBatchRecord == null ? null : routeBatchRecord.getBatchRecordVersionId())
                .setRuleType(ruleType)
                .setScopeKey(ASSIST_SCOPE_ALL)
                .setSignatureCellKey(StrUtil.blankToDefault(signatureCellKey, ""))
                .setSignatureRole(signatureRole)
                .setCandidateSourceType(rule.getCandidateSourceType())
                .setCandidateSourceIds(joinIds(rule.getCandidateSourceIds()))
                .setCompletionPolicy(rule.getCompletionPolicy())
                .setDueMinutes(rule.getDueMinutes())
                .setEnabled(Boolean.TRUE.equals(rule.getEnabled()))
                .setFillableScopeJson(null)
                .setRemark(rule.getRemark());
    }

    private MesProEdhrProcessFormPermissionRuleRespVO.CandidateRule toCandidateResp(
            MesProEdhrProcessFormPermissionRuleDO rule) {
        List<Long> sourceIds = parseIds(rule.getCandidateSourceIds());
        return new MesProEdhrProcessFormPermissionRuleRespVO.CandidateRule()
                .setCandidateSourceType(rule.getCandidateSourceType())
                .setCandidateSourceIds(sourceIds)
                .setCompletionPolicy(rule.getCompletionPolicy())
                .setDueMinutes(rule.getDueMinutes())
                .setEnabled(rule.getEnabled())
                .setRemark(rule.getRemark())
                .setCandidateUsers(toCandidateUsers(resolveEnabledUsers(rule.getCandidateSourceType(), sourceIds)));
    }

    private MesProEdhrProcessFormPermissionRuleRespVO.FillAssignment toFillAssignmentResp(
            MesProEdhrProcessFormPermissionRuleDO rule) {
        List<Long> sourceIds = parseIds(rule.getCandidateSourceIds());
        return new MesProEdhrProcessFormPermissionRuleRespVO.FillAssignment()
                .setScopeKey(rule.getScopeKey())
                .setCandidateSourceType(rule.getCandidateSourceType())
                .setCandidateSourceIds(sourceIds)
                .setCompletionPolicy(rule.getCompletionPolicy())
                .setDueMinutes(rule.getDueMinutes())
                .setEnabled(rule.getEnabled())
                .setRemark(rule.getRemark())
                .setCandidateUsers(toCandidateUsers(resolveEnabledUsers(rule.getCandidateSourceType(), sourceIds)));
    }

    private boolean isAssistScopeRule(MesProEdhrProcessFormPermissionRuleDO rule) {
        String scopeKey = StrUtil.trim(rule.getScopeKey());
        return RULE_TYPE_FILL.equals(rule.getRuleType())
                && StrUtil.isNotBlank(scopeKey)
                && !ASSIST_SCOPE_ALL.equals(scopeKey);
    }

    private void validateCandidateRule(MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule rule) {
        if (!SUPPORTED_SOURCE_TYPES.contains(rule.getCandidateSourceType())) {
            throw exception(PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_SOURCE_INVALID, rule.getCandidateSourceType());
        }
        if (!SUPPORTED_COMPLETION_POLICIES.contains(rule.getCompletionPolicy())) {
            throw exception(PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_COMPLETION_POLICY_INVALID, rule.getCompletionPolicy());
        }
        List<Long> sourceIds = normalizeIds(rule.getCandidateSourceIds());
        if (sourceIds.isEmpty() || resolveEnabledUsers(rule.getCandidateSourceType(), sourceIds).isEmpty()) {
            throw exception(PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_CANDIDATE_EMPTY);
        }
    }

    private void validateFillAssignments(
            List<MesProEdhrBatchRecordFormPermissionRuleSaveReqVO.FillAssignment> fillAssignments,
            Map<String, BatchRecordReportAssistRowVO> assistRows) {
        Set<String> scopeKeys = new LinkedHashSet<>();
        for (MesProEdhrBatchRecordFormPermissionRuleSaveReqVO.FillAssignment assignment : fillAssignments) {
            if (assignment == null || StrUtil.isBlank(assignment.getScopeKey())) {
                throw new IllegalArgumentException("fill assignment scopeKey must not be blank");
            }
            String scopeKey = StrUtil.trim(assignment.getScopeKey());
            if (!scopeKeys.add(scopeKey)) {
                throw new IllegalArgumentException("duplicate fill assignment scopeKey " + scopeKey);
            }
            if (!assistRows.containsKey(scopeKey)) {
                throw new IllegalArgumentException("unknown assist row scopeKey " + scopeKey);
            }
            validateCandidateRule(toCandidateRule(assignment));
        }
        Set<String> missing = new LinkedHashSet<>(assistRows.keySet());
        missing.removeAll(scopeKeys);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("missing fill assignment for assist rows " + missing);
        }
    }

    private Map<String, BatchRecordReportAssistRowVO> requireAssistRowsByKey(String reportId) {
        String reportJson = jimuReportGateway.getReportJson(reportId);
        if (StrUtil.isBlank(reportJson)) {
            throw new IllegalArgumentException("assist rows require existing report json " + reportId);
        }
        JSONObject root = JSON.parseObject(reportJson);
        List<BatchRecordReportAssistRowVO> assistRows = MesProBatchRecordCellRuleSupport.extractAssistRows(root);
        if (assistRows.isEmpty()) {
            throw new IllegalArgumentException("assist rows are required before fillAssignments can be saved");
        }
        Map<String, BatchRecordReportAssistRowVO> result = new LinkedHashMap<>();
        for (BatchRecordReportAssistRowVO assistRow : assistRows) {
            String rowKey = StrUtil.trim(assistRow.getRowKey());
            if (StrUtil.isBlank(rowKey) || assistRow.getFields() == null || assistRow.getFields().isEmpty()) {
                throw new IllegalArgumentException("invalid assist row " + rowKey);
            }
            if (result.put(rowKey, assistRow) != null) {
                throw new IllegalArgumentException("duplicate assist row scopeKey " + rowKey);
            }
        }
        return result;
    }

    private String buildAssistRowFillableScopeJson(BatchRecordReportAssistRowVO assistRow) {
        JSONObject scope = new JSONObject(true);
        scope.put("schemaVersion", 2);
        JSONArray cells = new JSONArray();
        for (BatchRecordReportAssistRowVO.FieldVO field : assistRow.getFields()) {
            JSONObject cell = new JSONObject(true);
            cell.put("sourceTableIndex", 0);
            cell.put("rowIndex", field.getRowIndex());
            cell.put("columnIndex", field.getColumnIndex());
            cells.add(cell);
        }
        scope.put("cells", cells);
        return scope.toJSONString();
    }

    private void validateSignatureRules(
            List<MesProEdhrProcessFormPermissionRuleSaveReqVO.SignatureRule> signatureRules) {
        if (CollUtil.isEmpty(signatureRules)) {
            return;
        }
        for (MesProEdhrProcessFormPermissionRuleSaveReqVO.SignatureRule signatureRule : signatureRules) {
            if (signatureRule == null || StrUtil.isBlank(signatureRule.getSignatureCellKey())) {
                throw exception(PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_SIGNATURE_ROLE_INVALID, "");
            }
            if (!SUPPORTED_SIGNATURE_ROLES.contains(signatureRule.getSignatureRole())) {
                throw exception(PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_SIGNATURE_ROLE_INVALID,
                        signatureRule.getSignatureRole());
            }
            if (signatureRule.getRule() == null) {
                throw exception(PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_CANDIDATE_EMPTY);
            }
            validateCandidateRule(signatureRule.getRule());
        }
    }

    private MesProEdhrProcessFormPermissionRuleRespVO.CandidateRule extractFillRule(
            List<MesProEdhrProcessFormPermissionRuleDO> rules) {
        return rules.stream()
                .filter(rule -> RULE_TYPE_FILL.equals(rule.getRuleType()) && !isAssistScopeRule(rule))
                .findFirst()
                .map(this::toCandidateResp)
                .orElse(null);
    }

    private List<MesProEdhrProcessFormPermissionRuleRespVO.SignatureRule> extractSignatureRules(
            List<MesProEdhrProcessFormPermissionRuleDO> rules) {
        return rules.stream()
                .filter(rule -> RULE_TYPE_SIGNATURE.equals(rule.getRuleType()))
                .map(this::toSignatureResp)
                .toList();
    }

    private MesProEdhrProcessFormPermissionRuleRespVO.SignatureRule toSignatureResp(
            MesProEdhrProcessFormPermissionRuleDO rule) {
        return new MesProEdhrProcessFormPermissionRuleRespVO.SignatureRule()
                .setSignatureCellKey(rule.getSignatureCellKey())
                .setSignatureRole(rule.getSignatureRole())
                .setRule(toCandidateResp(rule));
    }

    private List<AdminUserRespDTO> resolveEnabledUsers(String sourceType, List<Long> sourceIds) {
        List<AdminUserRespDTO> users;
        if (SOURCE_TYPE_USER.equals(sourceType) || SOURCE_TYPE_USERS.equals(sourceType)) {
            users = adminUserApi.getUserList(sourceIds);
        } else if (SOURCE_TYPE_ROLE.equals(sourceType)) {
            Set<Long> userIds = permissionApi.getUserRoleIdListByRoleIds(sourceIds);
            users = CollUtil.isEmpty(userIds) ? List.of() : adminUserApi.getUserList(userIds);
        } else {
            throw exception(PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_SOURCE_INVALID, sourceType);
        }
        return distinctEnabledUsers(users);
    }

    private List<AdminUserRespDTO> distinctEnabledUsers(Collection<AdminUserRespDTO> users) {
        if (CollUtil.isEmpty(users)) {
            return List.of();
        }
        Set<Long> seen = new LinkedHashSet<>();
        List<AdminUserRespDTO> enabledUsers = new ArrayList<>();
        for (AdminUserRespDTO user : users) {
            if (user == null || user.getId() == null
                    || !CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus())
                    || !seen.add(user.getId())) {
                continue;
            }
            enabledUsers.add(user);
        }
        return enabledUsers;
    }

    private List<MesProEdhrProcessFormPermissionRuleRespVO.CandidateUser> toCandidateUsers(
            List<AdminUserRespDTO> users) {
        return users.stream()
                .map(user -> new MesProEdhrProcessFormPermissionRuleRespVO.CandidateUser()
                        .setUserId(user.getId())
                        .setDisplayName(StrUtil.blankToDefault(user.getNickname(), String.valueOf(user.getId()))))
                .toList();
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String joinIds(List<Long> ids) {
        return normalizeIds(ids).stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private List<Long> parseIds(String rawIds) {
        if (StrUtil.isBlank(rawIds)) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String item : rawIds.split(",")) {
            if (StrUtil.isBlank(item)) {
                continue;
            }
            ids.add(Long.parseLong(item.trim()));
        }
        return ids;
    }
}
