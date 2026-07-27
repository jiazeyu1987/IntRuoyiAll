package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.enums.md.autocode.MesMdAutoCodeRuleCodeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionRuleCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeDetailResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeSaveCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteOwnerPermissionService;
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
    private MesProRouteProductMapper routeProductMapper;
    @Resource
    private MesProRouteProcessMapper routeProcessMapper;
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
        return generateForUploadedWord(batchRecordName, parsedTables, List.of(), productNames, null, null, false,
                expectedRouteId, expectedRouteVersionId, routeUpgradeConfirmed, false);
    }

    @Override
    public MesProBatchRecordRouteGenerationResult generateForUploadedWord(String batchRecordName,
                                                                           List<MesProBatchRecordParsedTable> parsedTables,
                                                                           List<MesProBatchRecordReportView> reports,
                                                                           List<String> productNames,
                                                                           Long batchRecordDefinitionId,
                                                                           Long batchRecordVersionId) {
        return generateForUploadedWord(batchRecordName, parsedTables, reports, productNames,
                batchRecordDefinitionId, batchRecordVersionId, true, null, null, false, false);
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
                expectedRouteId, expectedRouteVersionId, routeUpgradeConfirmed, false);
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
                batchRecordDefinitionId, batchRecordVersionId, true,
                expectedRouteId, expectedRouteVersionId, routeUpgradeConfirmed, applyExistingRouteRebuild);
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
                                                                           boolean applyExistingRouteRebuild) {
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
            routeOwnerPermissionService.bindCurrentUserAsOwner(route.getId());
        } else if (target.existing()) {
            MesProRouteVersionDO candidateRouteVersion = createCandidateRouteVersion(
                    route, sourceRouteVersion, bindings, normalizedProductNames, bindBatchRecordReports);
            return MesProBatchRecordRouteGenerationResult.builder()
                    .routeId(route.getId())
                    .routeCode(route.getCode())
                    .routeName(route.getName())
                    .routeVersionId(candidateRouteVersion.getId())
                    .routeVersionNo(candidateRouteVersion.getVersionNo())
                    .routeProcessCount(bindings.size())
                    .batchRecordRouteBindingCount(bindBatchRecordReports ? bindings.size() : 0)
                    .boundProductNameCount(0)
                    .boundProductCodeCount(0)
                    .skippedProductNames(normalizedProductNames)
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
        RouteProductBindingResult productBindingResult = bindRouteProducts(route.getId(), normalizedProductNames);
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
        List<MesProRouteDO> routes = routeMapper.selectListByName(routeName);
        if (routes.size() > 1) {
            String routeCodes = routes.stream()
                    .map(route -> route.getCode() + "/" + route.getId())
                    .collect(Collectors.joining("、"));
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_DUPLICATE,
                    routeName, routeCodes);
        }
        if (routes.isEmpty()) {
            if (expectedRouteId != null || expectedRouteVersionId != null) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_UPGRADE_TARGET_CHANGED,
                        expectedRouteId, expectedRouteVersionId, null, null);
            }
            return new RouteGenerationTarget(null, null, false);
        }
        MesProRouteDO route = routes.get(0);
        MesProRouteVersionDO activeVersion = routeVersionMapper.selectActiveByRouteId(route.getId());
        if (!Boolean.TRUE.equals(routeUpgradeConfirmed)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_UPGRADE_CONFIRM_REQUIRED,
                    routeName);
        }
        Long currentVersionId = activeVersion == null ? null : activeVersion.getId();
        if (!Objects.equals(expectedRouteId, route.getId())
                || !Objects.equals(expectedRouteVersionId, currentVersionId)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_UPGRADE_TARGET_CHANGED,
                    expectedRouteId, expectedRouteVersionId, route.getId(), currentVersionId);
        }
        return new RouteGenerationTarget(route, activeVersion, true);
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
                .routeSnapshotJson(snapshot.toJSONString())
                .remark(sourceVersion == null ? "eDHR Word导入自动生成路线版本" : "eDHR Word导入升版路线版本")
                .build();
        routeVersionMapper.insert(routeVersion);
        return routeVersion;
    }

    private MesProRouteVersionDO createCandidateRouteVersion(MesProRouteDO route,
                                                             MesProRouteVersionDO sourceVersion,
                                                             List<RouteProcessReportBinding> bindings,
                                                             List<String> productNames,
                                                             boolean bindBatchRecordReports) {
        JSONObject snapshot = new JSONObject(true);
        snapshot.put("routeId", route.getId());
        snapshot.put("routeCode", route.getCode());
        snapshot.put("routeName", route.getName());
        snapshot.put("status", route.getStatus());
        snapshot.put("candidateSource", "EDHR_WORD_IMPORT");
        snapshot.put("productNames", productNames);
        List<Map<String, Object>> processSnapshots = new ArrayList<>();
        List<Map<String, Object>> batchUseConfigSnapshots = new ArrayList<>();
        for (int index = 0; index < bindings.size(); index++) {
            RouteProcessReportBinding binding = bindings.get(index);
            Map<String, Object> process = new LinkedHashMap<>();
            process.put("sort", index + 1);
            process.put("processName", binding.processName());
            if (bindBatchRecordReports && binding.report() != null) {
                process.put("reportId", binding.report().reportId());
                process.put("reportCode", binding.report().reportCode());
                process.put("reportName", binding.report().reportName());
                process.put("batchRecordDefinitionId", binding.report().batchRecordDefinitionId());
                process.put("batchRecordVersionId", binding.report().batchRecordVersionId());
                process.put("formSlotType", binding.report().formSlotType());
                Map<String, Object> batchUseConfig = new LinkedHashMap<>();
                batchUseConfig.put("sort", index + 1);
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
                batchUseConfig.put("permissionScopeId", (long) index + 1);
                batchUseConfig.put("recordCategorySnapshotHash", buildSnapshotHash(route.getId(),
                        (long) index + 1, binding.report().reportId()));
                batchUseConfig.put("requiredPolicy", REQUIRED_POLICY_REQUIRED);
                batchUseConfig.put("ownerRoleKey", OWNER_ROLE_PRODUCTION);
                batchUseConfig.put("archiveVisibility", ARCHIVE_VISIBILITY_FINAL_DHR);
                batchUseConfig.put("slotConfigSnapshotHash", buildSnapshotHash(route.getId(),
                        (long) index + 1, binding.report().reportId()));
                batchUseConfigSnapshots.add(batchUseConfig);
            }
            processSnapshots.add(process);
        }
        snapshot.put("processes", processSnapshots);
        JSONObject configSnapshots = new JSONObject(true);
        configSnapshots.put("flowGraph", buildCandidateFlowGraphSnapshot(processSnapshots));
        configSnapshots.put("products", productNames);
        configSnapshots.put("scheduleConfigs", List.of());
        configSnapshots.put("scheduleUseConfigs", List.of());
        configSnapshots.put("batchUseConfigs", batchUseConfigSnapshots);
        snapshot.put("configSnapshots", configSnapshots);

        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder()
                .routeId(route.getId())
                .versionNo(nextRouteVersionNo(route.getId()))
                .active(false)
                .lifecycleStatus(STATUS_DRAFT)
                .sourceRouteVersionId(sourceVersion == null ? null : sourceVersion.getId())
                .routeSnapshotJson(snapshot.toJSONString())
                .changeSummaryJson("{\"source\":\"EDHR_WORD_IMPORT\",\"changeType\":\"ROUTE_REBUILD_CANDIDATE\"}")
                .remark("eDHR Word导入生成路线候选版本，待发布后生效")
                .build();
        routeVersionMapper.insert(routeVersion);
        return routeVersion;
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
        flowGraph.put("nodes", processSnapshots);
        flowGraph.put("edges", edges);
        return flowGraph;
    }

    private void updateRouteVersionSnapshot(MesProRouteVersionDO routeVersion, JSONObject snapshot) {
        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(routeVersion.getId());
        update.setRouteSnapshotJson(snapshot.toJSONString());
        routeVersionMapper.updateById(update);
        routeVersion.setRouteSnapshotJson(update.getRouteSnapshotJson());
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
        configSnapshots.put("products", productNames);
        configSnapshots.put("scheduleConfigs", List.of());
        configSnapshots.put("scheduleUseConfigs", List.of());
        configSnapshots.put("batchUseConfigs", batchUseConfigSnapshots);
        snapshot.put("configSnapshots", configSnapshots);
        return snapshot;
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
        Map<String, Object> flowGraph = new LinkedHashMap<>();
        flowGraph.put("graphVersion", 1L);
        flowGraph.put("nodes", processSnapshots);
        flowGraph.put("edges", edges);
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

    private RouteProductBindingResult bindRouteProducts(Long routeId, List<String> productNames) {
        Set<Long> boundItemIds = new LinkedHashSet<>();
        Set<String> boundProductNames = new LinkedHashSet<>();
        List<String> skippedProductNames = new ArrayList<>();
        for (String productName : productNames) {
            List<Long> itemIds = resolveDccProjectProductIds(productName);
            if (itemIds.isEmpty()) {
                skippedProductNames.add(productName);
                continue;
            }
            boundProductNames.add(productName);
            boundItemIds.addAll(itemIds);
        }
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

    private List<Long> resolveDccProjectProductIds(String productName) {
        List<DccProjectCodeDO> projectCodes = dccProjectCodeMapper.selectEnabledListByProjectName(productName);
        if (CollUtil.isEmpty(projectCodes)) {
            return List.of();
        }
        Set<Long> itemIds = new LinkedHashSet<>();
        for (DccProjectCodeDO projectCode : projectCodes) {
            String normalizedProjectCode = StrUtil.trim(projectCode.getProjectCode());
            if (StrUtil.isBlank(normalizedProjectCode)) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_BIND_FAILED,
                        "DCC项目代码为空：" + productName);
            }
            itemIds.add(resolveDccProjectItemId(projectCode.getProjectName(), normalizedProjectCode));
        }
        return new ArrayList<>(itemIds);
    }

    private Long resolveDccProjectItemId(String projectName, String projectCode) {
        MesMdItemDO existing = itemMapper.selectByCode(projectCode);
        if (existing != null) {
            if (!StrUtil.equals(existing.getName(), projectName)) {
                throw exception(PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_BIND_FAILED,
                        "DCC项目代码已存在不同产品名称：" + projectCode);
            }
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
