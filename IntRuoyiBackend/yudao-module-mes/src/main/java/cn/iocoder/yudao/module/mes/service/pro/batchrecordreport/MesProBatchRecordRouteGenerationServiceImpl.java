package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowBoundaryEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionSnapshotIdentityWriter;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowBoundaryEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import cn.iocoder.yudao.module.mes.enums.md.autocode.MesMdAutoCodeRuleCodeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionRuleCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeDetailResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeSaveCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteOwnerPermissionService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PROCESS_EMPTY;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PRODUCT_INFO_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PRODUCT_INFO_NOT_FIRST;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_BIND_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_EMPTY;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_NAME_REQUIRED;

@Service
@Validated
public class MesProBatchRecordRouteGenerationServiceImpl implements MesProBatchRecordRouteGenerationService {

    private static final String PRODUCT_INFO_KEYWORD = "产品信息";
    private static final String USE_TYPE_BATCH = MesProRouteFlowConfigTypeEnum.BATCH.getType();
    private static final String EXECUTION_MODE_SEQUENTIAL = "SEQUENTIAL";
    private static final String RECORD_CATEGORY_BATCH_RECORD = "BATCH_RECORD";
    private static final String VALIDATION_PROFILE_CONTROLLED_BATCH = "CONTROLLED_BATCH";
    private static final String REQUIRED_POLICY_REQUIRED = "REQUIRED";
    private static final String ARCHIVE_VISIBILITY_FINAL_DHR = "FINAL_DHR";
    private static final String OWNER_ROLE_PRODUCTION = "PRODUCTION";
    private static final String PROCESS_CODE_PREFIX = "ER";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String OBJECT_TYPE_ROUTE_PROCESS_BATCH_RECORD = "ROUTE_PROCESS_BATCH_RECORD";
    private static final String PERMISSION_SUBJECT_TYPE_USER = "USER";
    private static final String PERMISSION_DECISION_ALLOW = "ALLOW";
    private static final String PERMISSION_STATUS_ENABLED = "ENABLED";
    private static final String SNAPSHOT_CONFIGS_KEY = "configSnapshots";
    private static final String FLOW_GRAPH_KEY = "flowGraph";
    private static final String BATCH_USE_CONFIGS_KEY = "batchUseConfigs";
    private static final String BATCH_RECORD_REPORTS_KEY = "batchRecordReports";
    private static final String FORM_BINDINGS_KEY = "formBindings";
    private static final String ROUTE_START_PRODUCTION_LEADERS_KEY = "routeStartProductionLeaders";
    private static final String BATCH_RECORD_ATTACHMENT_OWNERS_KEY = "batchRecordAttachmentOwners";
    private static final int BATCH_RECORD_BINDING_PERMISSION_PRIORITY = 10;
    private static final List<String> BATCH_RECORD_BINDING_ABILITIES = List.of("VIEW", "FILL");

    @Resource
    private MesMdAutoCodeRecordService autoCodeRecordService;
    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProProcessMapper processMapper;
    @Resource
    private MesMdItemMapper itemMapper;
    @Resource
    private DccProjectCodeMapper dccProjectCodeMapper;
    @Resource
    private MesRouteDccProjectBindingMapper routeDccProjectBindingMapper;
    @Resource
    private MesProRouteProductMapper routeProductMapper;
    @Resource
    private MesProRouteProcessMapper routeProcessMapper;
    @Resource
    private MesProRouteProcessFlowBoundaryEdgeMapper routeProcessFlowBoundaryEdgeMapper;
    @Resource
    private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    @Resource
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Resource
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Resource
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProRouteOwnerPermissionService routeOwnerPermissionService;
    @Resource
    private MesProEdhrPermissionScopeService permissionScopeService;
    @Resource
    private MesProRouteService routeService;

    @Override
    public void validateUploadedWordRoute(List<MesProBatchRecordParsedTable> parsedTables) {
        if (CollUtil.isEmpty(parsedTables)) {
            throw exception(PRO_BATCH_RECORD_REPORT_PRODUCT_INFO_MISSING);
        }
        int productInfoIndex = findProductInfoIndex(parsedTables);
        if (productInfoIndex < 0) {
            throw exception(PRO_BATCH_RECORD_REPORT_PRODUCT_INFO_MISSING);
        }
        if (productInfoIndex != 0) {
            throw exception(PRO_BATCH_RECORD_REPORT_PRODUCT_INFO_NOT_FIRST);
        }
        if (parsedTables.size() <= 1) {
            throw exception(PRO_BATCH_RECORD_REPORT_PROCESS_EMPTY);
        }
    }

    @Override
    public MesProBatchRecordRouteGenerationResult generateForUploadedWord(String batchRecordName,
                                                                           List<MesProBatchRecordParsedTable> parsedTables,
                                                                           List<MesProBatchRecordReportView> reports,
                                                                           List<String> productNames) {
        return generateForUploadedWord(batchRecordName, parsedTables, reports, productNames, null, null);
    }

    @Override
    public MesProBatchRecordRouteGenerationResult generateRouteOnlyForUploadedWord(String batchRecordName,
                                                                                   List<MesProBatchRecordParsedTable> parsedTables,
                                                                                   List<String> productNames) {
        return generateRouteOnlyForUploadedWord(batchRecordName, parsedTables, productNames, null, null, false);
    }

    @Override
    public MesProBatchRecordRouteGenerationResult generateRouteOnlyForUploadedWord(String batchRecordName,
                                                                                   List<MesProBatchRecordParsedTable> parsedTables,
                                                                                   List<String> productNames,
                                                                                   Long expectedRouteId,
                                                                                   Long expectedRouteVersionId,
                                                                                   Boolean routeUpgradeConfirmed) {
        return generateRouteOnlyForUploadedWord(batchRecordName, parsedTables, productNames,
                expectedRouteId, expectedRouteVersionId, routeUpgradeConfirmed, null);
    }

    @Override
    public MesProBatchRecordRouteGenerationResult generateRouteOnlyForUploadedWord(String batchRecordName,
                                                                                   List<MesProBatchRecordParsedTable> parsedTables,
                                                                                   List<String> productNames,
                                                                                   Long expectedRouteId,
                                                                                   Long expectedRouteVersionId,
                                                                                   Boolean routeUpgradeConfirmed,
                                                                                   Long expectedRouteCandidateVersionId) {
        return generateForUploadedWord(batchRecordName, parsedTables, List.of(), productNames, null, null, false,
                expectedRouteId, expectedRouteVersionId, routeUpgradeConfirmed,
                expectedRouteCandidateVersionId, false, null);
    }

    @Override
    public MesProBatchRecordRouteGenerationResult generateRouteOnlyForUploadedWord(String batchRecordName,
                                                                                   List<MesProBatchRecordParsedTable> parsedTables,
                                                                                   List<String> productNames,
                                                                                   Long expectedRouteId,
                                                                                   Long expectedRouteVersionId,
                                                                                   Boolean routeUpgradeConfirmed,
                                                                                   Long expectedRouteCandidateVersionId,
                                                                                   Long dccProjectCodeId) {
        return generateForUploadedWord(batchRecordName, parsedTables, List.of(), productNames, null, null, false,
                expectedRouteId, expectedRouteVersionId, routeUpgradeConfirmed,
                expectedRouteCandidateVersionId, false, dccProjectCodeId);
    }

    @Override
    public MesProBatchRecordRouteGenerationResult generateForUploadedWord(String batchRecordName,
                                                                           List<MesProBatchRecordParsedTable> parsedTables,
                                                                           List<MesProBatchRecordReportView> reports,
                                                                           List<String> productNames,
                                                                           Long batchRecordDefinitionId,
                                                                           Long batchRecordVersionId) {
        return generateForUploadedWord(batchRecordName, parsedTables, reports, productNames,
                batchRecordDefinitionId, batchRecordVersionId, true, null, null, false, null, false, null);
    }

    @Override
    public MesProBatchRecordRouteGenerationResult generateForUploadedWord(String batchRecordName,
                                                                           List<MesProBatchRecordParsedTable> parsedTables,
                                                                           List<MesProBatchRecordReportView> reports,
                                                                           List<String> productNames,
                                                                           Long batchRecordDefinitionId,
                                                                           Long batchRecordVersionId,
                                                                           Long expectedRouteId,
                                                                           Long expectedRouteVersionId,
                                                                           Boolean routeUpgradeConfirmed) {
        return generateForUploadedWord(batchRecordName, parsedTables, reports, productNames,
                batchRecordDefinitionId, batchRecordVersionId, true,
                expectedRouteId, expectedRouteVersionId, routeUpgradeConfirmed, null, false, null);
    }

    @Override
    public MesProBatchRecordRouteGenerationResult generateForUploadedWord(String batchRecordName,
                                                                           List<MesProBatchRecordParsedTable> parsedTables,
                                                                           List<MesProBatchRecordReportView> reports,
                                                                           List<String> productNames,
                                                                           Long batchRecordDefinitionId,
                                                                           Long batchRecordVersionId,
                                                                           Long expectedRouteId,
                                                                           Long expectedRouteVersionId,
                                                                           Boolean routeUpgradeConfirmed,
                                                                           boolean applyExistingRouteRebuild) {
        return generateForUploadedWord(batchRecordName, parsedTables, reports, productNames,
                batchRecordDefinitionId, batchRecordVersionId, expectedRouteId, expectedRouteVersionId,
                routeUpgradeConfirmed, null, applyExistingRouteRebuild);
    }

    @Override
    public MesProBatchRecordRouteGenerationResult generateForUploadedWord(String batchRecordName,
                                                                           List<MesProBatchRecordParsedTable> parsedTables,
                                                                           List<MesProBatchRecordReportView> reports,
                                                                           List<String> productNames,
                                                                           Long batchRecordDefinitionId,
                                                                           Long batchRecordVersionId,
                                                                           Long expectedRouteId,
                                                                           Long expectedRouteVersionId,
                                                                           Boolean routeUpgradeConfirmed,
                                                                           Long expectedRouteCandidateVersionId,
                                                                           boolean applyExistingRouteRebuild) {
        return generateForUploadedWord(batchRecordName, parsedTables, reports, productNames,
                batchRecordDefinitionId, batchRecordVersionId, true,
                expectedRouteId, expectedRouteVersionId, routeUpgradeConfirmed,
                expectedRouteCandidateVersionId, applyExistingRouteRebuild, null);
    }

    @Override
    public MesProBatchRecordRouteGenerationResult generateForUploadedWord(String batchRecordName,
                                                                           List<MesProBatchRecordParsedTable> parsedTables,
                                                                           List<MesProBatchRecordReportView> reports,
                                                                           List<String> productNames,
                                                                           Long batchRecordDefinitionId,
                                                                           Long batchRecordVersionId,
                                                                           Long expectedRouteId,
                                                                           Long expectedRouteVersionId,
                                                                           Boolean routeUpgradeConfirmed,
                                                                           Long expectedRouteCandidateVersionId,
                                                                           boolean applyExistingRouteRebuild,
                                                                           Long dccProjectCodeId) {
        return generateForUploadedWord(batchRecordName, parsedTables, reports, productNames,
                batchRecordDefinitionId, batchRecordVersionId, true,
                expectedRouteId, expectedRouteVersionId, routeUpgradeConfirmed,
                expectedRouteCandidateVersionId, applyExistingRouteRebuild, dccProjectCodeId);
    }

    @Override
    public MesProBatchRecordRouteGenerationResult generateBatchRecordBindingCandidateForUploadedWord(
            String batchRecordName,
            List<MesProBatchRecordParsedTable> parsedTables,
            List<MesProBatchRecordReportView> reports,
            Long batchRecordDefinitionId,
            Long batchRecordVersionId,
            Long expectedRouteId,
            Long expectedRouteVersionId,
            Boolean routeUpgradeConfirmed) {
        return generateBatchRecordBindingCandidateForUploadedWord(batchRecordName, parsedTables, reports,
                batchRecordDefinitionId, batchRecordVersionId, expectedRouteId, expectedRouteVersionId,
                routeUpgradeConfirmed, null);
    }

    @Override
    public MesProBatchRecordRouteGenerationResult generateBatchRecordBindingCandidateForUploadedWord(
            String batchRecordName,
            List<MesProBatchRecordParsedTable> parsedTables,
            List<MesProBatchRecordReportView> reports,
            Long batchRecordDefinitionId,
            Long batchRecordVersionId,
            Long expectedRouteId,
            Long expectedRouteVersionId,
            Boolean routeUpgradeConfirmed,
            Long expectedRouteCandidateVersionId) {
        return generateBatchRecordBindingCandidateForUploadedWord(batchRecordName, parsedTables, reports,
                batchRecordDefinitionId, batchRecordVersionId, expectedRouteId, expectedRouteVersionId,
                routeUpgradeConfirmed, expectedRouteCandidateVersionId, null);
    }

    @Override
    public MesProBatchRecordRouteGenerationResult generateBatchRecordBindingCandidateForUploadedWord(
            String batchRecordName,
            List<MesProBatchRecordParsedTable> parsedTables,
            List<MesProBatchRecordReportView> reports,
            Long batchRecordDefinitionId,
            Long batchRecordVersionId,
            Long expectedRouteId,
            Long expectedRouteVersionId,
            Boolean routeUpgradeConfirmed,
            Long expectedRouteCandidateVersionId,
            Long dccProjectCodeId) {
        validateUploadedWordRoute(parsedTables);
        List<RouteProcessReportBinding> bindings = buildProcessReportBindings(parsedTables, reports, true);
        RouteGenerationTarget target = resolveRouteGenerationTarget(
                batchRecordName, expectedRouteId, expectedRouteVersionId, routeUpgradeConfirmed);
        if (!target.existing() || target.route() == null || target.activeVersion() == null) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "正式批记录绑定候选必须基于已存在的激活工艺路线");
        }
        MesProRouteDO route = target.route();
        validateRouteDccProjectBinding(route.getId(), dccProjectCodeId);
        MesProRouteVersionDO activeVersion = target.activeVersion();
        MesProRouteVersionDO candidate = lockAndValidateRouteCandidate(
                route, activeVersion, expectedRouteCandidateVersionId);
        route = restoreRouteIfDisabledForWordImport(route);
        String snapshotJson = candidate == null
                ? routeService.buildCurrentRouteSnapshotJson(route.getId(), activeVersion.getId())
                : candidate.getRouteSnapshotJson();
        if (StrUtil.isBlank(snapshotJson)) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "工艺路线完整快照为空：" + route.getId());
        }
        JSONObject snapshot;
        try {
            snapshot = JSON.parseObject(snapshotJson);
        } catch (RuntimeException ex) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "工艺路线完整快照无效：" + route.getId());
        }
        applyFormalBatchRecordBindings(snapshot, route.getId(), bindings,
                batchRecordDefinitionId, batchRecordVersionId);
        ensureCandidateStartConfigurationArrays(snapshot, activeVersion.getId());
        snapshot.put("routeId", route.getId());
        snapshot.put("routeCode", route.getCode());
        snapshot.put("routeName", route.getName());
        snapshot.put("status", route.getStatus());
        snapshot.put("candidateSource", "EDHR_WORD_IMPORT");
        snapshot.put("batchRecordBindingSnapshotExplicit", true);

        if (candidate == null) {
            candidate = MesProRouteVersionDO.builder()
                    .routeId(route.getId())
                    .versionNo(nextRouteVersionNo(route.getId()))
                    .active(false)
                    .lifecycleStatus(STATUS_DRAFT)
                    .sourceRouteVersionId(activeVersion.getId())
                    .changeSummaryJson(JSON.toJSONString(Map.of(
                            "source", "EDHR_WORD_IMPORT",
                            "changeType", "BATCH_RECORD_BINDING_CANDIDATE")))
                    .remark("eDHR Word导入更新逐工序批记录表单绑定，待发布后生效")
                    .build();
            MesProRouteVersionSnapshotIdentityWriter.apply(candidate, snapshot.toJSONString());
            routeVersionMapper.insert(candidate);
        } else {
            MesProRouteVersionDO update = new MesProRouteVersionDO();
            update.setId(candidate.getId());
            MesProRouteVersionSnapshotIdentityWriter.apply(update, snapshot.toJSONString());
            update.setChangeSummaryJson(JSON.toJSONString(Map.of(
                    "source", "EDHR_WORD_IMPORT",
                    "changeType", "BATCH_RECORD_BINDING_CANDIDATE")));
            update.setRemark("eDHR Word导入更新逐工序批记录表单绑定，待发布后生效");
            routeVersionMapper.updateById(update);
            candidate.setRouteSnapshotJson(update.getRouteSnapshotJson());
            candidate.setRouteSnapshotSha256(update.getRouteSnapshotSha256());
            candidate.setRouteSnapshotFormatVersion(update.getRouteSnapshotFormatVersion());
        }
        return MesProBatchRecordRouteGenerationResult.builder()
                .routeId(route.getId())
                .routeCode(route.getCode())
                .routeName(route.getName())
                .routeVersionId(candidate.getId())
                .routeVersionNo(candidate.getVersionNo())
                .routeProcessCount(bindings.size())
                .batchRecordRouteBindingCount(bindings.size())
                .boundProductNameCount(0)
                .boundProductCodeCount(0)
                .skippedProductNames(List.of())
                .build();
    }

    private void applyFormalBatchRecordBindings(JSONObject snapshot,
                                                Long routeId,
                                                List<RouteProcessReportBinding> bindings,
                                                Long batchRecordDefinitionId,
                                                Long batchRecordVersionId) {
        JSONObject configSnapshots = snapshot.getJSONObject("configSnapshots");
        JSONObject flowGraph = configSnapshots == null ? null : configSnapshots.getJSONObject("flowGraph");
        JSONArray nodes = flowGraph == null ? null : flowGraph.getJSONArray("nodes");
        JSONArray batchUseConfigs = configSnapshots == null ? null : configSnapshots.getJSONArray("batchUseConfigs");
        if (nodes == null || batchUseConfigs == null) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "工艺路线快照缺少 flowGraph.nodes 或 batchUseConfigs：" + routeId);
        }
        List<JSONObject> sortedNodes = sortCandidateObjects(nodes, "flowGraph.nodes");
        if (sortedNodes.size() != bindings.size()) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "Word 工序数量与当前路线不一致：" + bindings.size() + "/" + sortedNodes.size());
        }
        Map<Long, JSONObject> configsByRouteProcessId = new LinkedHashMap<>();
        for (JSONObject config : sortCandidateObjects(batchUseConfigs, "batchUseConfigs")) {
            Long routeProcessId = config.getLong("routeProcessId");
            if (routeProcessId == null || configsByRouteProcessId.putIfAbsent(routeProcessId, config) != null) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                        "batchUseConfigs 缺少或重复 routeProcessId：" + routeProcessId);
            }
        }
        for (int index = 0; index < sortedNodes.size(); index++) {
            JSONObject node = sortedNodes.get(index);
            RouteProcessReportBinding binding = bindings.get(index);
            Long routeProcessId = node.getLong("routeProcessId");
            Long processId = node.getLong("processId");
            Integer sort = node.getInteger("sort");
            if (routeProcessId == null || processId == null || sort == null) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                        "flowGraph.nodes 缺少 routeProcessId/processId/sort：" + routeId);
            }
            if (!Objects.equals(StrUtil.trim(node.getString("processName")), binding.processName())) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                        "Word 工序与当前路线工序不一致：" + binding.processName());
            }
            JSONObject config = configsByRouteProcessId.get(routeProcessId);
            if (config == null) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                        "当前路线工序缺少批记录用途配置：" + routeProcessId);
            }
            requireExplicitFormBindings(config, routeId, routeProcessId);
            config.put("routeProcessId", routeProcessId);
            config.put("processId", processId);
            config.put("sort", sort);
            config.put("processName", binding.processName());
            config.put("batchRecordBindingSnapshotExplicit", true);
            config.put("batchRecordReports", new JSONArray(List.of(
                    buildFormalBatchRecordReportSnapshot(routeId, routeProcessId, binding.report(),
                            batchRecordDefinitionId, batchRecordVersionId))));
        }
    }

    private void requireExplicitFormBindings(JSONObject config, Long routeId, Long routeProcessId) {
        Object value = config.get(FORM_BINDINGS_KEY);
        if (!(value instanceof JSONArray formBindings)) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "工艺路线快照缺少独立 formBindings 来源：routeId=" + routeId
                            + ", routeProcessId=" + routeProcessId);
        }
        for (Object formBinding : formBindings) {
            if (!(formBinding instanceof JSONObject)) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                        "工艺路线 formBindings 必须只包含对象：routeId=" + routeId
                                + ", routeProcessId=" + routeProcessId);
            }
        }
    }

    private void ensureCandidateStartConfigurationArrays(JSONObject snapshot, Long sourceRouteVersionId) {
        JSONObject configSnapshots = snapshot.getJSONObject(SNAPSHOT_CONFIGS_KEY);
        if (configSnapshots == null) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "工艺路线快照缺少 configSnapshots：routeVersionId=" + sourceRouteVersionId);
        }
        ensureCandidateStartConfigurationArray(configSnapshots,
                ROUTE_START_PRODUCTION_LEADERS_KEY, sourceRouteVersionId);
        ensureCandidateStartConfigurationArray(configSnapshots,
                BATCH_RECORD_ATTACHMENT_OWNERS_KEY, sourceRouteVersionId);
    }

    private void ensureCandidateStartConfigurationArray(JSONObject configSnapshots,
                                                        String configKey,
                                                        Long sourceRouteVersionId) {
        if (!configSnapshots.containsKey(configKey)) {
            configSnapshots.put(configKey, new JSONArray());
            return;
        }
        Object value = configSnapshots.get(configKey);
        if (!(value instanceof JSONArray array)) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "工艺路线开始配置快照无效：configKey=" + configKey
                            + ", routeVersionId=" + sourceRouteVersionId);
        }
        for (Object item : array) {
            if (!(item instanceof JSONObject)) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                        "工艺路线开始配置必须只包含对象：configKey=" + configKey
                                + ", routeVersionId=" + sourceRouteVersionId);
            }
        }
    }

    private List<JSONObject> sortCandidateObjects(JSONArray values, String fieldName) {
        List<JSONObject> result = new ArrayList<>();
        for (Object value : values) {
            if (!(value instanceof JSONObject object)) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                        fieldName + " 必须只包含对象");
            }
            result.add(object);
        }
        result.sort(Comparator.comparing(object -> object.getInteger("sort"),
                Comparator.nullsLast(Integer::compareTo)));
        return result;
    }

    private JSONObject buildFormalBatchRecordReportSnapshot(Long routeId,
                                                            Long routeProcessId,
                                                            MesProBatchRecordReportView report,
                                                            Long batchRecordDefinitionId,
                                                            Long batchRecordVersionId) {
        if (report == null || StrUtil.isBlank(report.reportId())
                || !Objects.equals(batchRecordDefinitionId, report.batchRecordDefinitionId())
                || !Objects.equals(batchRecordVersionId, report.batchRecordVersionId())
                || !Objects.equals(MesProBatchRecordFormSlotType.MAIN.getType(), report.formSlotType())) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "正式批记录报表版本或槽位归属不一致：" + routeProcessId);
        }
        String snapshotHash = buildSnapshotHash(routeId, routeProcessId, report.reportId());
        JSONObject result = new JSONObject(true);
        result.put("batchRecordReportId", report.reportId());
        result.put("reportId", report.reportId());
        result.put("reportCode", report.reportCode());
        result.put("reportName", report.reportName());
        result.put("batchRecordDefinitionId", batchRecordDefinitionId);
        result.put("batchRecordVersionId", batchRecordVersionId);
        result.put("formSlotType", MesProBatchRecordFormSlotType.MAIN.getType());
        result.put("recordCategory", RECORD_CATEGORY_BATCH_RECORD);
        result.put("validationProfile", VALIDATION_PROFILE_CONTROLLED_BATCH);
        result.put("permissionScopeId", routeProcessId);
        result.put("recordCategorySnapshotHash", snapshotHash);
        result.put("requiredPolicy", REQUIRED_POLICY_REQUIRED);
        result.put("ownerRoleKey", OWNER_ROLE_PRODUCTION);
        result.put("archiveVisibility", ARCHIVE_VISIBILITY_FINAL_DHR);
        result.put("slotConfigSnapshotHash", snapshotHash);
        result.put("reportSort", 1);
        result.put("sourceTableIndex", report.sourceTableIndex());
        result.put("tableTitle", report.tableTitle());
        result.put("remark", "eDHR Word导入更新逐工序正式批记录表单绑定");
        return result;
    }

    private MesProBatchRecordRouteGenerationResult generateForUploadedWord(String batchRecordName,
                                                                           List<MesProBatchRecordParsedTable> parsedTables,
                                                                           List<MesProBatchRecordReportView> reports,
                                                                           List<String> productNames,
                                                                           Long batchRecordDefinitionId,
                                                                           Long batchRecordVersionId,
                                                                           boolean bindBatchRecordReports,
                                                                           Long expectedRouteId,
                                                                           Long expectedRouteVersionId,
                                                                           Boolean routeUpgradeConfirmed,
                                                                           Long expectedRouteCandidateVersionId,
                                                                           boolean applyExistingRouteRebuild,
                                                                           Long dccProjectCodeId) {
        validateUploadedWordRoute(parsedTables);
        List<String> normalizedProductNames = normalizeProductNames(productNames);
        List<RouteProcessReportBinding> bindings = buildProcessReportBindings(parsedTables, reports,
                bindBatchRecordReports);
        RouteGenerationTarget target = resolveRouteGenerationTarget(batchRecordName,
                expectedRouteId, expectedRouteVersionId, routeUpgradeConfirmed);
        MesProRouteDO route = target.route();
        MesProRouteVersionDO sourceRouteVersion = target.activeVersion();
        if (route == null) {
            String routeCode = generateRouteCode();
            route = MesProRouteDO.builder()
                    .code(routeCode)
                    .name(batchRecordName)
                    .status(CommonStatusEnum.ENABLE.getStatus())
                    .remark("eDHR Word导入自动生成")
                    .build();
            routeMapper.insert(route);
            bindRouteToDccProject(route.getId(), dccProjectCodeId);
            routeOwnerPermissionService.bindCurrentUserAsOwner(route.getId());
        } else if (target.existing()) {
            validateRouteDccProjectBinding(route.getId(), dccProjectCodeId);
            route = restoreRouteIfDisabledForWordImport(route);
            MesProRouteVersionDO candidateRouteVersion = createOrUpdateCandidateRouteVersion(
                    route, sourceRouteVersion, bindings, normalizedProductNames, bindBatchRecordReports,
                    expectedRouteCandidateVersionId);
            RouteProductBindingResult productBindingResult = inspectExistingRouteProductBindings(
                    route.getId(), normalizedProductNames, dccProjectCodeId);
            return MesProBatchRecordRouteGenerationResult.builder()
                    .routeId(route.getId())
                    .routeCode(route.getCode())
                    .routeName(route.getName())
                    .routeVersionId(candidateRouteVersion.getId())
                    .routeVersionNo(candidateRouteVersion.getVersionNo())
                    .routeProcessCount(bindings.size())
                    .batchRecordRouteBindingCount(bindBatchRecordReports ? bindings.size() : 0)
                    .boundProductNameCount(productBindingResult.boundProductNameCount())
                    .boundProductCodeCount(productBindingResult.boundProductCodeCount())
                    .skippedProductNames(productBindingResult.skippedProductNames())
                    .build();
        }
        RouteUpgradePreservedData preservedData = target.preservedData();
        MesProRouteVersionDO routeVersion = createInitialActiveRouteVersionForUploadedWord(route, sourceRouteVersion);

        List<MesProProcessDO> processes = new ArrayList<>(bindings.size());
        for (RouteProcessReportBinding binding : bindings) {
            processes.add(resolveProcess(binding.processName()));
        }

        MesProRouteFlowConfigDO flowConfig = MesProRouteFlowConfigDO.builder()
                .routeId(route.getId())
                .useType(USE_TYPE_BATCH)
                    .configVersion(route.getCode() + "-BATCH-" + routeVersion.getVersionNo())
                    .remark(target.existing() ? "eDHR Word导入升版刷新工艺流程批记录配置"
                            : "eDHR Word导入自动生成工艺流程批记录配置")
                    .build();
        routeFlowConfigMapper.insert(flowConfig);

        int bindingCount = 0;
        List<MesProRouteProcessDO> generatedRouteProcesses = new ArrayList<>(bindings.size());
        Map<Long, Long> batchRecordPermissionScopeIds = new LinkedHashMap<>();
        Map<Long, Long> preservedRouteProcessIdMap = new LinkedHashMap<>();
        Map<Long, Integer> preservedProcessIndexes = new HashMap<>();
        for (int index = 0; index < bindings.size(); index++) {
            RouteProcessReportBinding binding = bindings.get(index);
            MesProProcessDO process = processes.get(index);
            PreservedRouteProcess preservedRouteProcess =
                    consumePreservedProcess(preservedData, preservedProcessIndexes, process.getId());
            MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                    .routeId(route.getId())
                    .processId(process.getId())
                    .sort(index + 1)
                    .prepareTime(preservedRouteProcess == null ? null : preservedRouteProcess.prepareTime())
                    .waitTime(preservedRouteProcess == null ? null : preservedRouteProcess.waitTime())
                    .colorCode(preservedRouteProcess == null ? null : preservedRouteProcess.colorCode())
                    .keyFlag(preservedRouteProcess != null && Boolean.TRUE.equals(preservedRouteProcess.keyFlag()))
                    .checkFlag(preservedRouteProcess != null && Boolean.TRUE.equals(preservedRouteProcess.checkFlag()))
                    .remark(preservedRouteProcess == null ? "eDHR Word导入自动生成"
                            : StrUtil.blankToDefault(preservedRouteProcess.remark(), "eDHR Word导入自动生成"))
                    .build();
            routeProcessMapper.insert(routeProcess);
            generatedRouteProcesses.add(routeProcess);
            if (preservedRouteProcess != null) {
                preservedRouteProcessIdMap.put(preservedRouteProcess.id(), routeProcess.getId());
            }

            MesProRouteFlowProcessConfigDO processConfig = MesProRouteFlowProcessConfigDO.builder()
                    .routeFlowConfigId(flowConfig.getId())
                    .routeId(route.getId())
                    .routeProcessId(routeProcess.getId())
                    .useType(USE_TYPE_BATCH)
                    .enabled(true)
                    .executionMode(EXECUTION_MODE_SEQUENTIAL)
                    .remark(bindBatchRecordReports ? "eDHR Word导入自动绑定批记录报表"
                            : "eDHR Word导入自动生成工艺流程配置")
                    .build();
            routeFlowProcessConfigMapper.insert(processConfig);

            if (binding.report() != null && StrUtil.isNotBlank(binding.report().reportId())) {
                Long permissionScopeId = bindBatchRecordPermissionScope(routeProcess.getId(), binding.report().reportId());
                batchRecordPermissionScopeIds.put(routeProcess.getId(), permissionScopeId);
                routeFlowProcessBatchRecordMapper.insert(MesProRouteFlowProcessBatchRecordDO.builder()
                        .routeFlowProcessConfigId(processConfig.getId())
                        .routeId(route.getId())
                        .routeProcessId(routeProcess.getId())
                        .useType(USE_TYPE_BATCH)
                        .batchRecordReportId(binding.report().reportId())
                        .batchRecordDefinitionId(batchRecordDefinitionId)
                        .batchRecordVersionId(batchRecordVersionId)
                        .formSlotType(MesProBatchRecordFormSlotType.MAIN.getType())
                        .recordCategory(RECORD_CATEGORY_BATCH_RECORD)
                        .validationProfile(VALIDATION_PROFILE_CONTROLLED_BATCH)
                        .permissionScopeId(permissionScopeId)
                        .recordCategorySnapshotHash(buildSnapshotHash(route.getId(), routeProcess.getId(),
                                binding.report().reportId()))
                        .requiredPolicy(REQUIRED_POLICY_REQUIRED)
                        .ownerRoleKey(OWNER_ROLE_PRODUCTION)
                        .archiveVisibility(ARCHIVE_VISIBILITY_FINAL_DHR)
                        .slotConfigSnapshotHash(buildSnapshotHash(route.getId(), routeProcess.getId(),
                                binding.report().reportId()))
                        .reportSort(1)
                        .remark("eDHR Word导入自动绑定批记录报表")
                        .build());
                bindingCount++;
            }
        }
        insertRouteProcessFlowEdges(route.getId(), generatedRouteProcesses, preservedData, preservedRouteProcessIdMap);
        RouteProductBindingResult productBindingResult = bindRouteProducts(
                route.getId(), normalizedProductNames, dccProjectCodeId);
        updateRouteVersionSnapshot(routeVersion, buildGeneratedRouteSnapshot(
                route, generatedRouteProcesses, bindings, normalizedProductNames, bindBatchRecordReports,
                batchRecordPermissionScopeIds));

        return MesProBatchRecordRouteGenerationResult.builder()
                .routeId(route.getId())
                .routeCode(route.getCode())
                .routeName(route.getName())
                .routeVersionId(routeVersion.getId())
                .routeVersionNo(routeVersion.getVersionNo())
                .routeProcessCount(bindings.size())
                .batchRecordRouteBindingCount(bindingCount)
                .boundProductNameCount(productBindingResult.boundProductNameCount())
                .boundProductCodeCount(productBindingResult.boundProductCodeCount())
                .skippedProductNames(productBindingResult.skippedProductNames())
                .build();
    }

    private RouteGenerationTarget resolveRouteGenerationTarget(String routeName, Long expectedRouteId,
                                                               Long expectedRouteVersionId,
                                                               Boolean routeUpgradeConfirmed) {
        if (expectedRouteId != null) {
            MesProRouteDO expectedRoute = routeMapper.selectById(expectedRouteId);
            MesProRouteVersionDO activeVersion = expectedRoute == null
                    ? null : routeVersionMapper.selectActiveByRouteId(expectedRouteId);
            Long currentVersionId = activeVersion == null ? null : activeVersion.getId();
            if (expectedRoute == null || !Objects.equals(expectedRouteVersionId, currentVersionId)) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_UPGRADE_TARGET_CHANGED,
                        expectedRouteId, expectedRouteVersionId,
                        expectedRoute == null ? null : expectedRoute.getId(), currentVersionId);
            }
            if (!Boolean.TRUE.equals(routeUpgradeConfirmed)) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_UPGRADE_CONFIRM_REQUIRED,
                        routeName);
            }
            return new RouteGenerationTarget(expectedRoute, activeVersion, true);
        }
        if (expectedRouteVersionId != null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_UPGRADE_TARGET_CHANGED,
                    null, expectedRouteVersionId, null, null);
        }
        return new RouteGenerationTarget(null, null, false);
    }

    private MesProRouteDO restoreRouteIfDisabledForWordImport(MesProRouteDO route) {
        if (route == null) {
            return null;
        }
        MesProRouteDO lockedRoute = routeMapper.selectByIdForUpdate(route.getId());
        if (lockedRoute == null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "工艺路线不存在：" + route.getId());
        }
        if (Objects.equals(CommonStatusEnum.ENABLE.getStatus(), lockedRoute.getStatus())) {
            return lockedRoute;
        }
        restoreMissingBoundaryEdgesForWordImport(lockedRoute.getId());
        routeService.updateRouteStatus(lockedRoute.getId(), CommonStatusEnum.ENABLE.getStatus());
        MesProRouteDO restoredRoute = routeMapper.selectByIdForUpdate(lockedRoute.getId());
        if (restoredRoute == null
                || !Objects.equals(CommonStatusEnum.ENABLE.getStatus(), restoredRoute.getStatus())) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "工艺路线恢复启用失败：" + lockedRoute.getId());
        }
        return restoredRoute;
    }

    private void restoreMissingBoundaryEdgesForWordImport(Long routeId) {
        List<MesProRouteProcessFlowBoundaryEdgeDO> currentBoundaryEdges =
                routeProcessFlowBoundaryEdgeMapper.selectListByRouteId(routeId);
        boolean startPresent = currentBoundaryEdges.stream()
                .anyMatch(edge -> "START".equals(edge.getBoundaryType()));
        boolean endPresent = currentBoundaryEdges.stream()
                .anyMatch(edge -> "END".equals(edge.getBoundaryType()));
        if (startPresent && endPresent) {
            return;
        }
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(routeId);
        List<MesProRouteProcessFlowEdgeDO> edges = routeProcessFlowEdgeMapper.selectListByRouteId(routeId);
        if (CollUtil.isEmpty(routeProcesses) || CollUtil.isEmpty(edges)) {
            return;
        }
        Set<Long> routeProcessIds = routeProcesses.stream()
                .map(MesProRouteProcessDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (routeProcessIds.size() != routeProcesses.size()
                || edges.stream().anyMatch(edge -> edge.getSourceRouteProcessId() == null
                || edge.getTargetRouteProcessId() == null
                || Objects.equals(edge.getSourceRouteProcessId(), edge.getTargetRouteProcessId())
                || !routeProcessIds.contains(edge.getSourceRouteProcessId())
                || !routeProcessIds.contains(edge.getTargetRouteProcessId()))) {
            return;
        }
        Set<Long> incomingRouteProcessIds = edges.stream()
                .map(MesProRouteProcessFlowEdgeDO::getTargetRouteProcessId)
                .collect(Collectors.toSet());
        Set<Long> outgoingRouteProcessIds = edges.stream()
                .map(MesProRouteProcessFlowEdgeDO::getSourceRouteProcessId)
                .collect(Collectors.toSet());
        List<Long> startRouteProcessIds = routeProcessIds.stream()
                .filter(routeProcessId -> !incomingRouteProcessIds.contains(routeProcessId))
                .toList();
        List<Long> endRouteProcessIds = routeProcessIds.stream()
                .filter(routeProcessId -> !outgoingRouteProcessIds.contains(routeProcessId))
                .toList();
        if (startRouteProcessIds.size() != 1 || endRouteProcessIds.size() != 1) {
            return;
        }
        long graphVersion = edges.stream()
                .map(MesProRouteProcessFlowEdgeDO::getGraphVersion)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(1L);
        if (!startPresent) {
            routeProcessFlowBoundaryEdgeMapper.insert(MesProRouteProcessFlowBoundaryEdgeDO.builder()
                    .routeId(routeId)
                    .graphVersion(graphVersion)
                    .boundaryType("START")
                    .routeProcessId(startRouteProcessIds.get(0))
                    .sort(1)
                    .build());
        }
        if (!endPresent) {
            routeProcessFlowBoundaryEdgeMapper.insert(MesProRouteProcessFlowBoundaryEdgeDO.builder()
                    .routeId(routeId)
                    .graphVersion(graphVersion)
                    .boundaryType("END")
                    .routeProcessId(endRouteProcessIds.get(0))
                    .sort(1)
                    .build());
        }
    }

    private RouteUpgradePreservedData loadPreservedData(Long routeId) {
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(routeId);
        List<MesProRouteProcessFlowEdgeDO> edges = routeProcessFlowEdgeMapper.selectListByRouteId(routeId);
        if (CollUtil.isEmpty(routeProcesses) && CollUtil.isEmpty(edges)) {
            return RouteUpgradePreservedData.empty();
        }
        Map<Long, List<PreservedRouteProcess>> processesByProcessId = routeProcesses.stream()
                .filter(routeProcess -> routeProcess.getProcessId() != null)
                .map(routeProcess -> new PreservedRouteProcess(
                        routeProcess.getId(),
                        routeProcess.getProcessId(),
                        routeProcess.getPrepareTime(),
                        routeProcess.getWaitTime(),
                        routeProcess.getColorCode(),
                        routeProcess.getKeyFlag(),
                        routeProcess.getCheckFlag(),
                        routeProcess.getRemark()))
                .collect(Collectors.groupingBy(PreservedRouteProcess::processId, LinkedHashMap::new,
                        Collectors.toList()));
        return new RouteUpgradePreservedData(processesByProcessId, edges);
    }

    private PreservedRouteProcess consumePreservedProcess(RouteUpgradePreservedData preservedData,
                                                          Map<Long, Integer> preservedProcessIndexes,
                                                          Long processId) {
        List<PreservedRouteProcess> preservedProcesses = preservedData.processesByProcessId().get(processId);
        if (CollUtil.isEmpty(preservedProcesses)) {
            return null;
        }
        int index = preservedProcessIndexes.getOrDefault(processId, 0);
        if (index >= preservedProcesses.size()) {
            return null;
        }
        preservedProcessIndexes.put(processId, index + 1);
        return preservedProcesses.get(index);
    }

    private void insertRouteProcessFlowEdges(Long routeId, List<MesProRouteProcessDO> routeProcesses,
                                             RouteUpgradePreservedData preservedData,
                                             Map<Long, Long> preservedRouteProcessIdMap) {
        Set<String> insertedPairs = new LinkedHashSet<>();
        for (int index = 1; index < routeProcesses.size(); index++) {
            MesProRouteProcessDO source = routeProcesses.get(index - 1);
            MesProRouteProcessDO target = routeProcesses.get(index);
            MesProRouteProcessFlowEdgeDO preservedEdge = findPreservedEdge(
                    preservedData.edges(), preservedRouteProcessIdMap, source.getId(), target.getId());
            if (preservedEdge != null) {
                insertMappedEdge(routeId, preservedEdge, preservedRouteProcessIdMap, insertedPairs);
                continue;
            }
            String pairKey = edgePairKey(source.getId(), target.getId());
            insertedPairs.add(pairKey);
            routeProcessFlowEdgeMapper.insert(MesProRouteProcessFlowEdgeDO.builder()
                    .routeId(routeId)
                    .graphVersion(1L)
                    .sourceRouteProcessId(source.getId())
                    .targetRouteProcessId(target.getId())
                    .relationType("NORMAL")
                    .sort(index)
                    .build());
        }
        for (MesProRouteProcessFlowEdgeDO preservedEdge : preservedData.edges()) {
            insertMappedEdge(routeId, preservedEdge, preservedRouteProcessIdMap, insertedPairs);
        }
        if (!routeProcesses.isEmpty()) {
            routeProcessFlowBoundaryEdgeMapper.insert(MesProRouteProcessFlowBoundaryEdgeDO.builder()
                    .routeId(routeId)
                    .graphVersion(1L)
                    .boundaryType("START")
                    .routeProcessId(routeProcesses.get(0).getId())
                    .sort(1)
                    .build());
            routeProcessFlowBoundaryEdgeMapper.insert(MesProRouteProcessFlowBoundaryEdgeDO.builder()
                    .routeId(routeId)
                    .graphVersion(1L)
                    .boundaryType("END")
                    .routeProcessId(routeProcesses.get(routeProcesses.size() - 1).getId())
                    .sort(1)
                    .build());
        }
    }

    private MesProRouteProcessFlowEdgeDO findPreservedEdge(List<MesProRouteProcessFlowEdgeDO> preservedEdges,
                                                            Map<Long, Long> preservedRouteProcessIdMap,
                                                            Long sourceRouteProcessId,
                                                            Long targetRouteProcessId) {
        for (MesProRouteProcessFlowEdgeDO preservedEdge : preservedEdges) {
            Long mappedSource = preservedRouteProcessIdMap.get(preservedEdge.getSourceRouteProcessId());
            Long mappedTarget = preservedRouteProcessIdMap.get(preservedEdge.getTargetRouteProcessId());
            if (Objects.equals(mappedSource, sourceRouteProcessId) && Objects.equals(mappedTarget, targetRouteProcessId)) {
                return preservedEdge;
            }
        }
        return null;
    }

    private void insertMappedEdge(Long routeId, MesProRouteProcessFlowEdgeDO preservedEdge,
                                  Map<Long, Long> preservedRouteProcessIdMap, Set<String> insertedPairs) {
        Long mappedSource = preservedRouteProcessIdMap.get(preservedEdge.getSourceRouteProcessId());
        Long mappedTarget = preservedRouteProcessIdMap.get(preservedEdge.getTargetRouteProcessId());
        if (mappedSource == null || mappedTarget == null) {
            return;
        }
        String pairKey = edgePairKey(mappedSource, mappedTarget);
        if (!insertedPairs.add(pairKey)) {
            return;
        }
        routeProcessFlowEdgeMapper.insert(MesProRouteProcessFlowEdgeDO.builder()
                .routeId(routeId)
                .graphVersion(preservedEdge.getGraphVersion())
                .sourceRouteProcessId(mappedSource)
                .targetRouteProcessId(mappedTarget)
                .relationType(preservedEdge.getRelationType())
                .sort(preservedEdge.getSort())
                .build());
    }

    private String edgePairKey(Long sourceRouteProcessId, Long targetRouteProcessId) {
        return sourceRouteProcessId + "->" + targetRouteProcessId;
    }

    private MesProRouteVersionDO createInitialActiveRouteVersionForUploadedWord(
            MesProRouteDO route, MesProRouteVersionDO sourceVersion) {
        if (sourceVersion != null && Boolean.TRUE.equals(sourceVersion.getActive())) {
            routeVersionMapper.deactivateById(sourceVersion.getId());
            sourceVersion.setActive(false);
        }
        JSONObject snapshot = new JSONObject(true);
        snapshot.put("routeId", route.getId());
        snapshot.put("routeCode", route.getCode());
        snapshot.put("routeName", route.getName());
        snapshot.put("status", route.getStatus());
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder()
                .routeId(route.getId())
                .versionNo(nextRouteVersionNo(route.getId()))
                .active(true)
                .lifecycleStatus(STATUS_ACTIVE)
                .sourceRouteVersionId(sourceVersion == null ? null : sourceVersion.getId())
                .remark(sourceVersion == null ? "eDHR Word导入自动生成路线版本" : "eDHR Word导入升版路线版本")
                .build();
        MesProRouteVersionSnapshotIdentityWriter.apply(routeVersion, snapshot.toJSONString());
        routeVersionMapper.insert(routeVersion);
        return routeVersion;
    }

    private MesProRouteVersionDO lockAndValidateRouteCandidate(MesProRouteDO route,
                                                               MesProRouteVersionDO sourceVersion,
                                                               Long expectedRouteCandidateVersionId) {
        MesProRouteVersionDO lockedActiveVersion = routeVersionMapper.selectActiveByRouteIdForUpdate(route.getId());
        Long expectedActiveVersionId = sourceVersion == null ? null : sourceVersion.getId();
        Long currentActiveVersionId = lockedActiveVersion == null ? null : lockedActiveVersion.getId();
        if (!Objects.equals(expectedActiveVersionId, currentActiveVersionId)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants
                            .PRO_BATCH_RECORD_REPORT_ROUTE_UPGRADE_TARGET_CHANGED,
                    route.getId(), expectedActiveVersionId, route.getId(), currentActiveVersionId);
        }
        MesProRouteVersionDO candidateVersion = routeVersionMapper.selectOpenCandidateByRouteId(route.getId());
        if (candidateVersion != null
                && (Objects.equals(MesProRouteVersionMapper.STATUS_PENDING_APPROVAL,
                        candidateVersion.getLifecycleStatus())
                || Objects.equals(MesProRouteVersionMapper.STATUS_READY_TO_PUBLISH,
                        candidateVersion.getLifecycleStatus()))) {
            throw exception(MesProBatchRecordReportErrorCodeConstants
                            .PRO_BATCH_RECORD_REPORT_ROUTE_CANDIDATE_STATUS_BLOCKED,
                    candidateVersion.getVersionNo(), candidateVersion.getLifecycleStatus());
        }
        Long currentCandidateVersionId = candidateVersion == null ? null : candidateVersion.getId();
        if (!Objects.equals(expectedRouteCandidateVersionId, currentCandidateVersionId)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants
                            .PRO_BATCH_RECORD_REPORT_ROUTE_CANDIDATE_TARGET_CHANGED,
                    expectedRouteCandidateVersionId, currentCandidateVersionId);
        }
        if (candidateVersion != null && !Objects.equals(STATUS_DRAFT, candidateVersion.getLifecycleStatus())) {
            throw exception(MesProBatchRecordReportErrorCodeConstants
                            .PRO_BATCH_RECORD_REPORT_ROUTE_CANDIDATE_STATUS_BLOCKED,
                    candidateVersion.getVersionNo(), candidateVersion.getLifecycleStatus());
        }
        if (candidateVersion != null
                && !Objects.equals(candidateVersion.getSourceRouteVersionId(), currentActiveVersionId)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants
                            .PRO_BATCH_RECORD_REPORT_ROUTE_CANDIDATE_SOURCE_CHANGED,
                    candidateVersion.getVersionNo(), candidateVersion.getSourceRouteVersionId(),
                    currentActiveVersionId);
        }
        return candidateVersion;
    }

    private MesProRouteVersionDO createOrUpdateCandidateRouteVersion(MesProRouteDO route,
                                                                     MesProRouteVersionDO sourceVersion,
                                                                     List<RouteProcessReportBinding> bindings,
                                                                     List<String> productNames,
                                                                     boolean bindBatchRecordReports,
                                                                     Long expectedRouteCandidateVersionId) {
        MesProRouteVersionDO candidateVersion = lockAndValidateRouteCandidate(
                route, sourceVersion, expectedRouteCandidateVersionId);
        JSONObject snapshot = new JSONObject(true);
        snapshot.put("routeId", route.getId());
        snapshot.put("routeCode", route.getCode());
        snapshot.put("routeName", route.getName());
        snapshot.put("status", route.getStatus());
        snapshot.put("candidateSource", "EDHR_WORD_IMPORT");
        snapshot.put("productNames", productNames);
        ActiveRouteCandidateSource activeSource = loadActiveRouteCandidateSource(route, sourceVersion);
        Map<Long, Integer> candidateOccurrenceByProcessId = new HashMap<>();
        Set<Long> mappedActiveRouteProcessIds = new LinkedHashSet<>();
        List<Map<String, Object>> processSnapshots = new ArrayList<>();
        List<Map<String, Object>> batchUseConfigSnapshots = new ArrayList<>();
        for (int index = 0; index < bindings.size(); index++) {
            RouteProcessReportBinding binding = bindings.get(index);
            MesProProcessDO processDefinition = resolveProcess(binding.processName());
            int occurrence = candidateOccurrenceByProcessId.merge(processDefinition.getId(), 1, Integer::sum);
            ActiveRouteProcessSnapshot activeRouteProcess = activeSource.find(processDefinition.getId(), occurrence);
            Map<String, Object> process = activeRouteProcess == null
                    ? new LinkedHashMap<>()
                    : new LinkedHashMap<>(activeRouteProcess.node());
            process.put("sort", index + 1);
            process.put("processId", processDefinition.getId());
            process.put("processName", binding.processName());
            if (activeRouteProcess == null) {
                process.put("clientRouteProcessId", -(long) (index + 1));
                process.put("keyFlag", false);
                process.put("checkFlag", false);
            } else {
                process.put("routeProcessId", activeRouteProcess.routeProcessId());
                process.remove("clientRouteProcessId");
                mappedActiveRouteProcessIds.add(activeRouteProcess.routeProcessId());
            }
            JSONObject activeBatchUseConfig = activeRouteProcess == null ? null
                    : activeSource.batchUseConfigByRouteProcessId().get(activeRouteProcess.routeProcessId());
            if (activeBatchUseConfig != null) {
                batchUseConfigSnapshots.add(remapActiveBatchUseConfig(activeBatchUseConfig,
                        processDefinition.getId(), binding.processName(), index + 1,
                        activeRouteProcess.routeProcessId()));
            } else if (bindBatchRecordReports && binding.report() != null) {
                batchUseConfigSnapshots.add(buildImportedBatchUseConfigSnapshot(
                        route.getId(), process, processDefinition.getId(), binding, index + 1));
            }
            processSnapshots.add(process);
        }
        requireAllConfiguredActiveProcessesMapped(activeSource, mappedActiveRouteProcessIds);
        snapshot.put("processes", processSnapshots);
        JSONObject configSnapshots = new JSONObject(true);
        configSnapshots.put("flowGraph", buildCandidateFlowGraphSnapshot(processSnapshots));
        configSnapshots.put("products", buildRouteProductSnapshots(route.getId()));
        configSnapshots.put("scheduleConfigs", List.of());
        configSnapshots.put("scheduleUseConfigs", List.of());
        configSnapshots.put("batchUseConfigs", batchUseConfigSnapshots);
        copyStartConfiguration(activeSource.configSnapshots(), configSnapshots,
                ROUTE_START_PRODUCTION_LEADERS_KEY, sourceVersion.getId());
        copyStartConfiguration(activeSource.configSnapshots(), configSnapshots,
                BATCH_RECORD_ATTACHMENT_OWNERS_KEY, sourceVersion.getId());
        snapshot.put("configSnapshots", configSnapshots);

        String snapshotJson = snapshot.toJSONString();
        String changeSummaryJson =
                "{\"source\":\"EDHR_WORD_IMPORT\",\"changeType\":\"ROUTE_REBUILD_CANDIDATE\"}";
        String remark = "eDHR Word导入更新路线候选版本，待发布后生效";
        if (candidateVersion == null) {
            candidateVersion = MesProRouteVersionDO.builder()
                    .routeId(route.getId())
                    .versionNo(nextRouteVersionNo(route.getId()))
                    .active(false)
                    .lifecycleStatus(STATUS_DRAFT)
                    .sourceRouteVersionId(sourceVersion == null ? null : sourceVersion.getId())
                    .changeSummaryJson(changeSummaryJson)
                    .remark(remark)
                    .build();
            MesProRouteVersionSnapshotIdentityWriter.apply(candidateVersion, snapshotJson);
            routeVersionMapper.insert(candidateVersion);
            return candidateVersion;
        }
        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(candidateVersion.getId());
        MesProRouteVersionSnapshotIdentityWriter.apply(update, snapshotJson);
        update.setChangeSummaryJson(changeSummaryJson);
        update.setRemark(remark);
        routeVersionMapper.updateById(update);
        candidateVersion.setRouteSnapshotJson(snapshotJson);
        candidateVersion.setRouteSnapshotSha256(update.getRouteSnapshotSha256());
        candidateVersion.setRouteSnapshotFormatVersion(update.getRouteSnapshotFormatVersion());
        candidateVersion.setChangeSummaryJson(changeSummaryJson);
        candidateVersion.setRemark(remark);
        return candidateVersion;
    }

    private ActiveRouteCandidateSource loadActiveRouteCandidateSource(MesProRouteDO route,
                                                                       MesProRouteVersionDO sourceVersion) {
        if (sourceVersion == null || sourceVersion.getId() == null) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "升版候选缺少 ACTIVE 来源版本：routeId=" + route.getId());
        }
        String snapshotJson = routeService.buildCurrentRouteSnapshotJson(route.getId(), sourceVersion.getId());
        if (StrUtil.isBlank(snapshotJson)) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "ACTIVE 工艺路线完整快照为空：routeId=" + route.getId()
                            + ", routeVersionId=" + sourceVersion.getId());
        }
        JSONObject activeSnapshot;
        try {
            activeSnapshot = JSON.parseObject(snapshotJson);
        } catch (RuntimeException ex) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "ACTIVE 工艺路线完整快照无效：routeId=" + route.getId()
                            + ", routeVersionId=" + sourceVersion.getId());
        }
        JSONObject configSnapshots = activeSnapshot == null
                ? null : activeSnapshot.getJSONObject(SNAPSHOT_CONFIGS_KEY);
        JSONObject flowGraph = configSnapshots == null ? null : configSnapshots.getJSONObject(FLOW_GRAPH_KEY);
        JSONArray nodes = flowGraph == null ? null : flowGraph.getJSONArray("nodes");
        JSONArray batchUseConfigs = configSnapshots == null
                ? null : configSnapshots.getJSONArray(BATCH_USE_CONFIGS_KEY);
        if (configSnapshots == null || flowGraph == null || nodes == null || batchUseConfigs == null) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "ACTIVE 工艺路线快照缺少 configSnapshots.flowGraph.nodes 或 batchUseConfigs：routeId="
                            + route.getId() + ", routeVersionId=" + sourceVersion.getId());
        }

        Map<Long, Integer> occurrenceByProcessId = new HashMap<>();
        Map<Long, List<ActiveRouteProcessSnapshot>> processesByProcessId = new LinkedHashMap<>();
        Map<Long, ActiveRouteProcessSnapshot> processByRouteProcessId = new LinkedHashMap<>();
        for (JSONObject node : sortCandidateObjects(nodes, "ACTIVE flowGraph.nodes")) {
            Long routeProcessId = node.getLong("routeProcessId");
            Long processId = node.getLong("processId");
            Integer sort = node.getInteger("sort");
            if (routeProcessId == null || processId == null || sort == null) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                        "ACTIVE 工序节点缺少 routeProcessId/processId/sort：routeId=" + route.getId());
            }
            int occurrence = occurrenceByProcessId.merge(processId, 1, Integer::sum);
            ActiveRouteProcessSnapshot process = new ActiveRouteProcessSnapshot(
                    new LinkedHashMap<>(node), processId, routeProcessId, occurrence);
            if (processByRouteProcessId.putIfAbsent(routeProcessId, process) != null) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                        "ACTIVE 工序节点 routeProcessId 重复：routeProcessId=" + routeProcessId);
            }
            processesByProcessId.computeIfAbsent(processId, ignored -> new ArrayList<>()).add(process);
        }

        Map<Long, JSONObject> batchUseConfigByRouteProcessId = new LinkedHashMap<>();
        for (JSONObject config : sortCandidateObjects(batchUseConfigs, "ACTIVE batchUseConfigs")) {
            Long routeProcessId = config.getLong("routeProcessId");
            ActiveRouteProcessSnapshot process = routeProcessId == null
                    ? null : processByRouteProcessId.get(routeProcessId);
            if (process == null) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                        "ACTIVE 批次用途配置无法定位正式工序：routeProcessId=" + routeProcessId);
            }
            requireIndependentBindingArrays(config, process);
            if (batchUseConfigByRouteProcessId.putIfAbsent(routeProcessId, config) != null) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                        "ACTIVE 批次用途配置 routeProcessId 重复：routeProcessId=" + routeProcessId);
            }
        }
        return new ActiveRouteCandidateSource(processesByProcessId,
                batchUseConfigByRouteProcessId, configSnapshots);
    }

    private void requireIndependentBindingArrays(JSONObject config, ActiveRouteProcessSnapshot process) {
        JSONArray batchRecordReports = config.getJSONArray(BATCH_RECORD_REPORTS_KEY);
        JSONArray formBindings = config.getJSONArray(FORM_BINDINGS_KEY);
        if (batchRecordReports == null || formBindings == null) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "ACTIVE 工序配置缺少独立 batchRecordReports/formBindings 来源：processId="
                            + process.processId() + ", routeProcessId=" + process.routeProcessId()
                            + ", occurrence=" + process.occurrence());
        }
        for (Object value : batchRecordReports) {
            if (!(value instanceof JSONObject report)
                    || StrUtil.isBlank(StrUtil.blankToDefault(report.getString("batchRecordReportId"),
                    report.getString("reportId")))) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                        "ACTIVE 正式批记录表单绑定无效：processId=" + process.processId()
                                + ", routeProcessId=" + process.routeProcessId()
                                + ", occurrence=" + process.occurrence());
            }
        }
        for (Object value : formBindings) {
            if (!(value instanceof JSONObject)) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                        "ACTIVE 表单槽位绑定无效：processId=" + process.processId()
                                + ", routeProcessId=" + process.routeProcessId()
                                + ", occurrence=" + process.occurrence());
            }
        }
    }

    private Map<String, Object> remapActiveBatchUseConfig(JSONObject activeConfig,
                                                           Long processId,
                                                           String processName,
                                                           int sort,
                                                           Long routeProcessId) {
        JSONObject config = JSON.parseObject(activeConfig.toJSONString());
        config.put("routeProcessId", routeProcessId);
        config.remove("clientRouteProcessId");
        config.put("processId", processId);
        config.put("processName", processName);
        config.put("sort", sort);
        config.remove("batchRecordReportId");
        config.remove("reportId");
        config.remove("reportCode");
        config.remove("reportName");
        config.remove("batchRecordDefinitionId");
        config.remove("batchRecordVersionId");
        config.remove("formSlotType");

        JSONArray remappedReports = new JSONArray();
        for (Object value : config.getJSONArray(BATCH_RECORD_REPORTS_KEY)) {
            JSONObject report = JSON.parseObject(((JSONObject) value).toJSONString());
            report.put("routeProcessId", routeProcessId);
            remappedReports.add(report);
        }
        config.put(BATCH_RECORD_REPORTS_KEY, remappedReports);

        JSONArray remappedFormBindings = new JSONArray();
        for (Object value : config.getJSONArray(FORM_BINDINGS_KEY)) {
            JSONObject formBinding = JSON.parseObject(((JSONObject) value).toJSONString());
            formBinding.put("routeProcessId", routeProcessId);
            remappedFormBindings.add(formBinding);
        }
        config.put(FORM_BINDINGS_KEY, remappedFormBindings);
        return new LinkedHashMap<>(config);
    }

    private Map<String, Object> buildImportedBatchUseConfigSnapshot(Long routeId,
                                                                    Map<String, Object> process,
                                                                    Long processId,
                                                                    RouteProcessReportBinding binding,
                                                                    int sort) {
        Long routeProcessReferenceId = (Long) process.get("routeProcessId");
        if (routeProcessReferenceId == null) {
            routeProcessReferenceId = (Long) process.get("clientRouteProcessId");
        }
        if (routeProcessReferenceId == null) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "候选新工序缺少可发布投影身份：processId=" + processId + ", sort=" + sort);
        }
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("routeProcessId", routeProcessReferenceId);
        config.put("processId", processId);
        config.put("sort", sort);
        config.put("processName", binding.processName());
        config.put("useType", USE_TYPE_BATCH);
        config.put("enabled", true);
        config.put("executionMode", EXECUTION_MODE_SEQUENTIAL);
        config.put("batchRecordBindingSnapshotExplicit", true);
        config.put(BATCH_RECORD_REPORTS_KEY, new JSONArray(List.of(
                buildFormalBatchRecordReportSnapshot(routeId, routeProcessReferenceId, binding.report(),
                        binding.report().batchRecordDefinitionId(), binding.report().batchRecordVersionId()))));
        config.put(FORM_BINDINGS_KEY, new JSONArray());
        return config;
    }

    private void requireAllConfiguredActiveProcessesMapped(ActiveRouteCandidateSource activeSource,
                                                            Set<Long> mappedActiveRouteProcessIds) {
        for (Map.Entry<Long, JSONObject> entry : activeSource.batchUseConfigByRouteProcessId().entrySet()) {
            if (mappedActiveRouteProcessIds.contains(entry.getKey())) {
                continue;
            }
            ActiveRouteProcessSnapshot process = activeSource.findByRouteProcessId(entry.getKey());
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "已配置旧工序无法唯一映射到 Word 候选：processId=" + process.processId()
                            + ", routeProcessId=" + process.routeProcessId()
                            + ", occurrence=" + process.occurrence());
        }
    }

    private void copyStartConfiguration(JSONObject activeConfigSnapshots,
                                        JSONObject candidateConfigSnapshots,
                                        String configKey,
                                        Long sourceRouteVersionId) {
        if (!activeConfigSnapshots.containsKey(configKey)) {
            candidateConfigSnapshots.put(configKey, new JSONArray());
            return;
        }
        Object value = activeConfigSnapshots.get(configKey);
        if (!(value instanceof JSONArray array)) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "ACTIVE 工艺路线开始配置快照无效：configKey=" + configKey
                            + ", routeVersionId=" + sourceRouteVersionId);
        }
        for (Object item : array) {
            if (!(item instanceof JSONObject)) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                        "ACTIVE 工艺路线开始配置必须只包含对象：configKey=" + configKey
                                + ", routeVersionId=" + sourceRouteVersionId);
            }
        }
        candidateConfigSnapshots.put(configKey, JSON.parseArray(array.toJSONString()));
    }

    private Map<String, Object> buildCandidateFlowGraphSnapshot(List<Map<String, Object>> processSnapshots) {
        List<Map<String, Object>> edges = new ArrayList<>();
        for (int index = 1; index < processSnapshots.size(); index++) {
            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("sourceSort", index);
            edge.put("targetSort", index + 1);
            edge.put("relationType", "NORMAL");
            edge.put("sort", index);
            edges.add(edge);
        }
        Map<String, Object> flowGraph = new LinkedHashMap<>();
        flowGraph.put("graphVersion", 1L);
        flowGraph.put("nodes", copyProcessSnapshots(processSnapshots));
        flowGraph.put("edges", edges);
        flowGraph.put("boundaryEdges", buildLinearBoundaryEdgeSnapshots(processSnapshots));
        return flowGraph;
    }

    private List<Map<String, Object>> buildLinearBoundaryEdgeSnapshots(
            List<Map<String, Object>> processSnapshots) {
        if (processSnapshots.isEmpty()) {
            return List.of();
        }
        Map<String, Object> start = new LinkedHashMap<>();
        start.put("boundaryType", "START");
        start.put("routeProcessSort", 1);
        start.put("sort", 1);
        Map<String, Object> end = new LinkedHashMap<>();
        end.put("boundaryType", "END");
        end.put("routeProcessSort", processSnapshots.size());
        end.put("sort", 1);
        return List.of(start, end);
    }

    private List<Map<String, Object>> copyProcessSnapshots(List<Map<String, Object>> processSnapshots) {
        List<Map<String, Object>> copies = new ArrayList<>(processSnapshots.size());
        for (Map<String, Object> processSnapshot : processSnapshots) {
            copies.add(new LinkedHashMap<>(processSnapshot));
        }
        return copies;
    }

    private void updateRouteVersionSnapshot(MesProRouteVersionDO routeVersion, JSONObject snapshot) {
        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(routeVersion.getId());
        MesProRouteVersionSnapshotIdentityWriter.apply(update, snapshot.toJSONString());
        routeVersionMapper.updateById(update);
        routeVersion.setRouteSnapshotJson(update.getRouteSnapshotJson());
        routeVersion.setRouteSnapshotSha256(update.getRouteSnapshotSha256());
        routeVersion.setRouteSnapshotFormatVersion(update.getRouteSnapshotFormatVersion());
    }

    private JSONObject buildGeneratedRouteSnapshot(MesProRouteDO route,
                                                   List<MesProRouteProcessDO> routeProcesses,
                                                   List<RouteProcessReportBinding> bindings,
                                                   List<String> productNames,
                                                   boolean bindBatchRecordReports,
                                                   Map<Long, Long> batchRecordPermissionScopeIds) {
        JSONObject snapshot = new JSONObject(true);
        snapshot.put("routeId", route.getId());
        snapshot.put("routeCode", route.getCode());
        snapshot.put("routeName", route.getName());
        snapshot.put("status", route.getStatus());

        List<Map<String, Object>> processSnapshots = new ArrayList<>();
        List<Map<String, Object>> batchUseConfigSnapshots = new ArrayList<>();
        for (int index = 0; index < routeProcesses.size(); index++) {
            MesProRouteProcessDO routeProcess = routeProcesses.get(index);
            RouteProcessReportBinding binding = bindings.get(index);
            Map<String, Object> process = new LinkedHashMap<>();
            process.put("routeProcessId", routeProcess.getId());
            process.put("processId", routeProcess.getProcessId());
            process.put("sort", routeProcess.getSort());
            process.put("processName", binding.processName());
            processSnapshots.add(process);
            if (!bindBatchRecordReports || binding.report() == null) {
                continue;
            }
            Map<String, Object> batchUseConfig = new LinkedHashMap<>();
            batchUseConfig.put("routeProcessId", routeProcess.getId());
            batchUseConfig.put("processId", routeProcess.getProcessId());
            batchUseConfig.put("sort", routeProcess.getSort());
            batchUseConfig.put("processName", binding.processName());
            batchUseConfig.put("useType", USE_TYPE_BATCH);
            batchUseConfig.put("enabled", true);
            batchUseConfig.put("executionMode", EXECUTION_MODE_SEQUENTIAL);
            batchUseConfig.put("batchRecordReportId", binding.report().reportId());
            batchUseConfig.put("reportId", binding.report().reportId());
            batchUseConfig.put("reportCode", binding.report().reportCode());
            batchUseConfig.put("reportName", binding.report().reportName());
            batchUseConfig.put("batchRecordDefinitionId", binding.report().batchRecordDefinitionId());
            batchUseConfig.put("batchRecordVersionId", binding.report().batchRecordVersionId());
            batchUseConfig.put("formSlotType", binding.report().formSlotType());
            batchUseConfig.put("recordCategory", RECORD_CATEGORY_BATCH_RECORD);
            batchUseConfig.put("validationProfile", VALIDATION_PROFILE_CONTROLLED_BATCH);
            Long permissionScopeId = batchRecordPermissionScopeIds.get(routeProcess.getId());
            if (permissionScopeId == null) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                        "批记录绑定权限范围缺失：" + routeProcess.getId());
            }
            batchUseConfig.put("permissionScopeId", permissionScopeId);
            batchUseConfig.put("recordCategorySnapshotHash", buildSnapshotHash(route.getId(),
                    routeProcess.getId(), binding.report().reportId()));
            batchUseConfig.put("requiredPolicy", REQUIRED_POLICY_REQUIRED);
            batchUseConfig.put("ownerRoleKey", OWNER_ROLE_PRODUCTION);
            batchUseConfig.put("archiveVisibility", ARCHIVE_VISIBILITY_FINAL_DHR);
            batchUseConfig.put("slotConfigSnapshotHash", buildSnapshotHash(route.getId(),
                    routeProcess.getId(), binding.report().reportId()));
            batchUseConfig.put("reportSort", 1);
            batchUseConfig.put("remark", "eDHR Word导入自动绑定批记录报表");
            batchUseConfigSnapshots.add(batchUseConfig);
        }

        JSONObject configSnapshots = new JSONObject(true);
        configSnapshots.put("flowGraph", buildGeneratedFlowGraphSnapshot(route.getId(), processSnapshots));
        configSnapshots.put("products", buildRouteProductSnapshots(route.getId()));
        configSnapshots.put("scheduleConfigs", List.of());
        configSnapshots.put("scheduleUseConfigs", List.of());
        configSnapshots.put("batchUseConfigs", batchUseConfigSnapshots);
        snapshot.put("configSnapshots", configSnapshots);
        return snapshot;
    }

    private List<Map<String, Object>> buildRouteProductSnapshots(Long routeId) {
        return routeProductMapper.selectListByRouteId(routeId).stream()
                .map(product -> {
                    if (product.getItemId() == null) {
                        throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_BIND_FAILED,
                                "工艺路线产品缺少正式物料编号：" + routeId);
                    }
                    Map<String, Object> snapshot = new LinkedHashMap<>();
                    snapshot.put("routeId", routeId);
                    snapshot.put("itemId", product.getItemId());
                    snapshot.put("quantity", product.getQuantity());
                    snapshot.put("productionTime", product.getProductionTime());
                    snapshot.put("timeUnitType", product.getTimeUnitType());
                    snapshot.put("remark", product.getRemark());
                    return snapshot;
                })
                .toList();
    }

    private Long bindBatchRecordPermissionScope(Long routeProcessId, String batchRecordReportId) {
        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();
        MesProEdhrPermissionScopeDetailResult scope = permissionScopeService.saveRules(
                new MesProEdhrPermissionScopeSaveCommand()
                        .setScopeName("route-process-batch-record-" + routeProcessId + "-"
                                + StrUtil.trim(batchRecordReportId))
                        .setObjectType(OBJECT_TYPE_ROUTE_PROCESS_BATCH_RECORD)
                        .setObjectId(buildBatchRecordPermissionScopeObjectId(routeProcessId, batchRecordReportId))
                        .setActorUserId(actorUserId)
                        .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                        .setRules(BATCH_RECORD_BINDING_ABILITIES.stream()
                                .map(ability -> new MesProEdhrPermissionRuleCommand()
                                        .setSubjectType(PERMISSION_SUBJECT_TYPE_USER)
                                        .setSubjectId(actorUserId)
                                        .setAbility(ability)
                                        .setDecision(PERMISSION_DECISION_ALLOW)
                                        .setPriority(BATCH_RECORD_BINDING_PERMISSION_PRIORITY)
                                        .setStatus(PERMISSION_STATUS_ENABLED))
                                .toList()));
        return scope.getScopeId();
    }

    private String buildBatchRecordPermissionScopeObjectId(Long routeProcessId, String batchRecordReportId) {
        return routeProcessId + "|" + StrUtil.trim(batchRecordReportId);
    }

    private Map<String, Object> buildGeneratedFlowGraphSnapshot(Long routeId, List<Map<String, Object>> processSnapshots) {
        List<Map<String, Object>> edges = routeProcessFlowEdgeMapper.selectListByRouteId(routeId).stream()
                .sorted(Comparator.comparing(MesProRouteProcessFlowEdgeDO::getSort, Comparator.nullsLast(Integer::compareTo)))
                .map(edge -> {
                    Map<String, Object> edgeSnapshot = new LinkedHashMap<>();
                    edgeSnapshot.put("sourceRouteProcessId", edge.getSourceRouteProcessId());
                    edgeSnapshot.put("targetRouteProcessId", edge.getTargetRouteProcessId());
                    edgeSnapshot.put("relationType", edge.getRelationType());
                    edgeSnapshot.put("sort", edge.getSort());
                    return edgeSnapshot;
                })
                .toList();
        List<Map<String, Object>> boundaryEdges = routeProcessFlowBoundaryEdgeMapper.selectListByRouteId(routeId).stream()
                .map(edge -> {
                    Map<String, Object> edgeSnapshot = new LinkedHashMap<>();
                    edgeSnapshot.put("boundaryType", edge.getBoundaryType());
                    edgeSnapshot.put("routeProcessId", edge.getRouteProcessId());
                    edgeSnapshot.put("sort", edge.getSort());
                    return edgeSnapshot;
                })
                .toList();
        Map<String, Object> flowGraph = new LinkedHashMap<>();
        flowGraph.put("graphVersion", 1L);
        flowGraph.put("nodes", processSnapshots);
        flowGraph.put("edges", edges);
        flowGraph.put("boundaryEdges", boundaryEdges);
        return flowGraph;
    }

    private String nextRouteVersionNo(Long routeId) {
        int maxVersion = routeVersionMapper.selectListByRouteId(routeId).stream()
                .map(MesProRouteVersionDO::getVersionNo)
                .mapToInt(this::parseRouteVersionNo)
                .max()
                .orElse(0);
        return "V" + (maxVersion + 1);
    }

    private int parseRouteVersionNo(String versionNo) {
        String normalized = StrUtil.trimToEmpty(versionNo).toUpperCase(Locale.ROOT);
        if (!normalized.matches("V\\d+")) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED, "路线版本号无效：" + versionNo);
        }
        return Integer.parseInt(normalized.substring(1));
    }

    private List<String> normalizeProductNames(List<String> productNames) {
        List<String> normalized = productNames == null ? List.of() : productNames.stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_NAME_REQUIRED);
        }
        return normalized;
    }

    private DccProjectCodeDO requireSelectedDccProjectCode(Long dccProjectCodeId) {
        if (dccProjectCodeId == null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants
                    .PRO_BATCH_RECORD_REPORT_DCC_PROJECT_CODE_REQUIRED);
        }
        DccProjectCodeDO projectCode = dccProjectCodeMapper.selectById(dccProjectCodeId);
        if (projectCode == null
                || !DccProjectCodeStatusConstants.ENABLE.equals(projectCode.getStatus())) {
            throw exception(MesProBatchRecordReportErrorCodeConstants
                    .PRO_BATCH_RECORD_REPORT_DCC_PROJECT_CODE_REQUIRED);
        }
        return projectCode;
    }

    private void bindRouteToDccProject(Long routeId, Long dccProjectCodeId) {
        DccProjectCodeDO selectedProjectCode = requireSelectedDccProjectCode(dccProjectCodeId);
        MesRouteDccProjectBindingDO current = routeDccProjectBindingMapper.selectCurrentByRouteIdForUpdate(routeId);
        if (current != null) {
            if (!Objects.equals(current.getDccProjectCodeId(), selectedProjectCode.getId())) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                        "工艺路线已绑定其它DCC项目代码：" + routeId);
            }
            return;
        }
        Long maxVersion = routeDccProjectBindingMapper.selectMaxVersionByRouteIdIncludeDeleted(routeId);
        MesRouteDccProjectBindingDO binding = MesRouteDccProjectBindingDO.builder()
                .routeId(routeId)
                .dccProjectCodeId(selectedProjectCode.getId())
                .version(maxVersion == null ? 1L : maxVersion + 1)
                .build();
        binding.setDeleted(false);
        routeDccProjectBindingMapper.insert(binding);
    }

    private void validateRouteDccProjectBinding(Long routeId, Long dccProjectCodeId) {
        DccProjectCodeDO selectedProjectCode = requireSelectedDccProjectCode(dccProjectCodeId);
        MesRouteDccProjectBindingDO current = routeDccProjectBindingMapper.selectCurrentByRouteId(routeId);
        if (current != null && Objects.equals(current.getDccProjectCodeId(), selectedProjectCode.getId())) {
            return;
        }
        if (current != null) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "工艺路线与所选DCC项目代码绑定关系不一致：" + routeId);
        }
        validateRouteProductMatchesDccProject(routeId, selectedProjectCode);
        bindRouteToDccProject(routeId, selectedProjectCode.getId());
    }

    private void validateRouteProductMatchesDccProject(Long routeId, DccProjectCodeDO selectedProjectCode) {
        String normalizedProjectCode = StrUtil.trim(selectedProjectCode.getProjectCode());
        if (StrUtil.isBlank(normalizedProjectCode)) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "DCC项目代码为空：" + selectedProjectCode.getProjectName());
        }
        MesMdItemDO item = itemMapper.selectByCode(normalizedProjectCode);
        if (item == null || !CommonStatusEnum.isEnable(item.getStatus()) || !Boolean.TRUE.equals(item.getBatchFlag())) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "DCC项目产品未启用批次绑定：" + normalizedProjectCode);
        }
        MesProRouteProductDO routeProduct = routeProductMapper.selectByRouteIdAndItemId(routeId, item.getId());
        if (routeProduct == null) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "工艺路线未绑定所选DCC项目产品：" + routeId + "/" + normalizedProjectCode);
        }
    }

    private RouteProductBindingResult bindRouteProducts(Long routeId, List<String> productNames,
                                                        Long dccProjectCodeId) {
        Set<Long> boundItemIds = new LinkedHashSet<>();
        Set<String> boundProductNames = new LinkedHashSet<>();
        List<String> skippedProductNames = new ArrayList<>();
        DccProjectCodeDO selectedProjectCode = requireSelectedDccProjectCode(dccProjectCodeId);
        List<Long> itemIds = resolveDccProjectProductIds(selectedProjectCode, productNames);
        boundProductNames.add(selectedProjectCode.getProjectName());
        boundItemIds.addAll(itemIds);
        if (boundItemIds.isEmpty()) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_EMPTY, String.join("、", productNames));
        }
        List<MesProRouteProductDO> conflictBindings = routeProductMapper.selectListByItemIds(boundItemIds).stream()
                .filter(binding -> !Objects.equals(binding.getRouteId(), routeId))
                .toList();
        if (!conflictBindings.isEmpty()) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_BIND_FAILED,
                    "产品已绑定其他工艺路线：" + formatRouteProductConflicts(conflictBindings));
        }
        try {
            for (Long itemId : boundItemIds) {
                if (routeProductMapper.selectByRouteIdAndItemId(routeId, itemId) != null) {
                    continue;
                }
                routeProductMapper.insert(MesProRouteProductDO.builder()
                        .routeId(routeId)
                        .itemId(itemId)
                        .quantity(1)
                        .remark("eDHR Word导入自动绑定")
                        .build());
            }
        } catch (Exception ex) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_BIND_FAILED, ex.getMessage());
        }
        return new RouteProductBindingResult(boundProductNames.size(), boundItemIds.size(), skippedProductNames);
    }

    private RouteProductBindingResult inspectExistingRouteProductBindings(Long routeId, List<String> productNames,
                                                                         Long dccProjectCodeId) {
        Set<Long> boundItemIds = new LinkedHashSet<>();
        Set<String> boundProductNames = new LinkedHashSet<>();
        List<String> skippedProductNames = new ArrayList<>();
        DccProjectCodeDO selectedProjectCode = requireSelectedDccProjectCode(dccProjectCodeId);
        List<Long> itemIds = resolveDccProjectProductIds(selectedProjectCode, productNames);
        List<MesProRouteProductDO> bindings = routeProductMapper.selectListByItemIds(itemIds);
        List<MesProRouteProductDO> conflictBindings = bindings.stream()
                .filter(binding -> !Objects.equals(binding.getRouteId(), routeId))
                .toList();
        if (!conflictBindings.isEmpty()) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_BIND_FAILED,
                    "产品已绑定其他工艺路线：" + formatRouteProductConflicts(conflictBindings));
        }
            List<Long> missingItemIds = itemIds.stream()
                    .filter(itemId -> bindings.stream().noneMatch(binding -> Objects.equals(binding.getRouteId(), routeId)
                            && Objects.equals(binding.getItemId(), itemId)))
                    .toList();
            if (!missingItemIds.isEmpty()) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_BIND_FAILED,
                        "产品未绑定当前工艺路线：" + missingItemIds);
            }
            boundProductNames.add(selectedProjectCode.getProjectName());
            boundItemIds.addAll(itemIds);
        if (boundItemIds.isEmpty()) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_EMPTY, String.join("、", productNames));
        }
        return new RouteProductBindingResult(boundProductNames.size(), boundItemIds.size(), skippedProductNames);
    }

    private String formatRouteProductConflicts(List<MesProRouteProductDO> bindings) {
        Set<Long> itemIds = bindings.stream()
                .map(MesProRouteProductDO::getItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesMdItemDO> itemMap = itemIds.isEmpty() ? Map.of()
                : itemMapper.selectListByIds(itemIds).stream()
                        .filter(item -> item.getId() != null)
                        .collect(Collectors.toMap(MesMdItemDO::getId, item -> item, (left, right) -> left,
                                LinkedHashMap::new));
        return bindings.stream()
                .map(binding -> {
                    MesMdItemDO item = itemMap.get(binding.getItemId());
                    String itemName = item == null ? String.valueOf(binding.getItemId())
                            : StrUtil.blankToDefault(item.getName(), item.getCode());
                    return itemName + "(routeId=" + binding.getRouteId() + ")";
                })
                .distinct()
                .collect(Collectors.joining("、"));
    }

    private List<Long> resolveDccProjectProductIds(DccProjectCodeDO selectedProjectCode,
                                                   List<String> productNames) {
        if (productNames.stream().noneMatch(productName -> StrUtil.equals(
                StrUtil.trim(productName), StrUtil.trim(selectedProjectCode.getProjectName())))) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_BIND_FAILED,
                    "导入产品名称与DCC项目不一致：" + productNames + "/" + selectedProjectCode.getProjectName());
        }
        Set<Long> itemIds = new LinkedHashSet<>();
        String normalizedProjectCode = StrUtil.trim(selectedProjectCode.getProjectCode());
        if (StrUtil.isBlank(normalizedProjectCode)) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_BIND_FAILED,
                    "DCC项目代码为空：" + selectedProjectCode.getProjectName());
        }
        itemIds.add(resolveDccProjectItemId(selectedProjectCode.getProjectName(), normalizedProjectCode));
        return new ArrayList<>(itemIds);
    }

    private Long resolveDccProjectItemId(String projectName, String projectCode) {
        MesMdItemDO existing = itemMapper.selectByCode(projectCode);
        if (existing != null) {
            if (!CommonStatusEnum.isEnable(existing.getStatus()) || !Boolean.TRUE.equals(existing.getBatchFlag())) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_BIND_FAILED,
                        "DCC项目产品未启用批次绑定：" + projectCode);
            }
            return existing.getId();
        }
        MesMdItemDO created = MesMdItemDO.builder()
                .code(projectCode)
                .name(projectName)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .safeStockFlag(false)
                .highValue(false)
                .batchFlag(true)
                .remark("eDHR Word导入根据DCC项目代码自动创建")
                .build();
        itemMapper.insert(created);
        return created.getId();
    }

    private List<RouteProcessReportBinding> buildProcessReportBindings(List<MesProBatchRecordParsedTable> parsedTables,
                                                                       List<MesProBatchRecordReportView> reports,
                                                                       boolean requireReportBinding) {
        Map<Integer, MesProBatchRecordReportView> reportBySourceIndex = reports == null ? Map.of()
                : reports.stream()
                .filter(report -> report.sourceTableIndex() != null)
                .collect(Collectors.toMap(MesProBatchRecordReportView::sourceTableIndex, report -> report,
                        (left, right) -> left, LinkedHashMap::new));
        List<RouteProcessReportBinding> bindings = new ArrayList<>();
        parsedTables.subList(1, parsedTables.size()).stream()
                .sorted(Comparator.comparing(MesProBatchRecordParsedTable::getSourceTableIndex,
                        Comparator.nullsLast(Integer::compareTo)))
                .forEach(parsedTable -> {
                    String processName = normalizeProcessName(parsedTable);
                    MesProBatchRecordReportView report = reportBySourceIndex.get(parsedTable.getSourceTableIndex());
                    if (report == null || StrUtil.isBlank(report.reportId())) {
                        if (requireReportBinding) {
                            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                                    "未找到工序对应的批记录报表：表" + parsedTable.getSourceTableIndex());
                        }
                        bindings.add(new RouteProcessReportBinding(processName, null));
                        return;
                    }
                    bindings.add(new RouteProcessReportBinding(processName, report));
                });
        if (bindings.isEmpty()) {
            throw exception(PRO_BATCH_RECORD_REPORT_PROCESS_EMPTY);
        }
        return bindings;
    }

    private String generateRouteCode() {
        String routeCode = StrUtil.trim(autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_ROUTE_CODE.getCode()));
        if (StrUtil.isBlank(routeCode)) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED, "路线编码生成结果为空");
        }
        MesProRouteDO existing = routeMapper.selectByCode(routeCode);
        if (existing != null) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED, "路线编码已存在：" + routeCode);
        }
        return routeCode;
    }

    private MesProProcessDO resolveProcess(String processName) {
        MesProProcessDO existing = processMapper.selectByName(processName);
        if (existing != null) {
            return existing;
        }
        String processCode = PROCESS_CODE_PREFIX
                + DigestUtil.sha256Hex(processName).substring(0, 12).toUpperCase(Locale.ROOT);
        MesProProcessDO existingByCode = processMapper.selectByCode(processCode);
        if (existingByCode != null) {
            if (processName.equals(existingByCode.getName())) {
                return existingByCode;
            }
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "工序编码冲突：" + processCode + " / " + processName);
        }
        MesProProcessDO process = MesProProcessDO.builder()
                .code(processCode)
                .name(processName)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .remark("eDHR Word导入自动补齐")
                .build();
        processMapper.insert(process);
        return process;
    }

    private String normalizeProcessName(MesProBatchRecordParsedTable parsedTable) {
        String processName = StrUtil.trim(parsedTable.getTableTitle());
        if (StrUtil.isBlank(processName)) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "工序名称为空：表" + parsedTable.getSourceTableIndex());
        }
        if (processName.endsWith("生产记录") && processName.contains("工序")) {
            processName = StrUtil.trim(StrUtil.removeSuffix(processName, "生产记录"));
        }
        if (StrUtil.isBlank(processName)) {
            throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED,
                    "工序名称为空：表" + parsedTable.getSourceTableIndex());
        }
        return processName;
    }

    private int findProductInfoIndex(List<MesProBatchRecordParsedTable> parsedTables) {
        for (int index = 0; index < parsedTables.size(); index++) {
            if (isProductInfo(parsedTables.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private boolean isProductInfo(MesProBatchRecordParsedTable parsedTable) {
        if (parsedTable == null || parsedTable.getTableTitle() == null) {
            return false;
        }
        return parsedTable.getTableTitle().replaceAll("\\s+", "").contains(PRODUCT_INFO_KEYWORD);
    }

    private String buildSnapshotHash(Long routeId, Long routeProcessId, String reportId) {
        return DigestUtil.sha256Hex(routeId + "|" + routeProcessId + "|" + reportId + "|"
                + RECORD_CATEGORY_BATCH_RECORD + "|" + VALIDATION_PROFILE_CONTROLLED_BATCH + "|1");
    }

    private record RouteProcessReportBinding(String processName, MesProBatchRecordReportView report) {
    }

    private record ActiveRouteCandidateSource(
            Map<Long, List<ActiveRouteProcessSnapshot>> processesByProcessId,
            Map<Long, JSONObject> batchUseConfigByRouteProcessId,
            JSONObject configSnapshots) {

        private ActiveRouteProcessSnapshot find(Long processId, int occurrence) {
            List<ActiveRouteProcessSnapshot> processes = processesByProcessId.get(processId);
            return processes == null || occurrence > processes.size() ? null : processes.get(occurrence - 1);
        }

        private ActiveRouteProcessSnapshot findByRouteProcessId(Long routeProcessId) {
            return processesByProcessId.values().stream()
                    .flatMap(List::stream)
                    .filter(process -> Objects.equals(routeProcessId, process.routeProcessId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "ACTIVE route process snapshot is required: routeProcessId=" + routeProcessId));
        }
    }

    private record ActiveRouteProcessSnapshot(Map<String, Object> node,
                                              Long processId,
                                              Long routeProcessId,
                                              int occurrence) {
    }

    private record RouteGenerationTarget(MesProRouteDO route, MesProRouteVersionDO activeVersion, boolean existing,
                                         RouteUpgradePreservedData preservedData) {

        private RouteGenerationTarget(MesProRouteDO route, MesProRouteVersionDO activeVersion, boolean existing) {
            this(route, activeVersion, existing, RouteUpgradePreservedData.empty());
        }
    }

    private record RouteUpgradePreservedData(Map<Long, List<PreservedRouteProcess>> processesByProcessId,
                                             List<MesProRouteProcessFlowEdgeDO> edges) {

        private static RouteUpgradePreservedData empty() {
            return new RouteUpgradePreservedData(Map.of(), List.of());
        }
    }

    private record PreservedRouteProcess(Long id, Long processId,
                                         Integer prepareTime, Integer waitTime, String colorCode,
                                         Boolean keyFlag, Boolean checkFlag, String remark) {
    }

    private record RouteProductBindingResult(int boundProductNameCount, int boundProductCodeCount,
                                             List<String> skippedProductNames) {
    }
}
