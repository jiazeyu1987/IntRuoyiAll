package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowBoundaryEdgeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowBoundaryEdgeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowEdgeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowEdgeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowGraphRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowLayoutReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowNodeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowValidationMessageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowValidationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process.MesProRouteProcessSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowBoundaryEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowLayoutDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowBoundaryEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowLayoutMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleCapacityModeEnum;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_PROCESS_FLOW_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_PROCESS_FLOW_VERSION_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE;

/**
 * MES 工艺路线工序流转关系图 Service 实现类
 */
@Service
@Validated
public class MesProRouteProcessFlowServiceImpl implements MesProRouteProcessFlowService {

    private static final String STATUS_EMPTY_PROCESS = "EMPTY_PROCESS";
    private static final String STATUS_UNINITIALIZED = "UNINITIALIZED";
    private static final String STATUS_INVALID = "INVALID";
    private static final String STATUS_VALID = "VALID";
    private static final String RELATION_TYPE_NORMAL = "NORMAL";
    private static final String BOUNDARY_TYPE_START = "START";
    private static final String BOUNDARY_TYPE_END = "END";
    private static final String SNAPSHOT_CONFIGS_KEY = "configSnapshots";
    private static final String FLOW_GRAPH_KEY = "flowGraph";
    private static final String SCHEDULE_CONFIGS_KEY = "scheduleConfigs";
    private static final Set<String> READABLE_CANDIDATE_STATUSES = Set.of(
            MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT,
            MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL,
            MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH);

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
    private MesProProcessService processService;
    @Resource
    @Lazy
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProRouteCandidateConfigService routeCandidateConfigService;
    @Resource
    private MesMdWorkstationService workstationService;

    @Override
    public MesProRouteProcessFlowGraphRespVO getGraph(Long routeId) {
        return getGraph(routeId, null);
    }

    @Override
    public MesProRouteProcessFlowGraphRespVO getGraph(Long routeId, Long routeVersionId) {
        validateRouteExists(routeId);
        MesProRouteVersionDO readableRouteVersion = resolveReadableRouteVersion(routeVersionId, routeId);
        if (isReadableCandidateVersion(readableRouteVersion)) {
            return getCandidateGraph(readableRouteVersion, routeId);
        }
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(routeId);
        List<MesProRouteProcessFlowEdgeDO> edges = flowEdgeMapper.selectListByRouteId(routeId);
        List<MesProRouteProcessFlowBoundaryEdgeDO> boundaryEdges = boundaryEdgeMapper.selectListByRouteId(routeId);
        List<MesProRouteProcessFlowLayoutDO> layouts = flowLayoutMapper.selectListByRouteId(routeId);
        Long graphVersion = currentGraphVersion(routeId);

        MesProRouteProcessFlowGraphRespVO respVO = new MesProRouteProcessFlowGraphRespVO();
        respVO.setRouteId(routeId);
        respVO.setGraphVersion(graphVersion);
        respVO.setNodes(buildNodes(routeProcesses, layouts));
        respVO.setEdges(edges.stream().map(this::toEdgeRespVO).toList());
        respVO.setBoundaryEdges(boundaryEdges.stream().map(this::toBoundaryEdgeRespVO).toList());

        if (CollUtil.isEmpty(routeProcesses)) {
            mark(respVO, true, STATUS_EMPTY_PROCESS, graphVersion, List.of());
            return respVO;
        }
        if (CollUtil.isEmpty(edges) && CollUtil.isEmpty(boundaryEdges)) {
            mark(respVO, false, STATUS_UNINITIALIZED, graphVersion,
                    List.of(message("ERROR", "UNINITIALIZED_GRAPH", "请先配置流转关系图",
                            routeProcessIds(routeProcesses).stream().toList())));
            return respVO;
        }
        MesProRouteProcessFlowSaveReqVO validateReq = new MesProRouteProcessFlowSaveReqVO();
        validateReq.setRouteId(routeId);
        validateReq.setGraphVersion(graphVersion);
        validateReq.setEdges(edges.stream().map(this::toEdgeReqVO).toList());
        validateReq.setBoundaryEdges(boundaryEdges.stream().map(this::toBoundaryEdgeReqVO).toList());
        validateReq.setLayouts(layouts.stream().map(this::toLayoutReqVO).toList());
        copyValidation(respVO, validateGraph(validateReq));
        return respVO;
    }

    private MesProRouteProcessFlowGraphRespVO getCandidateGraph(MesProRouteVersionDO candidateVersion,
                                                                Long routeId) {
        JSONObject flowGraph = resolveCandidateFlowGraphSnapshot(candidateVersion);
        Long graphVersion = flowGraph.getLong("graphVersion");
        if (graphVersion == null) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, candidateVersion.getId());
        }
        List<MesProRouteProcessDO> routeProcesses = parseCandidateRouteProcesses(candidateVersion, flowGraph);
        List<MesProRouteProcessFlowEdgeReqVO> edges = parseCandidateFlowEdges(candidateVersion, flowGraph, routeProcesses);
        List<MesProRouteProcessFlowBoundaryEdgeReqVO> boundaryEdges =
                parseCandidateBoundaryEdges(candidateVersion, flowGraph, routeProcesses);
        List<MesProRouteProcessFlowLayoutDO> layouts = parseCandidateLayouts(candidateVersion, flowGraph, routeProcesses);

        MesProRouteProcessFlowGraphRespVO respVO = new MesProRouteProcessFlowGraphRespVO();
        respVO.setRouteId(routeId);
        respVO.setGraphVersion(graphVersion);
        respVO.setNodes(buildNodes(routeProcesses, layouts));
        respVO.setEdges(edges.stream().map(this::toEdgeRespVO).toList());
        respVO.setBoundaryEdges(boundaryEdges.stream().map(this::toBoundaryEdgeRespVO).toList());

        if (CollUtil.isEmpty(routeProcesses)) {
            return mark(respVO, true, STATUS_EMPTY_PROCESS, graphVersion, List.of());
        }
        if (CollUtil.isEmpty(edges) && CollUtil.isEmpty(boundaryEdges)) {
            return mark(respVO, false, STATUS_UNINITIALIZED, graphVersion,
                    List.of(message("ERROR", "UNINITIALIZED_GRAPH", "请先配置流转关系图",
                            routeProcessIds(routeProcesses).stream().toList())));
        }
        List<MesProRouteProcessFlowValidationMessageRespVO> messages =
                validateEdges(routeProcesses, edges, boundaryEdges);
        return mark(respVO, messages.isEmpty(), messages.isEmpty() ? STATUS_VALID : STATUS_INVALID,
                graphVersion, messages);
    }

    private JSONObject resolveCandidateFlowGraphSnapshot(MesProRouteVersionDO candidateVersion) {
        if (candidateVersion == null || StrUtil.isBlank(candidateVersion.getRouteSnapshotJson())) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE,
                    candidateVersion == null ? null : candidateVersion.getId());
        }
        JSONObject routeSnapshot;
        try {
            routeSnapshot = JSON.parseObject(candidateVersion.getRouteSnapshotJson());
        } catch (RuntimeException ex) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, candidateVersion.getId());
        }
        JSONObject configSnapshots = routeSnapshot == null ? null : routeSnapshot.getJSONObject(SNAPSHOT_CONFIGS_KEY);
        JSONObject flowGraph = configSnapshots == null ? null : configSnapshots.getJSONObject(FLOW_GRAPH_KEY);
        if (flowGraph == null) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, candidateVersion.getId());
        }
        return flowGraph;
    }

    private List<MesProRouteProcessDO> parseCandidateRouteProcesses(MesProRouteVersionDO candidateVersion,
                                                                    JSONObject flowGraph) {
        JSONArray nodes = flowGraph.getJSONArray("nodes");
        if (nodes == null) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, candidateVersion.getId());
        }
        List<MesProRouteProcessDO> routeProcesses = new ArrayList<>();
        for (Object value : nodes) {
            JSONObject node = toCandidateJsonObject(candidateVersion, value);
            Long routeProcessId = node.getLong("routeProcessId");
            if (routeProcessId == null) {
                routeProcessId = node.getLong("clientRouteProcessId");
            }
            Long processId = node.getLong("processId");
            Integer sort = node.getInteger("sort");
            if (routeProcessId == null || processId == null || sort == null) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, candidateVersion.getId());
            }
            routeProcesses.add(MesProRouteProcessDO.builder()
                    .id(routeProcessId)
                    .routeId(candidateVersion.getRouteId())
                    .processId(processId)
                    .workstationId(node.getLong("workstationId"))
                    .sort(sort)
                    .keyFlag(Boolean.TRUE.equals(node.getBoolean("keyFlag")))
                    .checkFlag(Boolean.TRUE.equals(node.getBoolean("checkFlag")))
                    .build());
        }
        routeProcesses.sort(Comparator.comparing(
                MesProRouteProcessDO::getSort, Comparator.nullsLast(Integer::compareTo)));
        return routeProcesses;
    }

    private List<MesProRouteProcessFlowEdgeReqVO> parseCandidateFlowEdges(
            MesProRouteVersionDO candidateVersion,
            JSONObject flowGraph,
            List<MesProRouteProcessDO> routeProcesses) {
        JSONArray edges = flowGraph.getJSONArray("edges");
        if (edges == null) {
            return List.of();
        }
        List<MesProRouteProcessFlowEdgeReqVO> result = new ArrayList<>();
        for (Object value : edges) {
            JSONObject edge = toCandidateJsonObject(candidateVersion, value);
            MesProRouteProcessFlowEdgeReqVO reqVO = new MesProRouteProcessFlowEdgeReqVO();
            reqVO.setSourceRouteProcessId(resolveCandidateRouteProcessId(
                    candidateVersion, edge, "sourceSort", "sourceRouteProcessId", routeProcesses));
            reqVO.setTargetRouteProcessId(resolveCandidateRouteProcessId(
                    candidateVersion, edge, "targetSort", "targetRouteProcessId", routeProcesses));
            reqVO.setRelationType(normalRelationType(edge.getString("relationType")));
            result.add(reqVO);
        }
        return result;
    }

    private List<MesProRouteProcessFlowBoundaryEdgeReqVO> parseCandidateBoundaryEdges(
            MesProRouteVersionDO candidateVersion,
            JSONObject flowGraph,
            List<MesProRouteProcessDO> routeProcesses) {
        JSONArray boundaryEdges = flowGraph.getJSONArray("boundaryEdges");
        if (boundaryEdges == null) {
            return List.of();
        }
        List<MesProRouteProcessFlowBoundaryEdgeReqVO> result = new ArrayList<>();
        for (int index = 0; index < boundaryEdges.size(); index++) {
            JSONObject edge = toCandidateJsonObject(candidateVersion, boundaryEdges.get(index));
            MesProRouteProcessFlowBoundaryEdgeReqVO reqVO = new MesProRouteProcessFlowBoundaryEdgeReqVO();
            reqVO.setBoundaryType(edge.getString("boundaryType"));
            reqVO.setRouteProcessId(resolveCandidateRouteProcessId(
                    candidateVersion, edge, "routeProcessSort", "routeProcessId", routeProcesses));
            reqVO.setSort(edge.getInteger("sort") == null ? index + 1 : edge.getInteger("sort"));
            result.add(reqVO);
        }
        return result;
    }

    private List<MesProRouteProcessFlowLayoutDO> parseCandidateLayouts(
            MesProRouteVersionDO candidateVersion,
            JSONObject flowGraph,
            List<MesProRouteProcessDO> routeProcesses) {
        JSONArray layouts = flowGraph.getJSONArray("layouts");
        if (layouts == null) {
            return List.of();
        }
        List<MesProRouteProcessFlowLayoutDO> result = new ArrayList<>();
        Long graphVersion = flowGraph.getLong("graphVersion");
        for (Object value : layouts) {
            JSONObject layout = toCandidateJsonObject(candidateVersion, value);
            result.add(MesProRouteProcessFlowLayoutDO.builder()
                    .routeId(candidateVersion.getRouteId())
                    .routeProcessId(resolveCandidateRouteProcessId(
                            candidateVersion, layout, "routeProcessSort", "routeProcessId", routeProcesses))
                    .x(layout.getInteger("x"))
                    .y(layout.getInteger("y"))
                    .width(layout.getInteger("width"))
                    .height(layout.getInteger("height"))
                    .graphVersion(graphVersion)
                    .build());
        }
        return result;
    }

    private Long resolveCandidateRouteProcessId(MesProRouteVersionDO candidateVersion,
                                                JSONObject object,
                                                String sortKey,
                                                String idKey,
                                                List<MesProRouteProcessDO> routeProcesses) {
        Integer sort = object.getInteger(sortKey);
        if (sort != null) {
            return routeProcesses.stream()
                    .filter(routeProcess -> Objects.equals(routeProcess.getSort(), sort))
                    .map(MesProRouteProcessDO::getId)
                    .findFirst()
                    .orElseThrow(() -> exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, candidateVersion.getId()));
        }
        Long routeProcessId = object.getLong(idKey);
        if (routeProcessId != null && routeProcessIds(routeProcesses).contains(routeProcessId)) {
            return routeProcessId;
        }
        throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, candidateVersion.getId());
    }

    private JSONObject toCandidateJsonObject(MesProRouteVersionDO candidateVersion, Object value) {
        if (value instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(value));
        if (jsonObject == null) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, candidateVersion.getId());
        }
        return jsonObject;
    }

    @Override
    public MesProRouteProcessFlowValidationRespVO validateGraph(MesProRouteProcessFlowSaveReqVO reqVO) {
        validateRouteExists(reqVO.getRouteId());
        Long graphVersion = currentGraphVersion(reqVO.getRouteId());
        MesProRouteProcessFlowValidationRespVO respVO = new MesProRouteProcessFlowValidationRespVO();
        respVO.setGraphVersion(graphVersion);
        DraftRouteProcessValidation draftValidation = validateRouteProcessDraftChanges(reqVO);
        if (!draftValidation.messages().isEmpty()) {
            return mark(respVO, false, STATUS_INVALID, graphVersion, draftValidation.messages());
        }
        List<MesProRouteProcessDO> routeProcesses = draftValidation.routeProcesses();
        if (CollUtil.isEmpty(routeProcesses)) {
            return mark(respVO, true, STATUS_EMPTY_PROCESS, graphVersion, List.of());
        }
        List<MesProRouteProcessFlowValidationMessageRespVO> messages =
                validateEdges(routeProcesses, safeList(reqVO.getEdges()), safeList(reqVO.getBoundaryEdges()));
        return mark(respVO, messages.isEmpty(), messages.isEmpty() ? STATUS_VALID : STATUS_INVALID, graphVersion, messages);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProRouteProcessFlowValidationRespVO saveGraph(MesProRouteProcessFlowSaveReqVO reqVO) {
        Long currentVersion = currentGraphVersion(reqVO.getRouteId());
        if (!Objects.equals(currentVersion, reqVO.getGraphVersion())) {
            throw exception(PRO_ROUTE_PROCESS_FLOW_VERSION_CONFLICT);
        }
        MesProRouteProcessFlowValidationRespVO draftValidation = validateGraph(reqVO);
        if (!Boolean.TRUE.equals(draftValidation.getValid())) {
            throw exception(PRO_ROUTE_PROCESS_FLOW_INVALID);
        }
        MesProRouteVersionDO candidateVersion = resolveDraftCandidateVersion(
                reqVO.getRouteVersionId(), reqVO.getRouteId());
        if (candidateVersion != null) {
            Long nextVersion = currentVersion + 1;
            DraftRouteProcessValidation routeProcessDraftValidation = validateRouteProcessDraftChanges(reqVO);
            routeCandidateConfigService.saveConfigSnapshot(candidateVersion.getId(), "flowGraph",
                    buildCandidateFlowGraphSnapshot(reqVO, nextVersion, routeProcessDraftValidation.routeProcesses()));
            routeCandidateConfigService.saveConfigSnapshot(candidateVersion.getId(), SCHEDULE_CONFIGS_KEY,
                    buildCandidateScheduleConfigSnapshot(candidateVersion, routeProcessDraftValidation.routeProcesses()));
            return mark(new MesProRouteProcessFlowValidationRespVO(), true, STATUS_VALID, nextVersion, List.of());
        }
        Map<Long, Long> persistedRouteProcessIdMap = persistRouteProcessDraftChanges(reqVO);
        MesProRouteProcessFlowSaveReqVO persistedReqVO = remapRouteProcessDraftIds(reqVO, persistedRouteProcessIdMap);
        MesProRouteProcessFlowValidationRespVO validation = validateGraph(persistedReqVO);
        if (!Boolean.TRUE.equals(validation.getValid())) {
            throw exception(PRO_ROUTE_PROCESS_FLOW_INVALID);
        }
        Long nextVersion = currentVersion + 1;
        flowEdgeMapper.deleteByRouteId(persistedReqVO.getRouteId());
        boundaryEdgeMapper.deleteByRouteId(persistedReqVO.getRouteId());
        flowLayoutMapper.deleteByRouteId(persistedReqVO.getRouteId());

        List<MesProRouteProcessFlowEdgeReqVO> edges = safeList(persistedReqVO.getEdges());
        for (int index = 0; index < edges.size(); index++) {
            MesProRouteProcessFlowEdgeReqVO edge = edges.get(index);
            flowEdgeMapper.insert(MesProRouteProcessFlowEdgeDO.builder()
                    .routeId(persistedReqVO.getRouteId())
                    .graphVersion(nextVersion)
                    .sourceRouteProcessId(edge.getSourceRouteProcessId())
                    .targetRouteProcessId(edge.getTargetRouteProcessId())
                    .relationType(normalRelationType(edge.getRelationType()))
                    .sort(index + 1)
                    .build());
        }
        List<MesProRouteProcessFlowBoundaryEdgeReqVO> boundaryEdges = safeList(persistedReqVO.getBoundaryEdges());
        for (int index = 0; index < boundaryEdges.size(); index++) {
            MesProRouteProcessFlowBoundaryEdgeReqVO boundaryEdge = boundaryEdges.get(index);
            boundaryEdgeMapper.insert(MesProRouteProcessFlowBoundaryEdgeDO.builder()
                    .routeId(persistedReqVO.getRouteId())
                    .graphVersion(nextVersion)
                    .boundaryType(boundaryEdge.getBoundaryType())
                    .routeProcessId(boundaryEdge.getRouteProcessId())
                    .sort(boundaryEdge.getSort() == null ? index + 1 : boundaryEdge.getSort())
                    .build());
        }
        List<MesProRouteProcessFlowLayoutReqVO> layouts = safeList(persistedReqVO.getLayouts());
        for (MesProRouteProcessFlowLayoutReqVO layout : layouts) {
            flowLayoutMapper.insert(MesProRouteProcessFlowLayoutDO.builder()
                    .routeId(persistedReqVO.getRouteId())
                    .routeProcessId(layout.getRouteProcessId())
                    .x(layout.getX())
                    .y(layout.getY())
                    .width(layout.getWidth())
                    .height(layout.getHeight())
                    .graphVersion(nextVersion)
                    .build());
        }
        validation = mark(new MesProRouteProcessFlowValidationRespVO(), true, STATUS_VALID, nextVersion, List.of());
        validation.setRouteProcessIdMap(persistedRouteProcessIdMap);
        return validation;
    }

    private MesProRouteVersionDO resolveReadableCandidateVersion(Long routeVersionId, Long routeId) {
        return resolveCandidateVersion(routeVersionId, routeId, READABLE_CANDIDATE_STATUSES);
    }

    private MesProRouteVersionDO resolveReadableRouteVersion(Long routeVersionId, Long routeId) {
        if (routeVersionId == null) {
            return null;
        }
        MesProRouteVersionDO routeVersion = routeVersionMapper.selectById(routeVersionId);
        if (routeVersion == null) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, routeVersionId);
        }
        if (!Objects.equals(routeVersion.getRouteId(), routeId)) {
            throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                    routeVersion.getId(), routeVersion.getLifecycleStatus());
        }
        if (isReadableCandidateVersion(routeVersion) || isActiveRouteVersion(routeVersion)) {
            return routeVersion;
        }
        throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                routeVersion.getId(), routeVersion.getLifecycleStatus());
    }

    private MesProRouteVersionDO resolveDraftCandidateVersion(Long routeVersionId, Long routeId) {
        return resolveCandidateVersion(routeVersionId, routeId,
                Set.of(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT));
    }

    private MesProRouteVersionDO resolveCandidateVersion(Long routeVersionId, Long routeId,
                                                         Set<String> allowedStatuses) {
        if (routeVersionId == null) {
            return null;
        }
        MesProRouteVersionDO routeVersion = routeVersionMapper.selectById(routeVersionId);
        if (routeVersion == null) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, routeVersionId);
        }
        if (Boolean.FALSE.equals(routeVersion.getActive())
                && allowedStatuses.contains(routeVersion.getLifecycleStatus())) {
            if (!Objects.equals(routeVersion.getRouteId(), routeId)) {
                throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                        routeVersion.getId(), routeVersion.getLifecycleStatus());
            }
            return routeVersion;
        }
        throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                routeVersion.getId(), routeVersion.getLifecycleStatus());
    }

    private boolean isReadableCandidateVersion(MesProRouteVersionDO routeVersion) {
        return routeVersion != null
                && Boolean.FALSE.equals(routeVersion.getActive())
                && READABLE_CANDIDATE_STATUSES.contains(routeVersion.getLifecycleStatus());
    }

    private boolean isActiveRouteVersion(MesProRouteVersionDO routeVersion) {
        return routeVersion != null
                && Boolean.TRUE.equals(routeVersion.getActive())
                && MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE.equals(routeVersion.getLifecycleStatus());
    }

    private Map<String, Object> buildCandidateFlowGraphSnapshot(MesProRouteProcessFlowSaveReqVO reqVO,
                                                                Long nextVersion,
                                                                List<MesProRouteProcessDO> routeProcesses) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("routeId", reqVO.getRouteId());
        snapshot.put("graphVersion", nextVersion);
        snapshot.put("nodes", routeProcesses.stream()
                .sorted(Comparator.comparing(
                        MesProRouteProcessDO::getSort, Comparator.nullsLast(Integer::compareTo)))
                .map(this::buildCandidateFlowNodeSnapshot)
                .toList());
        snapshot.put("edges", safeList(reqVO.getEdges()));
        snapshot.put("boundaryEdges", safeList(reqVO.getBoundaryEdges()));
        snapshot.put("layouts", safeList(reqVO.getLayouts()));
        snapshot.put("routeProcessCreates", safeList(reqVO.getRouteProcessCreates()));
        snapshot.put("routeProcessUpdates", safeList(reqVO.getRouteProcessUpdates()));
        snapshot.put("routeProcessDeletes", safeList(reqVO.getRouteProcessDeletes()));
        return snapshot;
    }

    private Map<String, Object> buildCandidateFlowNodeSnapshot(MesProRouteProcessDO routeProcess) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("routeProcessId", routeProcess.getId());
        node.put("processId", routeProcess.getProcessId());
        node.put("workstationId", routeProcess.getWorkstationId());
        node.put("sort", routeProcess.getSort());
        node.put("keyFlag", Boolean.TRUE.equals(routeProcess.getKeyFlag()));
        node.put("checkFlag", Boolean.TRUE.equals(routeProcess.getCheckFlag()));
        return node;
    }

    private JSONObject buildCandidateScheduleConfigSnapshot(MesProRouteVersionDO candidateVersion,
                                                            List<MesProRouteProcessDO> routeProcesses) {
        JSONObject existingConfigs = resolveCandidateScheduleConfigMap(candidateVersion);
        JSONObject result = new JSONObject(true);
        routeProcesses.stream()
                .sorted(Comparator.comparing(MesProRouteProcessDO::getSort, Comparator.nullsLast(Integer::compareTo)))
                .forEach(routeProcess -> {
                    String routeProcessKey = String.valueOf(routeProcess.getId());
                    JSONObject existingConfig = existingConfigs.getJSONObject(routeProcessKey);
                    if (existingConfig == null) {
                        existingConfig = findScheduleConfigBySort(existingConfigs, routeProcess.getSort());
                    }
                    JSONObject config = existingConfig == null
                            ? buildDefaultScheduleConfigSnapshot(candidateVersion, routeProcess)
                            : copyScheduleConfigSnapshot(existingConfig, candidateVersion, routeProcess);
                    result.put(routeProcessKey, config);
                });
        return result;
    }

    private JSONObject resolveCandidateScheduleConfigMap(MesProRouteVersionDO candidateVersion) {
        if (StrUtil.isBlank(candidateVersion.getRouteSnapshotJson())) {
            return new JSONObject(true);
        }
        JSONObject snapshot = JSON.parseObject(candidateVersion.getRouteSnapshotJson());
        JSONObject configSnapshots = snapshot == null ? null : snapshot.getJSONObject(SNAPSHOT_CONFIGS_KEY);
        Object scheduleConfigs = configSnapshots == null ? null : configSnapshots.get(SCHEDULE_CONFIGS_KEY);
        JSONObject result = new JSONObject(true);
        if (scheduleConfigs == null) {
            return result;
        }
        if (scheduleConfigs instanceof JSONObject configsByRouteProcessId) {
            for (Map.Entry<String, Object> entry : configsByRouteProcessId.entrySet()) {
                JSONObject config = toScheduleConfigJson(entry.getValue());
                if (config.getLong("routeProcessId") == null) {
                    config.put("routeProcessId", Long.valueOf(entry.getKey()));
                }
                result.put(String.valueOf(config.getLong("routeProcessId")), config);
            }
            return result;
        }
        for (Object value : (JSONArray) scheduleConfigs) {
            JSONObject config = toScheduleConfigJson(value);
            Long routeProcessId = config.getLong("routeProcessId");
            if (routeProcessId != null) {
                result.put(String.valueOf(routeProcessId), config);
            }
        }
        return result;
    }

    private JSONObject toScheduleConfigJson(Object value) {
        if (value instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        return JSON.parseObject(JSON.toJSONString(value));
    }

    private JSONObject findScheduleConfigBySort(JSONObject configs, Integer sort) {
        if (sort == null) {
            return null;
        }
        for (Object value : configs.values()) {
            JSONObject config = toScheduleConfigJson(value);
            if (Objects.equals(sort, config.getInteger("sort"))) {
                return config;
            }
        }
        return null;
    }

    private JSONObject copyScheduleConfigSnapshot(JSONObject sourceConfig, MesProRouteVersionDO candidateVersion,
                                                  MesProRouteProcessDO routeProcess) {
        JSONObject config = new JSONObject(true);
        config.putAll(sourceConfig);
        config.put("routeVersionId", candidateVersion.getId());
        config.put("routeId", candidateVersion.getRouteId());
        config.put("routeProcessId", routeProcess.getId());
        config.put("sort", routeProcess.getSort());
        config.put("itemId", null);
        return config;
    }

    private JSONObject buildDefaultScheduleConfigSnapshot(MesProRouteVersionDO candidateVersion,
                                                          MesProRouteProcessDO routeProcess) {
        JSONObject config = new JSONObject(true);
        config.put("id", null);
        config.put("routeVersionId", candidateVersion.getId());
        config.put("routeId", candidateVersion.getRouteId());
        config.put("routeProcessId", routeProcess.getId());
        config.put("sort", routeProcess.getSort());
        config.put("capacityMode", MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode());
        config.put("hourlyCapacity", null);
        config.put("infiniteDurationQuantityFactor", null);
        config.put("infiniteDurationBaseMinutes", null);
        config.put("nightShiftEnabled", Boolean.FALSE);
        config.put("calendarRuleId", null);
        config.put("configVersion", null);
        config.put("remark", null);
        return config;
    }

    @Override
    public void validateRouteEnable(Long routeId) {
        MesProRouteProcessFlowGraphRespVO graph = getGraph(routeId);
        if (!STATUS_VALID.equals(graph.getValidationStatus()) && !STATUS_EMPTY_PROCESS.equals(graph.getValidationStatus())) {
            throw exception(PRO_ROUTE_PROCESS_FLOW_INVALID);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void copyGraph(Long sourceRouteId, Long targetRouteId, Map<Long, Long> routeProcessIdMap) {
        List<MesProRouteProcessFlowEdgeDO> sourceEdges = flowEdgeMapper.selectListByRouteId(sourceRouteId);
        List<MesProRouteProcessFlowBoundaryEdgeDO> sourceBoundaryEdges =
                boundaryEdgeMapper.selectListByRouteId(sourceRouteId);
        List<MesProRouteProcessFlowLayoutDO> sourceLayouts = flowLayoutMapper.selectListByRouteId(sourceRouteId);

        flowEdgeMapper.deleteByRouteId(targetRouteId);
        boundaryEdgeMapper.deleteByRouteId(targetRouteId);
        flowLayoutMapper.deleteByRouteId(targetRouteId);
        if (CollUtil.isEmpty(sourceEdges) && CollUtil.isEmpty(sourceBoundaryEdges)
                && CollUtil.isEmpty(sourceLayouts)) {
            return;
        }

        Long nextVersion = currentGraphVersion(targetRouteId) + 1;
        for (MesProRouteProcessFlowEdgeDO sourceEdge : sourceEdges) {
            Long targetSourceRouteProcessId = routeProcessIdMap.get(sourceEdge.getSourceRouteProcessId());
            Long targetTargetRouteProcessId = routeProcessIdMap.get(sourceEdge.getTargetRouteProcessId());
            if (targetSourceRouteProcessId == null || targetTargetRouteProcessId == null) {
                throw exception(PRO_ROUTE_PROCESS_FLOW_INVALID);
            }
            flowEdgeMapper.insert(MesProRouteProcessFlowEdgeDO.builder()
                    .routeId(targetRouteId)
                    .graphVersion(nextVersion)
                    .sourceRouteProcessId(targetSourceRouteProcessId)
                    .targetRouteProcessId(targetTargetRouteProcessId)
                    .relationType(normalRelationType(sourceEdge.getRelationType()))
                    .sort(sourceEdge.getSort())
                    .build());
        }
        for (MesProRouteProcessFlowBoundaryEdgeDO sourceBoundaryEdge : sourceBoundaryEdges) {
            Long targetRouteProcessId = routeProcessIdMap.get(sourceBoundaryEdge.getRouteProcessId());
            if (targetRouteProcessId == null) {
                throw exception(PRO_ROUTE_PROCESS_FLOW_INVALID);
            }
            boundaryEdgeMapper.insert(MesProRouteProcessFlowBoundaryEdgeDO.builder()
                    .routeId(targetRouteId)
                    .graphVersion(nextVersion)
                    .boundaryType(sourceBoundaryEdge.getBoundaryType())
                    .routeProcessId(targetRouteProcessId)
                    .sort(sourceBoundaryEdge.getSort())
                    .build());
        }
        for (MesProRouteProcessFlowLayoutDO sourceLayout : sourceLayouts) {
            Long targetRouteProcessId = routeProcessIdMap.get(sourceLayout.getRouteProcessId());
            if (targetRouteProcessId == null) {
                throw exception(PRO_ROUTE_PROCESS_FLOW_INVALID);
            }
            flowLayoutMapper.insert(MesProRouteProcessFlowLayoutDO.builder()
                    .routeId(targetRouteId)
                    .routeProcessId(targetRouteProcessId)
                    .x(sourceLayout.getX())
                    .y(sourceLayout.getY())
                    .width(sourceLayout.getWidth())
                    .height(sourceLayout.getHeight())
                    .graphVersion(nextVersion)
                    .build());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByRouteId(Long routeId) {
        flowEdgeMapper.deleteByRouteId(routeId);
        boundaryEdgeMapper.deleteByRouteId(routeId);
        flowLayoutMapper.deleteByRouteId(routeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByRouteProcessId(Long routeId, Long routeProcessId) {
        flowEdgeMapper.deleteByRouteProcessId(routeId, routeProcessId);
        boundaryEdgeMapper.deleteByRouteProcessId(routeId, routeProcessId);
        flowLayoutMapper.deleteByRouteProcessId(routeId, routeProcessId);
    }

    private DraftRouteProcessValidation validateRouteProcessDraftChanges(MesProRouteProcessFlowSaveReqVO reqVO) {
        MesProRouteVersionDO candidateVersion = resolveDraftCandidateVersion(reqVO.getRouteVersionId(), reqVO.getRouteId());
        if (candidateVersion != null) {
            return validateRouteProcessDraftChanges(reqVO, parseCandidateRouteProcesses(candidateVersion,
                    resolveCandidateFlowGraphSnapshot(candidateVersion)));
        }
        return validateRouteProcessDraftChanges(reqVO, routeProcessMapper.selectListByRouteId(reqVO.getRouteId()));
    }

    private DraftRouteProcessValidation validateRouteProcessDraftChanges(
            MesProRouteProcessFlowSaveReqVO reqVO,
            List<MesProRouteProcessDO> currentRouteProcesses) {
        List<MesProRouteProcessFlowValidationMessageRespVO> messages = new ArrayList<>();
        Map<Long, MesProRouteProcessDO> currentRouteProcessMap = safeList(currentRouteProcesses).stream()
                .collect(Collectors.toMap(MesProRouteProcessDO::getId, routeProcess -> routeProcess,
                        (left, right) -> left, LinkedHashMap::new));
        Set<Long> deleteIds = new LinkedHashSet<>(safeList(reqVO.getRouteProcessDeletes()));
        for (Long routeProcessId : deleteIds) {
            if (!currentRouteProcessMap.containsKey(routeProcessId)) {
                messages.add(message("ERROR", "DRAFT_DELETE_NOT_FOUND", "删除草稿包含非当前路线工序", List.of(routeProcessId)));
            }
        }

        Map<Long, MesProRouteProcessDO> draftRouteProcessMap = currentRouteProcessMap.values().stream()
                .filter(routeProcess -> !deleteIds.contains(routeProcess.getId()))
                .collect(Collectors.toMap(MesProRouteProcessDO::getId, routeProcess -> routeProcess,
                        (left, right) -> left, LinkedHashMap::new));

        for (MesProRouteProcessSaveReqVO updateReqVO : safeList(reqVO.getRouteProcessUpdates())) {
            Long routeProcessId = updateReqVO.getId();
            List<Long> impacted = routeProcessId == null ? List.of() : List.of(routeProcessId);
            MesProRouteProcessDO routeProcess = routeProcessId == null ? null : draftRouteProcessMap.get(routeProcessId);
            if (routeProcess == null) {
                messages.add(message("ERROR", "DRAFT_UPDATE_NOT_FOUND", "更新草稿包含非当前路线工序", impacted));
                continue;
            }
            if (updateReqVO.getRouteId() != null && !Objects.equals(updateReqVO.getRouteId(), reqVO.getRouteId())) {
                messages.add(message("ERROR", "DRAFT_UPDATE_ROUTE_INVALID", "更新草稿包含非当前路线工序", impacted));
                continue;
            }
            draftRouteProcessMap.put(routeProcessId, mergeRouteProcessDraftUpdate(routeProcess, updateReqVO));
        }

        List<MesProRouteProcessDO> draftRouteProcesses = new ArrayList<>(draftRouteProcessMap.values());
        List<Long> keyRouteProcessIds = draftRouteProcesses.stream()
                .filter(routeProcess -> Boolean.TRUE.equals(routeProcess.getKeyFlag()))
                .map(MesProRouteProcessDO::getId)
                .toList();
        if (keyRouteProcessIds.size() > 1) {
            messages.add(message("ERROR", "DRAFT_UPDATE_KEY_DUPLICATE", "关键工序只能存在一个", keyRouteProcessIds));
        }
        Set<Long> routeProcessIds = draftRouteProcesses.stream().map(MesProRouteProcessDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Integer> sorts = draftRouteProcesses.stream().map(MesProRouteProcessDO::getSort)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> processIds = draftRouteProcesses.stream().map(MesProRouteProcessDO::getProcessId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        boolean keyProcessExists = draftRouteProcesses.stream()
                .anyMatch(routeProcess -> Boolean.TRUE.equals(routeProcess.getKeyFlag()));

        for (MesProRouteProcessSaveReqVO createReqVO : safeList(reqVO.getRouteProcessCreates())) {
            Long clientRouteProcessId = createReqVO.getClientRouteProcessId();
            List<Long> impacted = clientRouteProcessId == null ? List.of() : List.of(clientRouteProcessId);
            if (clientRouteProcessId == null || clientRouteProcessId >= 0 || !routeProcessIds.add(clientRouteProcessId)) {
                messages.add(message("ERROR", "DRAFT_CREATE_ID_INVALID", "新增草稿工序编号无效", impacted));
                continue;
            }
            if (!Objects.equals(createReqVO.getRouteId(), reqVO.getRouteId())) {
                messages.add(message("ERROR", "DRAFT_CREATE_ROUTE_INVALID", "新增草稿包含非当前路线工序", impacted));
                continue;
            }
            if (processService.getProcess(createReqVO.getProcessId()) == null) {
                messages.add(message("ERROR", "DRAFT_CREATE_PROCESS_NOT_EXISTS", "新增草稿工序不存在", impacted));
                continue;
            }
            if (!sorts.add(createReqVO.getSort())) {
                messages.add(message("ERROR", "DRAFT_CREATE_SORT_DUPLICATE", "新增草稿工序序号重复", impacted));
                continue;
            }
            if (!processIds.add(createReqVO.getProcessId())) {
                messages.add(message("ERROR", "DRAFT_CREATE_PROCESS_DUPLICATE", "新增草稿工序重复", impacted));
                continue;
            }
            if (Boolean.TRUE.equals(createReqVO.getKeyFlag())) {
                if (keyProcessExists) {
                    messages.add(message("ERROR", "DRAFT_CREATE_KEY_DUPLICATE", "关键工序只能存在一个", impacted));
                    continue;
                }
                keyProcessExists = true;
            }
            draftRouteProcesses.add(MesProRouteProcessDO.builder()
                    .id(clientRouteProcessId)
                    .routeId(reqVO.getRouteId())
                    .processId(createReqVO.getProcessId())
                    .sort(createReqVO.getSort())
                    .keyFlag(createReqVO.getKeyFlag())
                    .checkFlag(createReqVO.getCheckFlag())
                    .build());
        }
        draftRouteProcesses.sort(Comparator.comparing(
                MesProRouteProcessDO::getSort, Comparator.nullsLast(Integer::compareTo)));
        return new DraftRouteProcessValidation(draftRouteProcesses, messages);
    }

    private MesProRouteProcessDO mergeRouteProcessDraftUpdate(MesProRouteProcessDO routeProcess,
                                                              MesProRouteProcessSaveReqVO updateReqVO) {
        return MesProRouteProcessDO.builder()
                .id(routeProcess.getId())
                .routeId(routeProcess.getRouteId())
                .processId(routeProcess.getProcessId())
                .workstationId(updateReqVO.getWorkstationId() == null
                        ? routeProcess.getWorkstationId() : updateReqVO.getWorkstationId())
                .sort(routeProcess.getSort())
                .nextProcessId(routeProcess.getNextProcessId())
                .linkType(routeProcess.getLinkType())
                .prepareTime(routeProcess.getPrepareTime())
                .waitTime(routeProcess.getWaitTime())
                .colorCode(routeProcess.getColorCode())
                .keyFlag(updateReqVO.getKeyFlag() == null ? routeProcess.getKeyFlag() : updateReqVO.getKeyFlag())
                .checkFlag(updateReqVO.getCheckFlag() == null ? routeProcess.getCheckFlag() : updateReqVO.getCheckFlag())
                .batchRecordReportId(routeProcess.getBatchRecordReportId())
                .remark(routeProcess.getRemark())
                .build();
    }

    private Map<Long, Long> persistRouteProcessDraftChanges(MesProRouteProcessFlowSaveReqVO reqVO) {
        Map<Long, Long> persistedRouteProcessIdMap = new HashMap<>();
        for (Long routeProcessId : safeList(reqVO.getRouteProcessDeletes())) {
            MesProRouteProcessDO routeProcess = routeProcessMapper.selectById(routeProcessId);
            if (routeProcess == null || !Objects.equals(routeProcess.getRouteId(), reqVO.getRouteId())) {
                throw exception(PRO_ROUTE_PROCESS_FLOW_INVALID);
            }
            routeProcessService.deleteRouteProcess(routeProcessId);
        }
        for (MesProRouteProcessSaveReqVO updateReqVO : orderRouteProcessUpdates(safeList(reqVO.getRouteProcessUpdates()))) {
            MesProRouteProcessDO routeProcess = routeProcessMapper.selectById(updateReqVO.getId());
            if (routeProcess == null || !Objects.equals(routeProcess.getRouteId(), reqVO.getRouteId())) {
                throw exception(PRO_ROUTE_PROCESS_FLOW_INVALID);
            }
            routeProcessService.updateRouteProcess(buildRouteProcessUpdateReq(routeProcess, updateReqVO));
        }
        for (MesProRouteProcessSaveReqVO createReqVO : safeList(reqVO.getRouteProcessCreates())) {
            MesProRouteProcessSaveReqVO persistedReqVO = new MesProRouteProcessSaveReqVO();
            persistedReqVO.setRouteId(reqVO.getRouteId());
            persistedReqVO.setProcessId(createReqVO.getProcessId());
            persistedReqVO.setSort(createReqVO.getSort());
            persistedReqVO.setLinkType(createReqVO.getLinkType());
            persistedReqVO.setPrepareTime(createReqVO.getPrepareTime());
            persistedReqVO.setWaitTime(createReqVO.getWaitTime());
            persistedReqVO.setColorCode(createReqVO.getColorCode());
            persistedReqVO.setKeyFlag(createReqVO.getKeyFlag());
            persistedReqVO.setCheckFlag(createReqVO.getCheckFlag());
            persistedReqVO.setRemark(createReqVO.getRemark());
            Long routeProcessId = routeProcessService.createRouteProcess(persistedReqVO);
            persistedRouteProcessIdMap.put(createReqVO.getClientRouteProcessId(), routeProcessId);
        }
        return persistedRouteProcessIdMap;
    }

    private List<MesProRouteProcessSaveReqVO> orderRouteProcessUpdates(List<MesProRouteProcessSaveReqVO> updateReqVOs) {
        return updateReqVOs.stream()
                .sorted(Comparator.comparing(updateReqVO -> Boolean.TRUE.equals(updateReqVO.getKeyFlag()) ? 1 : 0))
                .toList();
    }

    private MesProRouteProcessSaveReqVO buildRouteProcessUpdateReq(MesProRouteProcessDO routeProcess,
                                                                   MesProRouteProcessSaveReqVO updateReqVO) {
        MesProRouteProcessSaveReqVO persistedReqVO = new MesProRouteProcessSaveReqVO();
        persistedReqVO.setId(routeProcess.getId());
        persistedReqVO.setRouteId(routeProcess.getRouteId());
        persistedReqVO.setProcessId(routeProcess.getProcessId());
        persistedReqVO.setWorkstationId(updateReqVO.getWorkstationId() == null
                ? routeProcess.getWorkstationId() : updateReqVO.getWorkstationId());
        persistedReqVO.setSort(routeProcess.getSort());
        persistedReqVO.setLinkType(routeProcess.getLinkType());
        persistedReqVO.setPrepareTime(routeProcess.getPrepareTime());
        persistedReqVO.setWaitTime(routeProcess.getWaitTime());
        persistedReqVO.setColorCode(routeProcess.getColorCode());
        persistedReqVO.setKeyFlag(updateReqVO.getKeyFlag() == null ? routeProcess.getKeyFlag() : updateReqVO.getKeyFlag());
        persistedReqVO.setCheckFlag(updateReqVO.getCheckFlag() == null ? routeProcess.getCheckFlag() : updateReqVO.getCheckFlag());
        persistedReqVO.setRemark(routeProcess.getRemark());
        return persistedReqVO;
    }

    private MesProRouteProcessFlowSaveReqVO remapRouteProcessDraftIds(MesProRouteProcessFlowSaveReqVO reqVO,
                                                                      Map<Long, Long> persistedRouteProcessIdMap) {
        MesProRouteProcessFlowSaveReqVO persistedReqVO = new MesProRouteProcessFlowSaveReqVO();
        persistedReqVO.setRouteId(reqVO.getRouteId());
        persistedReqVO.setGraphVersion(reqVO.getGraphVersion());
        persistedReqVO.setEdges(safeList(reqVO.getEdges()).stream().map(edge -> {
            MesProRouteProcessFlowEdgeReqVO mappedEdge = new MesProRouteProcessFlowEdgeReqVO();
            mappedEdge.setSourceRouteProcessId(remapRouteProcessId(edge.getSourceRouteProcessId(), persistedRouteProcessIdMap));
            mappedEdge.setTargetRouteProcessId(remapRouteProcessId(edge.getTargetRouteProcessId(), persistedRouteProcessIdMap));
            mappedEdge.setRelationType(edge.getRelationType());
            return mappedEdge;
        }).toList());
        persistedReqVO.setBoundaryEdges(safeList(reqVO.getBoundaryEdges()).stream().map(boundaryEdge -> {
            MesProRouteProcessFlowBoundaryEdgeReqVO mappedBoundaryEdge =
                    new MesProRouteProcessFlowBoundaryEdgeReqVO();
            mappedBoundaryEdge.setBoundaryType(boundaryEdge.getBoundaryType());
            mappedBoundaryEdge.setRouteProcessId(
                    remapRouteProcessId(boundaryEdge.getRouteProcessId(), persistedRouteProcessIdMap));
            mappedBoundaryEdge.setSort(boundaryEdge.getSort());
            return mappedBoundaryEdge;
        }).toList());
        persistedReqVO.setLayouts(safeList(reqVO.getLayouts()).stream().map(layout -> {
            MesProRouteProcessFlowLayoutReqVO mappedLayout = new MesProRouteProcessFlowLayoutReqVO();
            mappedLayout.setRouteProcessId(remapRouteProcessId(layout.getRouteProcessId(), persistedRouteProcessIdMap));
            mappedLayout.setX(layout.getX());
            mappedLayout.setY(layout.getY());
            mappedLayout.setWidth(layout.getWidth());
            mappedLayout.setHeight(layout.getHeight());
            return mappedLayout;
        }).toList());
        persistedReqVO.setRouteProcessUpdates(safeList(reqVO.getRouteProcessUpdates()));
        return persistedReqVO;
    }

    private Long remapRouteProcessId(Long routeProcessId, Map<Long, Long> persistedRouteProcessIdMap) {
        if (routeProcessId == null || routeProcessId >= 0) {
            return routeProcessId;
        }
        Long persistedRouteProcessId = persistedRouteProcessIdMap.get(routeProcessId);
        if (persistedRouteProcessId == null) {
            throw exception(PRO_ROUTE_PROCESS_FLOW_INVALID);
        }
        return persistedRouteProcessId;
    }

    private MesProRouteDO validateRouteExists(Long routeId) {
        MesProRouteDO route = routeMapper.selectById(routeId);
        if (route == null) {
            throw exception(PRO_ROUTE_NOT_EXISTS);
        }
        return route;
    }

    private Long currentGraphVersion(Long routeId) {
        Long layoutVersion = flowLayoutMapper.selectMaxGraphVersionByRouteId(routeId);
        Long edgeVersion = flowEdgeMapper.selectMaxGraphVersionByRouteId(routeId);
        long normalizedLayoutVersion = layoutVersion == null ? 0L : layoutVersion;
        long normalizedEdgeVersion = edgeVersion == null ? 0L : edgeVersion;
        return Math.max(normalizedLayoutVersion, normalizedEdgeVersion);
    }

    private List<MesProRouteProcessFlowValidationMessageRespVO> validateEdges(
            List<MesProRouteProcessDO> routeProcesses,
            List<MesProRouteProcessFlowEdgeReqVO> edges,
            List<MesProRouteProcessFlowBoundaryEdgeReqVO> boundaryEdges) {
        List<MesProRouteProcessFlowValidationMessageRespVO> messages = new ArrayList<>();
        Set<Long> routeProcessIds = routeProcessIds(routeProcesses);
        Map<Long, Set<Long>> outgoing = new LinkedHashMap<>();
        Map<Long, Set<Long>> incoming = new LinkedHashMap<>();
        routeProcessIds.forEach(id -> {
            outgoing.put(id, new LinkedHashSet<>());
            incoming.put(id, new LinkedHashSet<>());
        });

        Set<String> seenEdges = new HashSet<>();
        for (MesProRouteProcessFlowEdgeReqVO edge : edges) {
            Long source = edge.getSourceRouteProcessId();
            Long target = edge.getTargetRouteProcessId();
            if (ObjUtil.equal(source, target)) {
                messages.add(message("ERROR", "SELF_LOOP", "工序不能连接自身", List.of(source)));
                continue;
            }
            if (!routeProcessIds.contains(source) || !routeProcessIds.contains(target)) {
                messages.add(message("ERROR", "CROSS_ROUTE_EDGE", "流转关系包含非当前路线工序", List.of(source, target)));
                continue;
            }
            String edgeKey = source + "->" + target;
            if (!seenEdges.add(edgeKey)) {
                messages.add(message("ERROR", "DUPLICATE_EDGE", "重复的工序流转关系", List.of(source, target)));
                continue;
            }
            outgoing.get(source).add(target);
            incoming.get(target).add(source);
        }

        Set<Long> expectedStartTargets = routeProcessIds.stream()
                .filter(id -> incoming.get(id).isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> expectedEndSources = routeProcessIds.stream()
                .filter(id -> outgoing.get(id).isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> startTargets = new LinkedHashSet<>();
        Set<Long> endSources = new LinkedHashSet<>();
        Set<String> seenBoundaryEdges = new HashSet<>();
        for (MesProRouteProcessFlowBoundaryEdgeReqVO boundaryEdge : boundaryEdges) {
            String boundaryType = boundaryEdge.getBoundaryType();
            Long routeProcessId = boundaryEdge.getRouteProcessId();
            List<Long> impacted = routeProcessId == null ? List.of() : List.of(routeProcessId);
            if (!BOUNDARY_TYPE_START.equals(boundaryType) && !BOUNDARY_TYPE_END.equals(boundaryType)) {
                messages.add(message("ERROR", "BOUNDARY_TYPE_INVALID", "边界类型必须为 START 或 END", impacted));
                continue;
            }
            if (!routeProcessIds.contains(routeProcessId)) {
                messages.add(message("ERROR", "BOUNDARY_ROUTE_PROCESS_INVALID",
                        "边界关系包含非当前路线工序", impacted));
                continue;
            }
            String boundaryKey = boundaryType + "->" + routeProcessId;
            if (!seenBoundaryEdges.add(boundaryKey)) {
                messages.add(message("ERROR", "DUPLICATE_BOUNDARY_EDGE", "重复的边界关系", impacted));
                continue;
            }
            if (BOUNDARY_TYPE_START.equals(boundaryType)) {
                startTargets.add(routeProcessId);
            } else {
                endSources.add(routeProcessId);
            }
        }

        List<Long> isolated = routeProcessIds.stream()
                .filter(id -> incoming.get(id).isEmpty() && outgoing.get(id).isEmpty())
                .toList();
        if (routeProcessIds.size() > 1 && !isolated.isEmpty()) {
            messages.add(message("ERROR", "ISOLATED_NODE", "存在未连接工序", isolated));
        }
        if (startTargets.isEmpty()) {
            messages.add(message("ERROR", "START_BOUNDARY_MISSING", "工序开始至少需要连接一个首工序",
                    expectedStartTargets.stream().toList()));
        }
        if (!startTargets.equals(expectedStartTargets)) {
            Set<Long> impacted = new LinkedHashSet<>(expectedStartTargets);
            impacted.addAll(startTargets);
            messages.add(message("ERROR", "START_BOUNDARY_MISMATCH",
                    "工序开始必须连接全部且仅连接无普通前置工序", impacted.stream().toList()));
        }
        if (endSources.isEmpty()) {
            messages.add(message("ERROR", "END_BOUNDARY_MISSING", "工序结束至少需要连接一个末工序",
                    expectedEndSources.stream().toList()));
        }
        if (!endSources.equals(expectedEndSources)) {
            Set<Long> impacted = new LinkedHashSet<>(expectedEndSources);
            impacted.addAll(endSources);
            messages.add(message("ERROR", "END_BOUNDARY_MISMATCH",
                    "工序结束必须连接全部且仅连接无普通后续工序", impacted.stream().toList()));
        }

        List<Long> cycle = findCycle(routeProcessIds, outgoing);
        if (!cycle.isEmpty()) {
            messages.add(message("ERROR", "CYCLE_DETECTED", "工序流转关系存在循环", cycle));
        }
        if (!startTargets.isEmpty()) {
            Set<Long> reachableFromStart = new LinkedHashSet<>();
            startTargets.forEach(start -> reachableFromStart.addAll(reachable(start, outgoing)));
            List<Long> unreachable = routeProcessIds.stream().filter(id -> !reachableFromStart.contains(id)).toList();
            if (!unreachable.isEmpty()) {
                messages.add(message("ERROR", "UNREACHABLE_FROM_START",
                        "存在无法从工序开始到达的工序", unreachable));
            }
        }
        if (!endSources.isEmpty()) {
            Map<Long, Set<Long>> reversed = reverse(outgoing);
            Set<Long> canReachEnd = new LinkedHashSet<>();
            endSources.forEach(end -> canReachEnd.addAll(reachable(end, reversed)));
            List<Long> cannotReachEnd = routeProcessIds.stream().filter(id -> !canReachEnd.contains(id)).toList();
            if (!cannotReachEnd.isEmpty()) {
                messages.add(message("ERROR", "UNREACHABLE_TO_END",
                "存在无法到达工序结束的工序", cannotReachEnd));
            }
        }
        return deduplicateMessages(messages);
    }

    private List<MesProRouteProcessFlowValidationMessageRespVO> deduplicateMessages(
            List<MesProRouteProcessFlowValidationMessageRespVO> messages) {
        Map<String, MesProRouteProcessFlowValidationMessageRespVO> deduplicated = new LinkedHashMap<>();
        for (MesProRouteProcessFlowValidationMessageRespVO message : messages) {
            String key = message.getCode() + ":" + message.getRouteProcessIds();
            deduplicated.putIfAbsent(key, message);
        }
        return new ArrayList<>(deduplicated.values());
    }

    private List<Long> findCycle(Set<Long> nodeIds, Map<Long, Set<Long>> outgoing) {
        Set<Long> visiting = new HashSet<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<Long> stack = new ArrayDeque<>();
        for (Long nodeId : nodeIds) {
            List<Long> cycle = dfsCycle(nodeId, outgoing, visiting, visited, stack);
            if (!cycle.isEmpty()) {
                return cycle;
            }
        }
        return List.of();
    }

    private List<Long> dfsCycle(Long nodeId, Map<Long, Set<Long>> outgoing, Set<Long> visiting,
                                Set<Long> visited, ArrayDeque<Long> stack) {
        if (visited.contains(nodeId)) {
            return List.of();
        }
        if (visiting.contains(nodeId)) {
            List<Long> path = new ArrayList<>(stack);
            int startIndex = path.indexOf(nodeId);
            if (startIndex >= 0) {
                return path.subList(startIndex, path.size());
            }
            return List.of(nodeId);
        }
        visiting.add(nodeId);
        stack.addLast(nodeId);
        for (Long next : outgoing.getOrDefault(nodeId, Set.of())) {
            List<Long> cycle = dfsCycle(next, outgoing, visiting, visited, stack);
            if (!cycle.isEmpty()) {
                return cycle;
            }
        }
        stack.removeLast();
        visiting.remove(nodeId);
        visited.add(nodeId);
        return List.of();
    }

    private Set<Long> reachable(Long start, Map<Long, Set<Long>> graph) {
        Set<Long> visited = new LinkedHashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            for (Long next : graph.getOrDefault(current, Set.of())) {
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }
        return visited;
    }

    private Map<Long, Set<Long>> reverse(Map<Long, Set<Long>> graph) {
        Map<Long, Set<Long>> reversed = new LinkedHashMap<>();
        graph.keySet().forEach(id -> reversed.put(id, new LinkedHashSet<>()));
        graph.forEach((source, targets) -> targets.forEach(target ->
                reversed.computeIfAbsent(target, ignored -> new LinkedHashSet<>()).add(source)));
        return reversed;
    }

    private MesProRouteProcessFlowValidationMessageRespVO message(String level, String code, String message,
                                                                  List<Long> routeProcessIds) {
        MesProRouteProcessFlowValidationMessageRespVO respVO = new MesProRouteProcessFlowValidationMessageRespVO();
        respVO.setLevel(level);
        respVO.setCode(code);
        respVO.setMessage(message);
        respVO.setRouteProcessIds(routeProcessIds.stream().filter(Objects::nonNull).distinct().toList());
        return respVO;
    }

    private <T extends MesProRouteProcessFlowValidationRespVO> T mark(
            T respVO, boolean valid, String status, Long graphVersion,
            List<MesProRouteProcessFlowValidationMessageRespVO> messages) {
        respVO.setValid(valid);
        respVO.setValidationStatus(status);
        respVO.setGraphVersion(graphVersion);
        respVO.setValidationMessages(new ArrayList<>(messages));
        respVO.setInvalidRouteProcessIds(messages.stream()
                .flatMap(message -> message.getRouteProcessIds().stream())
                .distinct()
                .toList());
        return respVO;
    }

    private void copyValidation(MesProRouteProcessFlowGraphRespVO graph,
                                MesProRouteProcessFlowValidationRespVO validation) {
        graph.setValid(validation.getValid());
        graph.setValidationStatus(validation.getValidationStatus());
        graph.setGraphVersion(validation.getGraphVersion());
        graph.setValidationMessages(validation.getValidationMessages());
        graph.setCyclePaths(validation.getCyclePaths());
        graph.setInvalidRouteProcessIds(validation.getInvalidRouteProcessIds());
        graph.setInvalidEdgeIds(validation.getInvalidEdgeIds());
    }

    private List<MesProRouteProcessFlowNodeRespVO> buildNodes(
            List<MesProRouteProcessDO> routeProcesses, List<MesProRouteProcessFlowLayoutDO> layouts) {
        Map<Long, MesProRouteProcessFlowLayoutDO> layoutMap = layouts.stream()
                .collect(Collectors.toMap(MesProRouteProcessFlowLayoutDO::getRouteProcessId, item -> item,
                        (left, right) -> right));
        List<Long> processIds = routeProcesses.stream()
                .map(MesProRouteProcessDO::getProcessId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MesProProcessDO> processMap = processService.getProcessList(processIds).stream()
                .collect(Collectors.toMap(MesProProcessDO::getId, item -> item, (left, right) -> right));
        List<MesMdWorkstationDO> processWorkstations = workstationService.getWorkstationListByProcessIds(processIds);
        if (processWorkstations == null) {
            processWorkstations = List.of();
        }
        Set<Long> explicitWorkstationIds = routeProcesses.stream()
                .map(MesProRouteProcessDO::getWorkstationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isNotEmpty(explicitWorkstationIds)) {
            Map<Long, MesMdWorkstationDO> loadedWorkstationMap = processWorkstations.stream()
                    .filter(workstation -> workstation.getId() != null)
                    .collect(Collectors.toMap(MesMdWorkstationDO::getId, item -> item, (left, right) -> right));
            List<Long> missingWorkstationIds = explicitWorkstationIds.stream()
                    .filter(id -> !loadedWorkstationMap.containsKey(id))
                    .toList();
            if (CollUtil.isNotEmpty(missingWorkstationIds)) {
                List<MesMdWorkstationDO> explicitWorkstations = workstationService.getWorkstationList(missingWorkstationIds);
                if (CollUtil.isNotEmpty(explicitWorkstations)) {
                    processWorkstations = new ArrayList<>(processWorkstations);
                    processWorkstations.addAll(explicitWorkstations);
                }
            }
        }
        normalizeWorkstationProcessIds(processWorkstations, processIds);
        Map<Long, MesMdWorkstationDO> workstationById = processWorkstations.stream()
                .filter(workstation -> workstation.getId() != null)
                .collect(Collectors.toMap(MesMdWorkstationDO::getId, item -> item, (left, right) -> right));
        Map<Long, List<MesMdWorkstationDO>> workstationListMap = processWorkstations.stream()
                .filter(workstation -> workstation.getProcessId() != null)
                .collect(Collectors.groupingBy(
                        MesMdWorkstationDO::getProcessId, LinkedHashMap::new, Collectors.toList()));
        return routeProcesses.stream()
                .sorted(Comparator.comparing(MesProRouteProcessDO::getSort, Comparator.nullsLast(Integer::compareTo)))
                .map(routeProcess -> {
                    MesProProcessDO process = processMap.get(routeProcess.getProcessId());
                    if (process == null) {
                        throw exception(PRO_PROCESS_NOT_EXISTS);
                    }
                    MesProRouteProcessFlowNodeRespVO node = new MesProRouteProcessFlowNodeRespVO();
                    node.setRouteProcessId(routeProcess.getId());
                    node.setProcessId(routeProcess.getProcessId());
                    node.setProcessCode(process.getCode());
                    node.setProcessName(process.getName());
                    node.setRouteProcessWorkstationId(routeProcess.getWorkstationId());
                    applyWorkstation(node, routeProcess,
                            workstationListMap.get(routeProcess.getProcessId()), workstationById);
                    node.setSort(routeProcess.getSort());
                    node.setKeyFlag(routeProcess.getKeyFlag());
                    node.setCheckFlag(routeProcess.getCheckFlag());
                    MesProRouteProcessFlowLayoutDO layout = layoutMap.get(routeProcess.getId());
                    if (layout != null) {
                        node.setX(layout.getX());
                        node.setY(layout.getY());
                    }
                    return node;
                }).toList();
    }

    private void normalizeWorkstationProcessIds(List<MesMdWorkstationDO> workstations, List<Long> targetProcessIds) {
        if (CollUtil.isEmpty(workstations) || CollUtil.isEmpty(targetProcessIds)) {
            return;
        }
        Map<Long, Long> processIdentityMap = routeProcessService.getProcessIdentityMap(targetProcessIds);
        if (CollUtil.isEmpty(processIdentityMap)) {
            return;
        }
        workstations.forEach(workstation -> {
            Long normalizedProcessId = processIdentityMap.get(workstation.getProcessId());
            if (normalizedProcessId != null) {
                workstation.setProcessId(normalizedProcessId);
            }
        });
    }

    private void applyWorkstation(MesProRouteProcessFlowNodeRespVO node,
                                  MesProRouteProcessDO routeProcess,
                                  List<MesMdWorkstationDO> processWorkstations,
                                  Map<Long, MesMdWorkstationDO> workstationById) {
        MesMdWorkstationDO workstation = resolvePrimaryWorkstation(routeProcess, processWorkstations, workstationById);
        if (workstation == null) {
            return;
        }
        node.setWorkstationId(workstation.getId());
        node.setWorkstationCode(workstation.getCode());
        node.setWorkstationName(workstation.getName());
    }

    private MesMdWorkstationDO resolvePrimaryWorkstation(MesProRouteProcessDO routeProcess,
                                                         List<MesMdWorkstationDO> processWorkstations,
                                                         Map<Long, MesMdWorkstationDO> workstationById) {
        if (routeProcess != null && routeProcess.getWorkstationId() != null) {
            if (CollUtil.isNotEmpty(processWorkstations)) {
                for (MesMdWorkstationDO workstation : processWorkstations) {
                    if (Objects.equals(workstation.getId(), routeProcess.getWorkstationId())) {
                        return workstation;
                    }
                }
            }
            return workstationById.get(routeProcess.getWorkstationId());
        }
        if (CollUtil.isEmpty(processWorkstations)) {
            return null;
        }
        return processWorkstations.stream()
                .min(Comparator.comparing(MesMdWorkstationDO::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
    }

    private MesProRouteProcessFlowEdgeRespVO toEdgeRespVO(MesProRouteProcessFlowEdgeDO edge) {
        MesProRouteProcessFlowEdgeRespVO respVO = new MesProRouteProcessFlowEdgeRespVO();
        respVO.setId(edge.getId());
        respVO.setSourceRouteProcessId(edge.getSourceRouteProcessId());
        respVO.setTargetRouteProcessId(edge.getTargetRouteProcessId());
        respVO.setRelationType(edge.getRelationType());
        return respVO;
    }

    private MesProRouteProcessFlowEdgeRespVO toEdgeRespVO(MesProRouteProcessFlowEdgeReqVO edge) {
        MesProRouteProcessFlowEdgeRespVO respVO = new MesProRouteProcessFlowEdgeRespVO();
        respVO.setSourceRouteProcessId(edge.getSourceRouteProcessId());
        respVO.setTargetRouteProcessId(edge.getTargetRouteProcessId());
        respVO.setRelationType(edge.getRelationType());
        return respVO;
    }

    private MesProRouteProcessFlowEdgeReqVO toEdgeReqVO(MesProRouteProcessFlowEdgeDO edge) {
        MesProRouteProcessFlowEdgeReqVO reqVO = new MesProRouteProcessFlowEdgeReqVO();
        reqVO.setSourceRouteProcessId(edge.getSourceRouteProcessId());
        reqVO.setTargetRouteProcessId(edge.getTargetRouteProcessId());
        reqVO.setRelationType(edge.getRelationType());
        return reqVO;
    }

    private MesProRouteProcessFlowBoundaryEdgeRespVO toBoundaryEdgeRespVO(
            MesProRouteProcessFlowBoundaryEdgeDO edge) {
        MesProRouteProcessFlowBoundaryEdgeRespVO respVO = new MesProRouteProcessFlowBoundaryEdgeRespVO();
        respVO.setBoundaryType(edge.getBoundaryType());
        respVO.setRouteProcessId(edge.getRouteProcessId());
        respVO.setSort(edge.getSort());
        return respVO;
    }

    private MesProRouteProcessFlowBoundaryEdgeRespVO toBoundaryEdgeRespVO(
            MesProRouteProcessFlowBoundaryEdgeReqVO edge) {
        MesProRouteProcessFlowBoundaryEdgeRespVO respVO = new MesProRouteProcessFlowBoundaryEdgeRespVO();
        respVO.setBoundaryType(edge.getBoundaryType());
        respVO.setRouteProcessId(edge.getRouteProcessId());
        respVO.setSort(edge.getSort());
        return respVO;
    }

    private MesProRouteProcessFlowBoundaryEdgeReqVO toBoundaryEdgeReqVO(
            MesProRouteProcessFlowBoundaryEdgeDO edge) {
        MesProRouteProcessFlowBoundaryEdgeReqVO reqVO = new MesProRouteProcessFlowBoundaryEdgeReqVO();
        reqVO.setBoundaryType(edge.getBoundaryType());
        reqVO.setRouteProcessId(edge.getRouteProcessId());
        reqVO.setSort(edge.getSort());
        return reqVO;
    }

    private MesProRouteProcessFlowLayoutReqVO toLayoutReqVO(MesProRouteProcessFlowLayoutDO layout) {
        MesProRouteProcessFlowLayoutReqVO reqVO = new MesProRouteProcessFlowLayoutReqVO();
        reqVO.setRouteProcessId(layout.getRouteProcessId());
        reqVO.setX(layout.getX());
        reqVO.setY(layout.getY());
        reqVO.setWidth(layout.getWidth());
        reqVO.setHeight(layout.getHeight());
        return reqVO;
    }

    private Set<Long> routeProcessIds(List<MesProRouteProcessDO> routeProcesses) {
        return routeProcesses.stream().map(MesProRouteProcessDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalRelationType(String relationType) {
        return relationType == null || relationType.isBlank() ? RELATION_TYPE_NORMAL : relationType;
    }

    private static <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private record DraftRouteProcessValidation(List<MesProRouteProcessDO> routeProcesses,
                                               List<MesProRouteProcessFlowValidationMessageRespVO> messages) {
    }

}
