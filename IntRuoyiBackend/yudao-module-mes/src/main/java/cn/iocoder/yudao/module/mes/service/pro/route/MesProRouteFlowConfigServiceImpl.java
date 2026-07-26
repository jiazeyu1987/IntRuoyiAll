package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteBatchRecordAttachmentOwnerInitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteBatchRecordAttachmentOwnerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteBatchRecordAttachmentOwnerSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowBatchRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowBatchRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowFormBindingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowFormBindingSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowProcessConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowProcessConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionGateCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionGateService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordFormSlotType;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleCategoryDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleCategoryMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_ROUTE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_ENABLED_USER_NOT_ENOUGH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_OWNER_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_ROLE_CATEGORY_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_BATCH_REPORT_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_BATCH_REPORT_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_BATCH_REPORT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_CONDITION_CONFIG_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_EXECUTION_MODE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_FILLER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_FILLER_SOURCE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_PUBLISHED_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_FORM_SLOT_TYPE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_RECORD_CATEGORY_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_REQUIRED_POLICY_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_PRODUCTION_QUANTITY_FACTOR_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_VALIDATION_PROFILE_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_TYPE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_PROCESS_IDENTITY_AMBIGUOUS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_PROCESS_IDENTITY_NOT_FOUND;

@Service
@Validated
public class MesProRouteFlowConfigServiceImpl implements MesProRouteFlowConfigService {

    public static final String RECORD_CATEGORY_BATCH = "BATCH_RECORD";
    public static final String RECORD_CATEGORY_INTERNAL = "INTERNAL_RECORD";
    public static final String VALIDATION_PROFILE_BATCH = "CONTROLLED_BATCH";
    public static final String VALIDATION_PROFILE_INTERNAL = "INTERNAL_TRACE";
    public static final String SLOT_TYPE_MAIN = "MAIN";
    public static final String SLOT_TYPE_LOSS_REPORT = "LOSS_REPORT";
    public static final String SLOT_TYPE_PROCESS_INSPECTION = "PROCESS_INSPECTION";
    public static final String SLOT_TYPE_PARAMETER_RECORD = "PARAMETER_RECORD";
    public static final String REQUIRED_POLICY_REQUIRED = "REQUIRED";
    public static final String REQUIRED_POLICY_CONDITIONAL_REQUIRED = "CONDITIONAL_REQUIRED";
    public static final String REQUIRED_POLICY_OPTIONAL = "OPTIONAL";
    public static final String REQUIRED_POLICY_SKIPPABLE_CONTROLLED = "SKIPPABLE_CONTROLLED";
    public static final String ARCHIVE_VISIBILITY_FINAL_DHR = "FINAL_DHR";
    public static final String ARCHIVE_VISIBILITY_INTERNAL_REVIEW = "INTERNAL_REVIEW";
    public static final String ARCHIVE_VISIBILITY_AUDIT_ONLY = "AUDIT_ONLY";
    public static final String ARCHIVE_VISIBILITY_ATTACHMENT_REFERENCE = "ATTACHMENT_REFERENCE";
    public static final String OWNER_ROLE_PRODUCTION = "PRODUCTION";
    public static final String OWNER_ROLE_QUALITY = "QUALITY";
    public static final String OWNER_ROLE_EQUIPMENT = "EQUIPMENT";
    private static final String CANDIDATE_SOURCE_TYPE_USER = "USER";
    private static final String CANDIDATE_SOURCE_TYPE_USERS = "USERS";
    private static final String CANDIDATE_SOURCE_TYPE_ROLE = "ROLE";
    private static final String EXECUTION_MODE_SEQUENTIAL = "SEQUENTIAL";
    private static final String EXECUTION_MODE_PARALLEL = "PARALLEL";
    private static final BigDecimal DEFAULT_PRODUCTION_QUANTITY_FACTOR = new BigDecimal("1.000000");
    private static final String SNAPSHOT_CONFIGS_KEY = "configSnapshots";
    private static final String FLOW_GRAPH_KEY = "flowGraph";
    private static final String BATCH_USE_CONFIGS_KEY = "batchUseConfigs";
    private static final String SCHEDULE_USE_CONFIGS_KEY = "scheduleUseConfigs";
    private static final String BATCH_RECORD_ATTACHMENT_OWNERS_KEY = "batchRecordAttachmentOwners";
    private static final String BATCH_RECORD_ROLE_CATEGORY_CODE = "batch-record";
    private static final int BATCH_RECORD_ATTACHMENT_MIN_USERS = 2;
    private static final int BATCH_RECORD_ATTACHMENT_MAX_USERS = 4;
    private static final Set<String> READABLE_CANDIDATE_STATUSES = Set.of(
            MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT,
            MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL,
            MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH);
    private static final List<BatchRecordAttachmentDefinition> BATCH_RECORD_ATTACHMENT_DEFINITIONS = List.of(
            new BatchRecordAttachmentDefinition("INCOMING_INSPECTION_REPORT", "来料检报告",
                    "BATCH_ATTACHMENT_INCOMING_INSPECTION_REPORT_UPLOAD_1", "来料检报告上传1", 1),
            new BatchRecordAttachmentDefinition("STERILIZATION_REPORT", "灭菌报告",
                    "BATCH_ATTACHMENT_STERILIZATION_REPORT_UPLOAD_1", "灭菌报告上传1", 2),
            new BatchRecordAttachmentDefinition("FINISHED_PRODUCT_INSPECTION_REPORT", "成品检报告",
                    "BATCH_ATTACHMENT_FINISHED_PRODUCT_INSPECTION_REPORT_UPLOAD_1", "成品检报告上传1", 3),
            new BatchRecordAttachmentDefinition("FINISHED_PRODUCT_INSPECTION_RECORD", "成品检记录",
                    "BATCH_ATTACHMENT_FINISHED_PRODUCT_INSPECTION_RECORD_UPLOAD_1", "成品检记录上传1", 4));

    private record BatchRecordAttachmentDefinition(String code, String name, String defaultRoleCode,
                                                   String defaultRoleName, int sort) {
    }

    private enum RouteVersionBatchBindingReadStrategy {
        SNAPSHOT,
        CURRENT_PROCESS_SETTINGS
    }

    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProRouteProcessMapper routeProcessMapper;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesProProcessMapper processMapper;
    @Resource
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Resource
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Resource
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Resource
    private MesProBatchRecordReportMapper batchRecordReportMapper;
    @Resource
    private FormTemplateVersionMapper formTemplateVersionMapper;
    @Resource
    private MesProEdhrPermissionGateService permissionGateService;
    @Resource
    private MesProRouteCandidateConfigService routeCandidateConfigService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private RoleApi roleApi;
    @Resource
    private AdminUserService adminUserService;
    @Resource
    private RoleService roleService;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private RoleCategoryMapper roleCategoryMapper;
    @Resource
    private PermissionService permissionService;

    @Override
    public List<MesProRouteFlowProcessConfigRespVO> getRouteFlowProcessConfigList(Long routeId, String useType) {
        return getRouteFlowProcessConfigList(routeId, useType, null);
    }

    @Override
    public List<MesProRouteFlowProcessConfigRespVO> getRouteFlowProcessConfigList(
            Long routeId, String useType, Long routeVersionId) {
        MesProRouteFlowConfigTypeEnum flowConfigType = validateUseType(useType);
        validateRouteExists(routeId);
        if (routeVersionId != null) {
            MesProRouteVersionDO routeVersion = requireReadableRouteVersion(routeVersionId, routeId);
            if (isReadableCandidate(routeVersion)) {
                return getRouteVersionSnapshotFlowProcessConfigList(
                        routeVersion, flowConfigType, resolveCandidateBatchBindingReadStrategy(routeVersion));
            }
            if (isPublishedSnapshotRouteVersion(routeVersion)) {
                return getRouteVersionSnapshotFlowProcessConfigList(
                        routeVersion, flowConfigType, RouteVersionBatchBindingReadStrategy.SNAPSHOT);
            }
        }
        MesProRouteVersionDO activeRouteVersion = routeVersionMapper.selectActiveByRouteId(routeId);
        if (isActiveRouteVersion(activeRouteVersion)) {
            return getRouteVersionSnapshotFlowProcessConfigList(
                    activeRouteVersion, flowConfigType, RouteVersionBatchBindingReadStrategy.SNAPSHOT);
        }
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(routeId);
        Map<Long, MesProRouteProcessDO> routeProcessMap =
                convertMap(routeProcesses, MesProRouteProcessDO::getId);
        Map<Long, MesProProcessDO> processMap = convertMap(
                processMapper.selectBatchIds(convertSet(routeProcesses, MesProRouteProcessDO::getProcessId)),
                MesProProcessDO::getId);
        MesProRouteFlowConfigDO flowConfig =
                routeFlowConfigMapper.selectByRouteIdAndUseType(routeId, flowConfigType.getType());
        List<MesProRouteFlowProcessConfigDO> ownedProcessConfigs =
                routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(routeId, flowConfigType.getType()).stream()
                        .filter(config -> MesProRouteFlowContextMatcher.isProcessConfigOwnedBy(
                                flowConfig, config, routeId, flowConfigType.getType()))
                        .toList();
        Map<Long, MesProRouteFlowProcessConfigDO> configBySnapshotRouteProcessId =
                convertMap(ownedProcessConfigs, MesProRouteFlowProcessConfigDO::getRouteProcessId);
        Map<Long, MesProRouteFlowProcessConfigDO> flowProcessConfigMap = new LinkedHashMap<>();
        for (MesProRouteFlowProcessConfigDO config : ownedProcessConfigs) {
            Long currentRouteProcessId = routeProcessMap.containsKey(config.getRouteProcessId())
                    ? config.getRouteProcessId()
                    : routeProcessService.resolveCurrentRouteProcess(
                            config.getRouteProcessId(), routeId, null).getId();
            flowProcessConfigMap.putIfAbsent(currentRouteProcessId, config);
        }
        Boolean routeConfigEnabled = Boolean.TRUE;
        List<MesProRouteFlowProcessBatchRecordDO> batchRecords =
                flowConfigType == MesProRouteFlowConfigTypeEnum.BATCH
                        ? routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(routeId, flowConfigType.getType())
                        : Collections.emptyList();
        batchRecords = batchRecords.stream()
                .filter(record -> isOwnedByProcessConfig(
                        record, configBySnapshotRouteProcessId.get(record.getRouteProcessId()),
                        routeId, flowConfigType.getType()))
                .toList();
        Map<Long, List<MesProRouteFlowProcessBatchRecordDO>> batchRecordMap = batchRecords.stream()
                .collect(Collectors.groupingBy(record -> routeProcessMap.containsKey(record.getRouteProcessId())
                                ? record.getRouteProcessId()
                                : routeProcessService.resolveCurrentRouteProcess(
                                        record.getRouteProcessId(), routeId, null).getId(),
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, MesProBatchRecordReportDO> reportMap = loadReportMap(batchRecords.stream()
                .map(MesProRouteFlowProcessBatchRecordDO::getBatchRecordReportId)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        List<MesProRouteFlowProcessConfigRespVO> result = new ArrayList<>(routeProcesses.size());
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            MesProRouteFlowProcessConfigRespVO vo = new MesProRouteFlowProcessConfigRespVO();
            vo.setRouteProcessId(routeProcess.getId());
            vo.setSort(routeProcess.getSort());
            vo.setUseType(flowConfigType.getType());
            vo.setRouteConfigEnabled(routeConfigEnabled);
            vo.setBaseBatchRecordReportId(routeProcess.getBatchRecordReportId());
            MesProProcessDO process = processMap.get(routeProcess.getProcessId());
            if (process != null) {
                vo.setProcessCode(process.getCode());
                vo.setProcessName(process.getName());
            }
            MesProRouteFlowProcessConfigDO config = flowProcessConfigMap.get(routeProcess.getId());
            if (config == null) {
                vo.setEnabled(Boolean.TRUE);
                vo.setExecutionMode(null);
                vo.setProductionQuantityFactor(DEFAULT_PRODUCTION_QUANTITY_FACTOR);
                vo.setBatchRecordReports(Collections.emptyList());
                vo.setFormBindings(Collections.emptyList());
                result.add(vo);
                continue;
            }
            vo.setEnabled(Boolean.TRUE);
            vo.setExecutionMode(null);
            vo.setProductionQuantityFactor(resolveProductionQuantityFactor(
                    config.getRouteProcessId(), config.getProductionQuantityFactor()));
            vo.setBatchRecordReports(toBatchRecordRespList(
                    batchRecordMap.getOrDefault(routeProcess.getId(), Collections.emptyList()), reportMap));
            vo.setFormBindings(toFormBindingRespList(
                    batchRecordMap.getOrDefault(routeProcess.getId(), Collections.emptyList())));
            vo.setRemark(config.getRemark());
            result.add(vo);
        }
        return result;
    }

    private List<MesProRouteFlowProcessConfigRespVO> getRouteVersionSnapshotFlowProcessConfigList(
            MesProRouteVersionDO routeVersion,
            MesProRouteFlowConfigTypeEnum flowConfigType,
            RouteVersionBatchBindingReadStrategy batchBindingReadStrategy) {
        JSONObject configSnapshots = resolveCandidateConfigSnapshots(routeVersion);
        List<MesProRouteProcessDO> snapshotRouteProcesses = parseCandidateRouteProcessesFromConfigSnapshots(
                routeVersion, configSnapshots);
        Map<Long, MesProProcessDO> snapshotProcessMap = loadProcessMap(snapshotRouteProcesses);
        List<MesProRouteProcessDO> routeProcesses = resolveRouteProcessesForSnapshotRead(
                routeVersion, snapshotRouteProcesses);
        Map<Long, MesProProcessDO> processMap = routeProcesses == snapshotRouteProcesses
                ? snapshotProcessMap
                : loadProcessMap(routeProcesses);
        Map<Long, MesProRouteFlowProcessConfigSaveReqVO> configMap = parseCandidateUseConfigMap(
                routeVersion,
                configSnapshots.get(flowConfigType == MesProRouteFlowConfigTypeEnum.BATCH
                        ? BATCH_USE_CONFIGS_KEY : SCHEDULE_USE_CONFIGS_KEY));
        boolean readCurrentBatchBindings = batchBindingReadStrategy
                == RouteVersionBatchBindingReadStrategy.CURRENT_PROCESS_SETTINGS;
        if (!readCurrentBatchBindings) {
            configMap = projectCandidateUseConfigMapToRouteProcesses(
                    routeVersion, configMap, snapshotRouteProcesses, snapshotProcessMap, routeProcesses, processMap);
        }
        List<MesProRouteFlowProcessBatchRecordDO> dynamicBatchRecords =
                readCurrentBatchBindings && flowConfigType == MesProRouteFlowConfigTypeEnum.BATCH
                        ? selectCurrentOwnedBatchRecords(routeVersion.getRouteId(), flowConfigType)
                        : Collections.emptyList();
        Map<Long, List<MesProRouteFlowProcessBatchRecordDO>> dynamicBatchRecordMap =
                readCurrentBatchBindings && flowConfigType == MesProRouteFlowConfigTypeEnum.BATCH
                        ? groupCurrentBatchRecordsBySnapshotRouteProcessId(
                                routeVersion, routeProcesses, processMap, dynamicBatchRecords)
                        : Collections.emptyMap();
        Map<String, MesProBatchRecordReportDO> reportMap = loadReportMap(
                readCurrentBatchBindings
                        ? collectBatchReportIds(dynamicBatchRecords)
                        : collectCandidateBatchReportIds(flowConfigType, configMap));
        List<MesProRouteFlowProcessConfigRespVO> result = new ArrayList<>(routeProcesses.size());
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            MesProProcessDO process = processMap.get(routeProcess.getProcessId());
            if (process == null) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
            }
            MesProRouteFlowProcessConfigSaveReqVO config = configMap.get(routeProcess.getId());
            MesProRouteFlowProcessConfigRespVO vo = new MesProRouteFlowProcessConfigRespVO();
            vo.setRouteProcessId(routeProcess.getId());
            vo.setSort(routeProcess.getSort());
            vo.setProcessCode(process.getCode());
            vo.setProcessName(process.getName());
            vo.setUseType(flowConfigType.getType());
            vo.setRouteConfigEnabled(Boolean.TRUE);
            vo.setBaseBatchRecordReportId(routeProcess.getBatchRecordReportId());
            vo.setEnabled(Boolean.TRUE);
            vo.setExecutionMode(null);
            vo.setProductionQuantityFactor(config == null
                    ? DEFAULT_PRODUCTION_QUANTITY_FACTOR
                    : resolveProductionQuantityFactor(routeProcess.getId(), config.getProductionQuantityFactor()));
            if (flowConfigType == MesProRouteFlowConfigTypeEnum.BATCH
                    && readCurrentBatchBindings
                    && !shouldReadCandidateBatchBindingSnapshot(routeVersion, config)) {
                List<MesProRouteFlowProcessBatchRecordDO> records = dynamicBatchRecordMap.getOrDefault(
                        routeProcess.getId(), Collections.emptyList());
                vo.setBatchRecordReports(toBatchRecordRespList(records, reportMap));
                vo.setFormBindings(toFormBindingRespList(records));
            } else {
                vo.setBatchRecordReports(flowConfigType == MesProRouteFlowConfigTypeEnum.BATCH && config != null
                        ? toCandidateBatchRecordRespList(config, reportMap)
                        : Collections.emptyList());
                vo.setFormBindings(flowConfigType == MesProRouteFlowConfigTypeEnum.BATCH && config != null
                        ? toCandidateFormBindingRespList(config)
                        : Collections.emptyList());
            }
            vo.setRemark(config == null ? null : config.getRemark());
            result.add(vo);
        }
        return result;
    }

    private List<MesProRouteProcessDO> resolveRouteProcessesForSnapshotRead(
            MesProRouteVersionDO routeVersion,
            List<MesProRouteProcessDO> snapshotRouteProcesses) {
        if (isActiveRouteVersion(routeVersion)) {
            List<MesProRouteProcessDO> currentRouteProcesses = routeProcessMapper.selectListByRouteId(routeVersion.getRouteId());
            if (CollUtil.isEmpty(currentRouteProcesses) && CollUtil.isNotEmpty(snapshotRouteProcesses)) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
            }
            return currentRouteProcesses;
        }
        return snapshotRouteProcesses;
    }

    private Map<Long, MesProRouteFlowProcessConfigSaveReqVO> projectCandidateUseConfigMapToRouteProcesses(
            MesProRouteVersionDO routeVersion,
            Map<Long, MesProRouteFlowProcessConfigSaveReqVO> configMap,
            List<MesProRouteProcessDO> snapshotRouteProcesses,
            Map<Long, MesProProcessDO> snapshotProcessMap,
            List<MesProRouteProcessDO> targetRouteProcesses,
            Map<Long, MesProProcessDO> targetProcessMap) {
        if (configMap.isEmpty()) {
            return configMap;
        }
        Map<Long, MesProRouteProcessDO> targetRouteProcessById =
                convertMap(targetRouteProcesses, MesProRouteProcessDO::getId);
        Map<String, MesProRouteProcessDO> targetRouteProcessByIdentity = buildRouteProcessByIdentity(
                routeVersion.getRouteId(), targetRouteProcesses, targetProcessMap);
        Map<Long, MesProRouteProcessDO> snapshotRouteProcessById =
                convertMap(snapshotRouteProcesses, MesProRouteProcessDO::getId);
        Map<Long, MesProRouteFlowProcessConfigSaveReqVO> result = new LinkedHashMap<>();
        Map<Long, List<Long>> sourceRouteProcessIdsByTargetId = new LinkedHashMap<>();
        for (Map.Entry<Long, MesProRouteFlowProcessConfigSaveReqVO> entry : configMap.entrySet()) {
            Long configRouteProcessId = entry.getKey();
            MesProRouteProcessDO sourceRouteProcess = resolveCandidateUseConfigRouteProcess(
                    routeVersion, configRouteProcessId, targetRouteProcessById, snapshotRouteProcessById);
            MesProProcessDO sourceProcess = targetProcessMap.get(sourceRouteProcess.getProcessId());
            if (sourceProcess == null) {
                sourceProcess = snapshotProcessMap.get(sourceRouteProcess.getProcessId());
            }
            if (sourceProcess == null) {
                sourceProcess = processMapper.selectById(sourceRouteProcess.getProcessId());
            }
            String processIdentity = requireProcessIdentity(routeVersion.getRouteId(), sourceRouteProcess, sourceProcess);
            MesProRouteProcessDO targetRouteProcess = targetRouteProcessByIdentity.get(processIdentity);
            if (targetRouteProcess == null) {
                throw exception(PRO_ROUTE_PROCESS_IDENTITY_NOT_FOUND,
                        routeVersion.getRouteId(), sourceRouteProcess.getProcessId(), sourceRouteProcess.getId(),
                        processIdentity);
            }
            List<Long> sourceRouteProcessIds = sourceRouteProcessIdsByTargetId.computeIfAbsent(
                    targetRouteProcess.getId(), key -> new ArrayList<>());
            sourceRouteProcessIds.add(configRouteProcessId);
            if (sourceRouteProcessIds.size() > 1) {
                throw exception(PRO_ROUTE_PROCESS_IDENTITY_AMBIGUOUS,
                        routeVersion.getRouteId(), targetRouteProcess.getProcessId(), processIdentity,
                        sourceRouteProcessIds);
            }
            result.put(targetRouteProcess.getId(), copyCandidateUseConfig(entry.getValue(), targetRouteProcess.getId()));
        }
        return result;
    }

    private MesProRouteProcessDO resolveCandidateUseConfigRouteProcess(
            MesProRouteVersionDO routeVersion,
            Long routeProcessId,
            Map<Long, MesProRouteProcessDO> targetRouteProcessById,
            Map<Long, MesProRouteProcessDO> snapshotRouteProcessById) {
        MesProRouteProcessDO routeProcess = targetRouteProcessById.get(routeProcessId);
        if (routeProcess != null) {
            return routeProcess;
        }
        routeProcess = snapshotRouteProcessById.get(routeProcessId);
        if (routeProcess != null) {
            return routeProcess;
        }
        routeProcess = routeProcessMapper.selectById(routeProcessId);
        if (routeProcess == null || !Objects.equals(routeVersion.getRouteId(), routeProcess.getRouteId())) {
            throw exception(PRO_ROUTE_PROCESS_IDENTITY_NOT_FOUND,
                    routeVersion.getRouteId(), null, routeProcessId, null);
        }
        return routeProcess;
    }

    private MesProRouteFlowProcessConfigSaveReqVO copyCandidateUseConfig(
            MesProRouteFlowProcessConfigSaveReqVO source,
            Long routeProcessId) {
        MesProRouteFlowProcessConfigSaveReqVO copy = new MesProRouteFlowProcessConfigSaveReqVO();
        copy.setRouteProcessId(routeProcessId);
        copy.setEnabled(source.getEnabled());
        copy.setExecutionMode(source.getExecutionMode());
        copy.setProductionQuantityFactor(source.getProductionQuantityFactor());
        copy.setBatchRecordReports(source.getBatchRecordReports());
        copy.setFormBindings(source.getFormBindings());
        copy.setBatchRecordBindingSnapshotExplicit(source.getBatchRecordBindingSnapshotExplicit());
        copy.setRemark(source.getRemark());
        return copy;
    }

    private boolean shouldReadCandidateBatchBindingSnapshot(
            MesProRouteVersionDO routeVersion,
            MesProRouteFlowProcessConfigSaveReqVO config) {
        return routeVersion != null
                && MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(routeVersion.getLifecycleStatus())
                && config != null
                && Boolean.TRUE.equals(config.getBatchRecordBindingSnapshotExplicit());
    }

    private Map<Long, MesProProcessDO> loadProcessMap(List<MesProRouteProcessDO> routeProcesses) {
        return convertMap(
                processMapper.selectBatchIds(convertSet(routeProcesses, MesProRouteProcessDO::getProcessId)),
                MesProProcessDO::getId);
    }

    private RouteVersionBatchBindingReadStrategy resolveCandidateBatchBindingReadStrategy(
            MesProRouteVersionDO routeVersion) {
        if (isReadableCandidate(routeVersion)) {
            return RouteVersionBatchBindingReadStrategy.CURRENT_PROCESS_SETTINGS;
        }
        return RouteVersionBatchBindingReadStrategy.SNAPSHOT;
    }

    private List<MesProRouteFlowProcessBatchRecordDO> selectCurrentOwnedBatchRecords(
            Long routeId,
            MesProRouteFlowConfigTypeEnum flowConfigType) {
        if (flowConfigType != MesProRouteFlowConfigTypeEnum.BATCH) {
            return Collections.emptyList();
        }
        MesProRouteFlowConfigDO flowConfig =
                routeFlowConfigMapper.selectByRouteIdAndUseType(routeId, flowConfigType.getType());
        Map<Long, MesProRouteFlowProcessConfigDO> configByRouteProcessId =
                routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(routeId, flowConfigType.getType()).stream()
                        .filter(config -> MesProRouteFlowContextMatcher.isProcessConfigOwnedBy(
                                flowConfig, config, routeId, flowConfigType.getType()))
                        .collect(Collectors.toMap(MesProRouteFlowProcessConfigDO::getRouteProcessId,
                                config -> config, (left, right) -> left, LinkedHashMap::new));
        return routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(routeId, flowConfigType.getType()).stream()
                .filter(record -> isOwnedByProcessConfig(record, configByRouteProcessId.get(record.getRouteProcessId()),
                        routeId, flowConfigType.getType()))
                .toList();
    }

    private Map<Long, List<MesProRouteFlowProcessBatchRecordDO>> groupCurrentBatchRecordsBySnapshotRouteProcessId(
            MesProRouteVersionDO routeVersion,
            List<MesProRouteProcessDO> snapshotRouteProcesses,
            Map<Long, MesProProcessDO> snapshotProcessMap,
            List<MesProRouteFlowProcessBatchRecordDO> currentBatchRecords) {
        if (CollUtil.isEmpty(currentBatchRecords)) {
            return Collections.emptyMap();
        }
        Map<Long, Long> currentToSnapshotRouteProcessIdMap =
                buildCurrentToSnapshotRouteProcessIdMap(routeVersion, snapshotRouteProcesses, snapshotProcessMap);
        Map<Long, List<MesProRouteFlowProcessBatchRecordDO>> result = new LinkedHashMap<>();
        for (MesProRouteFlowProcessBatchRecordDO record : currentBatchRecords) {
            Long snapshotRouteProcessId = currentToSnapshotRouteProcessIdMap.get(record.getRouteProcessId());
            if (snapshotRouteProcessId == null) {
                continue;
            }
            result.computeIfAbsent(snapshotRouteProcessId, key -> new ArrayList<>()).add(record);
        }
        return result;
    }

    private Map<Long, Long> buildCurrentToSnapshotRouteProcessIdMap(
            MesProRouteVersionDO routeVersion,
            List<MesProRouteProcessDO> snapshotRouteProcesses,
            Map<Long, MesProProcessDO> snapshotProcessMap) {
        Map<Long, Long> routeProcessIdMap = new LinkedHashMap<>();
        Map<String, MesProRouteProcessDO> snapshotRouteProcessByIdentity = buildRouteProcessByIdentity(
                routeVersion.getRouteId(), snapshotRouteProcesses, snapshotProcessMap);
        for (MesProRouteProcessDO snapshotRouteProcess : snapshotRouteProcesses) {
            routeProcessIdMap.put(snapshotRouteProcess.getId(), snapshotRouteProcess.getId());
        }

        List<MesProRouteProcessDO> currentRouteProcesses = routeProcessMapper.selectListByRouteId(routeVersion.getRouteId());
        Map<Long, MesProProcessDO> currentProcessMap = loadProcessMap(currentRouteProcesses);
        Map<String, List<MesProRouteProcessDO>> currentRouteProcessesByIdentity = new LinkedHashMap<>();
        for (MesProRouteProcessDO currentRouteProcess : currentRouteProcesses) {
            String processIdentity = requireProcessIdentity(
                    routeVersion.getRouteId(), currentRouteProcess, currentProcessMap.get(currentRouteProcess.getProcessId()));
            currentRouteProcessesByIdentity
                    .computeIfAbsent(processIdentity, key -> new ArrayList<>())
                    .add(currentRouteProcess);
        }

        for (Map.Entry<String, MesProRouteProcessDO> entry : snapshotRouteProcessByIdentity.entrySet()) {
            List<MesProRouteProcessDO> currentMatches = currentRouteProcessesByIdentity.get(entry.getKey());
            if (CollUtil.isEmpty(currentMatches)) {
                continue;
            }
            if (currentMatches.size() > 1) {
                throw exception(PRO_ROUTE_PROCESS_IDENTITY_AMBIGUOUS,
                        routeVersion.getRouteId(), entry.getValue().getProcessId(), entry.getKey(),
                        currentMatches.stream().map(MesProRouteProcessDO::getId).toList());
            }
            routeProcessIdMap.put(currentMatches.get(0).getId(), entry.getValue().getId());
        }
        return routeProcessIdMap;
    }

    private Map<String, MesProRouteProcessDO> buildRouteProcessByIdentity(
            Long routeId,
            List<MesProRouteProcessDO> routeProcesses,
            Map<Long, MesProProcessDO> processMap) {
        Map<String, MesProRouteProcessDO> routeProcessByIdentity = new LinkedHashMap<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            String processIdentity = requireProcessIdentity(
                    routeId, routeProcess, processMap.get(routeProcess.getProcessId()));
            MesProRouteProcessDO existing = routeProcessByIdentity.putIfAbsent(processIdentity, routeProcess);
            if (existing != null) {
                throw exception(PRO_ROUTE_PROCESS_IDENTITY_AMBIGUOUS,
                        routeId, routeProcess.getProcessId(), processIdentity,
                        List.of(existing.getId(), routeProcess.getId()));
            }
        }
        return routeProcessByIdentity;
    }

    private String requireProcessIdentity(Long routeId, MesProRouteProcessDO routeProcess, MesProProcessDO process) {
        String processCode = process == null ? null : StrUtil.trim(process.getCode());
        if (StrUtil.isBlank(processCode)) {
            throw exception(PRO_ROUTE_PROCESS_IDENTITY_NOT_FOUND,
                    routeId,
                    routeProcess == null ? null : routeProcess.getProcessId(),
                    routeProcess == null ? null : routeProcess.getId(),
                    processCode);
        }
        return processCode;
    }

    private JSONObject resolveCandidateConfigSnapshots(MesProRouteVersionDO routeVersion) {
        if (routeVersion == null || StrUtil.isBlank(routeVersion.getRouteSnapshotJson())) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion == null ? null : routeVersion.getId());
        }
        JSONObject snapshot;
        try {
            snapshot = JSON.parseObject(routeVersion.getRouteSnapshotJson());
        } catch (RuntimeException ex) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
        }
        JSONObject configSnapshots = snapshot == null ? null : snapshot.getJSONObject(SNAPSHOT_CONFIGS_KEY);
        if (configSnapshots == null) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
        }
        return configSnapshots;
    }

    private List<MesProRouteProcessDO> parseCandidateRouteProcessesFromConfigSnapshots(MesProRouteVersionDO routeVersion,
                                                                    JSONObject configSnapshots) {
        JSONObject flowGraph = configSnapshots.getJSONObject(FLOW_GRAPH_KEY);
        if (flowGraph == null) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
        }
        return parseCandidateRouteProcesses(routeVersion, flowGraph);
    }

    private List<MesProRouteProcessDO> parseCandidateRouteProcesses(MesProRouteVersionDO routeVersion,
                                                                    JSONObject flowGraph) {
        JSONArray nodes = flowGraph.getJSONArray("nodes");
        if (nodes == null) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
        }
        List<MesProRouteProcessDO> routeProcesses = new ArrayList<>();
        for (Object value : nodes) {
            JSONObject node = toCandidateJsonObject(routeVersion, value);
            Long routeProcessId = node.getLong("routeProcessId");
            if (routeProcessId == null) {
                routeProcessId = node.getLong("clientRouteProcessId");
            }
            Long processId = node.getLong("processId");
            Integer sort = node.getInteger("sort");
            if (routeProcessId == null || processId == null || sort == null) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
            }
            routeProcesses.add(MesProRouteProcessDO.builder()
                    .id(routeProcessId)
                    .routeId(routeVersion.getRouteId())
                    .processId(processId)
                    .sort(sort)
                    .keyFlag(Boolean.TRUE.equals(node.getBoolean("keyFlag")))
                    .checkFlag(Boolean.TRUE.equals(node.getBoolean("checkFlag")))
                    .batchRecordReportId(StrUtil.blankToDefault(node.getString("batchRecordReportId"), null))
                    .build());
        }
        routeProcesses.sort(Comparator.comparing(
                MesProRouteProcessDO::getSort, Comparator.nullsLast(Integer::compareTo)));
        return routeProcesses;
    }

    private Map<Long, MesProRouteFlowProcessConfigSaveReqVO> parseCandidateUseConfigMap(
            MesProRouteVersionDO routeVersion,
            Object snapshot) {
        if (snapshot == null) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
        }
        Map<Long, MesProRouteFlowProcessConfigSaveReqVO> result = new LinkedHashMap<>();
        if (snapshot instanceof JSONObject configsByRouteProcessId) {
            for (Map.Entry<String, Object> entry : configsByRouteProcessId.entrySet()) {
                JSONObject config = toCandidateJsonObject(routeVersion, entry.getValue());
                if (config.getLong("routeProcessId") == null) {
                    config.put("routeProcessId", parseCandidateRouteProcessIdKey(routeVersion, entry.getKey()));
                }
                addCandidateUseConfig(routeVersion, result, config);
            }
            return result;
        }
        if (snapshot instanceof JSONArray configs) {
            for (Object value : configs) {
                addCandidateUseConfig(routeVersion, result, toCandidateJsonObject(routeVersion, value));
            }
            return result;
        }
        throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
    }

    private void addCandidateUseConfig(MesProRouteVersionDO routeVersion,
                                       Map<Long, MesProRouteFlowProcessConfigSaveReqVO> result,
                                       JSONObject config) {
        Long routeProcessId = config.getLong("routeProcessId");
        if (routeProcessId == null) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
        }
        MesProRouteFlowProcessConfigSaveReqVO reqVO = new MesProRouteFlowProcessConfigSaveReqVO();
        reqVO.setRouteProcessId(routeProcessId);
        reqVO.setEnabled(config.getBoolean("enabled"));
        reqVO.setExecutionMode(config.getString("executionMode"));
        reqVO.setProductionQuantityFactor(config.getBigDecimal("productionQuantityFactor"));
        reqVO.setBatchRecordReports(parseCandidateBatchRecordReports(config));
        reqVO.setFormBindings(parseCandidateFormBindings(config));
        reqVO.setBatchRecordBindingSnapshotExplicit(config.getBoolean("batchRecordBindingSnapshotExplicit"));
        reqVO.setRemark(config.getString("remark"));
        result.put(routeProcessId, reqVO);
    }

    private List<MesProRouteFlowFormBindingSaveReqVO> parseCandidateFormBindings(JSONObject config) {
        JSONArray bindings = config.getJSONArray("formBindings");
        if (bindings == null || bindings.isEmpty()) {
            return Collections.emptyList();
        }
        List<MesProRouteFlowFormBindingSaveReqVO> result = new ArrayList<>();
        for (int index = 0; index < bindings.size(); index++) {
            JSONObject binding = toCandidateJsonObject(null, bindings.get(index));
            Long templateId = binding.getLong("formTemplateId");
            if (templateId == null) {
                templateId = binding.getLong("templateId");
            }
            if (templateId == null) {
                continue;
            }
            result.add(toCandidateFormBindingSaveReqVO(binding, index + 1));
        }
        return result;
    }

    private MesProRouteFlowFormBindingSaveReqVO toCandidateFormBindingSaveReqVO(JSONObject binding, int defaultSort) {
        Integer reportSort = binding.getInteger("reportSort");
        Long templateId = binding.getLong("formTemplateId");
        if (templateId == null) {
            templateId = binding.getLong("templateId");
        }
        return new MesProRouteFlowFormBindingSaveReqVO()
                .setFormBindingKey(binding.getString("formBindingKey"))
                .setFormTemplateId(templateId)
                .setFormTemplateName(StrUtil.blankToDefault(binding.getString("formTemplateName"),
                        binding.getString("formTemplateNameSnapshot")))
                .setFormSlotType(binding.getString("formSlotType"))
                .setLastPublishedTemplateVersionId(binding.getLong("lastPublishedTemplateVersionId"))
                .setLastPublishedTemplateVersionNo(binding.getString("lastPublishedTemplateVersionNo"))
                .setInstanceScope(binding.getString("instanceScope"))
                .setSharedFormKey(binding.getString("sharedFormKey"))
                .setFillableScopeJson(binding.getString("fillableScopeJson"))
                .setRecordCategory(binding.getString("recordCategory"))
                .setValidationProfile(binding.getString("validationProfile"))
                .setRecordbookEnabled(binding.getBoolean("recordbookEnabled"))
                .setPermissionScopeId(binding.getLong("permissionScopeId"))
                .setRequiredPolicy(binding.getString("requiredPolicy"))
                .setRequiredConditionJson(binding.getString("requiredConditionJson"))
                .setOwnerRoleKey(binding.getString("ownerRoleKey"))
                .setArchiveVisibility(binding.getString("archiveVisibility"))
                .setCandidateSourceType(binding.getString("candidateSourceType"))
                .setCandidateSourceIds(parseCandidateSourceIds(binding.get("candidateSourceIds")))
                .setCandidateSourceNames(parseCandidateSourceNames(binding.get("candidateSourceNames")))
                .setReportSort(reportSort == null ? defaultSort : reportSort)
                .setRemark(binding.getString("remark"));
    }

    private List<MesProRouteFlowFormBindingRespVO> toCandidateFormBindingRespList(
            MesProRouteFlowProcessConfigSaveReqVO config) {
        return normalizeFormBindings(config).stream()
                .map(binding -> {
                    MesProRouteFlowFormBindingRespVO vo = new MesProRouteFlowFormBindingRespVO();
                    vo.setFormBindingKey(StrUtil.trim(binding.getFormBindingKey()));
                    vo.setFormTemplateId(binding.getFormTemplateId());
                    vo.setFormTemplateName(StrUtil.trim(binding.getFormTemplateName()));
                    String formSlotType = resolveConfiguredFormSlotType(binding);
                    vo.setFormSlotType(formSlotType);
                    vo.setLastPublishedTemplateVersionId(binding.getLastPublishedTemplateVersionId());
                    vo.setLastPublishedTemplateVersionNo(binding.getLastPublishedTemplateVersionNo());
                    vo.setInstanceScope(resolveInstanceScope(binding.getInstanceScope()));
                    vo.setSharedFormKey(StrUtil.blankToDefault(StrUtil.trim(binding.getSharedFormKey()), null));
                    vo.setFillableScopeJson(StrUtil.blankToDefault(StrUtil.trim(binding.getFillableScopeJson()), null));
                    vo.setRecordCategory(resolveRecordCategory(binding.getRecordCategory(), formSlotType));
                    vo.setValidationProfile(resolveValidationProfile(vo.getRecordCategory(), binding.getValidationProfile()));
                    vo.setRecordbookEnabled(resolveRecordbookEnabled(binding.getRecordbookEnabled(), vo.getRecordCategory()));
                    vo.setPermissionScopeId(binding.getPermissionScopeId());
                    vo.setRequiredPolicy(resolveRequiredPolicy(binding.getRequiredPolicy()));
                    vo.setRequiredConditionJson(binding.getRequiredConditionJson());
                    vo.setOwnerRoleKey(resolveOwnerRoleKey(binding.getOwnerRoleKey(), formSlotType));
                    vo.setArchiveVisibility(resolveArchiveVisibility(binding.getArchiveVisibility()));
                    vo.setCandidateSourceType(normalizeCandidateSourceTypeOptional(binding.getCandidateSourceType()));
                    vo.setCandidateSourceIds(normalizeCandidateSourceIds(binding));
                    vo.setCandidateSourceNames(normalizeCandidateSourceNames(binding.getCandidateSourceNames()));
                    vo.setReportSort(binding.getReportSort());
                    vo.setRemark(binding.getRemark());
                    return vo;
                })
                .toList();
    }

    private List<MesProRouteFlowBatchRecordSaveReqVO> parseCandidateBatchRecordReports(JSONObject config) {
        JSONArray reports = config.getJSONArray("batchRecordReports");
        if (reports == null) {
            String reportId = StrUtil.blankToDefault(config.getString("batchRecordReportId"),
                    config.getString("reportId"));
            if (StrUtil.isBlank(reportId)) {
                return Collections.emptyList();
            }
            return List.of(toCandidateBatchRecordSaveReqVO(config, 1));
        }
        List<MesProRouteFlowBatchRecordSaveReqVO> result = new ArrayList<>();
        for (int index = 0; index < reports.size(); index++) {
            JSONObject report = toCandidateJsonObject(null, reports.get(index));
            String reportId = StrUtil.blankToDefault(report.getString("batchRecordReportId"),
                    report.getString("reportId"));
            if (StrUtil.isBlank(reportId)) {
                continue;
            }
            result.add(toCandidateBatchRecordSaveReqVO(report, index + 1));
        }
        return result;
    }

    private MesProRouteFlowBatchRecordSaveReqVO toCandidateBatchRecordSaveReqVO(JSONObject report, int defaultSort) {
        String reportId = StrUtil.blankToDefault(report.getString("batchRecordReportId"), report.getString("reportId"));
        Integer reportSort = report.getInteger("reportSort");
        return new MesProRouteFlowBatchRecordSaveReqVO()
                .setBatchRecordReportId(reportId)
                .setFormSlotType(report.getString("formSlotType"))
                .setInstanceScope(report.getString("instanceScope"))
                .setSharedFormKey(report.getString("sharedFormKey"))
                .setFillableScopeJson(report.getString("fillableScopeJson"))
                .setRecordCategory(report.getString("recordCategory"))
                .setValidationProfile(report.getString("validationProfile"))
                .setRecordbookEnabled(report.getBoolean("recordbookEnabled"))
                .setRequiredPolicy(report.getString("requiredPolicy"))
                .setRequiredConditionJson(report.getString("requiredConditionJson"))
                .setOwnerRoleKey(report.getString("ownerRoleKey"))
                .setArchiveVisibility(report.getString("archiveVisibility"))
                .setPermissionScopeId(report.getLong("permissionScopeId"))
                .setReportSort(reportSort == null ? defaultSort : reportSort)
                .setRemark(report.getString("remark"));
    }

    private Set<String> collectCandidateBatchReportIds(
            MesProRouteFlowConfigTypeEnum flowConfigType,
            Map<Long, MesProRouteFlowProcessConfigSaveReqVO> configMap) {
        if (flowConfigType != MesProRouteFlowConfigTypeEnum.BATCH) {
            return Collections.emptySet();
        }
        Set<String> reportIds = new LinkedHashSet<>();
        configMap.values().stream()
                .flatMap(config -> normalizeBatchRecordReports(config).stream())
                .map(MesProRouteFlowBatchRecordSaveReqVO::getBatchRecordReportId)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .forEach(reportIds::add);
        return reportIds;
    }

    private Set<String> collectBatchReportIds(List<MesProRouteFlowProcessBatchRecordDO> records) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptySet();
        }
        return records.stream()
                .map(MesProRouteFlowProcessBatchRecordDO::getBatchRecordReportId)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<MesProRouteFlowBatchRecordRespVO> toCandidateBatchRecordRespList(
            MesProRouteFlowProcessConfigSaveReqVO config,
            Map<String, MesProBatchRecordReportDO> reportMap) {
        return normalizeBatchRecordReports(config).stream()
                .map(report -> {
                    MesProBatchRecordReportDO metadata = reportMap.get(StrUtil.trim(report.getBatchRecordReportId()));
                    String formSlotType = resolveConfiguredFormSlotType(report, metadata);
                    MesProRouteFlowBatchRecordRespVO vo = new MesProRouteFlowBatchRecordRespVO();
                    vo.setBatchRecordReportId(StrUtil.trim(report.getBatchRecordReportId()));
                    vo.setBatchRecordReportCode(metadata == null ? null : metadata.getReportCode());
                    vo.setBatchRecordReportName(metadata == null ? null : metadata.getReportName());
                    vo.setFormSlotType(formSlotType);
                    vo.setInstanceScope(resolveInstanceScope(report.getInstanceScope()));
                    vo.setSharedFormKey(StrUtil.blankToDefault(StrUtil.trim(report.getSharedFormKey()), null));
                    vo.setFillableScopeJson(StrUtil.blankToDefault(StrUtil.trim(report.getFillableScopeJson()), null));
                    vo.setRecordCategory(resolveRecordCategory(report.getRecordCategory(), formSlotType));
                    vo.setValidationProfile(resolveValidationProfile(vo.getRecordCategory(), report.getValidationProfile()));
                    vo.setRecordbookEnabled(resolveRecordbookEnabled(report.getRecordbookEnabled(), vo.getRecordCategory()));
                    vo.setPermissionScopeId(report.getPermissionScopeId());
                    vo.setRequiredPolicy(resolveRequiredPolicy(report.getRequiredPolicy()));
                    vo.setRequiredConditionJson(report.getRequiredConditionJson());
                    vo.setOwnerRoleKey(resolveOwnerRoleKey(report.getOwnerRoleKey(), formSlotType));
                    vo.setArchiveVisibility(resolveArchiveVisibility(report.getArchiveVisibility()));
                    vo.setReportSort(report.getReportSort());
                    vo.setRemark(report.getRemark());
                    return vo;
                })
                .toList();
    }

    private Long parseCandidateRouteProcessIdKey(MesProRouteVersionDO routeVersion, String key) {
        try {
            return Long.valueOf(key);
        } catch (RuntimeException ex) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
        }
    }

    private JSONObject toCandidateJsonObject(MesProRouteVersionDO routeVersion, Object value) {
        if (value instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(value));
        if (jsonObject == null) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion == null ? null : routeVersion.getId());
        }
        return jsonObject;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRouteFlowConfig(MesProRouteFlowConfigSaveReqVO saveReqVO) {
        saveRouteFlowConfigInternal(saveReqVO, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRouteFlowConfigForConfigPackageImport(MesProRouteFlowConfigSaveReqVO saveReqVO) {
        saveRouteFlowConfigInternal(saveReqVO, false);
    }

    @Override
    public List<MesProRouteBatchRecordAttachmentOwnerRespVO> getBatchRecordAttachmentOwners(
            Long routeId, Long routeVersionId) {
        validateRouteExists(routeId);
        MesProRouteVersionDO routeVersion = routeVersionId == null
                ? routeVersionMapper.selectActiveByRouteId(routeId)
                : requireReadableRouteVersion(routeVersionId, routeId);
        Map<String, MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO> savedItems =
                routeVersion == null ? Collections.emptyMap()
                        : parseBatchRecordAttachmentOwnerSnapshot(routeVersion);
        return buildBatchRecordAttachmentOwnerRespList(savedItems, loadEnabledCurrentTenantUserMap());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MesProRouteBatchRecordAttachmentOwnerRespVO> initializeBatchRecordAttachmentOwners(
            MesProRouteBatchRecordAttachmentOwnerInitReqVO initReqVO) {
        MesProRouteDO route = validateRouteExists(initReqVO.getRouteId());
        requireRouteBatchRecordConfigEditAbility(route, "初始化批记录附件负责人");
        MesProRouteVersionDO routeVersion = requireDraftCandidateVersion(initReqVO.getRouteVersionId(), route.getId());
        RoleCategoryDO category = requireBatchRecordRoleCategory();
        List<AdminUserDO> enabledUsers = loadEnabledCurrentTenantUsers();
        if (enabledUsers.size() < BATCH_RECORD_ATTACHMENT_MIN_USERS) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_ENABLED_USER_NOT_ENOUGH);
        }
        Map<Long, AdminUserDO> enabledUserMap = convertMap(enabledUsers, AdminUserDO::getId);
        Map<String, MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO> snapshotItems = new LinkedHashMap<>();
        Map<Long, List<Long>> initializedRoleUserIds = new LinkedHashMap<>();
        for (BatchRecordAttachmentDefinition definition : BATCH_RECORD_ATTACHMENT_DEFINITIONS) {
            RoleDO role = ensureBatchRecordAttachmentRole(definition, category);
            List<Long> assignedUserIds = ensureBatchRecordAttachmentRoleUsers(definition, role, enabledUsers, enabledUserMap);
            initializedRoleUserIds.put(role.getId(), assignedUserIds);
            snapshotItems.put(definition.code(), new MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO()
                    .setAttachmentCode(definition.code())
                    .setCandidateSourceType(CANDIDATE_SOURCE_TYPE_ROLE)
                    .setCandidateSourceIds(List.of(role.getId()))
                    .setCandidateSourceNames(List.of(formatRoleSnapshotName(role)))
                    .setRemark("工序开始批记录附件默认上传角色"));
        }
        List<MesProRouteBatchRecordAttachmentOwnerRespVO> result =
                buildBatchRecordAttachmentOwnerRespList(snapshotItems, enabledUserMap, initializedRoleUserIds);
        saveBatchRecordAttachmentOwnerSnapshot(routeVersion.getId(), result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatchRecordAttachmentOwners(MesProRouteBatchRecordAttachmentOwnerSaveReqVO saveReqVO) {
        MesProRouteDO route = validateRouteExists(saveReqVO.getRouteId());
        requireRouteBatchRecordConfigEditAbility(route, "保存批记录附件负责人");
        MesProRouteVersionDO routeVersion = requireDraftCandidateVersion(saveReqVO.getRouteVersionId(), route.getId());
        Map<Long, AdminUserDO> enabledUserMap = loadEnabledCurrentTenantUserMap();
        Map<String, MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO> normalized =
                normalizeBatchRecordAttachmentOwnerSaveItems(saveReqVO.getItems(), enabledUserMap);
        List<MesProRouteBatchRecordAttachmentOwnerRespVO> result =
                buildBatchRecordAttachmentOwnerRespList(normalized, enabledUserMap);
        saveBatchRecordAttachmentOwnerSnapshot(routeVersion.getId(), result);
    }

    private void saveRouteFlowConfigInternal(MesProRouteFlowConfigSaveReqVO saveReqVO, boolean requireRouteEditPermission) {
        MesProRouteFlowConfigTypeEnum flowConfigType = validateUseType(saveReqVO.getUseType());
        MesProRouteDO route = validateRouteExists(saveReqVO.getRouteId());
        if (requireRouteEditPermission) {
            permissionGateService.requireAbility(new MesProEdhrPermissionGateCommand()
                    .setObjectType("ROUTE")
                    .setObjectId(String.valueOf(route.getId()))
                    .setAbility("ROUTE_EDIT")
                    .setRouteId(route.getId())
                    .setPermissionCode(resolveRouteFlowPermissionCode(flowConfigType))
                    .setActionName("保存路线工序使用配置"));
        }
        MesProRouteVersionDO routeVersion = requireDraftCandidateVersion(saveReqVO.getRouteVersionId(), route.getId());
        JSONObject configSnapshots = resolveCandidateConfigSnapshots(routeVersion);
        List<MesProRouteProcessDO> routeProcesses = parseCandidateRouteProcessesFromConfigSnapshots(routeVersion, configSnapshots);
        Map<Long, MesProRouteProcessDO> routeProcessMap =
                convertMap(routeProcesses, MesProRouteProcessDO::getId);
        Set<Long> validRouteProcessIds = new LinkedHashSet<>(routeProcessMap.keySet());
        for (MesProRouteFlowProcessConfigSaveReqVO processConfig : saveReqVO.getProcessConfigs()) {
            if (!validRouteProcessIds.contains(processConfig.getRouteProcessId())) {
                throw exception(PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED);
            }
        }
        Map<Long, MesProRouteFlowProcessConfigSaveReqVO> saveMap = new LinkedHashMap<>();
        for (MesProRouteFlowProcessConfigSaveReqVO processConfig : saveReqVO.getProcessConfigs()) {
            saveMap.put(processConfig.getRouteProcessId(), processConfig);
        }
        for (MesProRouteFlowProcessConfigSaveReqVO saveConfig : saveMap.values()) {
            validateBatchProcessConfig(flowConfigType, saveConfig);
        }
        String configKey = flowConfigType == MesProRouteFlowConfigTypeEnum.BATCH
                ? BATCH_USE_CONFIGS_KEY : SCHEDULE_USE_CONFIGS_KEY;
        routeCandidateConfigService.saveConfigSnapshot(routeVersion.getId(), configKey,
                buildCandidateUseConfigSnapshot(routeVersion, configKey, flowConfigType,
                        saveReqVO, validRouteProcessIds));
    }

    private Set<Long> resolveCandidateSaveRouteProcessIds(
            MesProRouteVersionDO routeVersion, Set<Long> currentRouteProcessIds) {
        if (routeVersion == null || StrUtil.isBlank(routeVersion.getRouteSnapshotJson())) {
            return currentRouteProcessIds;
        }
        JSONObject flowGraph = resolveCandidateConfigSnapshots(routeVersion).getJSONObject(FLOW_GRAPH_KEY);
        if (flowGraph == null) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
        }
        return parseCandidateRouteProcesses(routeVersion, flowGraph).stream()
                .map(MesProRouteProcessDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private MesProRouteVersionDO requireDraftCandidateVersion(Long routeVersionId, Long routeId) {
        if (routeVersionId == null) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, routeVersionId);
        }
        MesProRouteVersionDO routeVersion = routeVersionMapper.selectById(routeVersionId);
        if (routeVersion == null) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, routeVersionId);
        }
        if (Objects.equals(routeVersion.getRouteId(), routeId)
                && Boolean.FALSE.equals(routeVersion.getActive())
                && MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(routeVersion.getLifecycleStatus())) {
            return routeVersion;
        }
        throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                routeVersion.getId(), routeVersion.getLifecycleStatus());
    }

    private MesProRouteVersionDO requireReadableCandidateVersion(Long routeVersionId, Long routeId) {
        if (routeVersionId == null) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, routeVersionId);
        }
        MesProRouteVersionDO routeVersion = routeVersionMapper.selectById(routeVersionId);
        if (routeVersion == null) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, routeVersionId);
        }
        if (Objects.equals(routeVersion.getRouteId(), routeId)
                && Boolean.FALSE.equals(routeVersion.getActive())
                && READABLE_CANDIDATE_STATUSES.contains(routeVersion.getLifecycleStatus())) {
            return routeVersion;
        }
        throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                routeVersion.getId(), routeVersion.getLifecycleStatus());
    }

    private MesProRouteVersionDO requireReadableRouteVersion(Long routeVersionId, Long routeId) {
        MesProRouteVersionDO routeVersion = requireExistingRouteVersion(routeVersionId);
        if (Objects.equals(routeVersion.getRouteId(), routeId)
                && (isReadableCandidate(routeVersion) || isPublishedSnapshotRouteVersion(routeVersion))) {
            return routeVersion;
        }
        throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                routeVersion.getId(), routeVersion.getLifecycleStatus());
    }

    private MesProRouteVersionDO requireExistingRouteVersion(Long routeVersionId) {
        if (routeVersionId == null) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, routeVersionId);
        }
        MesProRouteVersionDO routeVersion = routeVersionMapper.selectById(routeVersionId);
        if (routeVersion == null) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, routeVersionId);
        }
        return routeVersion;
    }

    private boolean isReadableCandidate(MesProRouteVersionDO routeVersion) {
        return routeVersion != null
                && Boolean.FALSE.equals(routeVersion.getActive())
                && READABLE_CANDIDATE_STATUSES.contains(routeVersion.getLifecycleStatus());
    }

    private boolean isActiveRouteVersion(MesProRouteVersionDO routeVersion) {
        return routeVersion != null
                && Boolean.TRUE.equals(routeVersion.getActive())
                && MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE.equals(routeVersion.getLifecycleStatus());
    }

    private boolean isPublishedSnapshotRouteVersion(MesProRouteVersionDO routeVersion) {
        return isActiveRouteVersion(routeVersion)
                || (routeVersion != null
                && Boolean.FALSE.equals(routeVersion.getActive())
                && MesProRouteVersionLifecycleServiceImpl.STATUS_SUPERSEDED.equals(routeVersion.getLifecycleStatus()));
    }

    private List<MesProRouteFlowProcessConfigSaveReqVO> buildCandidateUseConfigSnapshot(
            MesProRouteVersionDO routeVersion,
            String configKey,
            MesProRouteFlowConfigTypeEnum flowConfigType,
            MesProRouteFlowConfigSaveReqVO saveReqVO,
            Set<Long> validRouteProcessIds) {
        Map<Long, MesProRouteFlowProcessConfigSaveReqVO> merged = new LinkedHashMap<>();
        Object existingSnapshot = resolveExistingCandidateUseConfigSnapshot(routeVersion, configKey);
        if (existingSnapshot != null) {
            parseCandidateUseConfigMap(routeVersion, existingSnapshot).forEach((routeProcessId, processConfig) -> {
                if (validRouteProcessIds.contains(routeProcessId)) {
                    merged.put(routeProcessId, processConfig);
                }
            });
        }
        if (saveReqVO.getProcessConfigs() != null) {
            for (MesProRouteFlowProcessConfigSaveReqVO processConfig : saveReqVO.getProcessConfigs()) {
                if (flowConfigType == MesProRouteFlowConfigTypeEnum.BATCH) {
                    processConfig.setBatchRecordBindingSnapshotExplicit(Boolean.TRUE);
                }
                merged.put(processConfig.getRouteProcessId(), processConfig);
            }
        }
        return merged.values().stream()
                .map(processConfig -> normalizeCandidateUseConfigSnapshot(flowConfigType, processConfig))
                .toList();
    }

    private MesProRouteFlowProcessConfigSaveReqVO normalizeCandidateUseConfigSnapshot(
            MesProRouteFlowConfigTypeEnum flowConfigType,
            MesProRouteFlowProcessConfigSaveReqVO processConfig) {
        processConfig.setExecutionMode(resolveExecutionMode(flowConfigType, processConfig.getExecutionMode()));
        processConfig.setProductionQuantityFactor(resolveProductionQuantityFactor(
                processConfig.getRouteProcessId(), processConfig.getProductionQuantityFactor()));
        if (flowConfigType == MesProRouteFlowConfigTypeEnum.BATCH) {
            boolean explicitBatchBindingSnapshot =
                    Boolean.TRUE.equals(processConfig.getBatchRecordBindingSnapshotExplicit());
            processConfig.setBatchRecordReports(Collections.emptyList());
            processConfig.setFormBindings(resolveAndNormalizeFormBindings(processConfig));
            processConfig.setBatchRecordBindingSnapshotExplicit(explicitBatchBindingSnapshot ? Boolean.TRUE : null);
        } else {
            processConfig.setFormBindings(Collections.emptyList());
            processConfig.setBatchRecordBindingSnapshotExplicit(null);
        }
        return processConfig;
    }

    private Object resolveExistingCandidateUseConfigSnapshot(MesProRouteVersionDO routeVersion, String configKey) {
        if (routeVersion == null || StrUtil.isBlank(routeVersion.getRouteSnapshotJson())) {
            return null;
        }
        JSONObject snapshot;
        try {
            snapshot = JSON.parseObject(routeVersion.getRouteSnapshotJson());
        } catch (RuntimeException ex) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
        }
        if (snapshot == null || snapshot.isEmpty()) {
            return null;
        }
        JSONObject configSnapshots = snapshot.getJSONObject(SNAPSHOT_CONFIGS_KEY);
        return configSnapshots == null ? null : configSnapshots.get(configKey);
    }

    private Map<String, MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO> parseBatchRecordAttachmentOwnerSnapshot(
            MesProRouteVersionDO routeVersion) {
        Object snapshot = resolveExistingCandidateUseConfigSnapshot(routeVersion, BATCH_RECORD_ATTACHMENT_OWNERS_KEY);
        if (snapshot == null) {
            return Collections.emptyMap();
        }
        if (!(snapshot instanceof JSONArray items)) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersion.getId());
        }
        Map<String, MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO> result = new LinkedHashMap<>();
        for (Object value : items) {
            JSONObject item = toCandidateJsonObject(routeVersion, value);
            String attachmentCode = StrUtil.trim(item.getString("attachmentCode"));
            if (!isBatchRecordAttachmentCode(attachmentCode)) {
                throw exception(PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_OWNER_INVALID, attachmentCode);
            }
            result.put(attachmentCode, new MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO()
                    .setAttachmentCode(attachmentCode)
                    .setCandidateSourceType(normalizeBatchRecordAttachmentCandidateSourceType(
                            item.getString("candidateSourceType")))
                    .setCandidateSourceIds(normalizeBatchRecordAttachmentCandidateSourceIds(
                            parseCandidateSourceIds(item.get("candidateSourceIds"))))
                    .setCandidateSourceNames(parseBatchRecordAttachmentCandidateSourceNames(
                            item.get("candidateSourceNames")))
                    .setRemark(item.getString("remark")));
        }
        return result;
    }

    private Map<String, MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO> normalizeBatchRecordAttachmentOwnerSaveItems(
            List<MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO> items,
            Map<Long, AdminUserDO> enabledUserMap) {
        Map<String, MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO> configuredByCode = new LinkedHashMap<>();
        for (MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO item : items) {
            String attachmentCode = StrUtil.trim(item.getAttachmentCode());
            if (!isBatchRecordAttachmentCode(attachmentCode)) {
                throw exception(PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_OWNER_INVALID, attachmentCode);
            }
            if (configuredByCode.putIfAbsent(attachmentCode, item) != null) {
                throw exception(PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_OWNER_INVALID, attachmentCode);
            }
        }
        Map<String, MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO> result = new LinkedHashMap<>();
        for (BatchRecordAttachmentDefinition definition : BATCH_RECORD_ATTACHMENT_DEFINITIONS) {
            MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO item = configuredByCode.get(definition.code());
            if (item == null) {
                throw exception(PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_OWNER_INVALID, definition.code());
            }
            String sourceType = normalizeBatchRecordAttachmentCandidateSourceType(item.getCandidateSourceType());
            List<Long> sourceIds = normalizeBatchRecordAttachmentCandidateSourceIds(item.getCandidateSourceIds());
            validateBatchRecordAttachmentCandidateSource(sourceType, sourceIds, enabledUserMap);
            List<String> sourceNames = resolveBatchRecordAttachmentCandidateSourceNames(
                    sourceType, sourceIds, item.getCandidateSourceNames(), enabledUserMap);
            result.put(definition.code(), new MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO()
                    .setAttachmentCode(definition.code())
                    .setCandidateSourceType(sourceType)
                    .setCandidateSourceIds(sourceIds)
                    .setCandidateSourceNames(sourceNames)
                    .setRemark(StrUtil.blankToDefault(StrUtil.trim(item.getRemark()), null)));
        }
        return result;
    }

    private List<MesProRouteBatchRecordAttachmentOwnerRespVO> buildBatchRecordAttachmentOwnerRespList(
            Map<String, MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO> savedItems,
            Map<Long, AdminUserDO> enabledUserMap) {
        return buildBatchRecordAttachmentOwnerRespList(savedItems, enabledUserMap, Collections.emptyMap());
    }

    private List<MesProRouteBatchRecordAttachmentOwnerRespVO> buildBatchRecordAttachmentOwnerRespList(
            Map<String, MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO> savedItems,
            Map<Long, AdminUserDO> enabledUserMap,
            Map<Long, List<Long>> assignedUserIdsByRoleId) {
        List<MesProRouteBatchRecordAttachmentOwnerRespVO> result = new ArrayList<>();
        for (BatchRecordAttachmentDefinition definition : BATCH_RECORD_ATTACHMENT_DEFINITIONS) {
            MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO savedItem = savedItems.get(definition.code());
            RoleDO defaultRole = roleMapper.selectByCode(definition.defaultRoleCode());
            List<Long> candidateSourceIds = savedItem == null
                    ? defaultRole == null ? Collections.emptyList() : List.of(defaultRole.getId())
                    : normalizeBatchRecordAttachmentCandidateSourceIds(savedItem.getCandidateSourceIds());
            String candidateSourceType = savedItem == null ? CANDIDATE_SOURCE_TYPE_ROLE
                    : normalizeBatchRecordAttachmentCandidateSourceType(savedItem.getCandidateSourceType());
            List<String> candidateSourceNames = savedItem == null
                    ? defaultRole == null ? List.of(definition.defaultRoleName()) : List.of(formatRoleSnapshotName(defaultRole))
                    : resolveBatchRecordAttachmentCandidateSourceNames(
                            candidateSourceType, candidateSourceIds, savedItem.getCandidateSourceNames(), enabledUserMap);
            Long defaultRoleId = defaultRole == null
                    ? resolveBatchRecordAttachmentDefaultRoleId(candidateSourceType, candidateSourceIds)
                    : defaultRole.getId();
            List<Long> assignedUserIds = resolveDefaultRoleAssignedEnabledUserIds(
                    defaultRoleId, enabledUserMap, assignedUserIdsByRoleId);
            result.add(new MesProRouteBatchRecordAttachmentOwnerRespVO()
                    .setAttachmentCode(definition.code())
                    .setAttachmentName(definition.name())
                    .setDefaultRoleCode(definition.defaultRoleCode())
                    .setDefaultRoleName(definition.defaultRoleName())
                    .setCandidateSourceType(candidateSourceType)
                    .setCandidateSourceIds(candidateSourceIds)
                    .setCandidateSourceNames(candidateSourceNames)
                    .setAssignedUserIds(assignedUserIds)
                    .setAssignedUserNames(assignedUserIds.stream()
                            .map(enabledUserMap::get)
                            .filter(Objects::nonNull)
                            .map(this::formatUserSnapshotName)
                            .toList())
                    .setSort(definition.sort())
                    .setRemark(savedItem == null ? null : savedItem.getRemark()));
        }
        return result;
    }

    private void saveBatchRecordAttachmentOwnerSnapshot(
            Long routeVersionId,
            List<MesProRouteBatchRecordAttachmentOwnerRespVO> owners) {
        List<Map<String, Object>> snapshot = owners.stream()
                .map(owner -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("attachmentCode", owner.getAttachmentCode());
                    item.put("attachmentName", owner.getAttachmentName());
                    item.put("defaultRoleCode", owner.getDefaultRoleCode());
                    item.put("defaultRoleName", owner.getDefaultRoleName());
                    item.put("candidateSourceType", owner.getCandidateSourceType());
                    item.put("candidateSourceIds", owner.getCandidateSourceIds());
                    item.put("candidateSourceNames", owner.getCandidateSourceNames());
                    item.put("sort", owner.getSort());
                    item.put("remark", owner.getRemark());
                    return item;
                })
                .toList();
        routeCandidateConfigService.saveConfigSnapshot(
                routeVersionId, BATCH_RECORD_ATTACHMENT_OWNERS_KEY, snapshot);
    }

    private RoleCategoryDO requireBatchRecordRoleCategory() {
        RoleCategoryDO category = roleCategoryMapper.selectByCode(BATCH_RECORD_ROLE_CATEGORY_CODE);
        if (category == null || !CommonStatusEnum.ENABLE.getStatus().equals(category.getStatus())) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_ROLE_CATEGORY_REQUIRED);
        }
        return category;
    }

    private RoleDO ensureBatchRecordAttachmentRole(BatchRecordAttachmentDefinition definition,
                                                   RoleCategoryDO category) {
        RoleDO role = roleMapper.selectByCode(definition.defaultRoleCode());
        if (role != null) {
            if (!CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())
                    || !Objects.equals(category.getId(), role.getCategoryId())) {
                throw exception(PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_OWNER_INVALID, definition.defaultRoleName());
            }
            return role;
        }
        RoleSaveReqVO reqVO = new RoleSaveReqVO();
        reqVO.setName(definition.defaultRoleName());
        reqVO.setCode(definition.defaultRoleCode());
        reqVO.setSort(900 + definition.sort());
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setCategoryId(category.getId());
        reqVO.setRemark("工序开始批记录附件默认上传角色");
        Long roleId = roleService.createRole(reqVO, null);
        RoleDO created = new RoleDO();
        created.setId(roleId);
        created.setName(definition.defaultRoleName());
        created.setCode(definition.defaultRoleCode());
        created.setStatus(CommonStatusEnum.ENABLE.getStatus());
        created.setCategoryId(category.getId());
        return created;
    }

    private List<Long> ensureBatchRecordAttachmentRoleUsers(BatchRecordAttachmentDefinition definition,
                                                            RoleDO role,
                                                            List<AdminUserDO> enabledUsers,
                                                            Map<Long, AdminUserDO> enabledUserMap) {
        Set<Long> assignedUserIds = permissionService.getUserRoleIdListByRoleId(Set.of(role.getId()));
        List<Long> enabledAssignedUserIds = assignedUserIds.stream()
                .filter(enabledUserMap::containsKey)
                .sorted()
                .toList();
        if (CollUtil.isNotEmpty(assignedUserIds)) {
            if (assignedUserIds.size() != enabledAssignedUserIds.size()
                    || enabledAssignedUserIds.size() < BATCH_RECORD_ATTACHMENT_MIN_USERS
                    || enabledAssignedUserIds.size() > BATCH_RECORD_ATTACHMENT_MAX_USERS) {
                throw exception(PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_OWNER_INVALID, definition.defaultRoleName());
            }
            return enabledAssignedUserIds;
        }
        List<AdminUserDO> selectedUsers = selectDeterministicAttachmentUsers(definition, enabledUsers);
        for (AdminUserDO user : selectedUsers) {
            Set<Long> nextRoleIds = new LinkedHashSet<>(permissionService.getUserRoleIdListByUserId(user.getId()));
            if (nextRoleIds.add(role.getId())) {
                permissionService.assignUserRole(user.getId(), nextRoleIds);
            }
        }
        return selectedUsers.stream()
                .map(AdminUserDO::getId)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    private List<AdminUserDO> selectDeterministicAttachmentUsers(BatchRecordAttachmentDefinition definition,
                                                                 List<AdminUserDO> enabledUsers) {
        int maxCount = Math.min(BATCH_RECORD_ATTACHMENT_MAX_USERS, enabledUsers.size());
        int countRange = maxCount - BATCH_RECORD_ATTACHMENT_MIN_USERS + 1;
        int selectedCount = BATCH_RECORD_ATTACHMENT_MIN_USERS
                + Math.floorMod(Objects.hash(TenantContextHolder.getTenantId(), definition.defaultRoleCode()), countRange);
        return enabledUsers.stream()
                .sorted(Comparator
                        .comparingInt((AdminUserDO user) -> Math.floorMod(
                                Objects.hash(TenantContextHolder.getTenantId(), definition.defaultRoleCode(), user.getId()),
                                Integer.MAX_VALUE))
                        .thenComparing(AdminUserDO::getId))
                .limit(selectedCount)
                .toList();
    }

    private Map<Long, AdminUserDO> loadEnabledCurrentTenantUserMap() {
        return convertMap(loadEnabledCurrentTenantUsers(), AdminUserDO::getId);
    }

    private List<AdminUserDO> loadEnabledCurrentTenantUsers() {
        return adminUserService.getUserListByStatus(CommonStatusEnum.ENABLE.getStatus()).stream()
                .filter(user -> user.getId() != null)
                .filter(user -> CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus()))
                .collect(Collectors.toMap(AdminUserDO::getId, user -> user, (left, right) -> left, LinkedHashMap::new))
                .values()
                .stream()
                .sorted(Comparator.comparing(AdminUserDO::getId))
                .toList();
    }

    private List<Long> resolveDefaultRoleAssignedEnabledUserIds(RoleDO role,
                                                                Map<Long, AdminUserDO> enabledUserMap) {
        return resolveDefaultRoleAssignedEnabledUserIds(
                role == null ? null : role.getId(), enabledUserMap, Collections.emptyMap());
    }

    private List<Long> resolveDefaultRoleAssignedEnabledUserIds(Long roleId,
                                                                Map<Long, AdminUserDO> enabledUserMap,
                                                                Map<Long, List<Long>> assignedUserIdsByRoleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        List<Long> assignedUserIds = assignedUserIdsByRoleId.get(roleId);
        if (assignedUserIds != null) {
            return assignedUserIds.stream()
                    .filter(enabledUserMap::containsKey)
                    .sorted()
                    .toList();
        }
        return permissionService.getUserRoleIdListByRoleId(Set.of(roleId)).stream()
                .filter(enabledUserMap::containsKey)
                .sorted()
                .toList();
    }

    private Long resolveBatchRecordAttachmentDefaultRoleId(String candidateSourceType, List<Long> candidateSourceIds) {
        if (!CANDIDATE_SOURCE_TYPE_ROLE.equals(candidateSourceType) || CollUtil.isEmpty(candidateSourceIds)) {
            return null;
        }
        return candidateSourceIds.get(0);
    }

    private List<String> resolveBatchRecordAttachmentCandidateSourceNames(
            String sourceType,
            List<Long> sourceIds,
            List<String> configuredNames,
            Map<Long, AdminUserDO> enabledUserMap) {
        List<String> names = normalizeCandidateSourceNames(configuredNames);
        if (!names.isEmpty()) {
            return names;
        }
        if (CANDIDATE_SOURCE_TYPE_USERS.equals(sourceType)) {
            return sourceIds.stream()
                    .map(enabledUserMap::get)
                    .filter(Objects::nonNull)
                    .map(this::formatUserSnapshotName)
                    .toList();
        }
        return roleService.getRoleList(sourceIds).stream()
                .filter(Objects::nonNull)
                .map(this::formatRoleSnapshotName)
                .toList();
    }

    private List<Long> normalizeBatchRecordAttachmentCandidateSourceIds(List<Long> ids) {
        List<Long> normalized = ids == null ? Collections.emptyList()
                : ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_OWNER_INVALID, "负责人为空");
        }
        return normalized;
    }

    private String normalizeBatchRecordAttachmentCandidateSourceType(String candidateSourceType) {
        String normalized = StrUtil.trim(candidateSourceType);
        if (CANDIDATE_SOURCE_TYPE_USER.equals(normalized)
                || CANDIDATE_SOURCE_TYPE_USERS.equals(normalized)) {
            return CANDIDATE_SOURCE_TYPE_USERS;
        }
        if (CANDIDATE_SOURCE_TYPE_ROLE.equals(normalized)) {
            return CANDIDATE_SOURCE_TYPE_ROLE;
        }
        throw exception(PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_OWNER_INVALID, candidateSourceType);
    }

    private void validateBatchRecordAttachmentCandidateSource(String sourceType,
                                                              List<Long> sourceIds,
                                                              Map<Long, AdminUserDO> enabledUserMap) {
        if (CANDIDATE_SOURCE_TYPE_USERS.equals(sourceType)) {
            boolean allEnabledCurrentTenantUsers = sourceIds.stream().allMatch(enabledUserMap::containsKey);
            if (!allEnabledCurrentTenantUsers) {
                throw exception(PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_OWNER_INVALID, sourceIds);
            }
            return;
        }
        roleService.validateRoleList(sourceIds);
    }

    private List<String> parseBatchRecordAttachmentCandidateSourceNames(Object rawValue) {
        if (rawValue == null) {
            return Collections.emptyList();
        }
        if (rawValue instanceof JSONArray array) {
            return normalizeCandidateSourceNames(array.stream()
                    .map(value -> value == null ? null : String.valueOf(value))
                    .toList());
        }
        if (rawValue instanceof List<?> list) {
            return normalizeCandidateSourceNames(list.stream()
                    .map(value -> value == null ? null : String.valueOf(value))
                    .toList());
        }
        String text = StrUtil.trim(String.valueOf(rawValue));
        if (StrUtil.isBlank(text)) {
            return Collections.emptyList();
        }
        if (text.startsWith("[") && text.endsWith("]")) {
            return normalizeCandidateSourceNames(JSON.parseArray(text, String.class));
        }
        return normalizeCandidateSourceNames(List.of(text));
    }

    private boolean isBatchRecordAttachmentCode(String attachmentCode) {
        return BATCH_RECORD_ATTACHMENT_DEFINITIONS.stream()
                .anyMatch(definition -> definition.code().equals(attachmentCode));
    }

    private String formatRoleSnapshotName(RoleDO role) {
        if (role == null) {
            return "";
        }
        if (StrUtil.isNotBlank(role.getName()) && StrUtil.isNotBlank(role.getCode())) {
            return role.getName() + "（" + role.getCode() + "）";
        }
        return StrUtil.blankToDefault(role.getName(), role.getCode());
    }

    private String formatUserSnapshotName(AdminUserDO user) {
        if (user == null) {
            return "";
        }
        if (StrUtil.isNotBlank(user.getNickname()) && StrUtil.isNotBlank(user.getUsername())) {
            return user.getNickname() + "（" + user.getUsername() + "）";
        }
        return StrUtil.blankToDefault(user.getNickname(),
                StrUtil.blankToDefault(user.getUsername(), String.valueOf(user.getId())));
    }

    private void requireRouteBatchRecordConfigEditAbility(MesProRouteDO route, String actionName) {
        permissionGateService.requireAbility(new MesProEdhrPermissionGateCommand()
                .setObjectType("ROUTE")
                .setObjectId(String.valueOf(route.getId()))
                .setAbility("ROUTE_EDIT")
                .setRouteId(route.getId())
                .setPermissionCode("mes:pro-route:batch-record-config:update")
                .setActionName(actionName));
    }

    private MesProRouteFlowProcessConfigDO findHistoricalProcessConfig(
            MesProRouteFlowConfigDO flowConfig, Long routeId, Long currentRouteProcessId, String useType) {
        return routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(routeId, useType).stream()
                .filter(config -> MesProRouteFlowContextMatcher.isProcessConfigOwnedBy(
                        flowConfig, config, routeId, useType))
                .filter(config -> config.getRouteProcessId() != null)
                .filter(config -> Objects.equals(currentRouteProcessId,
                        routeProcessService.resolveCurrentRouteProcess(
                                config.getRouteProcessId(), routeId, null).getId()))
                .findFirst()
                .orElse(null);
    }

    private void validateBatchProcessConfig(MesProRouteFlowConfigTypeEnum flowConfigType,
                                            MesProRouteFlowProcessConfigSaveReqVO saveConfig) {
        if (saveConfig.getProductionQuantityFactor() != null
                && saveConfig.getProductionQuantityFactor().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_PRODUCTION_QUANTITY_FACTOR_INVALID, saveConfig.getRouteProcessId());
        }
        if (flowConfigType != MesProRouteFlowConfigTypeEnum.BATCH) {
            return;
        }
        resolveExecutionMode(flowConfigType, saveConfig.getExecutionMode());
    }

    private String resolveExecutionMode(MesProRouteFlowConfigTypeEnum flowConfigType, String executionMode) {
        String normalized = StrUtil.blankToDefault(StrUtil.trim(executionMode), EXECUTION_MODE_SEQUENTIAL);
        if (EXECUTION_MODE_SEQUENTIAL.equals(normalized) || EXECUTION_MODE_PARALLEL.equals(normalized)) {
            return normalized;
        }
        throw exception(PRO_ROUTE_FLOW_CONFIG_EXECUTION_MODE_INVALID);
    }

    private BigDecimal resolveProductionQuantityFactor(Long routeProcessId, BigDecimal factor) {
        if (factor == null) {
            return DEFAULT_PRODUCTION_QUANTITY_FACTOR;
        }
        if (factor.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_PRODUCTION_QUANTITY_FACTOR_INVALID, routeProcessId);
        }
        return factor.setScale(6, RoundingMode.HALF_UP);
    }

    private String resolveRouteFlowPermissionCode(MesProRouteFlowConfigTypeEnum flowConfigType) {
        if (flowConfigType == MesProRouteFlowConfigTypeEnum.SCHEDULE) {
            return "mes:pro-route:schedule-config:update";
        }
        if (flowConfigType == MesProRouteFlowConfigTypeEnum.BATCH) {
            return "mes:pro-route:batch-record-config:update";
        }
        throw exception(PRO_ROUTE_FLOW_TYPE_INVALID);
    }

    private void replaceBatchRecordReports(MesProRouteDO route, MesProRouteFlowConfigTypeEnum flowConfigType,
                                           MesProRouteFlowProcessConfigDO processConfig,
                                           MesProRouteFlowProcessConfigSaveReqVO saveConfig) {
        Map<String, Long> existingPermissionScopeIds = loadExistingPermissionScopeIds(
                processConfig, flowConfigType);
        routeFlowProcessBatchRecordMapper.deleteByRouteProcessIdAndUseType(
                processConfig.getRouteProcessId(), flowConfigType.getType());
        if (flowConfigType != MesProRouteFlowConfigTypeEnum.BATCH) {
            return;
        }
        List<MesProRouteFlowBatchRecordSaveReqVO> reports = normalizeBatchRecordReports(saveConfig);
        Map<String, MesProBatchRecordReportDO> reportMap = loadReportMap(reports.stream()
                .map(MesProRouteFlowBatchRecordSaveReqVO::getBatchRecordReportId)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        for (MesProRouteFlowBatchRecordSaveReqVO report : reports) {
            Long permissionScopeId = resolvePermissionScopeId(report, existingPermissionScopeIds);
            String batchRecordReportId = StrUtil.trim(report.getBatchRecordReportId());
            MesProBatchRecordReportDO metadata = reportMap.get(batchRecordReportId);
            String formSlotType = resolveConfiguredFormSlotType(report, metadata);
            String instanceScope = resolveInstanceScope(report.getInstanceScope());
            validateSharedFormBinding(instanceScope, report);
            String recordCategory = resolveRecordCategory(report.getRecordCategory(), formSlotType);
            String validationProfile = resolveValidationProfile(recordCategory, report.getValidationProfile());
            Boolean recordbookEnabled = resolveRecordbookEnabled(report.getRecordbookEnabled(), recordCategory);
            String requiredPolicy = resolveRequiredPolicy(report.getRequiredPolicy());
            String archiveVisibility = resolveArchiveVisibility(report.getArchiveVisibility());
            String ownerRoleKey = resolveOwnerRoleKey(report.getOwnerRoleKey(), formSlotType);
            String slotConfigSnapshotHash = buildSlotConfigSnapshotHash(route.getId(), processConfig.getRouteProcessId(),
                    report, permissionScopeId, formSlotType, requiredPolicy, archiveVisibility,
                    recordCategory, validationProfile);
            routeFlowProcessBatchRecordMapper.insert(MesProRouteFlowProcessBatchRecordDO.builder()
                    .routeFlowProcessConfigId(processConfig.getId())
                    .routeId(route.getId())
                    .routeProcessId(processConfig.getRouteProcessId())
                    .useType(flowConfigType.getType())
                    .batchRecordReportId(batchRecordReportId)
                    .formSlotType(formSlotType)
                    .instanceScope(instanceScope)
                    .sharedFormKey(StrUtil.blankToDefault(StrUtil.trim(report.getSharedFormKey()), null))
                    .fillableScopeJson(StrUtil.blankToDefault(StrUtil.trim(report.getFillableScopeJson()), null))
                    .recordCategory(recordCategory)
                    .validationProfile(validationProfile)
                    .recordbookEnabled(recordbookEnabled)
                    .permissionScopeId(permissionScopeId)
                    .recordCategorySnapshotHash(buildRecordCategorySnapshotHash(route.getId(),
                            processConfig.getRouteProcessId(), report, permissionScopeId,
                            recordCategory, validationProfile))
                    .requiredPolicy(requiredPolicy)
                    .requiredConditionJson(StrUtil.blankToDefault(StrUtil.trim(report.getRequiredConditionJson()), null))
                    .ownerRoleKey(ownerRoleKey)
                    .archiveVisibility(archiveVisibility)
                    .slotConfigSnapshotHash(slotConfigSnapshotHash)
                    .reportSort(report.getReportSort())
                    .remark(report.getRemark())
                    .build());
        }
        insertDynamicFormBindings(route, flowConfigType, processConfig, saveConfig);
    }

    private void insertDynamicFormBindings(MesProRouteDO route, MesProRouteFlowConfigTypeEnum flowConfigType,
                                           MesProRouteFlowProcessConfigDO processConfig,
                                           MesProRouteFlowProcessConfigSaveReqVO saveConfig) {
        List<MesProRouteFlowFormBindingSaveReqVO> bindings = saveConfig.getFormBindings() == null
                ? Collections.emptyList() : saveConfig.getFormBindings();
        for (int index = 0; index < bindings.size(); index++) {
            MesProRouteFlowFormBindingSaveReqVO binding = bindings.get(index);
            String formSlotType = resolveConfiguredFormSlotType(binding);
            String recordCategory = resolveRecordCategory(binding.getRecordCategory(), formSlotType);
            String validationProfile = resolveValidationProfile(recordCategory, binding.getValidationProfile());
            Boolean recordbookEnabled = resolveRecordbookEnabled(binding.getRecordbookEnabled(), recordCategory);
            String requiredPolicy = resolveRequiredPolicy(binding.getRequiredPolicy());
            String ownerRoleKey = resolveOwnerRoleKey(binding.getOwnerRoleKey(), formSlotType);
            String archiveVisibility = resolveArchiveVisibility(binding.getArchiveVisibility());
            routeFlowProcessBatchRecordMapper.insert(MesProRouteFlowProcessBatchRecordDO.builder()
                    .routeFlowProcessConfigId(processConfig.getId())
                    .routeId(route.getId())
                    .routeProcessId(processConfig.getRouteProcessId())
                    .useType(flowConfigType.getType())
                    .batchRecordReportId(null)
                    .batchRecordDefinitionId(null)
                    .batchRecordVersionId(null)
                    .formSlotType(formSlotType)
                    .formBindingKey(resolveFormBindingKey(processConfig.getRouteProcessId(), binding, index + 1))
                    .formTemplateId(binding.getFormTemplateId())
                    .formTemplateNameSnapshot(StrUtil.trim(binding.getFormTemplateName()))
                    .lastPublishedTemplateVersionId(binding.getLastPublishedTemplateVersionId())
                    .lastPublishedTemplateVersionNo(StrUtil.trim(binding.getLastPublishedTemplateVersionNo()))
                    .instanceScope(resolveInstanceScope(binding.getInstanceScope()))
                    .sharedFormKey(StrUtil.blankToDefault(StrUtil.trim(binding.getSharedFormKey()), null))
                    .fillableScopeJson(StrUtil.blankToDefault(StrUtil.trim(binding.getFillableScopeJson()), null))
                    .recordCategory(recordCategory)
                    .validationProfile(validationProfile)
                    .recordbookEnabled(recordbookEnabled)
                    .permissionScopeId(binding.getPermissionScopeId())
                    .requiredPolicy(requiredPolicy)
                    .requiredConditionJson(StrUtil.blankToDefault(StrUtil.trim(binding.getRequiredConditionJson()), null))
                    .ownerRoleKey(ownerRoleKey)
                    .archiveVisibility(archiveVisibility)
                    .candidateSourceType(binding.getCandidateSourceType())
                    .candidateSourceIds(joinCandidateSourceIds(binding.getCandidateSourceIds()))
                    .candidateSourceNames(JSON.toJSONString(normalizeCandidateSourceNames(binding.getCandidateSourceNames())))
                    .reportSort(binding.getReportSort())
                    .remark(binding.getRemark())
                    .build());
        }
    }

    private String joinCandidateSourceIds(List<Long> candidateSourceIds) {
        if (CollUtil.isEmpty(candidateSourceIds)) {
            return null;
        }
        return candidateSourceIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private Map<String, Long> loadExistingPermissionScopeIds(
            MesProRouteFlowProcessConfigDO processConfig,
            MesProRouteFlowConfigTypeEnum flowConfigType) {
        if (flowConfigType != MesProRouteFlowConfigTypeEnum.BATCH || processConfig == null
                || processConfig.getRouteProcessId() == null) {
            return Collections.emptyMap();
        }
        return routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(
                        List.of(processConfig.getRouteProcessId()), flowConfigType.getType())
                .stream()
                .filter(record -> isOwnedByProcessConfig(record, processConfig,
                        processConfig.getRouteId(), flowConfigType.getType()))
                .filter(record -> StrUtil.isNotBlank(record.getBatchRecordReportId())
                        && record.getPermissionScopeId() != null)
                .collect(Collectors.toMap(record -> StrUtil.trim(record.getBatchRecordReportId()),
                        MesProRouteFlowProcessBatchRecordDO::getPermissionScopeId, (left, right) -> left,
                        LinkedHashMap::new));
    }

    private boolean isOwnedByProcessConfig(MesProRouteFlowProcessBatchRecordDO record,
                                           MesProRouteFlowProcessConfigDO processConfig,
                                           Long routeId,
                                           String useType) {
        return record != null && processConfig != null
                && Objects.equals(record.getRouteFlowProcessConfigId(), processConfig.getId())
                && Objects.equals(record.getRouteId(), routeId)
                && Objects.equals(processConfig.getRouteId(), routeId)
                && Objects.equals(record.getRouteProcessId(), processConfig.getRouteProcessId())
                && Objects.equals(record.getUseType(), useType)
                && Objects.equals(processConfig.getUseType(), useType);
    }

    private Long resolvePermissionScopeId(MesProRouteFlowBatchRecordSaveReqVO report,
                                          Map<String, Long> existingPermissionScopeIds) {
        if (report.getPermissionScopeId() != null) {
            return report.getPermissionScopeId();
        }
        return existingPermissionScopeIds.get(StrUtil.trim(report.getBatchRecordReportId()));
    }

    private List<MesProRouteFlowBatchRecordSaveReqVO> normalizeBatchRecordReports(
            MesProRouteFlowProcessConfigSaveReqVO saveConfig) {
        if (CollUtil.isEmpty(saveConfig.getBatchRecordReports())) {
            return Collections.emptyList();
        }
        return saveConfig.getBatchRecordReports().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(MesProRouteFlowBatchRecordSaveReqVO::getReportSort,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private List<MesProRouteFlowFormBindingSaveReqVO> normalizeFormBindings(
            MesProRouteFlowProcessConfigSaveReqVO saveConfig) {
        if (saveConfig == null || CollUtil.isEmpty(saveConfig.getFormBindings())) {
            return Collections.emptyList();
        }
        return saveConfig.getFormBindings().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(MesProRouteFlowFormBindingSaveReqVO::getReportSort,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private List<MesProRouteFlowFormBindingSaveReqVO> resolveAndNormalizeFormBindings(
            MesProRouteFlowProcessConfigSaveReqVO saveConfig) {
        List<MesProRouteFlowFormBindingSaveReqVO> bindings = normalizeFormBindings(saveConfig);
        if (bindings.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> templateIds = new LinkedHashSet<>();
        List<MesProRouteFlowFormBindingSaveReqVO> normalized = new ArrayList<>(bindings.size());
        for (int index = 0; index < bindings.size(); index++) {
            MesProRouteFlowFormBindingSaveReqVO binding = bindings.get(index);
            if (binding.getFormTemplateId() == null
                    || binding.getReportSort() == null || binding.getReportSort() <= 0) {
                throw exception(PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_REQUIRED);
            }
            if (!templateIds.add(binding.getFormTemplateId())) {
                throw exception(PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_DUPLICATE);
            }
            FormTemplateVersionDO publishedVersion = resolveLatestPublishedTemplateVersion(binding.getFormTemplateId());
            String formSlotType = resolveConfiguredFormSlotType(binding);
            String instanceScope = resolveInstanceScope(binding.getInstanceScope());
            validateSharedFormBinding(instanceScope, binding);
            String recordCategory = resolveRecordCategory(binding.getRecordCategory(), formSlotType);
            String validationProfile = resolveValidationProfile(recordCategory, binding.getValidationProfile());
            Boolean recordbookEnabled = resolveRecordbookEnabled(binding.getRecordbookEnabled(), recordCategory);
            String requiredPolicy = resolveRequiredPolicy(binding.getRequiredPolicy());
            if (REQUIRED_POLICY_CONDITIONAL_REQUIRED.equals(requiredPolicy)
                    && StrUtil.isBlank(binding.getRequiredConditionJson())) {
                throw exception(PRO_ROUTE_FLOW_CONFIG_CONDITION_CONFIG_MISSING);
            }
            String candidateSourceType = resolveCandidateSourceType(binding);
            List<Long> candidateSourceIds = normalizeCandidateSourceIds(binding);
            validateFormBindingCandidateSource(binding, candidateSourceType, candidateSourceIds);
            String archiveVisibility = resolveArchiveVisibility(binding.getArchiveVisibility());
            normalized.add(new MesProRouteFlowFormBindingSaveReqVO()
                    .setFormBindingKey(resolveFormBindingKey(saveConfig.getRouteProcessId(), binding, index + 1))
                    .setFormTemplateId(binding.getFormTemplateId())
                    .setFormTemplateName(StrUtil.trim(publishedVersion.getTemplateName()))
                    .setFormSlotType(formSlotType)
                    .setLastPublishedTemplateVersionId(publishedVersion.getId())
                    .setLastPublishedTemplateVersionNo(StrUtil.trim(publishedVersion.getVersionNo()))
                    .setInstanceScope(instanceScope)
                    .setSharedFormKey(StrUtil.blankToDefault(StrUtil.trim(binding.getSharedFormKey()), null))
                    .setFillableScopeJson(StrUtil.blankToDefault(StrUtil.trim(binding.getFillableScopeJson()), null))
                    .setRecordCategory(recordCategory)
                    .setValidationProfile(validationProfile)
                    .setRecordbookEnabled(recordbookEnabled)
                    .setPermissionScopeId(binding.getPermissionScopeId())
                    .setRequiredPolicy(requiredPolicy)
                    .setRequiredConditionJson(StrUtil.blankToDefault(StrUtil.trim(binding.getRequiredConditionJson()), null))
                    .setOwnerRoleKey(resolveOwnerRoleKey(binding.getOwnerRoleKey(), formSlotType))
                    .setArchiveVisibility(archiveVisibility)
                    .setCandidateSourceType(candidateSourceType)
                    .setCandidateSourceIds(candidateSourceIds)
                    .setCandidateSourceNames(normalizeCandidateSourceNames(binding.getCandidateSourceNames()))
                    .setReportSort(binding.getReportSort())
                    .setRemark(binding.getRemark()));
        }
        return normalized;
    }

    private FormTemplateVersionDO resolveLatestPublishedTemplateVersion(Long templateId) {
        FormTemplateVersionDO publishedVersion = formTemplateVersionMapper.selectLatestPublishedByTemplateId(
                TenantContextHolder.getRequiredTenantId(), templateId);
        if (publishedVersion == null || publishedVersion.getId() == null) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_PUBLISHED_VERSION_NOT_EXISTS, templateId);
        }
        return publishedVersion;
    }

    private String resolveFormBindingKey(Long routeProcessId, MesProRouteFlowFormBindingSaveReqVO binding,
                                          int defaultSort) {
        String configuredKey = StrUtil.trim(binding.getFormBindingKey());
        if (StrUtil.isNotBlank(configuredKey)) {
            return configuredKey;
        }
        Long templateId = binding.getFormTemplateId();
        if (routeProcessId == null || templateId == null) {
            return "FB_" + defaultSort;
        }
        return "FB_" + routeProcessId + "_" + templateId;
    }

    private String resolveCandidateSourceType(MesProRouteFlowFormBindingSaveReqVO binding) {
        return normalizeCandidateSourceTypeOptional(binding.getCandidateSourceType(),
                binding.getFormBindingKey());
    }

    private String normalizeCandidateSourceTypeOptional(String candidateSourceType) {
        return normalizeCandidateSourceTypeOptional(candidateSourceType, "unknown");
    }

    private String normalizeCandidateSourceTypeOptional(String candidateSourceType, String formBindingKey) {
        String normalized = StrUtil.trim(candidateSourceType);
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        if (CANDIDATE_SOURCE_TYPE_USER.equals(normalized)
                || CANDIDATE_SOURCE_TYPE_USERS.equals(normalized)) {
            return CANDIDATE_SOURCE_TYPE_USERS;
        }
        if (CANDIDATE_SOURCE_TYPE_ROLE.equals(normalized)) {
            return CANDIDATE_SOURCE_TYPE_ROLE;
        }
        throw exception(PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_FILLER_SOURCE_INVALID, formBindingKey, normalized);
    }

    private List<Long> normalizeCandidateSourceIds(MesProRouteFlowFormBindingSaveReqVO binding) {
        List<Long> ids = binding.getCandidateSourceIds() == null
                ? Collections.emptyList() : binding.getCandidateSourceIds();
        List<Long> normalized = ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        return normalized;
    }

    private List<String> normalizeCandidateSourceNames(List<String> names) {
        if (CollUtil.isEmpty(names)) {
            return Collections.emptyList();
        }
        return names.stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
    }

    private void validateFormBindingCandidateSource(MesProRouteFlowFormBindingSaveReqVO binding,
                                                    String candidateSourceType,
                                                    List<Long> candidateSourceIds) {
        if (StrUtil.isBlank(candidateSourceType) || candidateSourceIds.size() != 1) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_FILLER_REQUIRED, binding.getFormBindingKey());
        }
        if (CANDIDATE_SOURCE_TYPE_USERS.equals(candidateSourceType)) {
            adminUserApi.validateUserList(candidateSourceIds);
            return;
        }
        if (CANDIDATE_SOURCE_TYPE_ROLE.equals(candidateSourceType)) {
            roleApi.validRoleList(candidateSourceIds);
            return;
        }
        throw exception(PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_FILLER_SOURCE_INVALID,
                binding.getFormBindingKey(), candidateSourceType);
    }

    private List<Long> parseCandidateSourceIds(Object rawValue) {
        if (rawValue == null) {
            return Collections.emptyList();
        }
        if (rawValue instanceof JSONArray array) {
            return array.stream()
                    .map(value -> value == null ? null : Long.valueOf(String.valueOf(value)))
                    .filter(Objects::nonNull)
                    .toList();
        }
        if (rawValue instanceof List<?> list) {
            return list.stream()
                    .map(value -> value == null ? null : Long.valueOf(String.valueOf(value)))
                    .filter(Objects::nonNull)
                    .toList();
        }
        String text = StrUtil.trim(String.valueOf(rawValue));
        if (StrUtil.isBlank(text)) {
            return Collections.emptyList();
        }
        return java.util.Arrays.stream(text.split(","))
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .map(Long::valueOf)
                .toList();
    }

    private List<Long> parseCandidateSourceIdSnapshot(String rawValue) {
        return parseCandidateSourceIds(rawValue);
    }

    private List<String> parseCandidateSourceNameSnapshot(String rawValue) {
        return parseCandidateSourceNames(rawValue);
    }

    private Map<String, MesProBatchRecordReportDO> loadReportMap(Set<String> reportIds) {
        if (CollUtil.isEmpty(reportIds)) {
            return Collections.emptyMap();
        }
        return convertMap(batchRecordReportMapper.selectListByReportIds(reportIds), MesProBatchRecordReportDO::getReportId);
    }

    private List<MesProRouteFlowBatchRecordRespVO> toBatchRecordRespList(
            List<MesProRouteFlowProcessBatchRecordDO> records,
            Map<String, MesProBatchRecordReportDO> reportMap) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        return records.stream()
                .filter(record -> StrUtil.isNotBlank(record.getBatchRecordReportId()))
                .sorted(Comparator.comparing(MesProRouteFlowProcessBatchRecordDO::getReportSort,
                        Comparator.nullsLast(Integer::compareTo)))
                .map(record -> {
                    MesProBatchRecordReportDO report = reportMap.get(record.getBatchRecordReportId());
                    MesProRouteFlowBatchRecordRespVO vo = new MesProRouteFlowBatchRecordRespVO();
                    vo.setBatchRecordReportId(record.getBatchRecordReportId());
                    vo.setBatchRecordReportCode(report == null ? null : report.getReportCode());
                    vo.setBatchRecordReportName(report == null ? null : report.getReportName());
                    vo.setBatchRecordDefinitionId(record.getBatchRecordDefinitionId());
                    vo.setBatchRecordVersionId(record.getBatchRecordVersionId());
                    vo.setFormSlotType(resolveFormSlotType(record, report));
                    vo.setInstanceScope(resolveExistingInstanceScope(record));
                    vo.setSharedFormKey(record.getSharedFormKey());
                    vo.setFillableScopeJson(record.getFillableScopeJson());
                    vo.setRecordCategory(resolveExistingRecordCategory(record));
                    vo.setValidationProfile(resolveExistingValidationProfile(record));
                    vo.setRecordbookEnabled(resolveRecordbookEnabled(record.getRecordbookEnabled(), vo.getRecordCategory()));
                    vo.setPermissionScopeId(record.getPermissionScopeId());
                    vo.setRequiredPolicy(resolveExistingRequiredPolicy(record));
                    vo.setRequiredConditionJson(record.getRequiredConditionJson());
                    vo.setOwnerRoleKey(record.getOwnerRoleKey());
                    vo.setArchiveVisibility(resolveExistingArchiveVisibility(record));
                    vo.setSlotConfigSnapshotHash(record.getSlotConfigSnapshotHash());
                    vo.setReportSort(record.getReportSort());
                    vo.setRemark(record.getRemark());
                    return vo;
                })
                .toList();
    }

    private List<MesProRouteFlowFormBindingRespVO> toFormBindingRespList(
            List<MesProRouteFlowProcessBatchRecordDO> records) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        return records.stream()
                .filter(record -> record.getFormTemplateId() != null)
                .sorted(Comparator.comparing(MesProRouteFlowProcessBatchRecordDO::getReportSort,
                        Comparator.nullsLast(Integer::compareTo)))
                .map(record -> {
                    MesProRouteFlowFormBindingRespVO vo = new MesProRouteFlowFormBindingRespVO();
                    vo.setFormBindingKey(record.getFormBindingKey());
                    String formSlotType = resolveFormSlotType(record, null);
                    vo.setFormSlotType(formSlotType);
                    vo.setFormTemplateId(record.getFormTemplateId());
                    vo.setFormTemplateName(record.getFormTemplateNameSnapshot());
                    vo.setLastPublishedTemplateVersionId(record.getLastPublishedTemplateVersionId());
                    vo.setLastPublishedTemplateVersionNo(record.getLastPublishedTemplateVersionNo());
                    vo.setInstanceScope(resolveExistingInstanceScope(record));
                    vo.setSharedFormKey(record.getSharedFormKey());
                    vo.setFillableScopeJson(record.getFillableScopeJson());
                    vo.setRecordCategory(resolveRecordCategory(record.getRecordCategory(), formSlotType));
                    vo.setValidationProfile(resolveValidationProfile(vo.getRecordCategory(), record.getValidationProfile()));
                    vo.setRecordbookEnabled(resolveRecordbookEnabled(record.getRecordbookEnabled(), vo.getRecordCategory()));
                    vo.setPermissionScopeId(record.getPermissionScopeId());
                    vo.setRequiredPolicy(resolveExistingRequiredPolicy(record));
                    vo.setRequiredConditionJson(record.getRequiredConditionJson());
                    vo.setOwnerRoleKey(resolveOwnerRoleKey(record.getOwnerRoleKey(), formSlotType));
                    vo.setArchiveVisibility(resolveExistingArchiveVisibility(record));
                    vo.setSlotConfigSnapshotHash(record.getSlotConfigSnapshotHash());
                    vo.setCandidateSourceType(normalizeCandidateSourceTypeOptional(record.getCandidateSourceType()));
                    vo.setCandidateSourceIds(parseCandidateSourceIdSnapshot(record.getCandidateSourceIds()));
                    vo.setCandidateSourceNames(parseCandidateSourceNameSnapshot(record.getCandidateSourceNames()));
                    vo.setReportSort(record.getReportSort());
                    vo.setRemark(record.getRemark());
                    return vo;
                })
                .toList();
    }

    private MesProRouteFlowConfigTypeEnum validateUseType(String useType) {
        MesProRouteFlowConfigTypeEnum flowConfigType = MesProRouteFlowConfigTypeEnum.valueOfType(useType);
        if (flowConfigType == null) {
            throw exception(PRO_ROUTE_FLOW_TYPE_INVALID);
        }
        return flowConfigType;
    }

    private void validateRecordCategory(String recordCategory, String formSlotType) {
        String category = StrUtil.blankToDefault(StrUtil.trim(recordCategory), defaultRecordCategory(formSlotType));
        if (!RECORD_CATEGORY_BATCH.equals(category) && !RECORD_CATEGORY_INTERNAL.equals(category)) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_RECORD_CATEGORY_INVALID);
        }
    }

    private String resolveRecordCategory(String recordCategory, String formSlotType) {
        validateRecordCategory(recordCategory, formSlotType);
        return StrUtil.blankToDefault(StrUtil.trim(recordCategory), defaultRecordCategory(formSlotType));
    }

    private String defaultRecordCategory(String formSlotType) {
        return SLOT_TYPE_MAIN.equals(MesProBatchRecordFormSlotType.normalize(formSlotType))
                ? RECORD_CATEGORY_BATCH : RECORD_CATEGORY_INTERNAL;
    }

    private void validateValidationProfile(String recordCategory, String validationProfile) {
        String category = StrUtil.trim(recordCategory);
        String expectedProfile = RECORD_CATEGORY_INTERNAL.equals(category)
                ? VALIDATION_PROFILE_INTERNAL : VALIDATION_PROFILE_BATCH;
        String profile = StrUtil.blankToDefault(StrUtil.trim(validationProfile), expectedProfile);
        if (!expectedProfile.equals(profile)) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_VALIDATION_PROFILE_MISMATCH);
        }
    }

    private String resolveValidationProfile(String recordCategory, String validationProfile) {
        validateValidationProfile(recordCategory, validationProfile);
        String expectedProfile = RECORD_CATEGORY_INTERNAL.equals(StrUtil.trim(recordCategory))
                ? VALIDATION_PROFILE_INTERNAL : VALIDATION_PROFILE_BATCH;
        return StrUtil.blankToDefault(StrUtil.trim(validationProfile), expectedProfile);
    }

    private Boolean resolveRecordbookEnabled(Boolean recordbookEnabled, String recordCategory) {
        if (RECORD_CATEGORY_INTERNAL.equals(StrUtil.trim(recordCategory))) {
            return Boolean.FALSE;
        }
        return recordbookEnabled == null ? Boolean.TRUE : recordbookEnabled;
    }

    private String resolveExistingRecordCategory(MesProRouteFlowProcessBatchRecordDO record) {
        String formSlotType = MesProBatchRecordFormSlotType.normalize(record.getFormSlotType());
        return resolveRecordCategory(record.getRecordCategory(), formSlotType);
    }

    private String resolveExistingValidationProfile(MesProRouteFlowProcessBatchRecordDO record) {
        String formSlotType = MesProBatchRecordFormSlotType.normalize(record.getFormSlotType());
        String recordCategory = resolveRecordCategory(record.getRecordCategory(), formSlotType);
        return resolveValidationProfile(recordCategory, record.getValidationProfile());
    }

    private String resolveFormSlotType(MesProBatchRecordReportDO report) {
        return MesProBatchRecordFormSlotType.normalize(report == null ? null : report.getFormSlotType());
    }

    private String resolveFormSlotType(MesProRouteFlowProcessBatchRecordDO record, MesProBatchRecordReportDO report) {
        String rawRecordFormSlotType = StrUtil.trim(record == null ? null : record.getFormSlotType());
        if (StrUtil.isNotBlank(rawRecordFormSlotType)) {
            String normalized = MesProBatchRecordFormSlotType.normalize(rawRecordFormSlotType);
            if (StrUtil.isNotBlank(normalized)) {
                return normalized;
            }
        }
        return MesProBatchRecordFormSlotType.normalize(report == null ? null : report.getFormSlotType());
    }

    private String resolveConfiguredFormSlotType(MesProRouteFlowBatchRecordSaveReqVO report) {
        return resolveConfiguredFormSlotType(report, null);
    }

    private String resolveConfiguredFormSlotType(MesProRouteFlowFormBindingSaveReqVO binding) {
        String normalized = MesProBatchRecordFormSlotType.normalize(binding == null ? null : binding.getFormSlotType());
        if (StrUtil.isBlank(normalized)) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_FORM_SLOT_TYPE_INVALID);
        }
        return normalized;
    }

    private String resolveConfiguredFormSlotType(MesProRouteFlowBatchRecordSaveReqVO report,
                                                 MesProBatchRecordReportDO metadata) {
        String raw = StrUtil.blankToDefault(StrUtil.trim(report.getFormSlotType()),
                metadata == null ? null : StrUtil.trim(metadata.getFormSlotType()));
        String normalized = MesProBatchRecordFormSlotType.normalize(raw);
        if (StrUtil.isBlank(normalized)) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_FORM_SLOT_TYPE_INVALID);
        }
        return normalized;
    }

    private void validateSlotPolicy(MesProRouteFlowBatchRecordSaveReqVO report, String formSlotType) {
        String requiredPolicy = resolveRequiredPolicy(report.getRequiredPolicy());
        if (REQUIRED_POLICY_CONDITIONAL_REQUIRED.equals(requiredPolicy)
                && StrUtil.isBlank(report.getRequiredConditionJson())) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_CONDITION_CONFIG_MISSING);
        }
        resolveArchiveVisibility(report.getArchiveVisibility());
    }

    private String resolveOwnerRoleKey(String ownerRoleKey, String formSlotType) {
        String configured = StrUtil.trim(ownerRoleKey);
        if (StrUtil.isNotBlank(configured)) {
            return configured;
        }
        String normalizedFormSlotType = MesProBatchRecordFormSlotType.normalize(formSlotType);
        if (SLOT_TYPE_PROCESS_INSPECTION.equals(normalizedFormSlotType)) {
            return OWNER_ROLE_QUALITY;
        }
        if (SLOT_TYPE_PARAMETER_RECORD.equals(normalizedFormSlotType)) {
            return OWNER_ROLE_EQUIPMENT;
        }
        return OWNER_ROLE_PRODUCTION;
    }

    private String resolveRequiredPolicy(String requiredPolicy) {
        String policy = StrUtil.blankToDefault(StrUtil.trim(requiredPolicy), REQUIRED_POLICY_REQUIRED);
        if (!REQUIRED_POLICY_REQUIRED.equals(policy)
                && !REQUIRED_POLICY_CONDITIONAL_REQUIRED.equals(policy)
                && !REQUIRED_POLICY_OPTIONAL.equals(policy)
                && !REQUIRED_POLICY_SKIPPABLE_CONTROLLED.equals(policy)) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_REQUIRED_POLICY_INVALID);
        }
        return policy;
    }

    private String resolveExistingRequiredPolicy(MesProRouteFlowProcessBatchRecordDO record) {
        return resolveRequiredPolicy(record.getRequiredPolicy());
    }

    private String resolveArchiveVisibility(String archiveVisibility) {
        String visibility = StrUtil.blankToDefault(StrUtil.trim(archiveVisibility), ARCHIVE_VISIBILITY_FINAL_DHR);
        if (!ARCHIVE_VISIBILITY_FINAL_DHR.equals(visibility)
                && !ARCHIVE_VISIBILITY_INTERNAL_REVIEW.equals(visibility)
                && !ARCHIVE_VISIBILITY_AUDIT_ONLY.equals(visibility)
                && !ARCHIVE_VISIBILITY_ATTACHMENT_REFERENCE.equals(visibility)) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_REQUIRED_POLICY_INVALID);
        }
        return visibility;
    }

    private String resolveExistingArchiveVisibility(MesProRouteFlowProcessBatchRecordDO record) {
        return resolveArchiveVisibility(record.getArchiveVisibility());
    }

    private String buildRecordCategorySnapshotHash(Long routeId, Long routeProcessId,
                                                   MesProRouteFlowBatchRecordSaveReqVO report,
                                                   Long permissionScopeId,
                                                   String recordCategory,
                                                   String validationProfile) {
        return DigestUtil.sha256Hex(String.join("|",
                nullToEmpty(routeId),
                nullToEmpty(routeProcessId),
                StrUtil.nullToEmpty(report.getBatchRecordReportId()),
                StrUtil.nullToEmpty(recordCategory),
                StrUtil.nullToEmpty(validationProfile),
                nullToEmpty(resolveRecordbookEnabled(report.getRecordbookEnabled(), recordCategory)),
                nullToEmpty(permissionScopeId),
                nullToEmpty(report.getReportSort())));
    }

    private String buildSlotConfigSnapshotHash(Long routeId, Long routeProcessId,
                                               MesProRouteFlowBatchRecordSaveReqVO report,
                                                Long permissionScopeId, String formSlotType,
                                                String requiredPolicy, String archiveVisibility,
                                                String recordCategory, String validationProfile) {
        return DigestUtil.sha256Hex(String.join("|",
                nullToEmpty(routeId),
                nullToEmpty(routeProcessId),
                StrUtil.nullToEmpty(report.getBatchRecordReportId()),
                StrUtil.nullToEmpty(formSlotType),
                StrUtil.nullToEmpty(recordCategory),
                StrUtil.nullToEmpty(validationProfile),
                nullToEmpty(resolveRecordbookEnabled(report.getRecordbookEnabled(), recordCategory)),
                nullToEmpty(permissionScopeId),
                StrUtil.nullToEmpty(requiredPolicy),
                StrUtil.nullToEmpty(report.getRequiredConditionJson()),
                StrUtil.nullToEmpty(report.getOwnerRoleKey()),
                StrUtil.nullToEmpty(archiveVisibility),
                nullToEmpty(report.getReportSort()),
                StrUtil.nullToEmpty(resolveInstanceScope(report.getInstanceScope())),
                StrUtil.nullToEmpty(report.getSharedFormKey()),
                StrUtil.nullToEmpty(report.getFillableScopeJson())));
    }

    private String resolveInstanceScope(String instanceScope) {
        String scope = StrUtil.blankToDefault(StrUtil.trim(instanceScope), "PROCESS");
        if (!"PROCESS".equals(scope) && !"BATCH_SHARED".equals(scope)) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_REQUIRED_POLICY_INVALID);
        }
        return scope;
    }

    private String resolveExistingInstanceScope(MesProRouteFlowProcessBatchRecordDO record) {
        return resolveInstanceScope(record.getInstanceScope());
    }

    private void validateSharedFormBinding(String instanceScope, MesProRouteFlowBatchRecordSaveReqVO report) {
        if (!"BATCH_SHARED".equals(instanceScope)) {
            return;
        }
        if (StrUtil.isBlank(report.getSharedFormKey()) || StrUtil.isBlank(report.getFillableScopeJson())) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_CONDITION_CONFIG_MISSING);
        }
    }

    private void validateSharedFormBinding(String instanceScope, MesProRouteFlowFormBindingSaveReqVO binding) {
        if (!"BATCH_SHARED".equals(instanceScope)) {
            return;
        }
        if (StrUtil.isBlank(binding.getSharedFormKey()) || StrUtil.isBlank(binding.getFillableScopeJson())) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_CONDITION_CONFIG_MISSING);
        }
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private MesProRouteDO validateRouteExists(Long routeId) {
        MesProRouteDO route = routeMapper.selectById(routeId);
        if (route == null) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_ROUTE_REQUIRED);
        }
        return route;
    }

}
