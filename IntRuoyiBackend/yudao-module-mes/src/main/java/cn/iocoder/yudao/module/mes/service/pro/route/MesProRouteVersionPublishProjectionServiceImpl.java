package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicy;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormPolicySaveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormPolicySlotReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormPolicyRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalPolicyDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionPolicyDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalPolicyMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionPolicyMapper;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowBoundaryEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowLayoutDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowBoundaryEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowLayoutMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRouteFormFillEffectExecutor;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Projects a publishable route-version snapshot back into the live route tables.
 */
@Service
public class MesProRouteVersionPublishProjectionServiceImpl {

    private static final String SNAPSHOT_CONFIGS_KEY = "configSnapshots";
    private static final String FLOW_GRAPH_KEY = "flowGraph";
    private static final String PRODUCTS_KEY = "products";
    private static final String PRODUCT_BOMS_KEY = "productBoms";
    private static final String SCHEDULE_CONFIGS_KEY = "scheduleConfigs";
    private static final String BATCH_USE_CONFIGS_KEY = "batchUseConfigs";
    private static final String SCHEDULE_USE_CONFIGS_KEY = "scheduleUseConfigs";
    private static final String BATCH_USE_TYPE = "BATCH";
    private static final String SCHEDULE_USE_TYPE = "SCHEDULE";
    private static final String FORM_POLICY_DATA_DOMAIN = "MES";
    private static final String FORM_POLICY_SYSTEM_CODE = "MES";
    private static final String FORM_POLICY_OBJECT_TYPE = "EDHR_ROUTE_FORM";
    private static final String FORM_POLICY_OBJECT_STATE = "ACTIVE";
    private static final String FORM_POLICY_TYPE_REQUIRED = "REQUIRED";
    private static final String FORM_POLICY_APPROVAL_DIRECT = "DIRECT";
    private static final String FORM_POLICY_SLOT_CODE = "EDHR_ROUTE_FORM";
    private static final String INSTANCE_SCOPE_PROCESS = "PROCESS";
    private static final String INSTANCE_SCOPE_BATCH_SHARED = "BATCH_SHARED";
    private static final String SLOT_TYPE_MAIN = "MAIN";
    private static final String SLOT_TYPE_PROCESS_INSPECTION = "PROCESS_INSPECTION";
    private static final String SLOT_TYPE_PARAMETER_RECORD = "PARAMETER_RECORD";
    private static final String RECORD_CATEGORY_BATCH = "BATCH_RECORD";
    private static final String RECORD_CATEGORY_INTERNAL = "INTERNAL_RECORD";
    private static final String VALIDATION_PROFILE_BATCH = "CONTROLLED_BATCH";
    private static final String VALIDATION_PROFILE_INTERNAL = "INTERNAL_TRACE";
    private static final String REQUIRED_POLICY_REQUIRED = "REQUIRED";
    private static final String RULE_TYPE_FILL = "FILL";
    private static final String CANDIDATE_SOURCE_TYPE_USER = "USER";
    private static final String CANDIDATE_SOURCE_TYPE_USERS = "USERS";
    private static final String CANDIDATE_SOURCE_TYPE_ROLE = "ROLE";
    private static final String COMPLETION_POLICY_ANY_ONE = "ANY_ONE";
    private static final int UNLIMITED_DUE_MINUTES = Integer.MAX_VALUE;
    private static final String ARCHIVE_VISIBILITY_FINAL_DHR = "FINAL_DHR";
    private static final String OWNER_ROLE_PRODUCTION = "PRODUCTION";
    private static final String OWNER_ROLE_QUALITY = "QUALITY";
    private static final String OWNER_ROLE_EQUIPMENT = "EQUIPMENT";

    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProRouteProcessMapper routeProcessMapper;
    @Resource
    private MesProRouteProcessFlowEdgeMapper flowEdgeMapper;
    @Resource
    private MesProRouteProcessFlowBoundaryEdgeMapper boundaryEdgeMapper;
    @Resource
    private MesProRouteProcessFlowLayoutMapper flowLayoutMapper;
    @Resource
    private MesProRouteProductMapper routeProductMapper;
    @Resource
    private MesProRouteProductBomMapper routeProductBomMapper;
    @Resource
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Resource
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Resource
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Resource
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Resource
    private MesProProcessMapper processMapper;
    @Resource
    private MesMdItemMapper itemMapper;
    @Resource
    private FormCenterRuntimeService formCenterRuntimeService;
    @Resource
    private FormActionPolicyMapper formActionPolicyMapper;
    @Resource
    private BusinessApprovalPolicyMapper businessApprovalPolicyMapper;
    @Resource
    private MesProEdhrProcessFormPermissionRuleMapper processFormPermissionRuleMapper;

    @Transactional(rollbackFor = Exception.class)
    public void projectCandidate(MesProRouteVersionDO candidate) {
        JSONObject snapshot = parseSnapshot(candidate);
        Long routeId = requireRouteId(candidate);
        JSONObject configSnapshots = requireObject(snapshot, SNAPSHOT_CONFIGS_KEY);
        JSONObject flowGraph = requireObject(configSnapshots, FLOW_GRAPH_KEY);
        JSONArray nodes = requireFrozenFlowNodes(flowGraph);

        updateRoute(routeId, snapshot);
        RouteProcessProjection routeProcesses = projectProcesses(routeId, nodes);
        projectFlowEdges(routeId, flowGraph, routeProcesses);
        projectBoundaryEdges(routeId, flowGraph, routeProcesses);
        projectLayouts(routeId, flowGraph, routeProcesses);
        projectProducts(routeId, configSnapshots.get(PRODUCTS_KEY));
        projectProductBoms(routeId, configSnapshots.get(PRODUCT_BOMS_KEY));
        projectScheduleConfigs(candidate.getId(), configSnapshots.get(SCHEDULE_CONFIGS_KEY), routeProcesses);
        projectUseConfigs(candidate.getId(), routeId, BATCH_USE_TYPE,
                configSnapshots.getJSONArray(BATCH_USE_CONFIGS_KEY), routeProcesses);
        projectUseConfigs(candidate.getId(), routeId, SCHEDULE_USE_TYPE,
                configSnapshots.getJSONArray(SCHEDULE_USE_CONFIGS_KEY), routeProcesses);
    }

    private JSONObject parseSnapshot(MesProRouteVersionDO candidate) {
        if (candidate == null || StrUtil.isBlank(candidate.getRouteSnapshotJson())) {
            throw new IllegalArgumentException("route version snapshot is required");
        }
        return JSONObject.parseObject(candidate.getRouteSnapshotJson());
    }

    private Long requireRouteId(MesProRouteVersionDO candidate) {
        if (candidate == null || candidate.getRouteId() == null) {
            throw new IllegalArgumentException("route id is required");
        }
        return candidate.getRouteId();
    }

    private JSONObject requireObject(JSONObject parent, String key) {
        JSONObject value = parent.getJSONObject(key);
        if (value == null) {
            throw new IllegalArgumentException("route snapshot missing " + key);
        }
        return value;
    }

    private JSONArray requireFrozenFlowNodes(JSONObject flowGraph) {
        JSONArray nodes = flowGraph.getJSONArray("nodes");
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("flowGraph nodes are required");
        }
        return nodes;
    }

    private void updateRoute(Long routeId, JSONObject snapshot) {
        routeMapper.updateById(MesProRouteDO.builder()
                .id(routeId)
                .code(requireText(snapshot, "routeCode"))
                .name(requireText(snapshot, "routeName"))
                .status(snapshot.getInteger("status"))
                .build());
    }

    private RouteProcessProjection projectProcesses(Long routeId, JSONArray nodes) {
        routeProcessMapper.deleteByRouteId(routeId);
        List<JSONObject> processSnapshots = nodes.stream().map(JSONObject.class::cast).toList();
        Map<Integer, MesProRouteProcessDO> routeProcessBySort = new LinkedHashMap<>();
        Map<Long, MesProRouteProcessDO> routeProcessByOriginalId = new LinkedHashMap<>();
        processSnapshots.stream()
                .sorted(Comparator.comparing(node -> node.getInteger("sort")))
                .forEach(node -> {
                    Integer sort = node.getInteger("sort");
                    MesProProcessDO process = requireProcess(node);
                    MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                            .routeId(routeId)
                            .processId(process.getId())
                            .sort(sort)
                            .keyFlag(node.getBoolean("keyFlag"))
                            .checkFlag(node.getBoolean("checkFlag"))
                            .build();
                    routeProcessMapper.insert(routeProcess);
                    routeProcess = requireProjectedRouteProcessId(routeId, sort, routeProcess);
                    routeProcessBySort.put(sort, routeProcess);
                    Long originalRouteProcessId = node.getLong("routeProcessId");
                    if (originalRouteProcessId != null) {
                        routeProcessByOriginalId.put(originalRouteProcessId, routeProcess);
                    }
                    Long clientRouteProcessId = node.getLong("clientRouteProcessId");
                    if (clientRouteProcessId != null) {
                        routeProcessByOriginalId.put(clientRouteProcessId, routeProcess);
                    }
                });
        return new RouteProcessProjection(routeProcessBySort, routeProcessByOriginalId);
    }

    private MesProRouteProcessDO requireProjectedRouteProcessId(Long routeId, Integer sort,
                                                                MesProRouteProcessDO routeProcess) {
        if (routeProcess != null && routeProcess.getId() != null) {
            return routeProcess;
        }
        MesProRouteProcessDO persistedRouteProcess = routeProcessMapper.selectByRouteIdAndSort(routeId, sort);
        if (persistedRouteProcess == null || persistedRouteProcess.getId() == null) {
            throw new IllegalStateException("projected route process id is required: routeId="
                    + routeId + ", sort=" + sort);
        }
        return persistedRouteProcess;
    }

    private MesProProcessDO requireProcess(JSONObject node) {
        Long processId = node.getLong("processId");
        if (processId != null) {
            return MesProProcessDO.builder().id(processId).build();
        }
        String processName = requireText(node, "processName");
        MesProProcessDO process = processMapper.selectByName(processName);
        if (process == null || process.getId() == null) {
            throw new IllegalArgumentException("process does not exist: " + processName);
        }
        return process;
    }

    private void projectFlowEdges(Long routeId, JSONObject flowGraph,
                                  RouteProcessProjection routeProcesses) {
        flowEdgeMapper.deleteByRouteId(routeId);
        JSONArray edges = flowGraph.getJSONArray("edges");
        if (edges == null) {
            return;
        }
        Long graphVersion = flowGraph.getLong("graphVersion");
        List<IndexedFlowEdge> indexedEdges = new ArrayList<>();
        for (int index = 0; index < edges.size(); index++) {
            JSONObject edge = edges.getJSONObject(index);
            Integer sort = edge.getInteger("sort");
            indexedEdges.add(new IndexedFlowEdge(edge, sort == null ? index + 1 : sort, index));
        }
        indexedEdges.stream()
                .sorted(Comparator.comparingInt(IndexedFlowEdge::sort)
                        .thenComparingInt(IndexedFlowEdge::index))
                .forEach(indexedEdge -> {
                    JSONObject edge = indexedEdge.edge();
                    flowEdgeMapper.insert(MesProRouteProcessFlowEdgeDO.builder()
                        .routeId(routeId)
                        .graphVersion(graphVersion)
                        .sourceRouteProcessId(requireRouteProcess(routeProcesses, edge, "sourceSort", "sourceRouteProcessId").getId())
                        .targetRouteProcessId(requireRouteProcess(routeProcesses, edge, "targetSort", "targetRouteProcessId").getId())
                        .relationType(requireText(edge, "relationType"))
                        .sort(indexedEdge.sort())
                        .build());
                });
    }

    private void projectBoundaryEdges(Long routeId, JSONObject flowGraph, RouteProcessProjection routeProcesses) {
        boundaryEdgeMapper.deleteByRouteId(routeId);
        JSONArray boundaryEdges = flowGraph.getJSONArray("boundaryEdges");
        if (boundaryEdges == null) {
            return;
        }
        Long graphVersion = flowGraph.getLong("graphVersion");
        for (int index = 0; index < boundaryEdges.size(); index++) {
            JSONObject edge = boundaryEdges.getJSONObject(index);
            boundaryEdgeMapper.insert(MesProRouteProcessFlowBoundaryEdgeDO.builder()
                    .routeId(routeId)
                    .graphVersion(graphVersion)
                    .boundaryType(requireText(edge, "boundaryType"))
                    .routeProcessId(requireRouteProcess(routeProcesses, edge, "routeProcessSort", "routeProcessId").getId())
                    .sort(edge.getInteger("sort") == null ? index + 1 : edge.getInteger("sort"))
                    .build());
        }
    }

    private void projectLayouts(Long routeId, JSONObject flowGraph, RouteProcessProjection routeProcesses) {
        flowLayoutMapper.deleteByRouteId(routeId);
        JSONArray layouts = flowGraph.getJSONArray("layouts");
        if (layouts == null) {
            return;
        }
        Long graphVersion = flowGraph.getLong("graphVersion");
        layouts.stream()
                .map(JSONObject.class::cast)
                .forEach(layout -> flowLayoutMapper.insert(MesProRouteProcessFlowLayoutDO.builder()
                        .routeId(routeId)
                        .routeProcessId(requireRouteProcess(routeProcesses, layout, "routeProcessSort", "routeProcessId").getId())
                        .x(layout.getInteger("x"))
                        .y(layout.getInteger("y"))
                        .width(layout.getInteger("width"))
                        .height(layout.getInteger("height"))
                        .graphVersion(graphVersion)
                        .build()));
    }

    private void projectProducts(Long routeId, Object productSnapshot) {
        routeProductMapper.deleteByRouteId(routeId);
        if (productSnapshot == null) {
            return;
        }
        if (productSnapshot instanceof JSONObject productsByKey) {
            for (Object value : productsByKey.values()) {
                insertProductSnapshot(routeId, (JSONObject) value);
            }
            return;
        }
        for (Object value : (JSONArray) productSnapshot) {
            if (value instanceof JSONObject product) {
                insertProductSnapshot(routeId, product);
                continue;
            }
            String productName = String.valueOf(value);
            List<MesMdItemDO> products = itemMapper.selectListByName(productName);
            if (products == null || products.isEmpty() || products.get(0).getId() == null) {
                throw new IllegalArgumentException("product does not exist: " + productName);
            }
            routeProductMapper.insert(MesProRouteProductDO.builder()
                    .routeId(routeId)
                    .itemId(products.get(0).getId())
                    .build());
        }
    }

    private void insertProductSnapshot(Long routeId, JSONObject product) {
        Long itemId = product.getLong("itemId");
        if (itemId == null) {
            throw new IllegalArgumentException("product item id is required");
        }
        routeProductMapper.insert(MesProRouteProductDO.builder()
                .routeId(routeId)
                .itemId(itemId)
                .quantity(product.getInteger("quantity"))
                .productionTime(product.getBigDecimal("productionTime"))
                .timeUnitType(product.getString("timeUnitType"))
                .remark(product.getString("remark"))
                .build());
    }

    private void projectProductBoms(Long routeId, Object productBomSnapshot) {
        if (productBomSnapshot == null) {
            return;
        }
        routeProductBomMapper.deleteByRouteId(routeId);
        if (productBomSnapshot instanceof JSONObject bomsByKey) {
            for (Object value : bomsByKey.values()) {
                insertProductBomSnapshot(routeId, (JSONObject) value);
            }
            return;
        }
        for (Object value : (JSONArray) productBomSnapshot) {
            insertProductBomSnapshot(routeId, (JSONObject) value);
        }
    }

    private void insertProductBomSnapshot(Long routeId, JSONObject bom) {
        Long processId = bom.getLong("processId");
        Long productId = bom.getLong("productId");
        Long itemId = bom.getLong("itemId");
        if (processId == null || productId == null || itemId == null) {
            throw new IllegalArgumentException("product bom process/product/item id is required");
        }
        routeProductBomMapper.insert(MesProRouteProductBomDO.builder()
                .routeId(routeId)
                .processId(processId)
                .productId(productId)
                .itemId(itemId)
                .quantity(bom.getBigDecimal("quantity"))
                .remark(bom.getString("remark"))
                .build());
    }

    private void projectScheduleConfigs(Long routeVersionId, Object configSnapshot,
                                        RouteProcessProjection routeProcesses) {
        routeScheduleConfigMapper.deleteByRouteVersionId(routeVersionId);
        if (configSnapshot == null) {
            return;
        }
        if (configSnapshot instanceof JSONObject configsByRouteProcessId) {
            for (Map.Entry<String, Object> entry : configsByRouteProcessId.entrySet()) {
                JSONObject config = (JSONObject) entry.getValue();
                if (config.getLong("routeProcessId") == null) {
                    config.put("routeProcessId", Long.valueOf(entry.getKey()));
                }
                insertScheduleConfig(routeVersionId, config, routeProcesses);
            }
            return;
        }
        JSONArray configs = (JSONArray) configSnapshot;
        for (Object value : configs) {
            insertScheduleConfig(routeVersionId, (JSONObject) value, routeProcesses);
        }
    }

    private void insertScheduleConfig(Long routeVersionId, JSONObject config,
                                      RouteProcessProjection routeProcesses) {
        MesProRouteProcessDO routeProcess = requireConfigRouteProcess(config, routeProcesses);
        routeScheduleConfigMapper.insert(MesProRouteScheduleConfigDO.builder()
                .routeVersionId(routeVersionId)
                .routeProcessId(routeProcess.getId())
                .capacityMode(requireText(config, "capacityMode"))
                .hourlyCapacity(config.getBigDecimal("hourlyCapacity"))
                .infiniteDurationQuantityFactor(config.getBigDecimal("infiniteDurationQuantityFactor"))
                .infiniteDurationBaseMinutes(config.getBigDecimal("infiniteDurationBaseMinutes"))
                .nightShiftEnabled(config.getBoolean("nightShiftEnabled"))
                .calendarRuleId(config.getLong("calendarRuleId"))
                .configVersion(config.getString("configVersion"))
                .remark(config.getString("remark"))
                .build());
    }

    private void projectUseConfigs(Long routeVersionId, Long routeId, String useType, JSONArray configs,
                                   RouteProcessProjection routeProcesses) {
        routeFlowConfigMapper.deleteByRouteIdAndUseType(routeId, useType);
        routeFlowProcessConfigMapper.deleteByRouteIdAndUseType(routeId, useType);
        routeFlowProcessBatchRecordMapper.deleteByRouteIdAndUseType(routeId, useType);
        if (configs == null || configs.isEmpty()) {
            return;
        }
        MesProRouteFlowConfigDO flowConfig = MesProRouteFlowConfigDO.builder()
                .routeId(routeId)
                .useType(useType)
                .enabled(Boolean.TRUE)
                .build();
        routeFlowConfigMapper.insert(flowConfig);
        configs.stream()
                .map(JSONObject.class::cast)
                .forEach(config -> projectUseConfig(routeVersionId, routeId, flowConfig, useType, config, routeProcesses));
    }

    private void projectUseConfig(Long routeVersionId, Long routeId, MesProRouteFlowConfigDO flowConfig, String useType,
                                  JSONObject config,
                                   RouteProcessProjection routeProcesses) {
        MesProRouteProcessDO routeProcess = requireConfigRouteProcess(config, routeProcesses);
        MesProRouteFlowProcessConfigDO processConfig = MesProRouteFlowProcessConfigDO.builder()
                .routeFlowConfigId(flowConfig.getId())
                .routeId(routeId)
                .routeProcessId(routeProcess.getId())
                .useType(useType)
                .enabled(config.getBoolean("enabled"))
                .executionMode(requireText(config, "executionMode"))
                .productionQuantityFactor(config.getBigDecimal("productionQuantityFactor"))
                .batchRecordReportId(null)
                .remark(config.getString("remark"))
                .build();
        routeFlowProcessConfigMapper.insert(processConfig);
        if (!BATCH_USE_TYPE.equals(useType)) {
            return;
        }
        for (JSONObject binding : resolveProjectedFormBindings(config)) {
            Long formTemplateId = resolveProjectedFormTemplateId(binding);
            String formBindingKey = resolveProjectedFormBindingKey(routeProcess.getId(), binding);
            String instanceScope = resolveProjectedInstanceScope(binding);
            String recordCategory = resolveProjectedRecordCategory(binding, SLOT_TYPE_MAIN);
            String validationProfile = resolveProjectedValidationProfile(binding, recordCategory);
            String requiredPolicy = resolveProjectedRequiredPolicy(binding);
            String ownerRoleKey = resolveProjectedOwnerRoleKey(binding, SLOT_TYPE_MAIN);
            String archiveVisibility = resolveProjectedArchiveVisibility(binding);
            String candidateSourceType = resolveProjectedCandidateSourceType(binding);
            List<Long> candidateSourceIds = resolveProjectedCandidateSourceIds(binding);
            validateProjectedCandidateSourceOverride(formBindingKey, candidateSourceType, candidateSourceIds);
            String candidateSourceNames = resolveProjectedCandidateSourceNames(binding);
            routeFlowProcessBatchRecordMapper.insert(MesProRouteFlowProcessBatchRecordDO.builder()
                    .routeFlowProcessConfigId(processConfig.getId())
                    .routeId(routeId)
                    .routeProcessId(routeProcess.getId())
                    .useType(useType)
                    .batchRecordReportId(null)
                    .batchRecordDefinitionId(null)
                    .batchRecordVersionId(null)
                    .formSlotType(null)
                    .formBindingKey(formBindingKey)
                    .formTemplateId(formTemplateId)
                    .formTemplateNameSnapshot(requireText(binding, "formTemplateName"))
                    .lastPublishedTemplateVersionId(binding.getLong("lastPublishedTemplateVersionId"))
                    .lastPublishedTemplateVersionNo(requireText(binding, "lastPublishedTemplateVersionNo"))
                    .instanceScope(instanceScope)
                    .sharedFormKey(StrUtil.blankToDefault(StrUtil.trim(binding.getString("sharedFormKey")), null))
                    .fillableScopeJson(StrUtil.blankToDefault(StrUtil.trim(binding.getString("fillableScopeJson")), null))
                    .recordCategory(recordCategory)
                    .validationProfile(validationProfile)
                    .permissionScopeId(binding.getLong("permissionScopeId"))
                    .recordCategorySnapshotHash(binding.getString("recordCategorySnapshotHash"))
                    .requiredPolicy(requiredPolicy)
                    .requiredConditionJson(binding.getString("requiredConditionJson"))
                    .ownerRoleKey(ownerRoleKey)
                    .archiveVisibility(archiveVisibility)
                    .slotConfigSnapshotHash(binding.getString("slotConfigSnapshotHash"))
                    .candidateSourceType(candidateSourceType)
                    .candidateSourceIds(joinIds(candidateSourceIds))
                    .candidateSourceNames(candidateSourceNames)
                    .reportSort(resolveProjectedReportSort(binding))
                    .remark(binding.getString("remark"))
                    .build());
            syncRouteFormPolicy(routeVersionId, formBindingKey, formTemplateId, requiredPolicy);
            syncRouteFormFillRule(routeProcess.getId(), formBindingKey, candidateSourceType, candidateSourceIds);
        }
    }

    private List<JSONObject> resolveProjectedFormBindings(JSONObject config) {
        JSONArray reports = config.getJSONArray("formBindings");
        if (reports == null || reports.isEmpty()) {
            return List.of();
        }
        List<JSONObject> result = new ArrayList<>();
        for (Object value : reports) {
            if (!(value instanceof JSONObject binding)) {
                throw new IllegalArgumentException("formBindings must contain objects");
            }
            if (binding.getLong("formTemplateId") != null) {
                result.add(binding);
            }
        }
        return result;
    }

    private Long resolveProjectedFormTemplateId(JSONObject binding) {
        Long formTemplateId = binding.getLong("formTemplateId");
        if (formTemplateId == null) {
            throw new IllegalArgumentException("formTemplateId is required");
        }
        return formTemplateId;
    }

    private String resolveProjectedFormBindingKey(Long routeProcessId, JSONObject binding) {
        String formBindingKey = StrUtil.trim(binding.getString("formBindingKey"));
        if (StrUtil.isNotBlank(formBindingKey)) {
            return formBindingKey;
        }
        Long formTemplateId = resolveProjectedFormTemplateId(binding);
        if (routeProcessId == null) {
            throw new IllegalArgumentException("routeProcessId is required for form binding key");
        }
        return "FB_" + routeProcessId + "_" + formTemplateId;
    }

    private Integer resolveProjectedReportSort(JSONObject binding) {
        Integer reportSort = binding.getInteger("reportSort");
        if (reportSort == null || reportSort <= 0) {
            throw new IllegalArgumentException("form binding reportSort is required");
        }
        return reportSort;
    }

    private String resolveProjectedCandidateSourceType(JSONObject binding) {
        String sourceType = StrUtil.trim(binding.getString("candidateSourceType"));
        if (StrUtil.isBlank(sourceType)) {
            return null;
        }
        if (CANDIDATE_SOURCE_TYPE_USER.equals(sourceType) || CANDIDATE_SOURCE_TYPE_USERS.equals(sourceType)) {
            return CANDIDATE_SOURCE_TYPE_USERS;
        }
        if (CANDIDATE_SOURCE_TYPE_ROLE.equals(sourceType)) {
            return CANDIDATE_SOURCE_TYPE_ROLE;
        }
        throw new IllegalArgumentException("form binding candidateSourceType is invalid");
    }

    private List<Long> resolveProjectedCandidateSourceIds(JSONObject binding) {
        JSONArray rawIds = binding.getJSONArray("candidateSourceIds");
        if (rawIds == null || rawIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (Object rawId : rawIds) {
            if (rawId == null) {
                continue;
            }
            Long id = Long.valueOf(String.valueOf(rawId));
            if (id > 0 && !ids.contains(id)) {
                ids.add(id);
            }
        }
        if (ids.size() != 1) {
            throw new IllegalArgumentException("form binding must have exactly one candidate source id");
        }
        return ids;
    }

    private void validateProjectedCandidateSourceOverride(String formBindingKey,
                                                          String candidateSourceType,
                                                          List<Long> candidateSourceIds) {
        if (StrUtil.isBlank(candidateSourceType) || candidateSourceIds.size() != 1) {
            throw new IllegalArgumentException("form binding filler is required: " + formBindingKey);
        }
    }

    private String resolveProjectedCandidateSourceNames(JSONObject binding) {
        JSONArray names = binding.getJSONArray("candidateSourceNames");
        return names == null ? JSON.toJSONString(List.of()) : JSON.toJSONString(names);
    }

    private String joinIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (Long id : ids) {
            values.add(String.valueOf(id));
        }
        return String.join(",", values);
    }

    private String resolveProjectedInstanceScope(JSONObject report) {
        return StrUtil.blankToDefault(StrUtil.trim(report.getString("instanceScope")), INSTANCE_SCOPE_PROCESS);
    }

    private String resolveProjectedRecordCategory(JSONObject report, String formSlotType) {
        String recordCategory = StrUtil.trim(report.getString("recordCategory"));
        if (StrUtil.isBlank(recordCategory)
                || isExtraSlot(formSlotType)
                && RECORD_CATEGORY_BATCH.equals(recordCategory)
                && (StrUtil.isBlank(report.getString("ownerRoleKey"))
                        || OWNER_ROLE_PRODUCTION.equals(report.getString("ownerRoleKey")))) {
            return defaultRecordCategory(formSlotType);
        }
        return recordCategory;
    }

    private String resolveProjectedValidationProfile(JSONObject report, String recordCategory) {
        String profile = StrUtil.trim(report.getString("validationProfile"));
        String expectedProfile = defaultValidationProfile(recordCategory);
        if (StrUtil.isBlank(profile)
                || RECORD_CATEGORY_INTERNAL.equals(recordCategory) && VALIDATION_PROFILE_BATCH.equals(profile)) {
            return expectedProfile;
        }
        return profile;
    }

    private String resolveProjectedRequiredPolicy(JSONObject report) {
        return StrUtil.blankToDefault(StrUtil.trim(report.getString("requiredPolicy")), REQUIRED_POLICY_REQUIRED);
    }

    private String resolveProjectedOwnerRoleKey(JSONObject report, String formSlotType) {
        String roleKey = StrUtil.trim(report.getString("ownerRoleKey"));
        if (StrUtil.isBlank(roleKey) || isExtraSlot(formSlotType) && OWNER_ROLE_PRODUCTION.equals(roleKey)) {
            return defaultOwnerRoleKey(formSlotType);
        }
        return roleKey;
    }

    private String resolveProjectedArchiveVisibility(JSONObject report) {
        return StrUtil.blankToDefault(StrUtil.trim(report.getString("archiveVisibility")),
                ARCHIVE_VISIBILITY_FINAL_DHR);
    }

    private String defaultRecordCategory(String formSlotType) {
        return isExtraSlot(formSlotType) ? RECORD_CATEGORY_INTERNAL : RECORD_CATEGORY_BATCH;
    }

    private String defaultValidationProfile(String recordCategory) {
        return RECORD_CATEGORY_INTERNAL.equals(recordCategory) ? VALIDATION_PROFILE_INTERNAL : VALIDATION_PROFILE_BATCH;
    }

    private String defaultOwnerRoleKey(String formSlotType) {
        if (SLOT_TYPE_PROCESS_INSPECTION.equals(formSlotType)) {
            return OWNER_ROLE_QUALITY;
        }
        if (SLOT_TYPE_PARAMETER_RECORD.equals(formSlotType)) {
            return OWNER_ROLE_EQUIPMENT;
        }
        return OWNER_ROLE_PRODUCTION;
    }

    private boolean isExtraSlot(String formSlotType) {
        return !SLOT_TYPE_MAIN.equals(formSlotType);
    }

    private void syncRouteFormPolicy(Long routeVersionId, String formBindingKey, Long formTemplateId,
                                     String requiredPolicy) {
        String actionCode = routeFormActionCode(routeVersionId, formBindingKey);
        syncRouteFormCenterPolicy(actionCode, formTemplateId, requiredPolicy);
        syncRouteBusinessApprovalPolicy(actionCode);
    }

    private void syncRouteFormCenterPolicy(String actionCode, Long formTemplateId, String requiredPolicy) {
        disablePublishedRouteFormPolicies(actionCode);
        FormPolicyRespVO policy = formCenterRuntimeService.savePolicy(buildRouteFormPolicyReq(
                actionCode, formTemplateId, requiredPolicy));
        formCenterRuntimeService.publishPolicy(policy.getId());
    }

    private void syncRouteBusinessApprovalPolicy(String actionCode) {
        disablePublishedRouteBusinessApprovalPolicies(actionCode);
        businessApprovalPolicyMapper.insert(buildRouteBusinessApprovalPolicy(actionCode));
    }

    private FormPolicySaveReqVO buildRouteFormPolicyReq(String actionCode, Long formTemplateId,
                                                        String requiredPolicy) {
        FormPolicySlotReqVO slot = new FormPolicySlotReqVO();
        slot.setSlotCode(FORM_POLICY_SLOT_CODE);
        slot.setRequired(REQUIRED_POLICY_REQUIRED.equals(requiredPolicy));
        slot.setTemplateId(formTemplateId);
        FormPolicySaveReqVO reqVO = new FormPolicySaveReqVO();
        reqVO.setDataDomain(FORM_POLICY_DATA_DOMAIN);
        reqVO.setSystemCode(FORM_POLICY_SYSTEM_CODE);
        reqVO.setObjectType(FORM_POLICY_OBJECT_TYPE);
        reqVO.setActionCode(actionCode);
        reqVO.setObjectState(FORM_POLICY_OBJECT_STATE);
        reqVO.setPolicyType(FORM_POLICY_TYPE_REQUIRED);
        reqVO.setApprovalMode(FORM_POLICY_APPROVAL_DIRECT);
        reqVO.setEffectExecutorCode(MesProEdhrRouteFormFillEffectExecutor.EXECUTOR_CODE);
        reqVO.setSlots(List.of(slot));
        reqVO.setRemark("MES route dynamic eDHR form");
        return reqVO;
    }

    private void disablePublishedRouteFormPolicies(String actionCode) {
        for (FormActionPolicyDO policy : formActionPolicyMapper.selectPublishedByAction(
                TenantContextHolder.getRequiredTenantId(), FORM_POLICY_DATA_DOMAIN, FORM_POLICY_SYSTEM_CODE,
                FORM_POLICY_OBJECT_TYPE, actionCode, FORM_POLICY_OBJECT_STATE)) {
            policy.setStatus("DISABLED");
            formActionPolicyMapper.updateById(policy);
        }
    }

    private BusinessApprovalPolicyDO buildRouteBusinessApprovalPolicy(String actionCode) {
        return BusinessApprovalPolicyDO.builder()
                .tenantId(TenantContextHolder.getRequiredTenantId())
                .dataDomain(FORM_POLICY_DATA_DOMAIN)
                .systemCode(FORM_POLICY_SYSTEM_CODE)
                .objectType(FORM_POLICY_OBJECT_TYPE)
                .actionCode(actionCode)
                .objectState(FORM_POLICY_OBJECT_STATE)
                .policyMode(BusinessApprovalPolicyMode.DIRECT.name())
                .effectExecutorCode(MesProEdhrRouteFormFillEffectExecutor.EXECUTOR_CODE)
                .status(BusinessApprovalPolicy.STATUS_PUBLISHED)
                .remark("MES route dynamic eDHR form")
                .build();
    }

    private void disablePublishedRouteBusinessApprovalPolicies(String actionCode) {
        for (BusinessApprovalPolicyDO policy : businessApprovalPolicyMapper.selectPublishedByAction(
                TenantContextHolder.getRequiredTenantId(), FORM_POLICY_DATA_DOMAIN, FORM_POLICY_SYSTEM_CODE,
                FORM_POLICY_OBJECT_TYPE, actionCode, FORM_POLICY_OBJECT_STATE)) {
            policy.setStatus(BusinessApprovalPolicy.STATUS_DISABLED);
            businessApprovalPolicyMapper.updateById(policy);
        }
    }

    public static String routeFormActionCode(Long routeVersionId, String formBindingKey) {
        if (routeVersionId == null || StrUtil.isBlank(formBindingKey)) {
            throw new IllegalArgumentException("routeVersionId and formBindingKey are required");
        }
        return "EDHR_RF_" + routeVersionId + "_" + formBindingKey;
    }

    private void syncRouteFormFillRule(Long routeProcessId, String formBindingKey,
                                       String candidateSourceType, List<Long> candidateSourceIds) {
        processFormPermissionRuleMapper.physicalDeleteByRouteProcessAndReport(routeProcessId, formBindingKey);
        if (StrUtil.isBlank(candidateSourceType) && candidateSourceIds.isEmpty()) {
            return;
        }
        processFormPermissionRuleMapper.insert(MesProEdhrProcessFormPermissionRuleDO.builder()
                .routeProcessId(routeProcessId)
                .batchRecordReportId(formBindingKey)
                .batchRecordDefinitionId(null)
                .batchRecordVersionId(null)
                .ruleType(RULE_TYPE_FILL)
                .signatureCellKey("")
                .signatureRole(null)
                .candidateSourceType(candidateSourceType)
                .candidateSourceIds(joinIds(candidateSourceIds))
                .completionPolicy(COMPLETION_POLICY_ANY_ONE)
                .dueMinutes(UNLIMITED_DUE_MINUTES)
                .enabled(Boolean.TRUE)
                .remark("MES route dynamic form filler")
                .build());
    }

    private MesProRouteProcessDO requireConfigRouteProcess(JSONObject config,
                                                           RouteProcessProjection routeProcesses) {
        Integer sort = config.getInteger("sort");
        if (sort != null && routeProcesses.bySort().containsKey(sort)) {
            return routeProcesses.bySort().get(sort);
        }
        Long routeProcessId = config.getLong("routeProcessId");
        if (routeProcessId != null && routeProcesses.byOriginalId().containsKey(routeProcessId)) {
            return routeProcesses.byOriginalId().get(routeProcessId);
        }
        String processName = requireText(config, "processName");
        return routeProcesses.bySort().values().stream()
                .filter(routeProcess -> {
                    MesProProcessDO process = processMapper.selectByName(processName);
                    return process != null && process.getId() != null
                            && process.getId().equals(routeProcess.getProcessId());
                })
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("batch config process does not exist: " + processName));
    }

    private MesProRouteProcessDO requireRouteProcess(RouteProcessProjection routeProcesses, JSONObject object,
                                                     String sortKey, String idKey) {
        Integer sort = object.getInteger(sortKey);
        if (sort != null) {
            return requireRouteProcessBySort(routeProcesses.bySort(), sort);
        }
        Long routeProcessId = object.getLong(idKey);
        MesProRouteProcessDO routeProcess = routeProcessId == null ? null : routeProcesses.byOriginalId().get(routeProcessId);
        if (routeProcess == null || routeProcess.getId() == null) {
            throw new IllegalArgumentException("route process id does not exist: " + routeProcessId);
        }
        return routeProcess;
    }

    private MesProRouteProcessDO requireRouteProcessBySort(Map<Integer, MesProRouteProcessDO> routeProcessBySort, Integer sort) {
        MesProRouteProcessDO routeProcess = routeProcessBySort.get(sort);
        if (routeProcess == null || routeProcess.getId() == null) {
            throw new IllegalArgumentException("route process sort does not exist: " + sort);
        }
        return routeProcess;
    }

    private String requireText(JSONObject object, String key) {
        String value = object.getString(key);
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException("route snapshot missing " + key);
        }
        return value;
    }

    @SuppressWarnings("unused")
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper() {
        return routeScheduleConfigMapper;
    }

    private record RouteProcessProjection(Map<Integer, MesProRouteProcessDO> bySort,
                                          Map<Long, MesProRouteProcessDO> byOriginalId) {
    }

    private record IndexedFlowEdge(JSONObject edge, int sort, int index) {
    }
}
