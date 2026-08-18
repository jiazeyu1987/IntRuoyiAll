package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRoutePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchPolicySettingsRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleCalendarRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowBoundaryEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleCalendarRuleMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleCapacityModeEnum;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench.MesProSchedulerWorkbenchService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

/**
 * MES 工艺路线 Service 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Validated
public class MesProRouteServiceImpl implements MesProRouteService {

    private static final String OWNER_PREFIX = "[owner]";
    private static final String OWNER_SUFFIX = "[/owner]";
    private static final String COPY_NAME_SUFFIX = "-副本";
    private static final int MAX_COPY_NAME_SUFFIX_ATTEMPTS = 1000;
    private static final String SNAPSHOT_CONFIGS_KEY = "configSnapshots";
    private static final String FLOW_GRAPH_KEY = "flowGraph";
    private static final String PRODUCTS_KEY = "products";
    private static final String PRODUCT_BOMS_KEY = "productBoms";
    private static final String SCHEDULE_CONFIGS_KEY = "scheduleConfigs";
    private static final String BATCH_USE_CONFIGS_KEY = "batchUseConfigs";
    private static final String SCHEDULE_USE_CONFIGS_KEY = "scheduleUseConfigs";
    private static final String BATCH_RECORD_ATTACHMENT_OWNERS_KEY = "batchRecordAttachmentOwners";
    private static final String ROUTE_START_PRODUCTION_LEADERS_KEY = "routeStartProductionLeaders";
    public static final String DEFAULT_SCHEDULE_CONFIG_VERSION = "AUTO-DEFAULT";
    public static final String DEFAULT_SCHEDULE_REMARK = "[AUTO_DEFAULT_SCHEDULE_CONFIG]";
    private static final String DEFAULT_SCHEDULE_USE_CONFIG_VERSION = "AUTO-SCHEDULE";
    private static final String DEFAULT_SCHEDULE_USE_REMARK = "[AUTO_DEFAULT_SCHEDULE_USE]";
    private static final String ROUTE_VERSION_STATUS_ACTIVE = "ACTIVE";
    private static final String ROUTE_VERSION_STATUS_DRAFT = "DRAFT";
    private static final String ROUTE_VERSION_STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String ROUTE_VERSION_STATUS_READY_TO_PUBLISH = "READY_TO_PUBLISH";
    private static final String ROUTE_VERSION_STATUS_CANCELLED = "CANCELLED";
    private static final List<String> PENDING_ROUTE_VERSION_STATUSES = List.of(
            ROUTE_VERSION_STATUS_READY_TO_PUBLISH,
            ROUTE_VERSION_STATUS_PENDING_APPROVAL,
            ROUTE_VERSION_STATUS_DRAFT);

    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProRouteProductMapper routeProductMapper;
    @Resource
    private MesProRouteProcessMapper routeProcessMapper;
    @Resource
    private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    @Resource
    private MesProRouteProcessFlowBoundaryEdgeMapper routeProcessFlowBoundaryEdgeMapper;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
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
    @Lazy
    private MesProRouteProcessService routeProcessService;
    @Resource
    @Lazy
    private MesProRouteProductService routeProductService;
    @Resource
    @Lazy
    private MesProRouteProductBomService routeProductBomService;
    @Resource
    @Lazy
    private MesProRouteProcessFlowService routeProcessFlowService;
    @Resource
    @Lazy
    private MesMdItemService itemService;
    @Resource
    private MesProSchedulerWorkbenchService schedulerWorkbenchService;
    @Resource
    private MesProScheduleCalendarRuleMapper scheduleCalendarRuleMapper;
    @Resource
    private MesProRouteControlledContentAdapter platformAdapter;
    @Resource
    private MesProRouteOwnerPermissionService routeOwnerPermissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRoute(MesProRouteSaveReqVO createReqVO) {
        // 1. 校验编码唯一性
        validateRouteCodeUnique(null, createReqVO.getCode());
        validateRouteNameUnique(null, createReqVO.getName());
        // 2. 插入
        MesProRouteDO route = BeanUtils.toBean(createReqVO, MesProRouteDO.class);
        route.setRemark(buildRouteRemark(createReqVO.getRemark(), createReqVO.getOwnerName()));
        route.setStatus(CommonStatusEnum.DISABLE.getStatus());
        routeMapper.insert(route);
        createInitialRouteVersion(route);
        routeOwnerPermissionService.bindCurrentUserAsOwner(route.getId());
        return route.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoute(MesProRouteSaveReqVO updateReqVO) {
        // 1.1 校验存在
        MesProRouteDO oldRoute = validateRouteExists(updateReqVO.getId());
        // 1.2 校验编码唯一性
        validateRouteCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        validateRouteNameUnique(updateReqVO.getId(), updateReqVO.getName());

        // 2. 当前 active 路线不可变，生产影响字段变更先进入候选版本
        MesProRouteDO updateObj = BeanUtils.toBean(updateReqVO, MesProRouteDO.class);
        updateObj.setRemark(buildRouteRemark(updateReqVO.getRemark(), updateReqVO.getOwnerName()));
        MesProRouteVersionDO activeVersion = routeVersionMapper.selectActiveByRouteId(updateReqVO.getId());
        if (activeVersion != null) {
            MesProRouteDO snapshotRoute = mergeRouteForSnapshot(oldRoute, updateObj);
            createDraftCandidateRouteVersion(snapshotRoute, activeVersion);
            return;
        }

        // 3. 历史异常数据缺少 active 版本时，允许修正主表并补 V1
        routeMapper.updateById(updateObj);
        maintainRouteVersionAfterRouteUpdate(oldRoute, updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyRoute(Long sourceRouteId, String targetCode, String targetName) {
        MesProRouteDO sourceRoute = validateRouteExists(sourceRouteId);
        validateRouteCodeUnique(null, targetCode);
        String resolvedTargetName = resolveUniqueCopyRouteName(targetName);

        MesProRouteDO targetRoute = MesProRouteDO.builder()
                .code(targetCode)
                .name(resolvedTargetName)
                .description(sourceRoute.getDescription())
                .status(CommonStatusEnum.DISABLE.getStatus())
                .remark(sourceRoute.getRemark())
                .build();
        routeMapper.insert(targetRoute);
        routeOwnerPermissionService.bindCurrentUserAsOwner(targetRoute.getId());

        copyProductBindings(sourceRouteId, targetRoute.getId());

        Map<Long, Long> copiedRouteProcessIds = new HashMap<>();
        for (MesProRouteProcessDO sourceProcess : routeProcessMapper.selectListByRouteId(sourceRouteId)) {
            MesProRouteProcessDO targetProcess = BeanUtils.toBean(sourceProcess, MesProRouteProcessDO.class);
            targetProcess.setId(null);
            targetProcess.setRouteId(targetRoute.getId());
            routeProcessMapper.insert(targetProcess);
            copiedRouteProcessIds.put(sourceProcess.getId(), targetProcess.getId());
        }

        MesProRouteVersionDO sourceVersion = routeVersionMapper.selectActiveByRouteId(sourceRouteId);
        MesProRouteVersionDO targetVersion = createRouteVersion(targetRoute, "V1",
                sourceVersion == null ? null : sourceVersion.getId());
        JSONObject inheritedRouteLevelConfigSnapshots = extractReusableRouteLevelConfigSnapshots(sourceVersion);
        if (sourceVersion != null) {
            copyScheduleConfigs(sourceRouteId, sourceVersion.getId(), targetVersion.getId(), copiedRouteProcessIds);
        }
        copyRouteFlowConfigs(sourceRouteId, targetRoute.getId(), copiedRouteProcessIds);
        routeProcessFlowService.copyGraph(sourceRouteId, targetRoute.getId(), copiedRouteProcessIds);
        refreshRouteVersionSnapshot(targetRoute, targetVersion.getId(), inheritedRouteLevelConfigSnapshots);
        registerActiveVersionRef(targetVersion);
        return targetRoute.getId();
    }

    private void copyRouteFlowConfigs(Long sourceRouteId, Long targetRouteId, Map<Long, Long> copiedRouteProcessIds) {
        List<MesProRouteFlowConfigDO> sourceUseConfigs = routeFlowConfigMapper.selectList(
                MesProRouteFlowConfigDO::getRouteId, sourceRouteId);
        if (CollUtil.isEmpty(sourceUseConfigs)) {
            return;
        }
        for (MesProRouteFlowConfigDO sourceUseConfig : sourceUseConfigs) {
            MesProRouteFlowConfigDO targetUseConfig = BeanUtils.toBean(sourceUseConfig, MesProRouteFlowConfigDO.class);
            targetUseConfig.setId(null);
            targetUseConfig.setRouteId(targetRouteId);
            routeFlowConfigMapper.insert(targetUseConfig);

            List<MesProRouteFlowProcessConfigDO> sourceProcessConfigs =
                    routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(sourceRouteId, sourceUseConfig.getUseType())
                            .stream()
                            .filter(config -> MesProRouteFlowContextMatcher.isProcessConfigOwnedBy(
                                    sourceUseConfig, config, sourceRouteId, sourceUseConfig.getUseType()))
                            .toList();
            Map<Long, Long> copiedProcessConfigIds = new HashMap<>();
            for (MesProRouteFlowProcessConfigDO sourceProcessConfig : sourceProcessConfigs) {
                Long targetRouteProcessId = resolveCopiedRouteProcessId(
                        sourceRouteId, sourceProcessConfig.getRouteProcessId(), copiedRouteProcessIds);
                MesProRouteFlowProcessConfigDO targetProcessConfig =
                        BeanUtils.toBean(sourceProcessConfig, MesProRouteFlowProcessConfigDO.class);
                targetProcessConfig.setId(null);
                targetProcessConfig.setRouteFlowConfigId(targetUseConfig.getId());
                targetProcessConfig.setRouteId(targetRouteId);
                targetProcessConfig.setRouteProcessId(targetRouteProcessId);
                routeFlowProcessConfigMapper.insert(targetProcessConfig);
                copiedProcessConfigIds.put(sourceProcessConfig.getId(), targetProcessConfig.getId());
            }
            if (copiedProcessConfigIds.isEmpty()) {
                continue;
            }
            List<MesProRouteFlowProcessBatchRecordDO> sourceBatchRecords =
                    routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(sourceRouteId, sourceUseConfig.getUseType());
            for (MesProRouteFlowProcessBatchRecordDO sourceBatchRecord : sourceBatchRecords) {
                Long targetRouteProcessId = resolveCopiedRouteProcessId(
                        sourceRouteId, sourceBatchRecord.getRouteProcessId(), copiedRouteProcessIds);
                Long targetUseProcessConfigId = copiedProcessConfigIds.get(sourceBatchRecord.getRouteFlowProcessConfigId());
                if (targetUseProcessConfigId == null) {
                    continue;
                }
                MesProRouteFlowProcessBatchRecordDO targetBatchRecord =
                        BeanUtils.toBean(sourceBatchRecord, MesProRouteFlowProcessBatchRecordDO.class);
                targetBatchRecord.setId(null);
                targetBatchRecord.setRouteId(targetRouteId);
                targetBatchRecord.setRouteProcessId(targetRouteProcessId);
                targetBatchRecord.setRouteFlowProcessConfigId(targetUseProcessConfigId);
                routeFlowProcessBatchRecordMapper.insert(targetBatchRecord);
            }
        }
    }

    private void copyProductBindings(Long sourceRouteId, Long targetRouteId) {
        List<MesProRouteProductDO> sourceProducts = routeProductMapper.selectListByRouteId(sourceRouteId);
        if (CollUtil.isNotEmpty(sourceProducts)) {
            for (MesProRouteProductDO sourceProduct : sourceProducts) {
                MesProRouteProductDO targetProduct = BeanUtils.toBean(sourceProduct, MesProRouteProductDO.class);
                targetProduct.setId(null);
                targetProduct.setRouteId(targetRouteId);
                routeProductMapper.insert(targetProduct);
            }
        }

        List<MesProRouteProductBomDO> sourceProductBoms = routeProductBomMapper.selectList(sourceRouteId, null, null);
        if (CollUtil.isNotEmpty(sourceProductBoms)) {
            for (MesProRouteProductBomDO sourceProductBom : sourceProductBoms) {
                MesProRouteProductBomDO targetProductBom = BeanUtils.toBean(sourceProductBom,
                        MesProRouteProductBomDO.class);
                targetProductBom.setId(null);
                targetProductBom.setRouteId(targetRouteId);
                routeProductBomMapper.insert(targetProductBom);
            }
        }
    }

    @Override
    public void updateRouteStatus(Long id, Integer status) {
        // 1.1 校验存在
        MesProRouteDO route = validateRouteExists(id);
        // 1.2 启用时的校验
        if (CommonStatusEnum.ENABLE.getStatus().equals(status)) {
            validateRouteEnable(id);
        }

        // 2. 更新状态
        routeMapper.updateById(new MesProRouteDO().setId(id).setStatus(status));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoute(Long id) {
        // 1.1 校验存在
        validateRouteExists(id);
        // 1.2 已启用的工艺路线，不允许删除
        validateRouteNotEnable(id);
        // 1.3 删除路线前必须结束其开放候选，避免留下无法再编辑或发布的孤立草稿
        cancelOpenCandidateBeforeDelete(id);

        // 2.1 级联删除
        routeProcessFlowService.deleteByRouteId(id);
        routeProcessService.deleteRouteProcessByRouteId(id);
        routeProductService.deleteRouteProductByRouteId(id);
        routeProductBomService.deleteRouteProductBomByRouteId(id);
        // 2.2 删除工艺路线
        routeMapper.deleteById(id);
    }

    private void cancelOpenCandidateBeforeDelete(Long routeId) {
        MesProRouteVersionDO candidate = routeVersionMapper.selectOpenCandidateByRouteId(routeId);
        if (candidate == null) {
            return;
        }
        if (!(ROUTE_VERSION_STATUS_DRAFT.equals(candidate.getLifecycleStatus())
                || ROUTE_VERSION_STATUS_READY_TO_PUBLISH.equals(candidate.getLifecycleStatus()))) {
            throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                    candidate.getId(), candidate.getLifecycleStatus());
        }
        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(candidate.getId());
        update.setLifecycleStatus(ROUTE_VERSION_STATUS_CANCELLED);
        if (routeVersionMapper.updateById(update) != 1) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, candidate.getId());
        }
        platformAdapter.recordCancelled(candidate, SecurityFrameworkUtils.getLoginUserId());
    }

    @Override
    public MesProRouteDO validateRouteExists(Long id) {
        MesProRouteDO route = routeMapper.selectById(id);
        if (route == null) {
            throw exception(PRO_ROUTE_NOT_EXISTS);
        }
        return route;
    }

    private void validateRouteCodeUnique(Long id, String code) {
        MesProRouteDO route = routeMapper.selectByCode(code);
        if (route == null) {
            return;
        }
        if (ObjUtil.notEqual(route.getId(), id)) {
            throw exception(PRO_ROUTE_CODE_DUPLICATE);
        }
    }

    private void validateRouteNameUnique(Long id, String name) {
        MesProRouteDO route = routeMapper.selectByName(name);
        if (route == null) {
            return;
        }
        if (ObjUtil.notEqual(route.getId(), id)) {
            throw exception(PRO_ROUTE_NAME_DUPLICATE);
        }
    }

    private String resolveUniqueCopyRouteName(String targetName) {
        if (routeMapper.selectByName(targetName) == null) {
            return targetName;
        }
        String copyNameBase = targetName.endsWith(COPY_NAME_SUFFIX) ? targetName : targetName + COPY_NAME_SUFFIX;
        int startIndex = copyNameBase.equals(targetName) ? 2 : 1;
        for (int copyIndex = startIndex; copyIndex <= MAX_COPY_NAME_SUFFIX_ATTEMPTS; copyIndex++) {
            String candidateName = copyIndex == 1 ? copyNameBase : copyNameBase + copyIndex;
            if (routeMapper.selectByName(candidateName) == null) {
                return candidateName;
            }
        }
        throw exception(PRO_ROUTE_NAME_DUPLICATE);
    }

    /**
     * 启用工艺路线时的校验
     */
    private void validateRouteEnable(Long routeId) {
        // 1. 必须有工序
        List<MesProRouteProcessDO> processList = routeProcessService.getRouteProcessListByRouteId(routeId);
        if (CollUtil.isEmpty(processList)) {
            throw exception(PRO_ROUTE_ENABLE_NO_PROCESS);
        }
        // 2. 流转关系图必须完整有效
        routeProcessFlowService.validateRouteEnable(routeId);
        // 3. 所有产品必须配置了 BOM 消耗
    }

    @Override
    public MesProRouteDO getRoute(Long id) {
        return routeMapper.selectById(id);
    }

    private MesProRouteVersionDO createInitialRouteVersion(MesProRouteDO route) {
        MesProRouteVersionDO activeVersion = createRouteVersion(route, "V1", null);
        registerActiveVersionRef(activeVersion);
        return activeVersion;
    }

    private void maintainRouteVersionAfterRouteUpdate(MesProRouteDO oldRoute, MesProRouteDO updatedRoute) {
        MesProRouteVersionDO activeVersion = routeVersionMapper.selectActiveByRouteId(updatedRoute.getId());
        if (activeVersion == null) {
            createInitialRouteVersion(mergeRouteForSnapshot(oldRoute, updatedRoute));
            return;
        }
        MesProRouteDO snapshotRoute = mergeRouteForSnapshot(oldRoute, updatedRoute);
        MesProRouteVersionDO newVersion = createDraftCandidateRouteVersion(snapshotRoute, activeVersion);
        copyExistingProcessScheduleConfigs(activeVersion.getId(), newVersion.getId(), updatedRoute.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void maintainRouteVersionAfterProcessChange(Long routeId) {
        MesProRouteDO route = validateRouteExists(routeId);
        MesProRouteVersionDO activeVersion = routeVersionMapper.selectActiveByRouteId(routeId);
        if (activeVersion == null) {
            createInitialRouteVersion(route);
            return;
        }
        MesProRouteVersionDO draftCandidate = findReusableDraftCandidate(routeId, activeVersion);
        if (draftCandidate != null) {
            copyExistingProcessScheduleConfigs(activeVersion.getId(), draftCandidate.getId(), routeId);
            refreshRouteVersionSnapshot(route, draftCandidate.getId());
            return;
        }
        MesProRouteVersionDO newVersion = createDraftCandidateRouteVersion(route, activeVersion);
        copyExistingProcessScheduleConfigs(activeVersion.getId(), newVersion.getId(), routeId);
    }

    private MesProRouteVersionDO findReusableDraftCandidate(Long routeId, MesProRouteVersionDO activeVersion) {
        MesProRouteVersionDO openCandidate = routeVersionMapper.selectOpenCandidateByRouteId(routeId);
        if (openCandidate == null) {
            return null;
        }
        if (!ROUTE_VERSION_STATUS_DRAFT.equals(openCandidate.getLifecycleStatus())) {
            throw exception(PRO_ROUTE_VERSION_CONFLICT,
                    routeId, openCandidate.getId(), openCandidate.getLifecycleStatus());
        }
        if (!Objects.equals(openCandidate.getSourceRouteVersionId(), activeVersion.getId())) {
            throw exception(PRO_ROUTE_VERSION_CONFLICT,
                    routeId, openCandidate.getSourceRouteVersionId(), activeVersion.getId());
        }
        return openCandidate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureDefaultScheduleArtifacts(Long routeId, Long routeProcessId) {
        MesProRouteDO route = validateRouteExists(routeId);
        MesProRouteProcessDO routeProcess = routeProcessService.resolveCurrentRouteProcess(routeProcessId, routeId, null);
        if (routeProcess == null || ObjUtil.notEqual(routeId, routeProcess.getRouteId())) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED);
        }
        ensureDefaultScheduleUseConfig(route, routeProcess);
        ensureDefaultRouteScheduleConfig(route, routeProcess);
    }

    private MesProRouteVersionDO createDraftCandidateRouteVersion(MesProRouteDO route,
                                                                  MesProRouteVersionDO activeVersion) {
        MesProRouteVersionDO candidate = createRouteVersion(route, nextVersionNo(route.getId()), activeVersion.getId(),
                Boolean.FALSE, ROUTE_VERSION_STATUS_DRAFT,
                buildCompleteRouteConfigSnapshots(route.getId(), activeVersion.getId()));
        platformAdapter.recordCandidateCreated(activeVersion, candidate, null, "route version candidate created");
        return candidate;
    }

    private MesProRouteDO mergeRouteForSnapshot(MesProRouteDO oldRoute, MesProRouteDO updatedRoute) {
        return MesProRouteDO.builder()
                .id(updatedRoute.getId())
                .code(StrUtil.blankToDefault(updatedRoute.getCode(), oldRoute.getCode()))
                .name(StrUtil.blankToDefault(updatedRoute.getName(), oldRoute.getName()))
                .description(StrUtil.blankToDefault(updatedRoute.getDescription(), oldRoute.getDescription()))
                .status(oldRoute.getStatus())
                .remark(StrUtil.blankToDefault(updatedRoute.getRemark(), oldRoute.getRemark()))
                .build();
    }

    private MesProRouteVersionDO createRouteVersion(MesProRouteDO route, String versionNo, Long sourceRouteVersionId) {
        return createRouteVersion(route, versionNo, sourceRouteVersionId,
                Boolean.TRUE, ROUTE_VERSION_STATUS_ACTIVE, null);
    }

    private MesProRouteVersionDO createRouteVersion(MesProRouteDO route, String versionNo, Long sourceRouteVersionId,
                                                    Boolean active, String lifecycleStatus) {
        return createRouteVersion(route, versionNo, sourceRouteVersionId, active, lifecycleStatus, null);
    }

    private MesProRouteVersionDO createRouteVersion(MesProRouteDO route, String versionNo, Long sourceRouteVersionId,
                                                    Boolean active, String lifecycleStatus,
                                                    JSONObject configSnapshots) {
        MesProRouteVersionDO version = MesProRouteVersionDO.builder()
                .routeId(route.getId())
                .versionNo(versionNo)
                .active(active)
                .lifecycleStatus(lifecycleStatus)
                .sourceRouteVersionId(sourceRouteVersionId)
                .routeSnapshotJson(buildRouteSnapshotJson(route, configSnapshots))
                .build();
        routeVersionMapper.insert(version);
        return version;
    }

    private void registerActiveVersionRef(MesProRouteVersionDO activeVersion) {
        platformAdapter.recordActiveRegistered(activeVersion, null, "route active version registered");
    }

    private void ensureDefaultScheduleUseConfig(MesProRouteDO route, MesProRouteProcessDO routeProcess) {
        MesProSchedulerWorkbenchPolicySettingsRespVO defaults = schedulerWorkbenchService.getPolicySettings();
        MesProRouteFlowConfigDO flowConfig =
                routeFlowConfigMapper.selectByRouteIdAndUseType(route.getId(), MesProRouteFlowConfigTypeEnum.SCHEDULE.getType());
        if (flowConfig == null) {
            flowConfig = MesProRouteFlowConfigDO.builder()
                    .routeId(route.getId())
                    .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                    .configVersion(DEFAULT_SCHEDULE_USE_CONFIG_VERSION)
                    .remark(DEFAULT_SCHEDULE_USE_REMARK)
                    .build();
            routeFlowConfigMapper.insert(flowConfig);
        }
        MesProRouteFlowProcessConfigDO processConfig = routeFlowProcessConfigMapper
                .selectByRouteProcessIdAndUseType(routeProcess.getId(), MesProRouteFlowConfigTypeEnum.SCHEDULE.getType());
        if (processConfig == null) {
            processConfig = findHistoricalFlowProcessConfig(
                    route.getId(), routeProcess, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType());
        }
        if (processConfig != null) {
            if (!MesProRouteFlowContextMatcher.isProcessConfigOwnedBy(
                    flowConfig, processConfig, route.getId(), routeProcess.getId(),
                    MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())) {
                routeFlowProcessConfigMapper.updateById(MesProRouteFlowProcessConfigDO.builder()
                        .id(processConfig.getId())
                        .routeFlowConfigId(flowConfig.getId())
                        .routeId(route.getId())
                        .routeProcessId(routeProcess.getId())
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .build());
            }
            return;
        }
        routeFlowProcessConfigMapper.insert(MesProRouteFlowProcessConfigDO.builder()
                .routeFlowConfigId(flowConfig.getId())
                .routeId(route.getId())
                .routeProcessId(routeProcess.getId())
                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                .enabled(defaults.getDefaultScheduleUseEnabled())
                .executionMode("SEQUENTIAL")
                .batchRecordReportId(null)
                .remark(DEFAULT_SCHEDULE_USE_REMARK)
                .build());
    }

    private void ensureDefaultRouteScheduleConfig(MesProRouteDO route, MesProRouteProcessDO routeProcess) {
        MesProSchedulerWorkbenchPolicySettingsRespVO defaults = schedulerWorkbenchService.getPolicySettings();
        MesProRouteVersionDO activeVersion = routeVersionMapper.selectActiveByRouteId(route.getId());
        if (activeVersion == null) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED);
        }
        MesProRouteScheduleConfigDO existing = routeScheduleConfigMapper
                .selectByRouteVersionIdAndRouteProcessId(activeVersion.getId(), routeProcess.getId());
        if (existing == null) {
            existing = findHistoricalScheduleConfig(route.getId(), activeVersion.getId(), routeProcess);
        }
        if (existing != null) {
            if (!Objects.equals(existing.getRouteProcessId(), routeProcess.getId())) {
                routeScheduleConfigMapper.updateById(MesProRouteScheduleConfigDO.builder()
                        .id(existing.getId())
                        .routeProcessId(routeProcess.getId())
                        .build());
            }
            return;
        }
        boolean nightShiftEnabled = Boolean.TRUE.equals(defaults.getDefaultNightShiftEnabled());
        routeScheduleConfigMapper.insert(MesProRouteScheduleConfigDO.builder()
                .routeVersionId(activeVersion.getId())
                .routeProcessId(routeProcess.getId())
                .capacityMode(MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode())
                .hourlyCapacity(null)
                .infiniteDurationQuantityFactor(null)
                .infiniteDurationBaseMinutes(null)
                .nightShiftEnabled(nightShiftEnabled)
                .calendarRuleId(resolveDefaultCalendarRuleId(nightShiftEnabled))
                .configVersion(DEFAULT_SCHEDULE_CONFIG_VERSION)
                .copiedFromConfigId(null)
                .remark(DEFAULT_SCHEDULE_REMARK)
                .build());
    }

    private MesProRouteFlowProcessConfigDO findHistoricalFlowProcessConfig(
            Long routeId, MesProRouteProcessDO currentRouteProcess, String useType) {
        for (MesProRouteFlowProcessConfigDO config :
                routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(routeId, useType)) {
            if (config.getRouteProcessId() == null) {
                continue;
            }
            MesProRouteProcessDO resolved =
                    routeProcessService.resolveCurrentRouteProcess(config.getRouteProcessId(), routeId, null);
            if (Objects.equals(resolved.getId(), currentRouteProcess.getId())) {
                return config;
            }
        }
        return null;
    }

    private MesProRouteScheduleConfigDO findHistoricalScheduleConfig(
            Long routeId, Long routeVersionId, MesProRouteProcessDO currentRouteProcess) {
        for (MesProRouteScheduleConfigDO config :
                routeScheduleConfigMapper.selectListByRouteVersionId(routeVersionId)) {
            if (config.getRouteProcessId() == null) {
                continue;
            }
            MesProRouteProcessDO resolved =
                    routeProcessService.resolveCurrentRouteProcess(config.getRouteProcessId(), routeId, null);
            if (Objects.equals(resolved.getId(), currentRouteProcess.getId())) {
                return config;
            }
        }
        return null;
    }

    private Long resolveDefaultCalendarRuleId(boolean nightShiftEnabled) {
        if (!nightShiftEnabled) {
            return null;
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        MesProScheduleCalendarRuleDO calendarRule = scheduleCalendarRuleMapper.selectByTenantId(tenantId);
        if (calendarRule == null || calendarRule.getId() == null) {
            throw new IllegalStateException("默认夜班已开启，但当前租户缺少排程日历规则");
        }
        return calendarRule.getId();
    }

    private BigDecimal hoursToMinutes(BigDecimal hours) {
        return hours == null ? null : hours.multiply(BigDecimal.valueOf(60L));
    }

    private String nextVersionNo(Long routeId) {
        String maxVersionNo = routeVersionMapper.selectMaxVersionNoByRouteId(routeId);
        if (StrUtil.isBlank(maxVersionNo)) {
            return "V1";
        }
        int marker = maxVersionNo.lastIndexOf("-V");
        String prefix = marker >= 0 ? maxVersionNo.substring(0, marker + 2) : "V";
        String numberText = marker >= 0 ? maxVersionNo.substring(marker + 2) : maxVersionNo.substring(1);
        int number = Integer.parseInt(numberText);
        return prefix + (number + 1);
    }

    private void copyScheduleConfigs(Long sourceRouteId, Long sourceRouteVersionId, Long targetRouteVersionId,
            Map<Long, Long> copiedRouteProcessIds) {
        List<MesProRouteScheduleConfigDO> sourceConfigs =
                routeScheduleConfigMapper.selectListByRouteVersionId(sourceRouteVersionId);
        Map<Long, MesProRouteScheduleConfigDO> sourceConfigByCurrentRouteProcessId = new LinkedHashMap<>();
        for (MesProRouteScheduleConfigDO sourceConfig : sourceConfigs) {
            Long currentRouteProcessId =
                    resolveCurrentSourceRouteProcessId(sourceRouteId, sourceConfig.getRouteProcessId());
            if (sourceConfigByCurrentRouteProcessId.putIfAbsent(currentRouteProcessId, sourceConfig) != null) {
                throw exception(PRO_ROUTE_SCHEDULE_CONFIG_REQUIRED, sourceRouteVersionId, currentRouteProcessId);
            }
        }
        for (Long sourceRouteProcessId : copiedRouteProcessIds.keySet()) {
            if (!sourceConfigByCurrentRouteProcessId.containsKey(sourceRouteProcessId)) {
                throw exception(PRO_ROUTE_SCHEDULE_CONFIG_REQUIRED, sourceRouteVersionId, sourceRouteProcessId);
            }
        }
        for (Map.Entry<Long, MesProRouteScheduleConfigDO> entry : sourceConfigByCurrentRouteProcessId.entrySet()) {
            MesProRouteScheduleConfigDO sourceConfig = entry.getValue();
            Long targetRouteProcessId = copiedRouteProcessIds.get(entry.getKey());
            MesProRouteScheduleConfigDO targetConfig = BeanUtils.toBean(sourceConfig, MesProRouteScheduleConfigDO.class);
            targetConfig.setId(null);
            targetConfig.setRouteVersionId(targetRouteVersionId);
            targetConfig.setRouteProcessId(targetRouteProcessId);
            targetConfig.setItemId(null);
            targetConfig.setCopiedFromConfigId(sourceConfig.getId());
            normalizeCopiedScheduleConfig(targetConfig);
            routeScheduleConfigMapper.insert(targetConfig);
        }
    }

    private void copyExistingProcessScheduleConfigs(Long sourceRouteVersionId, Long targetRouteVersionId, Long routeId) {
        Set<Long> currentRouteProcessIds = routeProcessMapper.selectListByRouteId(routeId).stream()
                .map(MesProRouteProcessDO::getId)
                .collect(Collectors.toSet());
        List<MesProRouteScheduleConfigDO> sourceConfigs =
                routeScheduleConfigMapper.selectListByRouteVersionId(sourceRouteVersionId);
        Map<Long, MesProRouteScheduleConfigDO> sourceConfigByCurrentRouteProcessId = new LinkedHashMap<>();
        for (MesProRouteScheduleConfigDO sourceConfig : sourceConfigs) {
            Long currentRouteProcessId =
                    resolveCurrentSourceRouteProcessId(routeId, sourceConfig.getRouteProcessId());
            if (sourceConfigByCurrentRouteProcessId.putIfAbsent(currentRouteProcessId, sourceConfig) != null) {
                throw exception(PRO_ROUTE_SCHEDULE_CONFIG_REQUIRED, sourceRouteVersionId, currentRouteProcessId);
            }
        }
        for (Long currentRouteProcessId : currentRouteProcessIds) {
            if (!sourceConfigByCurrentRouteProcessId.containsKey(currentRouteProcessId)) {
                throw exception(PRO_ROUTE_SCHEDULE_CONFIG_REQUIRED, sourceRouteVersionId, currentRouteProcessId);
            }
        }
        for (Map.Entry<Long, MesProRouteScheduleConfigDO> entry : sourceConfigByCurrentRouteProcessId.entrySet()) {
            if (!currentRouteProcessIds.contains(entry.getKey())) {
                continue;
            }
            MesProRouteScheduleConfigDO sourceConfig = entry.getValue();
            MesProRouteScheduleConfigDO existingTargetConfig = routeScheduleConfigMapper
                    .selectByRouteVersionIdAndRouteProcessId(targetRouteVersionId, entry.getKey());
            if (existingTargetConfig != null) {
                continue;
            }
            MesProRouteScheduleConfigDO targetConfig = BeanUtils.toBean(sourceConfig, MesProRouteScheduleConfigDO.class);
            targetConfig.setId(null);
            targetConfig.setRouteVersionId(targetRouteVersionId);
            targetConfig.setRouteProcessId(entry.getKey());
            targetConfig.setItemId(null);
            targetConfig.setCopiedFromConfigId(sourceConfig.getId());
            normalizeCopiedScheduleConfig(targetConfig);
            routeScheduleConfigMapper.insert(targetConfig);
        }
    }

    private void normalizeCopiedScheduleConfig(MesProRouteScheduleConfigDO targetConfig) {
        if (MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode().equals(targetConfig.getCapacityMode())) {
            targetConfig.setCapacityMode(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode());
        }
        if (MesProScheduleCapacityModeEnum.isManualOverrideLike(targetConfig.getCapacityMode())) {
            targetConfig.setInfiniteDurationQuantityFactor(null);
            targetConfig.setInfiniteDurationBaseMinutes(null);
            return;
        }
        targetConfig.setHourlyCapacity(null);
        if (MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode().equals(targetConfig.getCapacityMode())) {
            targetConfig.setInfiniteDurationQuantityFactor(null);
            targetConfig.setInfiniteDurationBaseMinutes(null);
        }
    }

    private Long resolveCopiedRouteProcessId(
            Long sourceRouteId, Long snapshotRouteProcessId, Map<Long, Long> copiedRouteProcessIds) {
        Long targetRouteProcessId = copiedRouteProcessIds.get(snapshotRouteProcessId);
        if (targetRouteProcessId != null) {
            return targetRouteProcessId;
        }
        Long currentRouteProcessId =
                resolveCurrentSourceRouteProcessId(sourceRouteId, snapshotRouteProcessId);
        targetRouteProcessId = copiedRouteProcessIds.get(currentRouteProcessId);
        if (targetRouteProcessId == null) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED);
        }
        return targetRouteProcessId;
    }

    private Long resolveCurrentSourceRouteProcessId(Long routeId, Long snapshotRouteProcessId) {
        if (snapshotRouteProcessId == null) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED);
        }
        MesProRouteProcessDO currentRouteProcess =
                routeProcessService.resolveCurrentRouteProcess(snapshotRouteProcessId, routeId, null);
        return currentRouteProcess.getId();
    }

    private void refreshRouteVersionSnapshot(MesProRouteDO route, Long routeVersionId) {
        refreshRouteVersionSnapshot(route, routeVersionId, null);
    }

    private void refreshRouteVersionSnapshot(MesProRouteDO route, Long routeVersionId,
                                             JSONObject inheritedConfigSnapshots) {
        JSONObject configSnapshots = buildCompleteRouteConfigSnapshots(route.getId(), routeVersionId);
        inheritConfigSnapshotIfMissing(configSnapshots, inheritedConfigSnapshots, BATCH_RECORD_ATTACHMENT_OWNERS_KEY);
        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(routeVersionId);
        update.setRouteSnapshotJson(buildRouteSnapshotJson(route, configSnapshots));
        routeVersionMapper.updateById(update);
    }

    @Override
    public String buildCurrentRouteSnapshotJson(Long routeId, Long routeVersionId) {
        MesProRouteDO route = routeMapper.selectById(routeId);
        if (route == null) {
            throw exception(PRO_ROUTE_NOT_EXISTS);
        }
        return buildRouteSnapshotJson(route, buildCompleteRouteConfigSnapshots(routeId, routeVersionId));
    }

    private JSONObject buildCompleteRouteConfigSnapshots(Long routeId, Long routeVersionId) {
        JSONObject configSnapshots = new JSONObject(true);
        configSnapshots.put(FLOW_GRAPH_KEY, JSON.toJSON(routeProcessFlowService.getGraph(routeId)));
        configSnapshots.put(PRODUCTS_KEY, JSON.toJSON(routeProductMapper.selectListByRouteId(routeId)));
        configSnapshots.put(PRODUCT_BOMS_KEY, JSON.toJSON(routeProductBomMapper.selectList(routeId, null, null)));
        configSnapshots.put(SCHEDULE_CONFIGS_KEY,
                JSON.toJSON(routeScheduleConfigMapper.selectListByRouteVersionId(routeVersionId)));
        configSnapshots.put(BATCH_USE_CONFIGS_KEY, buildBatchUseConfigSnapshots(routeId));
        configSnapshots.put(SCHEDULE_USE_CONFIGS_KEY, JSON.toJSON(routeFlowProcessConfigMapper
                .selectListByRouteIdAndUseType(routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())));
        Object batchRecordAttachmentOwners =
                resolveExistingConfigSnapshot(routeVersionId, BATCH_RECORD_ATTACHMENT_OWNERS_KEY);
        if (batchRecordAttachmentOwners != null) {
            configSnapshots.put(BATCH_RECORD_ATTACHMENT_OWNERS_KEY, batchRecordAttachmentOwners);
        }
        Object routeStartProductionLeaders =
                resolveExistingConfigSnapshot(routeVersionId, ROUTE_START_PRODUCTION_LEADERS_KEY);
        if (routeStartProductionLeaders != null) {
            configSnapshots.put(ROUTE_START_PRODUCTION_LEADERS_KEY, routeStartProductionLeaders);
        }
        return configSnapshots;
    }

    private Object resolveExistingConfigSnapshot(Long routeVersionId, String configKey) {
        if (routeVersionId == null || StrUtil.isBlank(configKey)) {
            return null;
        }
        MesProRouteVersionDO routeVersion = routeVersionMapper.selectById(routeVersionId);
        if (routeVersion == null || StrUtil.isBlank(routeVersion.getRouteSnapshotJson())) {
            return null;
        }
        JSONObject snapshot;
        try {
            snapshot = JSON.parseObject(routeVersion.getRouteSnapshotJson());
        } catch (RuntimeException ex) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
        }
        JSONObject configSnapshots = snapshot == null ? null : snapshot.getJSONObject(SNAPSHOT_CONFIGS_KEY);
        if (configSnapshots == null || !configSnapshots.containsKey(configKey)) {
            return null;
        }
        Object configSnapshot = configSnapshots.get(configKey);
        if (configSnapshot == null
                || ((BATCH_RECORD_ATTACHMENT_OWNERS_KEY.equals(configKey)
                || ROUTE_START_PRODUCTION_LEADERS_KEY.equals(configKey))
                && !(configSnapshot instanceof JSONArray))) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
        }
        return configSnapshot;
    }

    private JSONArray buildBatchUseConfigSnapshots(Long routeId) {
        List<MesProRouteFlowProcessConfigDO> processConfigs = routeFlowProcessConfigMapper
                .selectListByRouteIdAndUseType(routeId, MesProRouteFlowConfigTypeEnum.BATCH.getType());
        List<MesProRouteFlowProcessBatchRecordDO> records = routeFlowProcessBatchRecordMapper
                .selectListByRouteIdAndUseType(routeId, MesProRouteFlowConfigTypeEnum.BATCH.getType());
        Map<Long, MesProRouteFlowProcessConfigDO> configById = processConfigs.stream()
                .collect(Collectors.toMap(MesProRouteFlowProcessConfigDO::getId,
                        config -> config, (left, right) -> left, LinkedHashMap::new));
        Map<Long, List<MesProRouteFlowProcessBatchRecordDO>> recordsByConfigId = records.stream()
                .filter(record -> isSnapshotBatchRecordOwnedByConfig(record, configById.get(record.getRouteFlowProcessConfigId())))
                .collect(Collectors.groupingBy(MesProRouteFlowProcessBatchRecordDO::getRouteFlowProcessConfigId,
                        LinkedHashMap::new, Collectors.toList()));
        JSONArray result = new JSONArray();
        for (MesProRouteFlowProcessConfigDO processConfig : processConfigs) {
            JSONObject config = (JSONObject) JSON.toJSON(processConfig);
            List<MesProRouteFlowProcessBatchRecordDO> ownedRecords =
                    recordsByConfigId.getOrDefault(processConfig.getId(), Collections.emptyList());
            config.put("formBindings", buildFormBindingSnapshots(ownedRecords));
            config.put("batchRecordReports", buildBatchRecordReportSnapshots(ownedRecords));
            result.add(config);
        }
        return result;
    }

    private boolean isSnapshotBatchRecordOwnedByConfig(MesProRouteFlowProcessBatchRecordDO record,
                                                       MesProRouteFlowProcessConfigDO processConfig) {
        return record != null && processConfig != null
                && Objects.equals(record.getRouteFlowProcessConfigId(), processConfig.getId())
                && Objects.equals(record.getRouteId(), processConfig.getRouteId())
                && Objects.equals(record.getRouteProcessId(), processConfig.getRouteProcessId())
                && Objects.equals(record.getUseType(), processConfig.getUseType());
    }

    private JSONArray buildFormBindingSnapshots(List<MesProRouteFlowProcessBatchRecordDO> records) {
        JSONArray result = new JSONArray();
        records.stream()
                .filter(record -> record.getFormTemplateId() != null)
                .sorted(Comparator.comparing(MesProRouteFlowProcessBatchRecordDO::getReportSort,
                        Comparator.nullsLast(Integer::compareTo)))
                .map(this::buildFormBindingSnapshot)
                .forEach(result::add);
        return result;
    }

    private JSONObject buildFormBindingSnapshot(MesProRouteFlowProcessBatchRecordDO record) {
        JSONObject binding = new JSONObject(true);
        binding.put("routeBindingId", record.getId());
        binding.put("batchRecordReportId", record.getBatchRecordReportId());
        binding.put("batchRecordDefinitionId", record.getBatchRecordDefinitionId());
        binding.put("batchRecordVersionId", record.getBatchRecordVersionId());
        binding.put("formSlotType", record.getFormSlotType());
        binding.put("formBindingKey", record.getFormBindingKey());
        binding.put("globalSyncKey", record.getGlobalSyncKey());
        binding.put("formTemplateId", record.getFormTemplateId());
        binding.put("formTemplateName", record.getFormTemplateNameSnapshot());
        binding.put("lastPublishedTemplateVersionId", record.getLastPublishedTemplateVersionId());
        binding.put("lastPublishedTemplateVersionNo", record.getLastPublishedTemplateVersionNo());
        binding.put("instanceScope", record.getInstanceScope());
        binding.put("sharedFormKey", record.getSharedFormKey());
        binding.put("fillableScopeJson", record.getFillableScopeJson());
        binding.put("recordCategory", record.getRecordCategory());
        binding.put("validationProfile", record.getValidationProfile());
        binding.put("recordbookEnabled", record.getRecordbookEnabled());
        binding.put("permissionScopeId", record.getPermissionScopeId());
        binding.put("recordCategorySnapshotHash", record.getRecordCategorySnapshotHash());
        binding.put("requiredPolicy", record.getRequiredPolicy());
        binding.put("requiredConditionJson", record.getRequiredConditionJson());
        binding.put("ownerRoleKey", record.getOwnerRoleKey());
        binding.put("archiveVisibility", record.getArchiveVisibility());
        binding.put("slotConfigSnapshotHash", record.getSlotConfigSnapshotHash());
        binding.put("candidateSourceType", record.getCandidateSourceType());
        binding.put("candidateSourceIds", parseLongArray(record.getCandidateSourceIds()));
        binding.put("candidateSourceNames", parseStringArray(record.getCandidateSourceNames()));
        binding.put("reportSort", record.getReportSort());
        binding.put("remark", record.getRemark());
        return binding;
    }

    private JSONArray buildBatchRecordReportSnapshots(List<MesProRouteFlowProcessBatchRecordDO> records) {
        JSONArray result = new JSONArray();
        records.stream()
                .filter(record -> StrUtil.isNotBlank(record.getBatchRecordReportId()))
                .sorted(Comparator.comparing(MesProRouteFlowProcessBatchRecordDO::getReportSort,
                        Comparator.nullsLast(Integer::compareTo)))
                .map(this::buildBatchRecordReportSnapshot)
                .forEach(result::add);
        return result;
    }

    private JSONObject buildBatchRecordReportSnapshot(MesProRouteFlowProcessBatchRecordDO record) {
        JSONObject report = new JSONObject(true);
        report.put("batchRecordReportId", record.getBatchRecordReportId());
        report.put("batchRecordDefinitionId", record.getBatchRecordDefinitionId());
        report.put("batchRecordVersionId", record.getBatchRecordVersionId());
        report.put("formSlotType", record.getFormSlotType());
        report.put("instanceScope", record.getInstanceScope());
        report.put("sharedFormKey", record.getSharedFormKey());
        report.put("fillableScopeJson", record.getFillableScopeJson());
        report.put("recordCategory", record.getRecordCategory());
        report.put("validationProfile", record.getValidationProfile());
        report.put("permissionScopeId", record.getPermissionScopeId());
        report.put("recordCategorySnapshotHash", record.getRecordCategorySnapshotHash());
        report.put("requiredPolicy", record.getRequiredPolicy());
        report.put("requiredConditionJson", record.getRequiredConditionJson());
        report.put("ownerRoleKey", record.getOwnerRoleKey());
        report.put("archiveVisibility", record.getArchiveVisibility());
        report.put("slotConfigSnapshotHash", record.getSlotConfigSnapshotHash());
        report.put("reportSort", record.getReportSort());
        report.put("remark", record.getRemark());
        return report;
    }

    private JSONArray parseLongArray(String rawValue) {
        JSONArray result = new JSONArray();
        String text = StrUtil.trim(rawValue);
        if (StrUtil.isBlank(text)) {
            return result;
        }
        if (text.startsWith("[")) {
            return JSON.parseArray(text);
        }
        for (String value : text.split(",")) {
            String trimmed = StrUtil.trim(value);
            if (StrUtil.isNotBlank(trimmed)) {
                result.add(Long.valueOf(trimmed));
            }
        }
        return result;
    }

    private JSONArray parseStringArray(String rawValue) {
        JSONArray result = new JSONArray();
        String text = StrUtil.trim(rawValue);
        if (StrUtil.isBlank(text)) {
            return result;
        }
        if (text.startsWith("[")) {
            return JSON.parseArray(text);
        }
        for (String value : text.split(",")) {
            String trimmed = StrUtil.trim(value);
            if (StrUtil.isNotBlank(trimmed)) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private JSONObject extractConfigSnapshots(MesProRouteVersionDO sourceVersion) {
        if (sourceVersion == null || StrUtil.isBlank(sourceVersion.getRouteSnapshotJson())) {
            return null;
        }
        JSONObject snapshot = JSON.parseObject(sourceVersion.getRouteSnapshotJson());
        return snapshot == null ? null : snapshot.getJSONObject(SNAPSHOT_CONFIGS_KEY);
    }

    private JSONObject extractReusableRouteLevelConfigSnapshots(MesProRouteVersionDO sourceVersion) {
        JSONObject sourceConfigSnapshots = extractConfigSnapshots(sourceVersion);
        if (sourceConfigSnapshots == null) {
            return null;
        }
        JSONObject reusableConfigSnapshots = new JSONObject(true);
        if (sourceConfigSnapshots.containsKey(BATCH_RECORD_ATTACHMENT_OWNERS_KEY)) {
            Object owners = sourceConfigSnapshots.get(BATCH_RECORD_ATTACHMENT_OWNERS_KEY);
            if (!(owners instanceof JSONArray)) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, sourceVersion.getId());
            }
            reusableConfigSnapshots.put(BATCH_RECORD_ATTACHMENT_OWNERS_KEY, owners);
        }
        return reusableConfigSnapshots.isEmpty() ? null : reusableConfigSnapshots;
    }

    private void inheritConfigSnapshotIfMissing(JSONObject targetConfigSnapshots,
                                                JSONObject inheritedConfigSnapshots,
                                                String configKey) {
        if (targetConfigSnapshots == null || inheritedConfigSnapshots == null
                || targetConfigSnapshots.containsKey(configKey)
                || !inheritedConfigSnapshots.containsKey(configKey)) {
            return;
        }
        targetConfigSnapshots.put(configKey, inheritedConfigSnapshots.get(configKey));
    }

    private String buildRouteSnapshotJson(MesProRouteDO route, JSONObject configSnapshots) {
        JSONObject snapshot = new JSONObject(true);
        snapshot.put("routeId", route.getId());
        snapshot.put("routeCode", route.getCode());
        snapshot.put("routeName", route.getName());
        snapshot.put("id", route.getId());
        snapshot.put("code", route.getCode());
        snapshot.put("name", route.getName());
        snapshot.put("description", StrUtil.nullToEmpty(route.getDescription()));
        snapshot.put("status", route.getStatus());
        snapshot.put("remark", StrUtil.nullToEmpty(route.getRemark()));
        if (configSnapshots != null) {
            snapshot.put(SNAPSHOT_CONFIGS_KEY, configSnapshots);
        }
        return snapshot.toJSONString();
    }

    @Override
    public MesProRouteRespVO getRouteRespVO(Long id) {
        MesProRouteDO route = routeMapper.selectById(id);
        if (route == null) {
            route = routeMapper.selectByIdIgnoreDeleted(id);
        }
        if (route == null) {
            return null;
        }
        MesProRouteRespVO respVO = BeanUtils.toBean(route, MesProRouteRespVO.class);
        enrichRouteDisplayFields(List.of(respVO));
        return respVO;
    }

    @Override
    public PageResult<MesProRouteDO> getRoutePage(MesProRoutePageReqVO pageReqVO) {
        return routeMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<MesProRouteRespVO> getRoutePageRespVO(MesProRoutePageReqVO pageReqVO) {
        PageResult<MesProRouteDO> pageResult = routeMapper.selectPage(pageReqVO);
        List<MesProRouteRespVO> list = BeanUtils.toBean(pageResult.getList(), MesProRouteRespVO.class);
        enrichRouteDisplayFields(list);
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public List<MesProRouteDO> getRouteList() {
        return routeMapper.selectList();
    }

    @Override
    public List<MesProRouteDO> getRouteListByStatus(Integer status) {
        return routeMapper.selectListByStatus(status);
    }

    @Override
    public void validateRouteNotEnable(Long routeId) {
        MesProRouteDO route = routeMapper.selectById(routeId);
        if (route != null && CommonStatusEnum.ENABLE.getStatus().equals(route.getStatus())) {
            throw exception(PRO_ROUTE_IS_ENABLE);
        }
    }

    @Override
    public List<MesProRouteDO> getRouteList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return routeMapper.selectByIds(ids);
    }

    @Override
    public List<MesProRouteDO> getRouteListIgnoreDeleted(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return routeMapper.selectListByIdsIgnoreDeleted(ids);
    }

    private void enrichRouteDisplayFields(List<MesProRouteRespVO> routes) {
        if (CollUtil.isEmpty(routes)) {
            return;
        }

        Map<Long, String> parsedOwnerByRouteId = new HashMap<>();
        for (MesProRouteRespVO route : routes) {
            RouteRemarkParts parts = splitRouteRemark(route.getRemark());
            route.setRemark(parts.visibleRemark());
            if (StrUtil.isNotBlank(parts.ownerName())) {
                route.setOwnerName(parts.ownerName());
                parsedOwnerByRouteId.put(route.getId(), parts.ownerName());
            }
        }

        Set<Long> routeIds = routes.stream()
                .map(MesProRouteRespVO::getId)
                .filter(ObjUtil::isNotNull)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(routeIds)) {
            return;
        }

        Map<Long, String> ownerByRouteId = buildOwnerByRouteId(routeIds);
        Map<Long, String> keyProcessByRouteId = buildKeyProcessByRouteId(routeIds);
        Map<Long, String> lastProcessByRouteId = buildLastProcessByRouteId(routeIds);
        Map<Long, String> productCodesByRouteId = buildProductCodesByRouteId(routeIds);
        Map<Long, Boolean> flowGraphConfiguredByRouteId = buildFlowGraphConfiguredByRouteId(routeIds);
        Map<Long, Map<String, Boolean>> flowConfigEnabledByRouteId = buildRouteFlowConfigEnabledByRouteId(routeIds);
        List<MesProRouteVersionDO> routeVersions = routeVersionMapper.selectListByRouteIds(routeIds);
        Map<Long, MesProRouteVersionDO> activeVersionByRouteId = routeVersions.stream()
                .filter(version -> Boolean.TRUE.equals(version.getActive()))
                .collect(Collectors.toMap(MesProRouteVersionDO::getRouteId, version -> version, (left, right) -> left,
                        LinkedHashMap::new));
        Map<Long, List<MesProRouteVersionDO>> pendingVersionsByRouteId = buildPendingVersionsByRouteId(routeVersions);
        for (MesProRouteRespVO route : routes) {
            route.setOwnerName(StrUtil.blankToDefault(route.getOwnerName(), ownerByRouteId.get(route.getId())));
            route.setKeyProcessName(keyProcessByRouteId.get(route.getId()));
            route.setLastProcessName(lastProcessByRouteId.get(route.getId()));
            route.setProductCodes(productCodesByRouteId.get(route.getId()));
            route.setFlowGraphConfigured(Boolean.TRUE.equals(flowGraphConfiguredByRouteId.get(route.getId())));
            Map<String, Boolean> flowConfigEnabled = flowConfigEnabledByRouteId.getOrDefault(route.getId(), Collections.emptyMap());
            route.setScheduleRouteEnabled(Boolean.TRUE.equals(flowConfigEnabled.get(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())));
            route.setBatchRouteEnabled(Boolean.TRUE.equals(flowConfigEnabled.get(MesProRouteFlowConfigTypeEnum.BATCH.getType())));
            MesProRouteVersionDO activeVersion = activeVersionByRouteId.get(route.getId());
            route.setActiveRouteVersionId(activeVersion == null ? null : activeVersion.getId());
            route.setActiveRouteVersionNo(activeVersion == null ? null : activeVersion.getVersionNo());
            List<MesProRouteVersionDO> pendingVersions = pendingVersionsByRouteId.getOrDefault(route.getId(), Collections.emptyList());
            MesProRouteVersionDO pendingVersion = CollUtil.isEmpty(pendingVersions) ? null : pendingVersions.get(0);
            route.setPendingRouteVersionId(pendingVersion == null ? null : pendingVersion.getId());
            route.setPendingRouteVersionNo(pendingVersion == null ? null : pendingVersion.getVersionNo());
            route.setPendingRouteVersionStatus(pendingVersion == null ? null : pendingVersion.getLifecycleStatus());
            route.setPendingRouteVersionCount(pendingVersions.size());
        }
    }

    private Map<Long, List<MesProRouteVersionDO>> buildPendingVersionsByRouteId(List<MesProRouteVersionDO> versions) {
        if (CollUtil.isEmpty(versions)) {
            return Collections.emptyMap();
        }
        return versions.stream()
                .filter(version -> version.getRouteId() != null)
                .filter(this::isPendingRouteVersion)
                .sorted(Comparator
                        .comparingInt((MesProRouteVersionDO version) ->
                                pendingRouteVersionPriority(version.getLifecycleStatus()))
                        .thenComparing(version -> version.getId() == null ? 0L : version.getId(),
                                Comparator.reverseOrder()))
                .collect(Collectors.groupingBy(MesProRouteVersionDO::getRouteId, LinkedHashMap::new,
                        Collectors.toList()));
    }

    private boolean isPendingRouteVersion(MesProRouteVersionDO version) {
        return !Boolean.TRUE.equals(version.getActive())
                && PENDING_ROUTE_VERSION_STATUSES.contains(version.getLifecycleStatus());
    }

    private int pendingRouteVersionPriority(String lifecycleStatus) {
        int priority = PENDING_ROUTE_VERSION_STATUSES.indexOf(lifecycleStatus);
        return priority < 0 ? PENDING_ROUTE_VERSION_STATUSES.size() : priority;
    }

    private Map<Long, Boolean> buildFlowGraphConfiguredByRouteId(Set<Long> routeIds) {
        Set<Long> configuredRouteIds = new LinkedHashSet<>();
        configuredRouteIds.addAll(routeProcessFlowEdgeMapper.selectConfiguredRouteIdsByRouteIds(routeIds));
        configuredRouteIds.addAll(routeProcessFlowBoundaryEdgeMapper.selectConfiguredRouteIdsByRouteIds(routeIds));
        configuredRouteIds.addAll(routeProcessMapper.selectRelationConfiguredRouteIdsByRouteIds(routeIds));
        if (CollUtil.isEmpty(configuredRouteIds)) {
            return Collections.emptyMap();
        }
        return configuredRouteIds.stream()
                .collect(Collectors.toMap(routeId -> routeId, routeId -> true, (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Map<Long, Map<String, Boolean>> buildRouteFlowConfigEnabledByRouteId(Set<Long> routeIds) {
        List<MesProRouteFlowConfigDO> flowConfigs = routeFlowConfigMapper.selectListByRouteIds(routeIds);
        if (CollUtil.isEmpty(flowConfigs)) {
            return Collections.emptyMap();
        }
        Map<Long, Map<String, Boolean>> result = new HashMap<>();
        for (MesProRouteFlowConfigDO flowConfig : flowConfigs) {
            result.computeIfAbsent(flowConfig.getRouteId(), key -> new HashMap<>())
                    .put(flowConfig.getUseType(), Boolean.TRUE.equals(flowConfig.getEnabled()));
        }
        return result;
    }

    private Map<Long, String> buildOwnerByRouteId(Set<Long> routeIds) {
        List<MesProRouteProductDO> routeProducts = routeProductMapper.selectListByRouteIds(routeIds);
        Map<Long, String> ownerByRouteId = new HashMap<>();
        routeProducts.stream()
                .sorted(Comparator.comparing(MesProRouteProductDO::getId, Comparator.nullsLast(Long::compareTo)))
                .forEach(routeProduct -> {
                    String ownerName = extractOwnerName(routeProduct.getRemark());
                    if (StrUtil.isNotBlank(ownerName)) {
                        ownerByRouteId.putIfAbsent(routeProduct.getRouteId(), ownerName);
                    }
                });
        return ownerByRouteId;
    }

    private Map<Long, String> buildLastProcessByRouteId(Set<Long> routeIds) {
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteIds(routeIds);
        if (CollUtil.isEmpty(routeProcesses)) {
            return Collections.emptyMap();
        }

        Map<Long, MesProRouteProcessDO> lastRouteProcessByRouteId = new HashMap<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            MesProRouteProcessDO current = lastRouteProcessByRouteId.get(routeProcess.getRouteId());
            if (current == null
                    || Comparator.comparing(MesProRouteProcessDO::getSort, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(MesProRouteProcessDO::getId, Comparator.nullsLast(Long::compareTo))
                    .compare(routeProcess, current) > 0) {
                lastRouteProcessByRouteId.put(routeProcess.getRouteId(), routeProcess);
            }
        }

        Set<Long> processIds = lastRouteProcessByRouteId.values().stream()
                .map(MesProRouteProcessDO::getProcessId)
                .filter(ObjUtil::isNotNull)
                .collect(Collectors.toSet());
        Map<Long, MesProProcessDO> processMap = processMapper.selectListByIds(processIds).stream()
                .collect(Collectors.toMap(MesProProcessDO::getId, process -> process));

        Map<Long, String> lastProcessNameByRouteId = new HashMap<>();
        for (Map.Entry<Long, MesProRouteProcessDO> entry : lastRouteProcessByRouteId.entrySet()) {
            MesProProcessDO process = processMap.get(entry.getValue().getProcessId());
            if (process != null) {
                lastProcessNameByRouteId.put(entry.getKey(), process.getName());
            }
        }
        return lastProcessNameByRouteId;
    }

    private Map<Long, String> buildKeyProcessByRouteId(Set<Long> routeIds) {
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteIds(routeIds);
        if (CollUtil.isEmpty(routeProcesses)) {
            return Collections.emptyMap();
        }

        Map<Long, MesProRouteProcessDO> keyProcessByRouteId = new HashMap<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            if (!Boolean.TRUE.equals(routeProcess.getKeyFlag())) {
                continue;
            }
            MesProRouteProcessDO current = keyProcessByRouteId.get(routeProcess.getRouteId());
            if (current == null
                    || Comparator.comparing(MesProRouteProcessDO::getSort, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(MesProRouteProcessDO::getId, Comparator.nullsLast(Long::compareTo))
                    .compare(routeProcess, current) < 0) {
                keyProcessByRouteId.put(routeProcess.getRouteId(), routeProcess);
            }
        }

        Set<Long> processIds = keyProcessByRouteId.values().stream()
                .map(MesProRouteProcessDO::getProcessId)
                .filter(ObjUtil::isNotNull)
                .collect(Collectors.toSet());
        Map<Long, MesProProcessDO> processMap = processIds.isEmpty()
                ? Collections.emptyMap()
                : processMapper.selectListByIds(processIds).stream()
                .collect(Collectors.toMap(MesProProcessDO::getId, process -> process));

        Map<Long, String> keyProcessNameByRouteId = new HashMap<>();
        for (Map.Entry<Long, MesProRouteProcessDO> entry : keyProcessByRouteId.entrySet()) {
            MesProProcessDO process = processMap.get(entry.getValue().getProcessId());
            if (process != null) {
                keyProcessNameByRouteId.put(entry.getKey(), process.getName());
            }
        }
        return keyProcessNameByRouteId;
    }

    private Map<Long, String> buildProductCodesByRouteId(Set<Long> routeIds) {
        List<MesProRouteProductDO> routeProducts = routeProductMapper.selectListByRouteIds(routeIds);
        if (CollUtil.isEmpty(routeProducts)) {
            return Collections.emptyMap();
        }

        Set<Long> itemIds = routeProducts.stream()
                .map(MesProRouteProductDO::getItemId)
                .filter(ObjUtil::isNotNull)
                .collect(Collectors.toSet());
        Map<Long, MesMdItemDO> itemMap = itemIds.isEmpty() ? Collections.emptyMap() : itemService.getItemMap(itemIds);

        Map<Long, LinkedHashSet<String>> productCodesByRouteId = new HashMap<>();
        routeProducts.stream()
                .sorted(Comparator.comparing(MesProRouteProductDO::getId, Comparator.nullsLast(Long::compareTo)))
                .forEach(routeProduct -> {
                    MesMdItemDO item = itemMap.get(routeProduct.getItemId());
                    if (item == null || StrUtil.isBlank(item.getCode())) {
                        return;
                    }
                    productCodesByRouteId
                            .computeIfAbsent(routeProduct.getRouteId(), key -> new LinkedHashSet<>())
                            .add(item.getCode());
                });

        Map<Long, String> joined = new HashMap<>();
        for (Map.Entry<Long, LinkedHashSet<String>> entry : productCodesByRouteId.entrySet()) {
            joined.put(entry.getKey(), StrUtil.join("、", entry.getValue()));
        }
        return joined;
    }

    private String extractOwnerName(String remark) {
        RouteRemarkParts parts = splitRouteRemark(remark);
        if (StrUtil.isNotBlank(parts.ownerName())) {
            return parts.ownerName();
        }
        String normalized = parts.visibleRemark();
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        int ownerIndex = normalized.lastIndexOf("owner ");
        if (ownerIndex >= 0) {
            String ownerName = StrUtil.trim(normalized.substring(ownerIndex + "owner ".length()));
            return StrUtil.blankToDefault(ownerName, null);
        }
        int chineseOwnerIndex = normalized.lastIndexOf("负责人");
        if (chineseOwnerIndex >= 0) {
            String ownerName = StrUtil.trim(normalized.substring(chineseOwnerIndex + "负责人".length()));
            return StrUtil.blankToDefault(ownerName, null);
        }
        return null;
    }

    private String buildRouteRemark(String visibleRemark, String ownerName) {
        String normalizedRemark = StrUtil.trimToEmpty(visibleRemark);
        String normalizedOwner = StrUtil.trimToEmpty(ownerName);
        if (StrUtil.isBlank(normalizedOwner)) {
            return normalizedRemark;
        }
        if (StrUtil.isBlank(normalizedRemark)) {
            return OWNER_PREFIX + normalizedOwner + OWNER_SUFFIX;
        }
        return OWNER_PREFIX + normalizedOwner + OWNER_SUFFIX + "\n" + normalizedRemark;
    }

    private RouteRemarkParts splitRouteRemark(String remark) {
        if (StrUtil.isBlank(remark)) {
            return new RouteRemarkParts(null, null);
        }
        String normalized = StrUtil.trimToEmpty(remark);
        int prefixIndex = normalized.indexOf(OWNER_PREFIX);
        int suffixIndex = normalized.indexOf(OWNER_SUFFIX);
        if (prefixIndex >= 0 && suffixIndex > prefixIndex) {
            String ownerName = StrUtil.trim(normalized.substring(prefixIndex + OWNER_PREFIX.length(), suffixIndex));
            String visibleRemark = StrUtil.trim(
                    normalized.substring(0, prefixIndex) + normalized.substring(suffixIndex + OWNER_SUFFIX.length()));
            return new RouteRemarkParts(StrUtil.blankToDefault(ownerName, null), StrUtil.blankToDefault(visibleRemark, null));
        }
        return new RouteRemarkParts(null, StrUtil.blankToDefault(normalized, null));
    }

    private record RouteRemarkParts(String ownerName, String visibleRemark) {
    }

}
